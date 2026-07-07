package dev.x341.mrw;

import dev.x341.mrw.mod.Init;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric-specific bootstrap. Loader glue only — it is NOT copied to the Forge module
 * (the Forge {@code setupFiles} task copies just {@code dev.x341.mrw.mod.*}).
 */
public final class MRWFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Init.init();
    }
}
