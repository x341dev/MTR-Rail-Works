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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
	private ShapeProfile shapeProfile;
	private BlockState[] shapeStates;
	private boolean[] autoSlab;
	private BlockState[] slabTopStates;
	private BlockState[] slabBottomStates;
	private final ObjectOpenHashSet<BlockPos> slabHalfPositions = new ObjectOpenHashSet<>();
	private BlockState[][] rotatedStates;

	private static final double INCREMENT = 0.1;
	// rail.railMath.getPosition() runs curved segments through spline/trig math (unlike straight
	// segments, which interpolate linearly), so a rail that is vertically flat can still come back
	// with a y like 5.999999999997 instead of an exact 6.0. Comparing that against Math.floor/ceil
	// exactly made whether the top wall block got included flicker block-to-block along a curve,
	// even though every sample is meant to be at the same whole-number height.
	private static final double WHOLE_NUMBER_EPSILON = 1e-6;
	// Cooperative work budget: each server tick is ~50ms total across all game logic, so building
	// a rail action incrementally (rather than all at once) caps how much of that a single action
	// can eat per tick, keeping long builds from stalling the whole server.
	private static final long TICK_BUDGET_MILLIS = 2;

	public RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, int wallSide) {
		this(serverWorld, serverPlayerEntity, railActionType, rail, radius, height, state, wallSide, isRailAgainstPlayerFacing(serverPlayerEntity, rail), false, false, false);
	}

	public RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, Rail rail, ShapeProfile profile, BlockState[] paletteStates, BlockPos pairStart, BlockPos pairEnd) {
		this(serverWorld, serverPlayerEntity, RailActionType.RAIL_WORKER_SHAPE, rail, ShapeProfile.SIZE / 2, 0, null, WallSide.BOTH, isRailAgainstNodeOffset(pairStart, pairEnd, rail), false, false, false);
		shapeProfile = profile;
		shapeStates = paletteStates;
		autoSlab = new boolean[paletteStates.length];
		slabTopStates = new BlockState[paletteStates.length];
		slabBottomStates = new BlockState[paletteStates.length];
		for (int i = 0; i < paletteStates.length && i < profile.getPalette().size(); i++) {
			final String entry = profile.getPalette().get(i);
			if (paletteStates[i] != null && ShapeBlocks.isAutoSlab(entry)) {
				slabTopStates[i] = ShapeBlocks.toState(ShapeBlocks.baseId(entry) + "[type=top]");
				slabBottomStates[i] = ShapeBlocks.toState(ShapeBlocks.baseId(entry) + "[type=bottom]");
				autoSlab[i] = slabTopStates[i] != null && slabBottomStates[i] != null;
			}
		}
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
		id = ThreadLocalRandom.current().nextLong();
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
			case RAIL_WORKER_SHAPE:
				return createShape();
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

	private boolean createShape() {
		final long startTime = System.currentTimeMillis();
		final int half = ShapeProfile.SIZE / 2;
		while (System.currentTimeMillis() - startTime < TICK_BUDGET_MILLIS) {
			final Vector pos1 = rail.railMath.getPosition(distance, false);
			distance += INCREMENT;
			final Vector pos2 = rail.railMath.getPosition(distance, false);
			final double tangentX = pos2.x - pos1.x;
			final double tangentZ = pos2.z - pos1.z;
			final double tangentLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);

			if (tangentLength > 1e-9) {
				final double unitX = tangentX / tangentLength;
				final double unitZ = tangentZ / tangentLength;
				// Same perpendicular as the wall builders, so left/right keeps the same convention
				final Vector perpendicular = new Vector(tangentX, 0, tangentZ).normalize().rotateY((float) Math.PI / 2);
				// Travel direction as the player clicked it, for rotating directional blocks
				final BlockRotation rotation = rotationFor(invertWallSide ? tangentX : -tangentX, invertWallSide ? tangentZ : -tangentZ);
				final int baseY = (int) Math.floor(pos1.y + WHOLE_NUMBER_EPSILON);
				final int minX = (int) Math.floor(pos1.x) - half - 1;
				final int minZ = (int) Math.floor(pos1.z) - half - 1;

				if (dev.x341.mrw.mod.MrwDebug.isEnabled() && distance <= 2 * INCREMENT) {
					dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] shape: start pos=({}, {}, {}) tangent=({}, {}) invert={} rotation={}", pos1.x, pos1.y, pos1.z, tangentX, tangentZ, invertWallSide, rotation);
				}

				// Same split as MTR's bridge: rail in the upper half of its block -> bottom slab one row up, otherwise top slab
				final boolean upperHalf = pos1.y + WHOLE_NUMBER_EPSILON - baseY >= 0.5;
				for (int blockX = minX; blockX <= minX + 2 * (half + 1); blockX++) {
					for (int blockZ = minZ; blockZ <= minZ + 2 * (half + 1); blockZ++) {
						final double deltaX = blockX + 0.5 - pos1.x;
						final double deltaZ = blockZ + 0.5 - pos1.z;
						// Only blocks whose centre is level with this rail sample, so each block lands on one column
						if (Math.abs(deltaX * unitX + deltaZ * unitZ) > INCREMENT) {
							continue;
						}
						final double offset = deltaX * perpendicular.x + deltaZ * perpendicular.z;
						if (Math.abs(offset) >= half + 0.5) {
							continue;
						}
						final int column = Math.max(0, Math.min(ShapeProfile.SIZE - 1, (int) Math.floor((invertWallSide ? -offset : offset) + half + 0.5)));
						for (int row = 0; row < ShapeProfile.SIZE; row++) {
							final byte cell = shapeProfile.getCell(column, row);
							if (cell != ShapeProfile.EMPTY && cell < shapeStates.length) {
								final BlockPos target = new BlockPos(blockX, baseY + row - ShapeProfile.RAIL_ROW, blockZ);
								if (autoSlab[cell]) {
									placeAutoSlab(target, cell, upperHalf);
								} else {
									placeShape(target, rotatedState(cell, rotation));
								}
							}
						}
					}
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

	private void placeShape(BlockPos blockPos, @Nullable BlockState blockState) {
		if (blockState == null || blacklistedPositions.contains(blockPos) || !canPlace(serverWorld, blockPos)) {
			return;
		}
		if (blockState.isAir() && serverWorld.getBlockState(blockPos).isAir()) {
			return;
		}
		serverWorld.setBlockState(blockPos, blockState);
		blacklistedPositions.add(blockPos);
	}

	private void placeAutoSlab(BlockPos blockPos, int paletteIndex, boolean upperHalf) {
		final BlockPos target = upperHalf ? new BlockPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()) : blockPos;
		final BlockPos halfKey = new BlockPos(target.getX(), target.getY() * 2 + (upperHalf ? 0 : 1), target.getZ());
		if (slabHalfPositions.contains(halfKey) || !canPlace(serverWorld, target)) {
			return;
		}
		serverWorld.setBlockState(target, upperHalf ? slabBottomStates[paletteIndex] : slabTopStates[paletteIndex]);
		slabHalfPositions.add(halfKey);
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
		while (System.currentTimeMillis() - startTime < TICK_BUDGET_MILLIS) {
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

	private static BlockRotation rotationFor(double forwardX, double forwardZ) {
		if (forwardX == 0 && forwardZ == 0) {
			return BlockRotation.NONE;
		}
		if (Math.abs(forwardX) >= Math.abs(forwardZ)) {
			return forwardX > 0 ? BlockRotation.CLOCKWISE_90 : BlockRotation.COUNTERCLOCKWISE_90;
		}
		return forwardZ > 0 ? BlockRotation.CLOCKWISE_180 : BlockRotation.NONE;
	}

	private BlockState rotatedState(int paletteIndex, BlockRotation rotation) {
		if (rotatedStates == null) {
			rotatedStates = new BlockState[4][shapeStates.length];
		}
		final int rotationIndex = rotation.ordinal();
		BlockState state = rotatedStates[rotationIndex][paletteIndex];
		if (state == null && shapeStates[paletteIndex] != null) {
			state = shapeStates[paletteIndex].rotate(rotation);
			rotatedStates[rotationIndex][paletteIndex] = state;
		}
		return state;
	}
}
