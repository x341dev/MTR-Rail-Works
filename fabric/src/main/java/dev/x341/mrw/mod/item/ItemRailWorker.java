package dev.x341.mrw.mod.item;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.client.RailWorkerClickState;
import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.packet.PacketCycleRailWorkerTargetSlot;
import dev.x341.mrw.mod.screen.RailWorkerConfigScreen;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.Hand;
import org.mtr.mapping.holder.HitResultType;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockNode;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Builds bridge floors, tunnel excavations, and (optionally) walls/ceiling along rails, all from
 * one configurable tool. Unlike {@link ItemBridgeWallCreator} this class owns its behavior
 * outright (no baked width/height variants, no mixins into a shared MTR base class) since every
 * setting lives in this item's own NBT and is edited through {@link RailWorkerConfigScreen}.
 */
public class ItemRailWorker extends ItemExtension {

	public static final String TAG_MODE = "mrw_mode";
	public static final String TAG_REPLACE = "mrw_replace";
	public static final String TAG_WIDTH = "mrw_width";
	public static final String TAG_HEIGHT = "mrw_height";
	public static final String TAG_FLOOR_BLOCK = "mrw_floor_block";
	public static final String TAG_WALL_BLOCK = "mrw_wall_block";
	public static final String TAG_TARGET_SLOT = "mrw_target_slot";

	public static final int TARGET_FLOOR = 0;
	public static final int TARGET_WALL = 1;

	public static final int DEFAULT_WIDTH = 3;
	public static final int DEFAULT_HEIGHT = 1;

	public ItemRailWorker(ItemSettings itemSettings) {
		super(itemSettings);
	}

	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
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
			// Node selection only makes sense on an actual MTR rail node, not any block
			if (context.getWorld().getBlockState(blockPos).getBlock().data instanceof BlockNode) {
				RailWorkerClickState.getInstance().onNodeClick(blockPos);
			}
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Sneak + use in the air opens the config screen. Every setting the screen edits already lives
	 * in this item's own (server-synced) NBT, so unlike MTR's block-entity screens, no packet is
	 * needed just to open it — only to persist changes back (see {@link RailWorkerConfigScreen}).
	 */
	@Override
	public void useWithoutResult(World world, PlayerEntity playerEntity, Hand hand) {
		if (!playerEntity.isSneaking() || playerEntity.raycast(5, 1, false).getType() != HitResultType.MISS) {
			return;
		}
		if (world.isClient()) {
			RailWorkerClickState.getInstance().reset();
			MinecraftClient.getInstance().openScreen(new Screen(new RailWorkerConfigScreen(playerEntity.getStackInHand(hand))));
		}
	}

	@Override
	public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
		final BlockPos pair1Node = RailWorkerClickState.getInstance().getPair1Node();
		if (pair1Node != null) {
			tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_pair1", pair1Node.toShortString()).formatted(TextFormatting.GOLD));
		}
		final BlockPos pair2Node = RailWorkerClickState.getInstance().getPair2Node();
		if (pair2Node != null) {
			tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_pair2", pair2Node.toShortString()).formatted(TextFormatting.GOLD));
		}

