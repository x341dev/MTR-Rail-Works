package dev.x341.mrw.mod.client;

import dev.x341.mrw.mod.item.ItemRailWorker;
import dev.x341.mrw.mod.packet.PacketApplyRailWorkerBuild;
import dev.x341.mrw.mod.packet.PacketCycleRailWorkerTargetSlot;
import dev.x341.mrw.mod.screen.RailWorkerConfigScreen;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.generated.lang.TranslationProvider;

public class RailWorkerClientHelper {

    private RailWorkerClientHelper() {}

    public static void openConfigScreen(ItemStack stack) {
        MinecraftClient.getInstance().openScreen(new Screen(new RailWorkerConfigScreen(stack)));
    }

    public static void showRailNotFound() {
        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if (player != null) {
            player.sendMessage(TranslationProvider.GUI_MTR_RAIL_NOT_FOUND_ACTION.getText(), true);
        }
    }

    public static void sendBuildPacket(ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path, BlockPos start, BlockPos end) {
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(path, start, end));
    }

    public static void sendBuildPacket(
            ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path, BlockPos pair1Start, BlockPos pair1End,
            ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair2Path, BlockPos pair2Start, BlockPos pair2End
    ) {
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(pair1Path, pair1Start, pair1End, pair2Path, pair2Start, pair2End));
    }

    public static void cycleTargetSlot(ClientPlayerEntity player) {
        final ItemStack itemStack = player.getMainHandStack();
        if (!(itemStack.getItem().data instanceof ItemRailWorker)) {
            return;
        }

        final CompoundTag tag = itemStack.getOrCreateTag();
        final int next = tag.getInt(ItemRailWorker.TAG_TARGET_SLOT) == ItemRailWorker.TARGET_WALL
                ? ItemRailWorker.TARGET_FLOOR
                : ItemRailWorker.TARGET_WALL;
        tag.putInt(ItemRailWorker.TAG_TARGET_SLOT, next);

        final String targetNameKey = next == ItemRailWorker.TARGET_WALL
                ? "tooltip.mrw.rail_worker_target_wall"
                : "tooltip.mrw.rail_worker_target_floor";
        player.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.rail_worker_target_switched", TextHelper.translatable(targetNameKey).getString()).data), true);

        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketCycleRailWorkerTargetSlot());
    }
}
