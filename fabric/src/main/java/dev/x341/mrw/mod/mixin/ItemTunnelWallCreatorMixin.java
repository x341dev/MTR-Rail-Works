package dev.x341.mrw.mod.mixin;

import dev.x341.mrw.mod.data.WallSide;
import org.mtr.core.data.Rail;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mod.item.ItemTunnelWallCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MTR's own {@code RailAction} cannot build just one wall side, so when a single-side mode is
 * active on a Tunnel Wall Creator the connect is routed to MRW's rail action module instead.
 * With the default mode (both sides) the vanilla MTR path runs untouched.
 */
@Mixin(value = ItemTunnelWallCreator.class, remap = false)
public abstract class ItemTunnelWallCreatorMixin {

    @Inject(method = "onConnect(Lorg/mtr/core/data/Rail;Lorg/mtr/mapping/holder/ServerPlayerEntity;Lorg/mtr/mapping/holder/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void mrw$connectWithWallSide(Rail rail, ServerPlayerEntity serverPlayerEntity, ItemStack itemStack, int radius, int height, CallbackInfo ci) {
        final int wallSide = itemStack.getOrCreateTag().getInt(WallSide.TAG_WALL_SIDE);
        if (wallSide == WallSide.BOTH) {
            return;
        }
        final BlockState blockState = ((ItemNodeModifierSelectableBlockBaseAccessor) this).mrw$invokeGetSavedState(itemStack);
        if (blockState != null) {
            dev.x341.mrw.mod.Init.getRailActionModule(serverPlayerEntity.getServerWorld(), railActionModule -> railActionModule.markRailForTunnelWall(rail, serverPlayerEntity, radius, height, blockState, wallSide));
        }
        ci.cancel();
    }
}
