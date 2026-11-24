package dev.x341.mrw.mod.registry;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public final class BlockEntityTypes {

	public static final BlockEntityTypeRegistryObject<BlockEntityShapeDesigner> SHAPE_DESIGNER = Init.REGISTRY.registerBlockEntityType(new Identifier(Init.MOD_ID, "shape_designer"), BlockEntityShapeDesigner::new, Blocks.SHAPE_DESIGNER::get);

	private BlockEntityTypes() {
	}

	public static void init() {
		Init.LOGGER.info("Registering MRW block entity types");
	}
}
