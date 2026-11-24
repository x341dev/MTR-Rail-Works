package dev.x341.mrw.mod.registry;

import dev.x341.mrw.blocks.BlockShapeDesigner;
import dev.x341.mrw.mod.Init;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.BlockRegistryObject;

public class Blocks {
    public static final BlockRegistryObject SHAPE_DESIGNER = Init.REGISTRY.registerBlockWithBlockItem(new Identifier(Init.MOD_ID, "shape_designer"), () -> new Block(new BlockShapeDesigner()), CreativeModeTabs.RAIL_WORKS)    ;

    private Blocks() {
    }

    public static void init() {
        Init.LOGGER.info("Registering MRW blocks");
    }
}
