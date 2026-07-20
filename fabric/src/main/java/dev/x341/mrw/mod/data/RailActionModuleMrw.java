package dev.x341.mrw.mod.data;

import org.mtr.core.data.Rail;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
}
