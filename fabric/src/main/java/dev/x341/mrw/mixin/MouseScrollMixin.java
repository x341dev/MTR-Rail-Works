package dev.x341.mrw.mixin;

import dev.x341.mrw.mod.client.RailWorkerClientHelper;
import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.Mouse.class)
public abstract class MouseScrollMixin {

	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void mrw$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player == null || !Screen.hasControlDown() || !(player.getMainHandStack().getItem().data instanceof ItemRailWorker)) {
			return;
		}
		RailWorkerClientHelper.cycleTargetSlot(player);
		ci.cancel();
	}
}
