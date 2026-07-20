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
 * (bridge walls) or does not support fully (tunnel walls with a single-side mode).
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
	private final double length;
	private final BlockState state;
	private final ObjectOpenHashSet<BlockPos> blacklistedPositions = new ObjectOpenHashSet<>();

	private static final double INCREMENT = 0.1;

	public RailActionMrw(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, int wallSide) {
		id = new Random().nextLong();
		this.serverWorld = serverWorld;
		uuid = serverPlayerEntity.getUuid();
		playerName = serverPlayerEntity.getName().getString();
		this.railActionType = railActionType;
		this.rail = rail;
		this.radius = radius;
		this.height = height;
		this.wallSide = wallSide;
		this.invertWallSide = isRailAgainstPlayerFacing(serverPlayerEntity, rail);
		this.state = state;
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
		return (pos2.x - pos1.x) * forwardX + (pos2.z - pos1.z) * forwardZ < 0;
	}

	public String getDescription() {
		return TextHelper.translatable(railActionType.nameTranslationKey, playerName, Utilities.round(length, 1), state == null ? "" : TextHelper.translatable(state.getBlock().getTranslationKey()).getString()).getString();
	}

	public int getColor() {
		return railActionType.color;
	}

	public boolean build() {
		switch (railActionType) {
			case TUNNEL_WALL:
				return createTunnelWall();
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

	private void place(BlockPos blockPos) {
		if (!blacklistedPositions.contains(blockPos) && canPlace(serverWorld, blockPos)) {
			serverWorld.setBlockState(blockPos, state);
			blacklistedPositions.add(blockPos);
		}
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
				final boolean wholeNumber = Math.floor(editPos.y) == Math.ceil(editPos.y);
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
