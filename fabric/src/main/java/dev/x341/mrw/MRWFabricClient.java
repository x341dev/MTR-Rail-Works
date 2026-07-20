package dev.x341.mrw;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.client.RailWorkerClickState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;

/**
 * Fabric-specific client bootstrap. Loader glue only — not copied to the Forge module.
 */
public final class MRWFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
            if (player != null) {
                RailWorkerClickState.getInstance().tick(player);
            }
        });
    }
}
