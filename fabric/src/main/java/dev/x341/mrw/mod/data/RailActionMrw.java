package dev.x341.mrw.mod.data;

import org.mtr.core.data.Rail;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockNode;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * MRW's version of MTR's {@code RailAction}, covering the action types MTR 4.0.x does not have
     * (bridge walls, Rail Worker's walls/ceiling) or does not support fully (tunnel walls with a
 * single-side mode).
 */
public class RailActionMrw {

	private double distance;

	public final long id;
	private final ServerWorld serverWorld;
	private final UUID uuid;
	private final String playerName;
	private final RailActionType railActionType;
	private final Rail rail;
	private final int radius;
	private final int height;
	private final int wallSide;
	private final boolean invertWallSide;
	private final boolean sidesOnly;
	private final boolean replace;
	private final double length;
	private final BlockState state;
	private final boolean waitForVanillaTunnelQueue;
	private final ObjectOpenHashSet<BlockPos> blacklistedPositions = new ObjectOpenHashSet<>();

	private static final double INCREMENT = 0.1;
	// rail.railMath.getPosition() runs curved segments through spline/trig math (unlike straight
	// segments, which interpolate linearly), so a rail that is vertically flat can still come back
	// with a y like 5.999999999997 instead of an exact 6.0. Comparing that against Math.floor/ceil
	// exactly made whether the top wall block got included flicker block-to-block along a curve,
	// even though every sample is meant to be at the same whole-number height.
	private static final double WHOLE_NUMBER_EPSILON = 1e-6;

