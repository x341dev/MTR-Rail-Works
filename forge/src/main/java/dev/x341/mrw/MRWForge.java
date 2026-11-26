package dev.x341.mrw;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.BlockCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

/**
 * Forge-specific bootstrap. Loader glue only — this file is committed in the Forge module and
 * is never overwritten by {@code setupFiles} (which only replaces {@code dev.x341.mrw.mod.*}).
 * The shared {@code dev.x341.mrw.mod.*} classes are copied in from the Fabric module at build time.
 */
@Mod(Init.MOD_ID)
public final class MRWForge {

    public MRWForge() {
        Init.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BlockCatalog.setSupplier(() -> ForgeRegistries.BLOCKS.getKeys().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toList()));
            InitClient.init();
            MinecraftForge.EVENT_BUS.register(new RailWorkerClientHandler());
            MinecraftForge.EVENT_BUS.register(new ShapeDesignerIconHandler());
        }
    }
}
