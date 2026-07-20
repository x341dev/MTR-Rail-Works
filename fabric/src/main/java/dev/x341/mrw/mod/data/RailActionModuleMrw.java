package dev.x341.mrw.mod.data;

import org.mtr.core.data.Rail;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;

/**
 * MRW's version of MTR's {@code RailActionModule}: one per {@link ServerWorld}, building the
 * queued {@link RailActionMrw}s one at a time from the end-of-world-tick hook in
 * {@link dev.x341.mrw.mod.Init}. Actions queued here are not broadcast to MTR's
 * "Current Build Operations" GUI (that packet only carries MTR's own rail actions).
 */
public class RailActionModuleMrw {

	private final ServerWorld serverWorld;
	private final ObjectArrayList<RailActionMrw> railActions = new ObjectArrayList<>();

	public RailActionModuleMrw(ServerWorld serverWorld) {
		this.serverWorld = serverWorld;
	}

	public void tick() {
		if (!railActions.isEmpty() && railActions.get(0).build()) {
			railActions.remove(0);
		}
	}

	public void markRailForTunnelWall(Rail rail, ServerPlayerEntity serverPlayerEntity, int radius, int height, BlockState blockState, int wallSide) {
		railActions.add(new RailActionMrw(serverWorld, serverPlayerEntity, RailActionType.TUNNEL_WALL, rail, radius + 1, height + 1, blockState, wallSide));
	}

	public void markRailForBridgeWall(Rail rail, ServerPlayerEntity serverPlayerEntity, int radius, int height, BlockState blockState, int wallSide) {
		railActions.add(new RailActionMrw(serverWorld, serverPlayerEntity, RailActionType.BRIDGE_WALL, rail, radius + 1, height + 1, blockState, wallSide));
	}

	/**
	 * Rail Worker's walls/ceiling. {@code sidesOnly} picks Walls Only (edges only) vs.
	 * Walls + Ceiling (edges + a middle cap); {@code pairStart}/{@code pairEnd} are the two raw
	 * node positions the player clicked for this segment's pair, used to derive a consistent wall
	 * side independent of where the player is standing when the action actually builds.
	 * {@code wallSide} (resolved by the caller — see {@link dev.x341.mrw.mod.packet.PacketApplyRailWorkerBuild})
	 * is {@code WallSide.BOTH} for a single selected pair (no second pair to leave the other edge
	 * open for), otherwise one of {@code LEFT}/{@code RIGHT}, mirrored between the two pairs of a
	 * 2-pair selection so the walls face outward for the common case of two parallel tracks.
	 */
	public void markRailForRailWorkerWalls(Rail rail, ServerPlayerEntity serverPlayerEntity, int radius, int height, BlockState blockState, boolean sidesOnly, boolean replace, BlockPos pairStart, BlockPos pairEnd, int wallSide) {
		railActions.add(new RailActionMrw(serverWorld, serverPlayerEntity, RailActionType.RAIL_WORKER_WALLS, rail, radius + 1, height + 1, blockState, wallSide, pairStart, pairEnd, sidesOnly, replace));
	}
}
