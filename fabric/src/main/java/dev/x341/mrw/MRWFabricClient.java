package dev.x341.mrw;

import dev.x341.mrw.mod.client.InitClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric-specific client bootstrap. Loader glue only — not copied to the Forge module.
 */
public final class MRWFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitClient.init();
    }
}
