package dev.x341.mrw;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.data.ShapeBlocks;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric-specific bootstrap. Loader glue only — it is NOT copied to the Forge module
 * (the Forge {@code setupFiles} task copies just {@code dev.x341.mrw.mod.*}).
 */
public final class MRWFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ShapeBlocks.setResolver(MRWFabric::resolveBlock);
        Init.init();
    }

    private static org.mtr.mapping.holder.BlockState resolveBlock(String id) {
        final net.minecraft.util.Identifier identifier = net.minecraft.util.Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        #if MC_VERSION >= "11903"
        return net.minecraft.registry.Registries.BLOCK.getOrEmpty(identifier).map(block -> new org.mtr.mapping.holder.BlockState(block.getDefaultState())).orElse(null);
        #else
        return net.minecraft.util.registry.Registry.BLOCK.getOrEmpty(identifier).map(block -> new org.mtr.mapping.holder.BlockState(block.getDefaultState())).orElse(null);
        #endif
    }
}
