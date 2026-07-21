package dev.x341.mrw.mod.packet;

import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.data.WallSide;
import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;
import org.mtr.mod.item.ItemNodeModifierBase;

import javax.annotation.Nullable;

/**
 * The one packet a Rail Worker operation sends: pair 1's resolved rail-segment path and raw click
 * endpoints (for the coordinate-delta wall side — see {@code RailActionMrw#isRailAgainstNodeOffset}),
 * plus an optional second pair for the 2-segment batched flow. Mode/replace/width/height/blocks are
 * deliberately not carried here — the server reads them from the held item's own already-synced,
 * already-validated NBT, the same way {@link PacketApplyRailAction} already trusts the item's own
 * radius/height over anything a packet could claim.
 */
public final class PacketApplyRailWorkerBuild extends PacketHandler {

	private final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path;
	private final BlockPos pair1Start;
	private final BlockPos pair1End;
	@Nullable
	private final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair2Path;
	@Nullable
	private final BlockPos pair2Start;
	@Nullable
	private final BlockPos pair2End;

	public PacketApplyRailWorkerBuild(PacketBufferReceiver packetBufferReceiver) {
		pair1Path = readPath(packetBufferReceiver);
		pair1Start = BlockPos.fromLong(packetBufferReceiver.readLong());
		pair1End = BlockPos.fromLong(packetBufferReceiver.readLong());
		if (packetBufferReceiver.readBoolean()) {
			pair2Path = readPath(packetBufferReceiver);
			pair2Start = BlockPos.fromLong(packetBufferReceiver.readLong());
			pair2End = BlockPos.fromLong(packetBufferReceiver.readLong());
		} else {
			pair2Path = null;
			pair2Start = null;
			pair2End = null;
		}
	}

	public PacketApplyRailWorkerBuild(ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path, BlockPos pair1Start, BlockPos pair1End) {
		this(pair1Path, pair1Start, pair1End, null, null, null);
	}

