package dev.x341.mrw.mod.screen;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.item.ItemRailWorker;
import dev.x341.mrw.mod.packet.PacketUpdateRailWorkerConfig;
import dev.x341.mrw.mod.ui.ScreenBase;
import dev.x341.mrw.mod.ui.components.StepSlider;
import dev.x341.mrw.mod.ui.components.Toggle;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.mapper.TextHelper;

import javax.annotation.Nullable;


public class RailWorkerConfigScreen extends ScreenBase {

	private static final int PANEL_WIDTH = 454;
	private static final int PANEL_HEIGHT = 184;
	private static final int CELL_WIDTH = 104;
	private static final int CELL_GAP = 6;
	private static final int SLIDER_WIDTH = 260;
	private static final int ROW_MODE = 0;
	private static final int ROW_REPLACE = 28;
	private static final int ROW_WIDTH = 56;
	private static final int ROW_HEIGHT = 84;
	private static final int ROW_FLOOR = 116;
	private static final int ROW_WALL = 128;
	private static final int WIDTH_STEPS = 4;
	private static final int HEIGHT_STEPS = 8;

	private static final String[] MODE_LABEL_KEYS = {
			"gui.mrw.rail_worker_mode_bridge",
			"gui.mrw.rail_worker_mode_tunnel",
			"gui.mrw.rail_worker_mode_walls_only",
			"gui.mrw.rail_worker_mode_walls_ceiling"
	};

	@Nullable
	private final BlockState floorState;
	@Nullable
	private final BlockState wallState;
	private final int initialWidthIndex;
	private final int initialHeightIndex;

	private int mode;
	private boolean replace;

	private Toggle[] modeToggles;
	private Toggle replaceToggle;
	private StepSlider widthSlider;
	private StepSlider heightSlider;

	public RailWorkerConfigScreen(ItemStack itemStack) {
		super("gui.mrw.rail_worker_title", PANEL_WIDTH, PANEL_HEIGHT);
		final CompoundTag tag = itemStack.getOrCreateTag();
		mode = tag.getInt(ItemRailWorker.TAG_MODE);
		replace = tag.getBoolean(ItemRailWorker.TAG_REPLACE);
		initialWidthIndex = Math.max(0, Math.min(WIDTH_STEPS - 1, (ItemRailWorker.getWidth(tag) - 3) / 2));
		initialHeightIndex = Math.max(0, Math.min(HEIGHT_STEPS - 1, ItemRailWorker.getHeight(tag) - 1));
		floorState = ItemRailWorker.getFloorState(tag);
		wallState = ItemRailWorker.getWallState(tag);
	}

	@Override
	protected void initContent() {
		final int top = contentTop();
		final int rowLeft = centeredX(4 * CELL_WIDTH + 3 * CELL_GAP);

		modeToggles = new Toggle[MODE_LABEL_KEYS.length];
		for (int column = 0; column < MODE_LABEL_KEYS.length; column++) {
			final int bit = 1 << column;
			modeToggles[column] = addToggle(rowLeft + column * (CELL_WIDTH + CELL_GAP), top + ROW_MODE, CELL_WIDTH, TextHelper.translatable(MODE_LABEL_KEYS[column]).getString(), (mode & bit) != 0, checked -> setModeBit(bit, checked));
		}

		replaceToggle = addToggle(centeredX(CELL_WIDTH), top + ROW_REPLACE, CELL_WIDTH, TextHelper.translatable("gui.mrw.rail_worker_replace").getString(), replace, checked -> replace = checked);
		replaceToggle.setEnabled(RailWorkerMode.hasWallsOnly(mode));

		widthSlider = addSlider(centeredX(SLIDER_WIDTH), top + ROW_WIDTH, SLIDER_WIDTH, WIDTH_STEPS, initialWidthIndex, index -> TextHelper.translatable("gui.mrw.rail_worker_width", 3 + 2 * index).getString());
		heightSlider = addSlider(centeredX(SLIDER_WIDTH), top + ROW_HEIGHT, SLIDER_WIDTH, HEIGHT_STEPS, initialHeightIndex, index -> TextHelper.translatable("gui.mrw.rail_worker_height", index + 1).getString());

		final int centerX = panelLeft() + PANEL_WIDTH / 2;
		addLabel(centerX, top + ROW_FLOOR, true, () -> TextHelper.translatable("gui.mrw.rail_worker_floor_block", blockName(floorState)).getString());
		addLabel(centerX, top + ROW_WALL, true, () -> TextHelper.translatable("gui.mrw.rail_worker_wall_block", blockName(wallState)).getString());
	}

	private void setModeBit(int bit, boolean checked) {
		if (checked) {
			mode |= bit;
			if (bit == RailWorkerMode.WALLS_ONLY) {
				mode &= ~RailWorkerMode.WALLS_CEILING;
				modeToggles[Integer.numberOfTrailingZeros(RailWorkerMode.WALLS_CEILING)].setChecked(false);
			} else if (bit == RailWorkerMode.WALLS_CEILING) {
				mode &= ~RailWorkerMode.WALLS_ONLY;
				modeToggles[Integer.numberOfTrailingZeros(RailWorkerMode.WALLS_ONLY)].setChecked(false);
			}
		} else {
			mode &= ~bit;
		}
		replaceToggle.setEnabled(RailWorkerMode.hasWallsOnly(mode));
	}

	private static String blockName(@Nullable BlockState state) {
		return state == null ? TextHelper.translatable("gui.mrw.rail_worker_block_not_set").getString() : TextHelper.translatable(state.getBlock().getTranslationKey()).getString();
	}

	@Override
	public void onClose2() {
		InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateRailWorkerConfig(mode, replace, 3 + 2 * widthSlider.getIndex(), heightSlider.getIndex() + 1));
		super.onClose2();
	}
}
