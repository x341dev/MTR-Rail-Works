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
 * outside this screen (sneak-click a block in the world) — the floor/wall block lines here are
 * read-only, taken from the item's NBT at the moment the screen opens.
 *
 * <p>Built directly on {@link ScreenExtension} (the thin vanilla-widget mapping layer) rather than
 * MTR's own {@code MTRScreenBase}/{@code WidgetShorterSlider} dashboard framework, since widgets
 * only render when added with {@link #addChild} (vanilla {@code addDrawableChild}).
 *
 * <p>Every checkbox's name is drawn manually (never via the vanilla inline label) so its color can
 * be controlled directly: mode checkboxes get a centered line below them (a horizontal-row inline
 * label would overlap the next checkbox for longer names like "Walls + Ceiling"), and Replace mode
 * gets a label to the right, centered as one group with its checkbox. Unavailable options
 * (currently just Replace, gated on Walls Only) stay visible and are dimmed via
 * {@link RailWorkerMode#hasWallsOnly} rather than being hidden outright.
 *
 * <p>{@link CheckboxWidgetExtension}'s convenience constructors take a {@code showMessage} flag,
 * not an initial checked state (confirmed by decompiling the mapping layer) — the widget always
 * starts unchecked, so every checkbox here is synced to its real initial value via
 * {@link CheckboxWidgetExtension#setChecked} right after construction.
 */
public class RailWorkerConfigScreen extends ScreenExtension {

	// Layout constants shared by init2() (widget placement) and render() (panel + labels). The
	// panel is vertically centered on screen and sized around its content: mode row -> mode labels
	// -> replace row -> width slider -> height slider -> floor/wall block lines.
	private static final int PANEL_HALF_WIDTH = 235;
	private static final int PANEL_TOP_PADDING = 20;
	private static final int PANEL_BOTTOM_PADDING = 20;
	private static final int COLUMN_WIDTH = 112;
	private static final int CHECKBOX_WIDTH = 100;
	private static final int CHECKBOX_HEIGHT = 22;
	private static final int SLIDER_WIDTH = 200;
	private static final int SLIDER_HEIGHT = 20;
	private static final int REPLACE_LABEL_GAP = 8;
	private static final int TEXT_LINE_HEIGHT = 9;
	private static final int COLOR_LABEL = 0xFFFFFFFF;
	private static final int COLOR_LABEL_DISABLED = 0xFFAAAAAA;

	// Vertical offsets of each row, relative to topY() (the mode checkbox row).
	private static final int MODE_LABEL_Y_OFFSET = CHECKBOX_HEIGHT + 8;
	private static final int REPLACE_Y_OFFSET = 58;
	private static final int WIDTH_SLIDER_Y_OFFSET = 100;
	private static final int HEIGHT_SLIDER_Y_OFFSET = 132;
	private static final int FLOOR_BLOCK_Y_OFFSET = HEIGHT_SLIDER_Y_OFFSET + SLIDER_HEIGHT + 18;
	private static final int WALL_BLOCK_Y_OFFSET = FLOOR_BLOCK_Y_OFFSET + 14;
	private static final int PANEL_BOTTOM_Y_OFFSET = WALL_BLOCK_Y_OFFSET + TEXT_LINE_HEIGHT + PANEL_BOTTOM_PADDING;

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
		final CompoundTag tag = itemStack.getOrCreateTag();
		mode = tag.getInt(ItemRailWorker.TAG_MODE);
		replace = tag.getBoolean(ItemRailWorker.TAG_REPLACE);
		width = ItemRailWorker.getWidth(tag);
		height = ItemRailWorker.getHeight(tag);
		floorState = ItemRailWorker.getFloorState(tag);
		wallState = ItemRailWorker.getWallState(tag);
	}

	private int topY() {
		final int panelHeight = PANEL_BOTTOM_Y_OFFSET + PANEL_TOP_PADDING;
		return (getHeightMapped() - panelHeight) / 2 + PANEL_TOP_PADDING;
	}

	private int modeColumnCenterX(int column) {
		final int centerX = getWidthMapped() / 2;
		return centerX - COLUMN_WIDTH * 2 + COLUMN_WIDTH * column + COLUMN_WIDTH / 2;
	}

	private int replaceLabelWidth() {
		return GraphicsHolder.getTextWidth(TextHelper.translatable("gui.mrw.rail_worker_replace"));
	}

	private int replaceGroupLeftX() {
		return getWidthMapped() / 2 - (CHECKBOX_HEIGHT + REPLACE_LABEL_GAP + replaceLabelWidth()) / 2;
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
		final int replaceY = topY + REPLACE_Y_OFFSET;
		replaceCheckbox = addCheckbox(replaceGroupLeftX(), replaceY, CHECKBOX_HEIGHT, replace, checked -> replace = checked);
		replaceCheckbox.setActiveMapped(RailWorkerMode.hasWallsOnly(mode));

		final int widthSliderY = topY + WIDTH_SLIDER_Y_OFFSET;
		widthSlider = new StepSlider(centerX - SLIDER_WIDTH / 2, widthSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, 4, widthToIndex(width), index -> TextHelper.translatable("gui.mrw.rail_worker_width", 3 + 2 * index).getString());
		addChild(new ClickableWidget(widthSlider));

		final int heightSliderY = topY + HEIGHT_SLIDER_Y_OFFSET;
		heightSlider = new StepSlider(centerX - SLIDER_WIDTH / 2, heightSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, 8, height - 1, index -> TextHelper.translatable("gui.mrw.rail_worker_height", index + 1).getString());
		addChild(new ClickableWidget(heightSlider));
	}

	/**
	 * {@link CheckboxWidgetExtension}'s constructors never set the initial checked state (see class
	 * doc) — the widget always starts unchecked, so {@code checked} is applied afterwards via
	 * {@link CheckboxWidgetExtension#setChecked}, which is safe to call pre-open (no click sound,
	 * routes straight through to {@code onPress} so callers observe the correct starting value).
	 */
	private CheckboxWidgetExtension addCheckbox(int x, int y, int width, boolean checked, Consumer<Boolean> onPress) {
		final CheckboxWidgetExtension checkbox = new CheckboxWidgetExtension(x, y, width, CHECKBOX_HEIGHT, false, onPress);
		addChild(new ClickableWidget(checkbox));
		if (checked) {
			checkbox.setChecked(true);
		}
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
		final int panelTop = topY - PANEL_TOP_PADDING;
		final int panelBottom = topY + PANEL_BOTTOM_Y_OFFSET;
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		// drawRectangle takes two corners (x1, y1, x2, y2), not (x, y, width, height).
		guiDrawing.drawRectangle(centerX - PANEL_HALF_WIDTH, panelTop, centerX + PANEL_HALF_WIDTH, panelBottom, 0xC0101010);
		guiDrawing.finishDrawingRectangle();

		super.render(graphicsHolder, mouseX, mouseY, delta);

		for (int column = 0; column < MODE_LABEL_KEYS.length; column++) {
			graphicsHolder.drawCenteredText(TextHelper.translatable(MODE_LABEL_KEYS[column]), modeColumnCenterX(column), topY + MODE_LABEL_Y_OFFSET, COLOR_LABEL);
		}

		final boolean replaceActive = RailWorkerMode.hasWallsOnly(mode);
		final int replaceLabelCenterX = replaceGroupLeftX() + CHECKBOX_HEIGHT + REPLACE_LABEL_GAP + replaceLabelWidth() / 2;
		final int replaceLabelY = topY + REPLACE_Y_OFFSET + (CHECKBOX_HEIGHT - TEXT_LINE_HEIGHT) / 2;
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_replace"), replaceLabelCenterX, replaceLabelY, replaceActive ? COLOR_LABEL : COLOR_LABEL_DISABLED);

		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_floor_block", blockName(floorState)), centerX, topY + FLOOR_BLOCK_Y_OFFSET, COLOR_LABEL);
		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.rail_worker_wall_block", blockName(wallState)), centerX, topY + WALL_BLOCK_Y_OFFSET, COLOR_LABEL);
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
