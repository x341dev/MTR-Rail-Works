package dev.x341.mrw;

import dev.x341.mrw.mod.item.ItemRailWorker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.Screen;

/**
 * Forge's equivalent of the Fabric module's {@code MouseScrollMixin}: Ctrl+scroll cycles Rail
 * Worker's floor/wall target slot. Forge already exposes scrolling as a proper cancelable event,
 * so unlike Fabric this needs no mixin at all — just a normal event bus subscriber, registered in
 * {@link MRWForge} on the client dist only.
 */
public final class RailWorkerClientHandler {

	// Forge renamed InputEvent.MouseScrollEvent to InputEvent.MouseScrollingEvent between 1.18.2
	// and 1.19.2; both extend the same Event base and are @Cancelable, so the subscriber method is
	// duplicated per name here and delegates to the shared, version-independent handler below.
	#if MC_VERSION >= "11902"
	@SubscribeEvent
	public void onScroll(InputEvent.MouseScrollingEvent event) {
		handleScroll(event);
	}
	#else
	@SubscribeEvent
	public void onScroll(InputEvent.MouseScrollEvent event) {
		handleScroll(event);
	}
	#endif

	private void handleScroll(InputEvent event) {
		final LocalPlayer rawPlayer = Minecraft.getInstance().player;
		if (rawPlayer == null || !Screen.hasControlDown()) {
			return;
		}

		final ClientPlayerEntity player = new ClientPlayerEntity(rawPlayer);
		if (!(player.getMainHandStack().getItem().data instanceof ItemRailWorker)) {
			return;
		}

		ItemRailWorker.cycleTargetSlot(player);
		event.setCanceled(true);
	}
}
