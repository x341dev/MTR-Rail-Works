package dev.x341.mrw.mod.item;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.client.RailPathFinder;
import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.packet.PacketApplyRailWorkerBuild;
import dev.x341.mrw.mod.packet.PacketCycleRailWorkerTargetSlot;
import dev.x341.mrw.mod.screen.RailWorkerConfigScreen;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
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
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.generated.lang.TranslationProvider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Builds bridge floors, tunnel excavations, and (optionally) walls/ceiling along rails, all from
 * one configurable tool. Unlike {@link ItemBridgeWallCreator} this class owns its behavior
 * outright (no baked width/height variants, no mixins into a shared MTR base class) since every
 * setting lives in this item's own NBT and is edited through {@link RailWorkerConfigScreen}.
 *
 * <p>Node-click progress (the pair-selection state machine) also lives entirely in this specific
 * item stack's own NBT, exactly like {@link ItemBridgeWallCreator}'s saved {@code TAG_POS} — never
 * in a shared static field — so two Rail Worker stacks (different hotbar slots, one dropped and
 * another picked up, ...) never bleed progress into each other. Click order is interleaved rather
 * than sequential: 1st click is pair 1's start, 2nd click either completes pair 1 (if it
 * BFS-connects to the 1st click — the operation then fires immediately using pair 1 alone) or, if
 * it does not connect, is treated as pair 2's start; the 3rd click then completes pair 1's end and
 * the 4th completes pair 2's end, firing both in one batched packet. This click order (start,
 * start, end, end) matches walking along two parallel rails and clicking both starts before
 * walking to the far end, rather than finishing one pair before starting the next. Sneak + use on
 * a node ({@link #onSneakNodeClick}) bypasses the automatic BFS-connectivity decision and forces
 * that click to become pair 2's start outright, for the case where a pair 1/pair 2 candidate
 * happen to be technically connected somewhere in the network but two independent segments are
 * wanted anyway.
 */
public class ItemRailWorker extends ItemNodeModifierSelectableBlockBase {

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

	/** 0 = idle, 1 = pair 1 start placed, 2 = both starts placed (dual mode), awaiting pair 1's end, 3 = pair 1 done, awaiting pair 2's end. */
	private static final String TAG_CLICK_STAGE = "mrw_click_stage";
	private static final String TAG_PAIR1_START = "mrw_pair1_start";
	private static final String TAG_PAIR1_END = "mrw_pair1_end";
	private static final String TAG_PAIR2_START = "mrw_pair2_start";

	public ItemRailWorker(ItemSettings itemSettings) {
		super(itemSettings);
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
			resetClickState(playerEntity.getStackInHand(hand).getOrCreateTag());
			MinecraftClient.getInstance().openScreen(new Screen(new RailWorkerConfigScreen(playerEntity.getStackInHand(hand))));
		}
	}

	@Override
	public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
		final CompoundTag tag = stack.getOrCreateTag();

		if (tag.contains(TAG_PAIR1_START)) {
			tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_pair1", BlockPos.fromLong(tag.getLong(TAG_PAIR1_START)).toShortString()).formatted(TextFormatting.GOLD));
		}
		if (tag.contains(TAG_PAIR2_START)) {
			tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_pair2", BlockPos.fromLong(tag.getLong(TAG_PAIR2_START)).toShortString()).formatted(TextFormatting.GOLD));
		}

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
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_force_pair2").formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_scroll").formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
		tooltip.add(TextHelper.translatable("tooltip.mrw.rail_worker_hint_configure", org.mtr.mod.InitClient.getShiftText()).formatted(TextFormatting.GRAY).formatted(TextFormatting.ITALIC));
	}

	@Override
	protected void saveOrClearBlock(ItemUsageContext context, PlayerEntity player) {
		final BlockPos blockPos = context.getBlockPos();
		final BlockState clickedState = context.getWorld().getBlockState(blockPos);

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

	@Override
	protected void onNodeClick(ItemUsageContext context, BlockPos clicked) {
		final CompoundTag tag = context.getStack().getOrCreateTag();
		final int stage = tag.getInt(TAG_CLICK_STAGE);
		switch (stage) {
			case 0:
				tag.putLong(TAG_PAIR1_START, clicked.asLong());
				tag.putInt(TAG_CLICK_STAGE, 1);
				debugLog("click at " + clicked + " (stage 0->1): pair1Start set");
				break;
			case 1: {
				final BlockPos pair1Start = BlockPos.fromLong(tag.getLong(TAG_PAIR1_START));
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = RailPathFinder.findPath(pair1Start, clicked);
				if (path != null) {
					// Directly connected: the player only wants a single pair, build now
					debugLog("click at " + clicked + " (stage 1): connects to pair1Start=" + pair1Start + " (" + path.size() + " segments) -> sending single-pair build");
					InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(path, pair1Start, clicked));
					resetClickState(tag);
				} else {
					// Not connected: treat this as the second pair's start instead of an error
					debugLog("click at " + clicked + " (stage 1->2): no path from pair1Start=" + pair1Start + " -> treating as pair2Start");
					tag.putLong(TAG_PAIR2_START, clicked.asLong());
					tag.putInt(TAG_CLICK_STAGE, 2);
				}
				break;
			}
			case 2: {
				final BlockPos pair1Start = BlockPos.fromLong(tag.getLong(TAG_PAIR1_START));
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = RailPathFinder.findPath(pair1Start, clicked);
				if (path == null) {
					debugLog("click at " + clicked + " (stage 2): no path from pair1Start=" + pair1Start + " -> rail not found, staying at stage 2");
					showRailNotFound();
					return;
				}
				tag.putLong(TAG_PAIR1_END, clicked.asLong());
				tag.putInt(TAG_CLICK_STAGE, 3);
				debugLog("click at " + clicked + " (stage 2->3): pair1End set, pair1 has " + path.size() + " segments");
				break;
			}
			case 3: {
				final BlockPos pair2Start = BlockPos.fromLong(tag.getLong(TAG_PAIR2_START));
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair2Path = RailPathFinder.findPath(pair2Start, clicked);
				if (pair2Path == null) {
					debugLog("click at " + clicked + " (stage 3): no path from pair2Start=" + pair2Start + " -> rail not found, staying at stage 3");
					showRailNotFound();
					return;
				}
				final BlockPos pair1Start = BlockPos.fromLong(tag.getLong(TAG_PAIR1_START));
				final BlockPos pair1End = BlockPos.fromLong(tag.getLong(TAG_PAIR1_END));
				// Recomputed fresh rather than cached from stage 2->3, since NBT only stores the
				// endpoints, not the resolved path
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path = RailPathFinder.findPath(pair1Start, pair1End);
				if (pair1Path == null) {
					debugLog("click at " + clicked + " (stage 3): pair1 (" + pair1Start + "->" + pair1End + ") no longer connects -> rail not found, staying at stage 3");
					showRailNotFound();
					return;
				}
				debugLog("click at " + clicked + " (stage 3): connects to pair2Start=" + pair2Start + " (" + pair2Path.size() + " segments) -> sending 2-pair build");
				InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(pair1Path, pair1Start, pair1End, pair2Path, pair2Start, clicked));
				resetClickState(tag);
				break;
			}
			default:
				resetClickState(tag);
				break;
		}
	}

	/**
	 * Sneak + use on a node forces that click to become (or replace) pair 2's start, skipping the
	 * automatic BFS-connectivity check {@link #onNodeClick} uses to decide single-pair vs.
	 * dual-pair — useful when a pair 1/pair 2 candidate happen to be technically connected
	 * somewhere in the network but the player wants two independent segments anyway. Only
	 * meaningful once pair 1 has a start (stage &gt;= 1); before that there is no "second" pair yet,
	 * so the sneak modifier is ignored and this behaves like a normal first click.
	 */
	@Override
	protected void onSneakNodeClick(ItemUsageContext context, BlockPos clicked) {
		final CompoundTag tag = context.getStack().getOrCreateTag();
		final int stage = tag.getInt(TAG_CLICK_STAGE);
		if (stage == 0) {
			onNodeClick(context, clicked);
			return;
		}

		tag.putLong(TAG_PAIR2_START, clicked.asLong());
		tag.putInt(TAG_CLICK_STAGE, 2);
		debugLog("sneak-click at " + clicked + " (stage " + stage + "->2): pair2Start forced");

		final PlayerEntity player = context.getPlayer();
		if (player != null) {
			player.sendMessage(new Text(TextHelper.translatable("tooltip.mrw.rail_worker_pair2_forced", clicked.toShortString()).data), true);
		}
	}

	private static void debugLog(String message) {
		if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
			dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] ItemRailWorker: {}", message);
		}
	}

	private static void showRailNotFound() {
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player != null) {
			player.sendMessage(TranslationProvider.GUI_MTR_RAIL_NOT_FOUND_ACTION.getText(), true);
		}
	}

	private static void resetClickState(CompoundTag tag) {
		tag.remove(TAG_CLICK_STAGE);
		tag.remove(TAG_PAIR1_START);
		tag.remove(TAG_PAIR1_END);
		tag.remove(TAG_PAIR2_START);
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
	 * Drives the item's model override: 0 = nothing selected, 0.5 = working on pair 1 only,
	 * 1.0 = committed to a second pair. Vanilla clamps model-predicate values to [0, 1] before
	 * comparing them against override predicates, so this can't just return 0/1/2 — those get
	 * clamped down to 1.0 and the "committed to pair 2" override could never match.
	 */
	public static float getTextureIndex(CompoundTag tag) {
		final int stage = tag.getInt(TAG_CLICK_STAGE);
		return stage >= 2 ? 1.0f : stage >= 1 ? 0.5f : 0.0f;
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