	public PacketApplyRailWorkerBuild(ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair1Path, BlockPos pair1Start, BlockPos pair1End, @Nullable ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> pair2Path, @Nullable BlockPos pair2Start, @Nullable BlockPos pair2End) {
		this.pair1Path = pair1Path;
		this.pair1Start = pair1Start;
		this.pair1End = pair1End;
		this.pair2Path = pair2Path;
		this.pair2Start = pair2Start;
		this.pair2End = pair2End;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		writePath(packetBufferSender, pair1Path);
		packetBufferSender.writeLong(pair1Start.asLong());
		packetBufferSender.writeLong(pair1End.asLong());
		final boolean hasPair2 = pair2Path != null;
		packetBufferSender.writeBoolean(hasPair2);
		if (hasPair2) {
			writePath(packetBufferSender, pair2Path);
			packetBufferSender.writeLong(pair2Start.asLong());
			packetBufferSender.writeLong(pair2End.asLong());
		}
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final ItemStack itemStack = serverPlayerEntity.getMainHandStack();
		if (!(itemStack.getItem().data instanceof ItemRailWorker)) {
			return;
		}

		final CompoundTag tag = itemStack.getOrCreateTag();
		final int mode = RailWorkerMode.sanitize(tag.getInt(ItemRailWorker.TAG_MODE));
		final boolean replace = tag.getBoolean(ItemRailWorker.TAG_REPLACE);
		final int width = ItemRailWorker.getWidth(tag);
		final int radius = width / 2;
		final int height = ItemRailWorker.getHeight(tag);
		final BlockState floorState = ItemRailWorker.getFloorState(tag);
		final BlockState wallState = ItemRailWorker.getWallState(tag);
		// Walls + Ceiling degrades to Walls Only whenever the ceiling would sit below head height
		final boolean sidesOnly = RailWorkerMode.hasWallsOnly(mode) || (RailWorkerMode.hasWallsCeiling(mode) && height < 4);
		final boolean anyWalls = RailWorkerMode.hasAnyWalls(mode);

		if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
			dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] Rail Worker build: mode={} replace={} width={} height={} sidesOnly={} anyWalls={} pair1=({}->{}, {} segments) pair2={}",
					mode, replace, width, height, sidesOnly, anyWalls, pair1Start, pair1End, pair1Path.size(), pair2Path == null ? "none" : (pair2Start + "->" + pair2End + ", " + pair2Path.size() + " segments"));
		}

		// A single selected pair has no "other side" to leave open, so it walls both edges. Once a
		// second pair is involved, each pair is restricted to one side, and the two are mirrored
		// (opposite nominal sides) rather than both resolving the same way: pair1/pair2 share the
		// same "forward" reference direction (both starts were clicked before either end), so a
		// shared nominal side would put both walls on the same absolute side of their own track —
		// correct for one pair but leaving the other's wall facing the middle, between the tracks,
		// instead of outward. Mirroring the nominal side puts both walls on the outward-facing side
		// for the common case of two parallel tracks — but only if pair1 is nominally "left" of
		// pair2 to begin with. Which pair the player selects first has no relation to which track
		// is physically on the left, so the mirrored side must be picked based on pair2's actual
		// position relative to pair1, not hardcoded, or a first-pair-on-the-right selection ends up
		// with both walls facing inward instead of outward.
		final int pair1WallSide;
		final int pair2WallSide;
		if (pair2Path == null) {
			pair1WallSide = WallSide.BOTH;
			pair2WallSide = WallSide.BOTH;
		} else {
			// Same "left" definition RailActionMrw#isRailAgainstNodeOffset resolves WallSide.LEFT to:
			// pair1's own click direction rotated 90 degrees. If pair2 sits on that side of pair1,
			// pair1's outward (away-from-pair2) side is actually the nominal right, and pair2's is
			// the nominal left; otherwise it's the other way around (the common-case assumption the
			// old hardcoded assignment made).
			final Vector forward = new Vector(pair1End.getX() - pair1Start.getX(), 0, pair1End.getZ() - pair1Start.getZ());
			final Vector left = forward.normalize().rotateY((float) Math.PI / 2);
			final double toPair2X = pair2Start.getX() - pair1Start.getX();
			final double toPair2Z = pair2Start.getZ() - pair1Start.getZ();
			final boolean pair2IsLeftOfPair1 = left.x * toPair2X + left.z * toPair2Z > 0;
			pair1WallSide = pair2IsLeftOfPair1 ? WallSide.RIGHT : WallSide.LEFT;
			pair2WallSide = pair2IsLeftOfPair1 ? WallSide.LEFT : WallSide.RIGHT;
		}

		applyPair(serverPlayerEntity, pair1Path, pair1Start, pair1End, mode, replace, radius, height, floorState, wallState, sidesOnly, anyWalls, pair1WallSide);
		if (pair2Path != null) {
			applyPair(serverPlayerEntity, pair2Path, pair2Start, pair2End, mode, replace, radius, height, floorState, wallState, sidesOnly, anyWalls, pair2WallSide);
		}
	}

	private static void applyPair(ServerPlayerEntity serverPlayerEntity, ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path, BlockPos pairStart, BlockPos pairEnd, int mode, boolean replace, int radius, int height, @Nullable BlockState floorState, @Nullable BlockState wallState, boolean sidesOnly, boolean anyWalls, int wallSide) {
		final World world = serverPlayerEntity.getEntityWorld();
		for (final ObjectObjectImmutablePair<BlockPos, BlockPos> segment : path) {
			ItemNodeModifierBase.getRail(world, segment.left(), segment.right(), serverPlayerEntity, rail -> {
				if (RailWorkerMode.hasBridge(mode) && floorState != null) {
					Init.getRailActionModule(serverPlayerEntity.getServerWorld(), railActionModule -> railActionModule.markRailForBridge(rail, serverPlayerEntity, radius, floorState));
				}
				if (RailWorkerMode.hasTunnel(mode)) {
					Init.getRailActionModule(serverPlayerEntity.getServerWorld(), railActionModule -> railActionModule.markRailForTunnel(rail, serverPlayerEntity, radius, height));
				}
				if (anyWalls && wallState != null) {
					final boolean waitForVanillaTunnelQueue = RailWorkerMode.hasTunnel(mode);
					dev.x341.mrw.mod.Init.getRailActionModule(serverPlayerEntity.getServerWorld(), railActionModuleMrw -> railActionModuleMrw.markRailForRailWorkerWalls(rail, serverPlayerEntity, radius, height, wallState, sidesOnly, replace, pairStart, pairEnd, wallSide, waitForVanillaTunnelQueue));
				}
			});
		}
	}

	private static ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> readPath(PacketBufferReceiver packetBufferReceiver) {
		final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = new ObjectArrayList<>();
		final int count = packetBufferReceiver.readInt();
		for (int i = 0; i < count; i++) {
			final BlockPos start = BlockPos.fromLong(packetBufferReceiver.readLong());
			final BlockPos end = BlockPos.fromLong(packetBufferReceiver.readLong());
			path.add(new ObjectObjectImmutablePair<>(start, end));
		}
		return path;
	}

	private static void writePath(PacketBufferSender packetBufferSender, ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path) {
		packetBufferSender.writeInt(path.size());
		for (final ObjectObjectImmutablePair<BlockPos, BlockPos> pair : path) {
			packetBufferSender.writeLong(pair.left().asLong());
			packetBufferSender.writeLong(pair.right().asLong());
		}
	}
}
