package dev.x341.mrw.mod.client;

import dev.x341.mrw.mod.item.ItemRailWorker;
import dev.x341.mrw.mod.packet.PacketApplyRailWorkerBuild;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.generated.lang.TranslationProvider;

import javax.annotation.Nullable;

/**
 * Client-only state machine for Rail Worker's node selection. Pair 1 (2 clicks) is always
 * required; pair 2 is optional. Click order is interleaved rather than sequential: 1st click is
 * pair 1's start, 2nd click either completes pair 1 (if it BFS-connects to the 1st click — the
 * operation then fires immediately using pair 1 alone) or, if it does not connect, is treated as
 * pair 2's start; the 3rd click then completes pair 1's end and the 4th completes pair 2's end,
 * firing both in one batched packet. This click order (start, start, end, end) matches walking
 * along two parallel rails and clicking both starts before walking to the far end, rather than
 * finishing one pair before starting the next. Nothing is sent to the server until at least pair 1
 * resolves; click progress is transient and deliberately kept out of item NBT.
 */
public final class RailWorkerClickState {

	private static final RailWorkerClickState INSTANCE = new RailWorkerClickState();

	/** 0 = idle, 1 = pair 1 start placed, 2 = both starts placed (dual mode), awaiting pair 1's end, 3 = pair 1 done, awaiting pair 2's end. */
	private int stage;
	@Nullable
	private BlockPos pair1Start;
	@Nullable
	private BlockPos pair1End;
	@Nullable
	private ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path;
	@Nullable
	private BlockPos pair2Start;

	private RailWorkerClickState() {
	}

	public static RailWorkerClickState getInstance() {
		return INSTANCE;
	}

	/**
	 * Drives the item's model override: 0 = nothing selected, 0.5 = working on pair 1 only,
	 * 1.0 = committed to a second pair. Vanilla clamps model-predicate values to [0, 1] before
	 * comparing them against override predicates, so this can't just return 0/1/2 — those get
	 * clamped down to 1.0 and the "committed to pair 2" override could never match.
	 */
	public float getTextureIndex() {
		return stage >= 2 ? 1.0f : stage >= 1 ? 0.5f : 0.0f;
	}

	@Nullable
	public BlockPos getPair1Node() {
		return pair1Start;
	}

	@Nullable
	public BlockPos getPair2Node() {
		return pair2Start;
	}

	public void reset() {
		stage = 0;
		pair1Start = null;
		pair1End = null;
		pair1Path = null;
		pair2Start = null;
	}

	/** Resets the selection if the player is no longer holding a Rail Worker, so it can never get stuck showing a "selected" texture. */
	public void tick(ClientPlayerEntity player) {
		if (stage != 0 && !(player.getMainHandStack().getItem().data instanceof ItemRailWorker)) {
			reset();
		}
	}

	public void onNodeClick(BlockPos clicked) {
		switch (stage) {
			case 0:
				pair1Start = clicked;
				stage = 1;
				debugLog("click at " + clicked + " (stage 0->1): pair1Start set");
				break;
			case 1: {
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = RailPathFinder.findPath(pair1Start, clicked);
				if (path != null) {
					// Directly connected: the player only wants a single pair, build now
					debugLog("click at " + clicked + " (stage 1): connects to pair1Start=" + pair1Start + " (" + path.size() + " segments) -> sending single-pair build");
					InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(path, pair1Start, clicked));
					reset();
				} else {
					// Not connected: treat this as the second pair's start instead of an error
					debugLog("click at " + clicked + " (stage 1->2): no path from pair1Start=" + pair1Start + " -> treating as pair2Start");
					pair2Start = clicked;
					stage = 2;
				}
				break;
			}
			case 2: {
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = RailPathFinder.findPath(pair1Start, clicked);
				if (path == null) {
					debugLog("click at " + clicked + " (stage 2): no path from pair1Start=" + pair1Start + " -> rail not found, staying at stage 2");
					showRailNotFound();
					return;
				}
				pair1Path = path;
				pair1End = clicked;
				stage = 3;
				debugLog("click at " + clicked + " (stage 2->3): pair1End set, pair1 has " + path.size() + " segments");
				break;
			}
			case 3: {
				final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = RailPathFinder.findPath(pair2Start, clicked);
				if (path == null) {
					debugLog("click at " + clicked + " (stage 3): no path from pair2Start=" + pair2Start + " -> rail not found, staying at stage 3");
					showRailNotFound();
					return;
				}
				debugLog("click at " + clicked + " (stage 3): connects to pair2Start=" + pair2Start + " (" + path.size() + " segments) -> sending 2-pair build");
				InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketApplyRailWorkerBuild(pair1Path, pair1Start, pair1End, path, pair2Start, clicked));
				reset();
				break;
			}
			default:
				reset();
				break;
		}
	}

	private static void debugLog(String message) {
		if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
			dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] RailWorkerClickState: {}", message);
		}
	}

	private static void showRailNotFound() {
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player != null) {
			player.sendMessage(TranslationProvider.GUI_MTR_RAIL_NOT_FOUND_ACTION.getText(), true);
		}
	}
}
