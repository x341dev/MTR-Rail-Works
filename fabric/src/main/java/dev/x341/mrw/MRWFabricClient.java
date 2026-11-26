package dev.x341.mrw;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.BlockCatalog;
import net.fabricmc.api.ClientModInitializer;

import java.util.stream.Collectors;

/**
 * Fabric-specific client bootstrap. Loader glue only — not copied to the Forge module.
 */
public final class MRWFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        #if MC_VERSION >= "11903"
        BlockCatalog.setSupplier(() -> net.minecraft.registry.Registries.BLOCK.getIds().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
        #else
        BlockCatalog.setSupplier(() -> net.minecraft.util.registry.Registry.BLOCK.getIds().stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));
        #endif
        ShapeDesignerIconRenderer.register();
        InitClient.init();
    }

}
