package dev.x341.mrw.mod.item;

import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockNode;

/**
 * This mod's own analog of MTR's {@code org.mtr.mod.item.ItemNodeModifierSelectableBlockBase}
 * (not a subclass of it — MTR's version assumes a single saved block and a single 2-click connect
 * flow, neither of which fits Rail Worker's two material slots and 4-click dual-pair selection).
 * What it does carry over is the same shape: routes a sneak-click to material selection and a
 * plain click to node selection, and guarantees the latter only ever fires on an actual rail node
 * — any other block is silently ignored, exactly like MTR's own node-modifier items.
 */
public abstract class ItemNodeModifierSelectableBlockBase extends ItemExtension {

	protected ItemNodeModifierSelectableBlockBase(ItemSettings itemSettings) {
		super(itemSettings);
	}

	@Override
	public final ActionResult useOnBlock2(ItemUsageContext context) {
		final PlayerEntity player = context.getPlayer();
		if (player == null) {
			return ActionResult.PASS;
		}

		if (player.isSneaking()) {
			if (!context.getWorld().isClient()) {
				saveOrClearBlock(context, player);
			}
		} else if (context.getWorld().isClient()) {
			final BlockPos blockPos = context.getBlockPos();
			if (context.getWorld().getBlockState(blockPos).getBlock().data instanceof BlockNode) {
				onNodeClick(context, blockPos);
			}
		}

		return ActionResult.SUCCESS;
	}

	/** Sneak-clicking a block saves or clears it as this item's currently targeted material (server-side only). */
	protected abstract void saveOrClearBlock(ItemUsageContext context, PlayerEntity player);

	/** A plain click that landed on an actual rail node (client-side only; clicks on any other block never reach here). */
	protected abstract void onNodeClick(ItemUsageContext context, BlockPos clicked);
}
