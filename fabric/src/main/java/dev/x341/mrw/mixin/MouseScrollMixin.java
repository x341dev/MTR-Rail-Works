package dev.x341.mrw.mixin;

import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ctrl+scroll cycles Rail Worker's floor/wall target slot (see {@link ItemRailWorker#cycleTargetSlot}).
 * Deliberately kept outside {@code dev.x341.mrw.mod} (Forge's {@code setupFiles} task only copies
 * that package) and deliberately NOT {@code remap = false}: unlike every other mixin in this repo,
 * this one targets a real vanilla class ({@code net.minecraft.client.Mouse}), so it needs Loom's
 * normal Yarn-to-intermediary remapping. Forge has no equivalent of this file — it gets the same
 * behavior for free from {@code net.minecraftforge.client.event.InputEvent.MouseScrollingEvent}
 * (see {@code RailWorkerClientHandler} in the Forge module), since Forge already exposes scrolling
 * as a proper event and needs no mixin at all.
 */
@Mixin(net.minecraft.client.Mouse.class)
public abstract class MouseScrollMixin {

	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void mrw$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player == null || !Screen.hasControlDown() || !(player.getMainHandStack().getItem().data instanceof ItemRailWorker)) {
			return;
		}
		ItemRailWorker.cycleTargetSlot(player);
		ci.cancel();
	}
}
