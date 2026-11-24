package dev.x341.mrw.mod.registry;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.block.BlockShapeDesigner;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class Blocks {

	public static final BlockRegistryObject SHAPE_DESIGNER = Init.REGISTRY.registerBlockWithBlockItem(new Identifier(Init.MOD_ID, "shape_designer"), () -> new Block(new BlockShapeDesigner()), CreativeModeTabs.RAIL_WORKS);

	private Blocks() {
	}

	public static void init() {
		Init.LOGGER.info("Registering MRW blocks");
	}
}