	public RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, int wallSide) {
		this(serverWorld, serverPlayerEntity, railActionType, rail, radius, height, state, wallSide, isRailAgainstPlayerFacing(serverPlayerEntity, rail), false, false, false);
	}

	/**
	 * Used for {@link RailActionType#RAIL_WORKER_WALLS}, where the "wall side" reference direction
	 * comes from the raw coordinate offset between the two nodes the player clicked to define this
	 * segment's pair, rather than from the player's facing at build time ({@link #isRailAgainstPlayerFacing}).
	 * Rail Worker's node pairs can be disconnected from where the player is standing (or built later,
	 * from a saved macro), so a facing-based side has no reliable meaning here.
	 *
	 * @param waitForVanillaTunnelQueue when the same Rail Worker build also queued a vanilla MTR
	 *                                  tunnel-clear action for this rail, that action runs on its own
	 *                                  independent, separately-ticked queue ({@code org.mtr.mod.data.RailActionModule}),
	 *                                  not this one. Both queues process a couple of milliseconds per
	 *                                  tick, so without coordination they progress concurrently, and
	 *                                  the tunnel-clear can strip out wall blocks placed at its shared
	 *                                  edge before it has swept past that column — producing an
	 *                                  order-dependent, jagged wall (worse on tight curves, where the
	 *                                  two queues' progress along the rail diverges more per tick).
	 *                                  When set, {@link #build()} holds off starting until the vanilla
	 *                                  queue (checked via {@link dev.x341.mrw.mod.mixin.RailActionModuleAccessor})
	 *                                  is empty.
	 */
	public RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, int wallSide, BlockPos pairStart, BlockPos pairEnd, boolean sidesOnly, boolean replace, boolean waitForVanillaTunnelQueue) {
		this(serverWorld, serverPlayerEntity, railActionType, rail, radius, height, state, wallSide, isRailAgainstNodeOffset(pairStart, pairEnd, rail), sidesOnly, replace, waitForVanillaTunnelQueue);
	}

	private RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, int wallSide, boolean invertWallSide, boolean sidesOnly, boolean replace, boolean waitForVanillaTunnelQueue) {
		id = new Random().nextLong();
		this.serverWorld = serverWorld;
		uuid = serverPlayerEntity.getUuid();
		playerName = serverPlayerEntity.getName().getString();
		this.railActionType = railActionType;
		this.rail = rail;
		this.radius = radius;
		this.height = height;
		this.wallSide = wallSide;
		this.invertWallSide = invertWallSide;
		this.sidesOnly = sidesOnly;
		this.replace = replace;
		this.state = state;
		this.waitForVanillaTunnelQueue = waitForVanillaTunnelQueue;
		length = rail.railMath.getLength();
		distance = 0;
		RailActionBatchTracker.onEnqueue(uuid);
	}

	/**
	 * The rail's own parameterization runs from whichever node happens to be its internal start,
	 * which has no relation to how the player was facing when they connected the two nodes. Without
	 * this check, "left"/"right" would depend on that arbitrary internal direction instead of the
	 * player's point of view, flipping unpredictably from one wall action to the next.
	 */
	private static boolean isRailAgainstPlayerFacing(ServerPlayerEntity serverPlayerEntity, Rail rail) {
		final Vector pos1 = rail.railMath.getPosition(0, false);
		final Vector pos2 = rail.railMath.getPosition(Math.min(INCREMENT, rail.railMath.getLength()), false);
		final double yaw = Math.toRadians(serverPlayerEntity.getYaw(1));
		final double forwardX = -Math.sin(yaw);
		final double forwardZ = Math.cos(yaw);
		return (pos2.x - pos1.x) * forwardX + (pos2.z - pos1.z) * forwardZ > 0;
	}

	/**
	 * Same purpose as {@link #isRailAgainstPlayerFacing}, but for Rail Worker: the reference
	 * direction is the raw coordinate offset between the two nodes the player clicked for this
	 * pair (not the player's look vector, and not the rail's own internal parameterization vector),
	 * so the side stays consistent no matter where the player is standing when the build runs.
	 */
	private static boolean isRailAgainstNodeOffset(BlockPos pairStart, BlockPos pairEnd, Rail rail) {
		final Vector pos1 = rail.railMath.getPosition(0, false);
		final Vector pos2 = rail.railMath.getPosition(Math.min(INCREMENT, rail.railMath.getLength()), false);
		final double offsetX = pairEnd.getX() - pairStart.getX();
		final double offsetZ = pairEnd.getZ() - pairStart.getZ();
		final boolean invertWallSide = (pos2.x - pos1.x) * offsetX + (pos2.z - pos1.z) * offsetZ > 0;
		if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
			dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] wall side: pairStart={} pairEnd={} railDir=({}, {}) offset=({}, {}) invertWallSide={}",
					pairStart, pairEnd, pos2.x - pos1.x, pos2.z - pos1.z, offsetX, offsetZ, invertWallSide);
		}
		return invertWallSide;
	}

	public String getDescription() {
		return TextHelper.translatable(railActionType.nameTranslationKey, playerName, Utilities.round(length, 1), state == null ? "" : TextHelper.translatable(state.getBlock().getTranslationKey()).getString()).getString();
	}

	public int getColor() {
		return railActionType.color;
	}

	/**
	 * Only meaningful before the build has started (see {@link #waitForVanillaTunnelQueue}'s
	 * javadoc): once {@link #distance} has moved off 0, the build is left to run to completion
	 * even if the vanilla queue picks up unrelated work afterward.
	 */
	public boolean isBlockedByVanillaTunnelQueue() {
		if (!waitForVanillaTunnelQueue || distance > 0) {
			return false;
		}
		final boolean[] blocked = {false};
		Init.getRailActionModule(serverWorld, module -> blocked[0] = !((dev.x341.mrw.mod.mixin.RailActionModuleAccessor) module).mrw$getRailActions().isEmpty());
		return blocked[0];
	}

	public boolean build() {
		switch (railActionType) {
			case TUNNEL_WALL:
				return createTunnelWall();
			case RAIL_WORKER_WALLS:
				return createRailWorkerWalls();
			case BRIDGE_WALL:
			default:
				return createBridgeWall();
		}
	}

	private boolean createTunnelWall() {
		return create(false, false, vector -> place(fromVector(vector)));
	}

	private boolean createBridgeWall() {
		return create(false, true, vector -> place(fromVector(vector.add(0, -1, 0))));
	}

	private boolean createRailWorkerWalls() {
		return create(false, sidesOnly, vector -> place(fromVector(vector), replace));
	}

	private void place(BlockPos blockPos) {
		place(blockPos, false);
	}

	private void place(BlockPos blockPos, boolean replace) {
		if (blacklistedPositions.contains(blockPos)) {
			return;
		}
		if (!canPlace(serverWorld, blockPos)) {
			if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
				final boolean hasBlockEntity = serverWorld.getBlockEntity(blockPos) != null;
				final boolean isBlockNode = serverWorld.getBlockState(blockPos).getBlock().data instanceof BlockNode;
				dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] wall placement rejected: pos={} hasBlockEntity={} isBlockNode={} existingState={}", blockPos, hasBlockEntity, isBlockNode, serverWorld.getBlockState(blockPos));
			}
			return;
		}
		if (replace && serverWorld.getBlockState(blockPos).isAir()) {
			return;
		}
		serverWorld.setBlockState(blockPos, state);
		blacklistedPositions.add(blockPos);
	}

	private boolean create(boolean includeMiddle, boolean sidesOnly, Consumer<Vector> consumer) {
		final long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < 2) {
			final Vector pos1 = rail.railMath.getPosition(distance, false);
			distance += INCREMENT;
			final Vector pos2 = rail.railMath.getPosition(distance, false);
			final Vector vec3 = new Vector(pos2.x - pos1.x, 0, pos2.z - pos1.z).normalize().rotateY((float) Math.PI / 2);

			for (double x = -radius; x <= radius; x += INCREMENT) {
				final Vector editPos = pos1.add(vec3.multiply(x, 0, x));
				final boolean wholeNumber = Math.abs(editPos.y - Math.round(editPos.y)) < WHOLE_NUMBER_EPSILON;
				final boolean isEdge = Math.abs(x) > radius - INCREMENT || radius == 0;
				// wallSide restricts building to one edge of the rail (LEFT = negative x offset,
				// flipped by invertWallSide to keep left/right matching the player's point of view)
				final boolean useNegativeSide = (wallSide == WallSide.LEFT) != invertWallSide;
				final boolean isSelectedEdge = wallSide == WallSide.BOTH || radius == 0 || (useNegativeSide ? x < 0 : x > 0);
				if (includeMiddle || (isEdge && isSelectedEdge)) {
					for (int y = 0; y <= height; y++) {
						if (y < height || !wholeNumber || (height == 0 && radius == 0)) {
							consumer.accept(editPos.add(0, y, 0));
						}
					}
				} else if (!sidesOnly && !isEdge) {
					consumer.accept(editPos.add(0, Math.max(0, wholeNumber ? height - 1 : height), 0));
				}
			}

			if (length - distance < INCREMENT) {
				sendProgressMessage(100);
				return true;
			}
		}

		sendProgressMessage((float) Utilities.round(100 * distance / length, 1));
		return false;
	}

	private void sendProgressMessage(float percentage) {
		final PlayerEntity playerEntity = serverWorld.getPlayerByUuid(uuid);
		if (playerEntity != null) {
			final int[] batch = RailActionBatchTracker.get(uuid);
			if (batch != null && batch[1] > 1) {
				final String message = TextHelper.translatable(railActionType.progressTranslationKey, percentage).getString() + " / (" + (batch[0] + 1) + "/" + batch[1] + ")";
				playerEntity.sendMessage(new Text(TextHelper.literal(message).data), true);
			} else {
				playerEntity.sendMessage(new Text(TextHelper.translatable(railActionType.progressTranslationKey, percentage).data), true);
			}
		}

		if (percentage >= 100) {
			RailActionBatchTracker.onComplete(uuid);
		}
	}

	private static boolean canPlace(ServerWorld serverWorld, BlockPos pos) {
		return serverWorld.getBlockEntity(pos) == null && !(serverWorld.getBlockState(pos).getBlock().data instanceof BlockNode);
	}

	private static BlockPos fromVector(Vector vector) {
		return Init.newBlockPos(vector.x, vector.y, vector.z);
	}

}
