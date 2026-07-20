package dev.x341.mrw.mod.screen;

import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.item.ItemRailWorker;
import dev.x341.mrw.mod.packet.PacketUpdateRailWorkerConfig;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.SliderWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.holder.Text;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Rail Worker's config screen: opened by sneak-using the item in the air (see
 * {@link ItemRailWorker#useWithoutResult}). Every field here mirrors the item's own NBT; changes
 * are sent back to the server in one {@link PacketUpdateRailWorkerConfig} on close rather than per
 * click, so a half-toggled mode never becomes visible mid-edit. Block selection itself happens
 * outside this screen (sneak-click a block in the world) — the two rows here are read-only.
 *
 * <p>Built directly on {@link ScreenExtension} (the thin vanilla-widget mapping layer) rather than
 * MTR's own {@code MTRScreenBase}/{@code WidgetShorterSlider} dashboard framework, since widgets
 * only render when added with {@link #addChild} (vanilla {@code addDrawableChild}).
 *
 * <p>Checkboxes are given an empty vanilla label and their name is drawn as a separate centered
 * line below each one instead — in a horizontal row, the vanilla inline label (drawn immediately
 * to the right of the box, ignoring the widget's own width) overlaps the next checkbox for longer
 * names like "Walls + Ceiling". Unavailable options (currently just Replace, gated on Walls Only)
 * stay visible and are dimmed via {@link CheckboxWidgetExtension#setActiveMapped} plus a greyed
 * label, rather than being hidden outright.
 */
public class RailWorkerConfigScreen extends ScreenExtension {

	// Layout constants shared by init2() (widget placement) and render() (panel + labels). The
	// panel is anchored to the bottom of the screen rather than centered, and every vertical gap
	// below is double the original spacing (228px panel -> 456px) so content spreads out to fill
	// the extra room instead of leaving it empty.
	private static final int PANEL_HALF_WIDTH = 210;
	private static final int PANEL_HEIGHT = 456;
	private static final int BOTTOM_MARGIN = 20;
	private static final int COLUMN_WIDTH = 100;
	private static final int CHECKBOX_WIDTH = 90;
	private static final int CHECKBOX_HEIGHT = 20;
	private static final int SLIDER_WIDTH = 180;
	private static final int SLIDER_HEIGHT = 18;
	private static final int COLOR_LABEL = 0xFFFFFFFF;
	private static final int COLOR_LABEL_DISABLED = 0xFFAAAAAA;

	private static final String[] MODE_LABEL_KEYS = {
			"gui.mrw.rail_worker_mode_bridge",
			"gui.mrw.rail_worker_mode_tunnel",
			"gui.mrw.rail_worker_mode_walls_only",
			"gui.mrw.rail_worker_mode_walls_ceiling"
	};

	private final ItemStack itemStack;

	private int mode;
	private boolean replace;
	private int width;
	private int height;

	@Nullable
	private CheckboxWidgetExtension wallsOnlyCheckbox;
	@Nullable
	private CheckboxWidgetExtension wallsCeilingCheckbox;
	@Nullable
	private CheckboxWidgetExtension replaceCheckbox;
	@Nullable
	private StepSlider widthSlider;
	@Nullable
	private StepSlider heightSlider;

	public RailWorkerConfigScreen(ItemStack itemStack) {
		super(TextHelper.translatable("gui.mrw.rail_worker_title"));
		this.itemStack = itemStack;
		final CompoundTag tag = itemStack.getOrCreateTag();
		mode = tag.getInt(ItemRailWorker.TAG_MODE);
		replace = tag.getBoolean(ItemRailWorker.TAG_REPLACE);
		width = ItemRailWorker.getWidth(tag);
		height = ItemRailWorker.getHeight(tag);
	}

	private int topY() {
		// panelBottom (topY + 388) sits BOTTOM_MARGIN above the bottom of the screen
		return getHeightMapped() - BOTTOM_MARGIN - 388;
	}

	private int modeColumnCenterX(int column) {
		final int centerX = getWidthMapped() / 2;
		return centerX - COLUMN_WIDTH * 2 + COLUMN_WIDTH * column + COLUMN_WIDTH / 2;
	}

	@Override
	protected void init2() {
		final int topY = topY();
		final int checkboxRowY = topY;

		for (int column = 0; column < MODE_LABEL_KEYS.length; column++) {
			final int columnCenterX = modeColumnCenterX(column);
			final int bit = 1 << column;
			final CheckboxWidgetExtension checkbox = addCheckbox(columnCenterX - CHECKBOX_WIDTH / 2, checkboxRowY, CHECKBOX_WIDTH, (mode & bit) != 0, checked -> setModeBit(bit, checked));
			if (bit == RailWorkerMode.WALLS_ONLY) {
				wallsOnlyCheckbox = checkbox;
			} else if (bit == RailWorkerMode.WALLS_CEILING) {
				wallsCeilingCheckbox = checkbox;
			}
		}

		final int centerX = getWidthMapped() / 2;
		final int replaceY = topY + 100;
		replaceCheckbox = addCheckbox(centerX - 80, replaceY, 160, replace, checked -> replace = checked);
		replaceCheckbox.setActiveMapped(RailWorkerMode.hasWallsOnly(mode));

		final int widthSliderY = topY + 208;
		widthSlider = new StepSlider(centerX - SLIDER_WIDTH / 2, widthSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, 4, widthToIndex(width), index -> TextHelper.translatable("gui.mrw.rail_worker_width", 3 + 2 * index).getString());
		addChild(new ClickableWidget(widthSlider));

		final int heightSliderY = topY + 260;
		heightSlider = new StepSlider(centerX - SLIDER_WIDTH / 2, heightSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, 8, height - 1, index -> TextHelper.translatable("gui.mrw.rail_worker_height", index + 1).getString());
		addChild(new ClickableWidget(heightSlider));
	}

	private CheckboxWidgetExtension addCheckbox(int x, int y, int width, boolean checked, Consumer<Boolean> onPress) {
		final CheckboxWidgetExtension checkbox = new CheckboxWidgetExtension(x, y, width, CHECKBOX_HEIGHT, TextHelper.literal(""), checked, onPress);
		addChild(new ClickableWidget(checkbox));
		return checkbox;
	}

	private void setModeBit(int bit, boolean checked) {
		if (checked) {
			mode |= bit;
			if (bit == RailWorkerMode.WALLS_ONLY && wallsCeilingCheckbox != null) {
				mode &= ~RailWorkerMode.WALLS_CEILING;
				wallsCeilingCheckbox.setChecked(false);
			} else if (bit == RailWorkerMode.WALLS_CEILING && wallsOnlyCheckbox != null) {
				mode &= ~RailWorkerMode.WALLS_ONLY;
				wallsOnlyCheckbox.setChecked(false);
			}
		} else {
			mode &= ~bit;
		}
		if (replaceCheckbox != null) {
			replaceCheckbox.setActiveMapped(RailWorkerMode.hasWallsOnly(mode));
		}
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		renderBackground(graphicsHolder);

		final int centerX = getWidthMapped() / 2;
		final int topY = topY();
		final int panelTop = topY - 68;
		final int panelBottom = topY + 388;
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(centerX - PANEL_HALF_WIDTH, panelTop, PANEL_HALF_WIDTH * 2, PANEL_HEIGHT, 0xC0101010);
		guiDrawing.finishDrawingRectangle();

		super.render(graphicsHolder, mouseX, mouseY, delta);

		for (int column = 0; column < MODE_LABEL_KEYS.length; column++) {
			graphicsHolder.drawCenteredText(TextHelper.translatable(MODE_LABEL_KEYS[column]), modeColumnCenterX(column), topY + CHECKBOX_HEIGHT + 8, COLOR_LABEL);
		}
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_replace"), centerX, topY + 100 + CHECKBOX_HEIGHT + 8, RailWorkerMode.hasWallsOnly(mode) ? COLOR_LABEL : COLOR_LABEL_DISABLED);

		final CompoundTag tag = itemStack.getOrCreateTag();
		final BlockState floorState = ItemRailWorker.getFloorState(tag);
		final BlockState wallState = ItemRailWorker.getWallState(tag);
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_title"), centerX, topY - 40, COLOR_LABEL);
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_floor_block", blockName(floorState)), centerX, topY + 320, COLOR_LABEL);
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_wall_block", blockName(wallState)), centerX, topY + 348, COLOR_LABEL);
	}

	private static String blockName(@Nullable BlockState state) {
		return state == null ? TextHelper.translatable("gui.mrw.rail_worker_block_not_set").getString() : TextHelper.translatable(state.getBlock().getTranslationKey()).getString();
	}

	private static int widthToIndex(int width) {
		return Math.max(0, Math.min(3, (width - 3) / 2));
	}

	@Override
	public void onClose2() {
		if (widthSlider != null) {
			width = 3 + 2 * widthSlider.getIndex();
		}
		if (heightSlider != null) {
			height = heightSlider.getIndex() + 1;
		}
		InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateRailWorkerConfig(mode, replace, width, height));
		super.onClose2();
	}

	/**
	 * A discrete-step vanilla {@link SliderWidgetExtension}: its underlying value is the normal
	 * 0.0-1.0 double vanilla sliders use, snapped to one of {@code steps} evenly spaced positions.
	 */
	private static final class StepSlider extends SliderWidgetExtension {

		private final int steps;
		private final IntFunction<String> labelProvider;

		StepSlider(int x, int y, int width, int height, int steps, int initialIndex, IntFunction<String> labelProvider) {
			super(x, y, width, height);
			this.steps = steps;
			this.labelProvider = labelProvider;
			setValueMapped(indexToValue(initialIndex));
			updateMessage2();
		}

		int getIndex() {
			return Math.round((float) (getValueMapped() * (steps - 1)));
		}

		private double indexToValue(int index) {
			return steps <= 1 ? 0 : (double) index / (steps - 1);
		}

		@Override
		protected void updateMessage2() {
			setMessage2(new Text(TextHelper.literal(labelProvider.apply(getIndex())).data));
		}

		@Override
		protected void applyValue2() {
			setValueMapped(indexToValue(getIndex()));
		}
	}
}