		final CompoundTag tag = stack.getOrCreateTag();
		final int mode = tag.getInt(TAG_MODE);
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_mode",
				TextHelper.translatable(RailWorkerMode.hasBridge(mode) ? "tooltip.mrw.rail_worker_mode_on" : "tooltip.mrw.rail_worker_mode_off").getString(),
				TextHelper.translatable(RailWorkerMode.hasTunnel(mode) ? "tooltip.mrw.rail_worker_mode_on" : "tooltip.mrw.rail_worker_mode_off").getString()
		).formatted(TextFormatting.GRAY));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_walls", TextHelper.translatable(
				RailWorkerMode.hasWallsOnly(mode) ? "tooltip.mrw.rail_worker_walls_only" :
						RailWorkerMode.hasWallsCeiling(mode) ? "tooltip.mrw.rail_worker_walls_ceiling" : "tooltip.mrw.rail_worker_walls_none"
		).getString()).formatted(TextFormatting.GRAY));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_size", getWidth(tag), getHeight(tag)).formatted(TextFormatting.GRAY));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_save").formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_scroll").formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_configure", org.mtr.mod.InitClient.getShiftText()).formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
	}

	private static void saveOrClearBlock(ItemUsageContext context, PlayerEntity player) {
		final BlockPos blockPos = context.getBlockPos();
		final BlockState clickedState = context.getWorld().getBlockState(blockPos);
		if (clickedState.getBlock().data instanceof BlockNode) {
			return;
		}

		final CompoundTag tag = context.getStack().getOrCreateTag();
		final int targetSlot = tag.getInt(TAG_TARGET_SLOT);
		final String tagKey = targetSlot == TARGET_WALL ? TAG_WALL_BLOCK : TAG_FLOOR_BLOCK;
		final int clickedRawId = Block.getRawIdFromState(clickedState);
		final String targetNameKey = targetSlot == TARGET_WALL ? "tooltip.mrw.rail_worker_target_wall" : "tooltip.mrw.rail_worker_target_floor";

		if (tag.contains(tagKey) && tag.getInt(tagKey) == clickedRawId) {
			tag.remove(tagKey);
			player.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.rail_worker_block_cleared", TextHelper.translatable(targetNameKey).getString()).data), true);
		} else {
			tag.putInt(tagKey, clickedRawId);
			player.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.rail_worker_block_saved", TextHelper.translatable(targetNameKey).getString(), TextHelper.translatable(clickedState.getBlock().getTranslationKey()).getString()).data), true);
		}
	}

	public static int getWidth(CompoundTag tag) {
		return tag.contains(TAG_WIDTH) ? tag.getInt(TAG_WIDTH) : DEFAULT_WIDTH;
	}

	public static int getHeight(CompoundTag tag) {
		return tag.contains(TAG_HEIGHT) ? tag.getInt(TAG_HEIGHT) : DEFAULT_HEIGHT;
	}

	@Nullable
	public static BlockState getFloorState(CompoundTag tag) {
		return tag.contains(TAG_FLOOR_BLOCK) ? Block.getStateFromRawId(tag.getInt(TAG_FLOOR_BLOCK)) : null;
	}

	@Nullable
	public static BlockState getWallState(CompoundTag tag) {
		return tag.contains(TAG_WALL_BLOCK) ? Block.getStateFromRawId(tag.getInt(TAG_WALL_BLOCK)) : null;
	}

	/**
	 * Ctrl+scroll target-slot cycling. Called from each loader's own scroll hook (a Fabric mixin
	 * into vanilla {@code Mouse}, a Forge {@code InputEvent.MouseScrollingEvent} listener — neither
	 * can be shared code, see the Rail Worker implementation plan) so both loaders funnel through
	 * one place. Flips the client's own NBT copy immediately for instant feedback, then tells the
	 * server to apply the same flip to its authoritative copy.
	 */
	public static void cycleTargetSlot(ClientPlayerEntity player) {
		final ItemStack itemStack = player.getMainHandStack();
		if (!(itemStack.getItem().data instanceof ItemRailWorker)) {
			return;
		}

		final CompoundTag tag = itemStack.getOrCreateTag();
		final int next = tag.getInt(TAG_TARGET_SLOT) == TARGET_WALL ? TARGET_FLOOR : TARGET_WALL;
		tag.putInt(TAG_TARGET_SLOT, next);
		player.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.rail_worker_target_switched", TextHelper.translatable(next == TARGET_WALL ? "tooltip.mrw.rail_worker_target_wall" : "tooltip.mrw.rail_worker_target_floor").getString()).data), true);

		InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketCycleRailWorkerTargetSlot());
	}
}
