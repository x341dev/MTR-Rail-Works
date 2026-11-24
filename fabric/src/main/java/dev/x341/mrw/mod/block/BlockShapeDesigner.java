package dev.x341.mrw.mod.block;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.mapper.BlockWithEntity;

/**
 * A block that holds a 9x9 cross-section profile. The editing screen is added in a follow-up;
 * for now the block only stores and persists its profile.
 */
public class BlockShapeDesigner extends BlockExtension implements BlockWithEntity {

	public BlockShapeDesigner() {
		super(BlockHelper.createBlockSettings(true, false).strength(2));
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new BlockEntityShapeDesigner(blockPos, blockState);
	}
}
