package dev.x341.mrw.mod.client;

import org.mtr.core.data.Position;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;

/**
 * BFS across the client's rail node graph ({@link MinecraftClientData#positionsToRail}), turning
 * two clicked {@link BlockPos}es into an ordered list of directly-connected rail sub-segments.
 * Shared by anything that needs to resolve a possibly multi-hop path between two nodes, e.g.
 * {@link dev.x341.mrw.mod.mixin.ItemNodeModifierSelectableBlockBaseMixin} and
 * {@link dev.x341.mrw.mod.item.ItemRailWorker}.
 */
public final class RailPathFinder {

	private RailPathFinder() {
	}

	@Nullable
	public static ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> findPath(BlockPos startBlockPos, BlockPos endBlockPos) {
		final Position startPosition = Init.blockPosToPosition(startBlockPos);
		final Position endPosition = Init.blockPosToPosition(endBlockPos);

		if (!MinecraftClientData.getInstance().positionsToRail.containsKey(startPosition)) {
			return null;
		}

		final Object2ObjectOpenHashMap<Position, Position> parentMap = new Object2ObjectOpenHashMap<>();
		final ObjectArrayList<Position> queue = new ObjectArrayList<>();
		queue.add(startPosition);
		parentMap.put(startPosition, startPosition);

		boolean found = false;
		int queueIndex = 0;
		while (queueIndex < queue.size()) {
			final Position current = queue.get(queueIndex++);
			if (current.equals(endPosition)) {
				found = true;
				break;
			}
			MinecraftClientData.getInstance().positionsToRail.getOrDefault(current, new Object2ObjectOpenHashMap<>()).keySet().forEach(neighbor -> {
				if (!parentMap.containsKey(neighbor)) {
					parentMap.put(neighbor, current);
					queue.add(neighbor);
				}
			});
		}

		if (!found) {
			return null;
		}

		final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> path = new ObjectArrayList<>();
		Position current = endPosition;
		while (!parentMap.get(current).equals(current)) {
			final Position previous = parentMap.get(current);
			path.add(0, new ObjectObjectImmutablePair<>(Init.positionToBlockPos(previous), Init.positionToBlockPos(current)));
			current = previous;
		}
		return path;
	}
}
