package dev.x341.mrw.mod.mixin;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.WallSide;
import dev.x341.mrw.mod.item.ItemBridgeWallCreator;
import dev.x341.mrw.mod.packet.PacketApplyRailAction;
import org.mtr.core.data.Position;
import org.mtr.core.data.TransportMode;
import org.mtr.core.tool.Angle;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.Hand;
import org.mtr.mapping.holder.HitResultType;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.item.ItemBlockClickingBase;
import org.mtr.mod.item.ItemNodeModifierBase;
import org.mtr.mod.item.ItemNodeModifierSelectableBlockBase;
import org.mtr.mod.item.ItemTunnelWallCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;


@Mixin(value = ItemNodeModifierSelectableBlockBase.class, remap = false)
public abstract class ItemNodeModifierSelectableBlockBaseMixin {

    @Inject(method = "useOnBlock2", at = @At("HEAD"))
    private void mrw$sendMultiNodeRailAction(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        final World world = context.getWorld();
        final PlayerEntity player = context.getPlayer();
        // Sneaking only bypasses the connect flow when the item uses sneak-click for material
        // selection (canSaveBlock), matching the server-side branch in useOnBlock2
        if (!world.isClient() || player == null || (player.isSneaking() && ((ItemNodeModifierSelectableBlockBaseAccessor) this).mrw$getCanSaveBlock())) {
            return;
        }

        // The first click stored the start node position in the stack NBT (synced to the client)
        final CompoundTag compoundTag = context.getStack().getOrCreateTag();
        if (!compoundTag.contains(ItemBlockClickingBase.TAG_POS) || !((ItemNodeModifierBaseInvoker) this).mrw$invokeClickCondition(context)) {
            return;
        }

        final BlockPos startPos = BlockPos.fromLong(compoundTag.getLong(ItemBlockClickingBase.TAG_POS));
        final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = findRailPath(startPos, context.getBlockPos());
        if (path == null) {
            // No route between the two nodes; the server-side error message is suppressed by
            // mrw$connectWithoutError below, so show it locally instead
            player.sendMessage(TranslationProvider.GUI_MTR_RAIL_NOT_FOUND_ACTION.getText(), true);
        } else if (path.size() > 1) {
            InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailAction(path));
        }
    }

    /**
     * Replaces the vanilla end-click connect with the same {@code getRail} call minus the player,
     * which silences MTR's "Rail not found" action-bar message. That message was spurious for
     * multi-node selections (the start and end nodes have no direct rail; the segments are built
     * by {@link PacketApplyRailAction} instead) and for genuinely unconnected nodes the client
     * mixin above shows the message locally.
     */
    @Inject(
            method = "onConnect(Lorg/mtr/mapping/holder/World;Lorg/mtr/mapping/holder/ItemStack;Lorg/mtr/core/data/TransportMode;Lorg/mtr/mapping/holder/BlockState;Lorg/mtr/mapping/holder/BlockState;Lorg/mtr/mapping/holder/BlockPos;Lorg/mtr/mapping/holder/BlockPos;Lorg/mtr/core/tool/Angle;Lorg/mtr/core/tool/Angle;Lorg/mtr/mapping/holder/ServerPlayerEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mrw$connectWithoutError(World world, ItemStack itemStack, TransportMode transportMode, BlockState stateStart, BlockState stateEnd, BlockPos posStart, BlockPos posEnd, Angle facingStart, Angle facingEnd, @Nullable ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        if (serverPlayerEntity == null) {
            return; // the original method is a no-op without a player; let it run
        }
        final ItemNodeModifierSelectableBlockBaseAccessor accessor = (ItemNodeModifierSelectableBlockBaseAccessor) this;
        ItemNodeModifierBase.getRail(world, posStart, posEnd, null, rail -> accessor.mrw$invokeOnConnect(rail, serverPlayerEntity, itemStack, accessor.mrw$getRadius(), accessor.mrw$getHeight()));
        ci.cancel();
    }

    /**
     * Sneak-using a wall creator in the air cycles the wall side mode (both, left, right).
     * This overrides the empty {@code ItemExtension#useWithoutResult} hook; the raycast guard
     * stops the toggle when a block click (material selection) falls through to the use event.
     */
    public void useWithoutResult(World world, PlayerEntity playerEntity, Hand hand) {
        if (!mrw$hasWallSideMode() || !playerEntity.isSneaking() || playerEntity.raycast(5, 1, false).getType() != HitResultType.MISS) {
            return;
        }
        if (!world.isClient()) {
            final CompoundTag compoundTag = playerEntity.getStackInHand(hand).getOrCreateTag();
            final int wallSide = (compoundTag.getInt(WallSide.TAG_WALL_SIDE) + 1) % 3;
            compoundTag.putInt(WallSide.TAG_WALL_SIDE, wallSide);
            playerEntity.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.wall_side", TextHelper.translatable(WallSide.getTranslationKey(wallSide)).data).data), true);
        }
    }

    @Inject(method = "addTooltips", at = @At("TAIL"))
    private void mrw$addWallSideTooltip(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options, CallbackInfo ci) {
        if (mrw$hasWallSideMode()) {
            final int wallSide = stack.getOrCreateTag().getInt(WallSide.TAG_WALL_SIDE);
            tooltip.add(TextHelper.translatable("tooltip.mrw.wall_side_hint", org.mtr.mod.InitClient.getShiftText()).formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
            // Highlight single-side mode so an accidental toggle is easy to spot
            tooltip.add(TextHelper.translatable("tooltip.mrw.wall_side", TextHelper.translatable(WallSide.getTranslationKey(wallSide)).data).formatted(wallSide == WallSide.BOTH ? TextFormatting.GRAY : TextFormatting.AQUA));
        }
    }

    @Unique
    private boolean mrw$hasWallSideMode() {
        return (Object) this instanceof ItemTunnelWallCreator || (Object) this instanceof ItemBridgeWallCreator;
    }

    @Nullable
    private static ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> findRailPath(BlockPos startBlockPos, BlockPos endBlockPos) {
        final Position startPosition = Init.blockPosToPosition(startBlockPos);
        final Position endPosition = Init.blockPosToPosition(endBlockPos);

        if (!MinecraftClientData.getInstance().positionsToRail.containsKey(startPosition)) {
            return null;
        }

        final Object2ObjectOpenHashMap<Position, Position> parentMap = new Object2ObjectOpenHashMap<>();
        final ObjectArrayList<Position> queue = new ObjectArrayList<>();
        queue.add(startPosition);
        parentMap.put(startPosition, startPosition);

        boolean found = false;
        int queueIndex = 0;
        while (queueIndex < queue.size()) {
            final Position current = queue.get(queueIndex++);
            if (current.equals(endPosition)) {
                found = true;
                break;
            }
            MinecraftClientData.getInstance().positionsToRail.getOrDefault(current, new Object2ObjectOpenHashMap<>()).keySet().forEach(neighbor -> {
                if (!parentMap.containsKey(neighbor)) {
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            });
        }

        if (!found) {
            return null;
        }

        final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = new ObjectArrayList<>();
        Position current = endPosition;
        while (!parentMap.get(current).equals(current)) {
            final Position previous = parentMap.get(current);
            path.add(0, new ObjectObjectImmutablePair<>(Init.positionToBlockPos(previous), Init.positionToBlockPos(current)));
            current = previous;
        }
        return path;
    }
}
