package dev.x341.mrw.mod.data;

public enum RailActionType {
	TUNNEL_WALL("gui.mtr.percentage_complete_tunnel_wall", "gui.mtr.rail_action_tunnel_wall", 0xFF666666),
	BRIDGE_WALL("gui.mrw.percentage_complete_bridge_wall", "gui.mrw.rail_action_bridge_wall", 0xFF666666),
	RAIL_WORKER_WALLS("gui.mrw.percentage_complete_rail_worker_walls", "gui.mrw.rail_action_rail_worker_walls", 0xFF666666);

	public final String progressTranslationKey;
	public final String nameTranslationKey;
	public final int color;

	RailActionType(String progressTranslationKey, String nameTranslationKey, int color) {
		this.progressTranslationKey = progressTranslationKey;
		this.nameTranslationKey = nameTranslationKey;
		this.color = color;
	}
}
