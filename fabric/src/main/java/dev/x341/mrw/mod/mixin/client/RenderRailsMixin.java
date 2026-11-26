package dev.x341.mrw.mod.mixin.client;

import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mod.render.RenderRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderRails.class, remap = false)
public abstract class RenderRailsMixin {

    @Inject(method = "isHoldingRailRelated", at = @At("RETURN"), cancellable = true)
    private static void mrw$holdingRailWorker(ClientPlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && (isRailWorker(player.getMainHandStack()) || isRailWorker(player.getOffHandStack()))) {
            cir.setReturnValue(true);
        }
    }

    private static boolean isRailWorker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().data instanceof ItemRailWorker;
    }
}
