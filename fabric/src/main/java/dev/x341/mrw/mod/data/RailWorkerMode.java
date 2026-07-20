package dev.x341.mrw.mod.data;

/**
 * Rail Worker's build mode is a bitmask stored as an int in the item stack NBT
 * ({@link dev.x341.mrw.mod.item.ItemRailWorker#TAG_MODE}). Bridge and Tunnel are independent bits;
 * Walls Only and Walls + Ceiling are mutually exclusive (the config screen enforces this, and
 * {@link #sanitize} re-enforces it server-side so a modified client can't set both).
 */
public final class RailWorkerMode {

	public static final int BRIDGE = 1;
	public static final int TUNNEL = 1 << 1;
	public static final int WALLS_ONLY = 1 << 2;
	public static final int WALLS_CEILING = 1 << 3;

	private RailWorkerMode() {
	}

	public static boolean hasBridge(int mode) {
		return (mode & BRIDGE) != 0;
	}

	public static boolean hasTunnel(int mode) {
		return (mode & TUNNEL) != 0;
	}

	public static boolean hasWallsOnly(int mode) {
		return (mode & WALLS_ONLY) != 0;
	}

	public static boolean hasWallsCeiling(int mode) {
		return (mode & WALLS_CEILING) != 0;
	}

	public static boolean hasAnyWalls(int mode) {
		return hasWallsOnly(mode) || hasWallsCeiling(mode);
	}

	/** Drops Walls + Ceiling whenever Walls Only is also set, so the two stay mutually exclusive. */
	public static int sanitize(int mode) {
		return hasWallsOnly(mode) ? mode & ~WALLS_CEILING : mode;
	}
}
