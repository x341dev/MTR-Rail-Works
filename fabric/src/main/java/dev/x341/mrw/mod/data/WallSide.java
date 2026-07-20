package dev.x341.mrw.mod.data;

/**
 * Wall creators can build both edges of the rail or just one. The mode is stored as an int in the
 * item stack NBT ({@link #TAG_WALL_SIDE}) and cycled by sneak-using the item in the air.
 */
public final class WallSide {

	public static final String TAG_WALL_SIDE = "mrw_wall_side";

	public static final int BOTH = 0;
	public static final int LEFT = 1;
	public static final int RIGHT = 2;

	private WallSide() {
	}

	public static String getTranslationKey(int wallSide) {
		switch (wallSide) {
			case LEFT:
				return "tooltip.mrw.wall_side_left";
			case RIGHT:
				return "tooltip.mrw.wall_side_right";
			default:
				return "tooltip.mrw.wall_side_both";
		}
	}
}
