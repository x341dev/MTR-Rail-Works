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
 * What it does carry over is the same shape: a click on a rail node is routed to node selection
 * and a click on anything else to material selection, with the sneak modifier picking between two
 * variants of each — plain node click vs. sneak+node click, and sneak+non-node click for material
 * selection (a plain, non-sneak click on a non-node block is not a recognized gesture and is
 * silently ignored, same as MTR's own node-modifier items ignoring off-node clicks).
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

		final BlockPos blockPos = context.getBlockPos();
		final boolean isNode = context.getWorld().getBlockState(blockPos).getBlock().data instanceof BlockNode;

		if (player.isSneaking()) {
			if (isNode) {
				if (context.getWorld().isClient()) {
					onSneakNodeClick(context, blockPos);
				}
			} else if (!context.getWorld().isClient()) {
				saveOrClearBlock(context, player);
			}
		} else if (isNode && context.getWorld().isClient()) {
			onNodeClick(context, blockPos);
		}

		return ActionResult.SUCCESS;
	}

	/** Sneak-clicking a non-node block saves or clears it as this item's currently targeted material (server-side only). */
	protected abstract void saveOrClearBlock(ItemUsageContext context, PlayerEntity player);

	/** A plain click that landed on an actual rail node (client-side only; clicks on any other block never reach here). */
	protected abstract void onNodeClick(ItemUsageContext context, BlockPos clicked);

	/** A sneak-click that landed on an actual rail node (client-side only) — a distinct gesture from a plain node click. */
	protected abstract void onSneakNodeClick(ItemUsageContext context, BlockPos clicked);
}
