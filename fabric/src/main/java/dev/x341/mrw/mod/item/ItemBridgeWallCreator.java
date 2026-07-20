package dev.x341.mrw.mod.item;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.data.WallSide;
import org.mtr.core.data.Rail;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mod.item.ItemNodeModifierSelectableBlockBase;

/**
 * Like MTR's Tunnel Wall Creator but for bridges: builds walls along the rail edges, one block
 * below rail level. The side mode toggle and tooltip are added to the shared base class by
 * {@code ItemNodeModifierSelectableBlockBaseMixin}.
 */
public class ItemBridgeWallCreator extends ItemNodeModifierSelectableBlockBase {

	public ItemBridgeWallCreator(int height, int width, ItemSettings itemSettings) {
		super(true, height, width, itemSettings);
	}

	@Override
	protected void onConnect(Rail rail, ServerPlayerEntity serverPlayerEntity, ItemStack itemStack, int radius, int height) {
		final BlockState blockState = getSavedState(itemStack);
		if (blockState != null) {
			final int wallSide = itemStack.getOrCreateTag().getInt(WallSide.TAG_WALL_SIDE);
			Init.getRailActionModule(serverPlayerEntity.getServerWorld(), railActionModule -> railActionModule.markRailForBridgeWall(rail, serverPlayerEntity, radius, height, blockState, wallSide));
		}
	}
}
