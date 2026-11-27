package dev.x341.mrw.mod.screen;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.client.ShapeDesignerIcons;
import dev.x341.mrw.mod.data.BlockCatalog;
import dev.x341.mrw.mod.data.ShapeBlocks;
import dev.x341.mrw.mod.data.ShapeProfile;
import dev.x341.mrw.mod.packet.PacketUpdateShapeDesigner;
import dev.x341.mrw.mod.ui.UiStyle;
import dev.x341.mrw.mod.ui.ScreenBase;
import dev.x341.mrw.mod.ui.components.Button;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Left: the 9x9 grid (buttons underneath). Right: the 16-slot palette, rotation buttons, a search
 * box (by name or id) and the block list. Left click/drag paints the selected entry, right click/drag
 * empties cells (leaves the world untouched), middle click picks the entry of a cell. The Air entry
 * carves blocks away; an empty cell does nothing. Row 0 is the lowest row, drawn at the bottom.
 */
public class ShapeDesignerScreen extends ScreenBase {

    private static final int CELL = 20;
    private static final int GRID = CELL * ShapeProfile.SIZE;
    private static final int COLUMN_GAP = 12;
    private static final int SLOT = 18;
    private static final int PALETTE_COLUMNS = 8;
    private static final int PALETTE_ROWS = ShapeProfile.MAX_PALETTE / PALETTE_COLUMNS;
    private static final int LIST_COLUMNS = 8;
    private static final int LIST_ROWS = 6;
    private static final int SIDE_WIDTH = SLOT * LIST_COLUMNS;
    private static final int PANEL_WIDTH = 2 * (PADDING + 4) + GRID + COLUMN_GAP + SIDE_WIDTH;
    private static final int PANEL_HEIGHT = HEADER_HEIGHT + PADDING + 4 + 255 + PADDING;
    private static final int ERASER = -1;

    private final BlockPos blockPos;
    private final BlockEntityShapeDesigner blockEntity;
    private final ShapeProfile profile;
    private final List<String> filtered = new ArrayList<>();

    private int brush = ERASER;
    private int scrollRow = 0;
    private String message = "";
    private String hoveredEntry = null;
    private TextFieldWidgetExtension searchField;
    private Button propertyButton;
    private Button valueButton;
    private int propertyIndex = 0;

    private int lastColumn = -1;
    private int lastRow = -1;

    public ShapeDesignerScreen(BlockPos blockPos, BlockEntityShapeDesigner blockEntity) {
        super("gui.mrw.shape_designer_title", PANEL_WIDTH, PANEL_HEIGHT);
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;

        final CompoundTag tag = new CompoundTag();
        blockEntity.getProfile().writeTag(tag);
        profile = ShapeProfile.readTag(tag);
    }

    private int gridLeft() {
        return panelLeft() + PADDING + 4;
    }

    private int gridTop() {
        return contentTop() + 4;
    }

    private int sideLeft() {
        return gridLeft() + GRID + COLUMN_GAP;
    }

    private int selectedY() {
        return gridTop() + PALETTE_ROWS * SLOT + 4;
    }

    private int rotateY() {
        return selectedY() + 12;
    }

    private int searchY() {
        return rotateY() + COMPONENT_HEIGHT + 3;
    }

    private int listY() {
        return searchY() + 22;
    }

    @Override
    protected void initContent() {
        searchField = new TextFieldWidgetExtension(sideLeft(), searchY(), SIDE_WIDTH, 18, 40, TextCase.LOWER, "\\p{Cntrl}", TextHelper.translatable("gui.mrw.shape_designer_search").getString());
        searchField.setChangedListener2(this::applyFilter);
        addChild(new ClickableWidget(searchField));
        applyFilter("");

        final int buttonsY = gridTop() + GRID + 8;
        final int smallWidth = (GRID - 2 * 3) / 3;
        addKeyButton(gridLeft(), buttonsY, GRID, "gui.mrw.shape_designer_hand", () -> {
            final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
            final ItemStack stack = player == null ? null : player.getMainHandStack();
            final String id = stack == null ? null : ShapeBlocks.fromStack(stack);
            if (id == null) {
                message = TextHelper.translatable("gui.mrw.shape_designer_hand_invalid").getString();
            } else {
                addToPalette(id);
            }
        });
        final int row2Y = buttonsY + COMPONENT_HEIGHT + 3;
        addKeyButton(gridLeft(), row2Y, smallWidth, "gui.mrw.shape_designer_air", () -> addToPalette(ShapeBlocks.AIR));
        addKeyButton(gridLeft() + smallWidth + 3, row2Y, smallWidth, "gui.mrw.shape_designer_clear", () -> {
            profile.clear();
            brush = ERASER;
        });
        addKeyButton(gridLeft() + 2 * (smallWidth + 3), row2Y, smallWidth, "gui.mrw.shape_designer_save", this::saveAndClose);

        final int propertyWidth = (SIDE_WIDTH - 6) * 2 / 5;
        final int resetWidth = SIDE_WIDTH - 6 - 2 * propertyWidth;
        propertyButton = addButton(sideLeft(), rotateY(), propertyWidth, "-", this::nextProperty);
        valueButton = addButton(sideLeft() + propertyWidth + 3, rotateY(), propertyWidth, "-", this::nextValue);
        addKeyButton(sideLeft() + 2 * (propertyWidth + 3), rotateY(), resetWidth, "gui.mrw.shape_designer_rotate_reset", this::resetBrush);
    }

    private void addKeyButton(int x, int y, int width, String translationKey, Runnable onPress) {
        addButton(x, y, width, TextHelper.translatable(translationKey).getString(), onPress);
    }

    private void applyFilter(String text) {
        final String query = text.trim().toLowerCase();
        filtered.clear();
        for (final String id : BlockCatalog.getIds()) {
            if (BlockCatalog.matches(id, query)) {
                filtered.add(id);
            }
        }
        scrollRow = 0;
    }

    private int maxScrollRow() {
        return Math.max(0, (filtered.size() + LIST_COLUMNS - 1) / LIST_COLUMNS - LIST_ROWS);
    }

    private String brushEntry() {
        return brush >= 0 && brush < profile.getPalette().size() ? profile.getPalette().get(brush) : null;
    }

    private void addToPalette(String entry) {
        if (ShapeBlocks.toState(entry) == null) {
            message = TextHelper.translatable("gui.mrw.shape_designer_invalid_block").getString();
            return;
        }
        final int index = profile.getOrAddPaletteIndex(entry);
        if (index < 0) {
            message = TextHelper.translatable("gui.mrw.shape_designer_palette_full").getString();
        } else {
            brush = index;
            message = "";
        }
    }

    private String selectedProperty() {
        final String entry = brushEntry();
        if (entry == null || ShapeBlocks.AIR.equals(entry)) {
            return null;
        }
        final List<String> keys = ShapeBlocks.propertyKeys(entry);
        return keys.isEmpty() ? null : keys.get(Math.floorMod(propertyIndex, keys.size()));
    }

    private void nextProperty() {
        if (selectedProperty() == null) {
            message = TextHelper.translatable(brushEntry() == null ? "gui.mrw.shape_designer_rotate_select" : "gui.mrw.shape_designer_rotate_none").getString();
            return;
        }
        propertyIndex++;
    }

    private void nextValue() {
        final String entry = brushEntry();
        final String key = selectedProperty();
        if (entry == null || key == null) {
            message = TextHelper.translatable(entry == null ? "gui.mrw.shape_designer_rotate_select" : "gui.mrw.shape_designer_rotate_none").getString();
            return;
        }
        final List<String> values = ShapeBlocks.propertyValues(entry, key);
        final int next = (values.indexOf(ShapeBlocks.propertyValue(entry, key)) + 1) % Math.max(1, values.size());
        final String changed = values.isEmpty() ? null : ShapeBlocks.withProperty(entry, key, values.get(next));
        if (changed == null) {
            message = TextHelper.translatable("gui.mrw.shape_designer_rotate_none").getString();
        } else {
            addToPalette(changed);
        }
    }

    private void resetBrush() {
        final String entry = brushEntry();
        if (entry != null) {
            addToPalette(ShapeBlocks.baseId(entry));
        }
    }

    private void saveAndClose() {
        profile.compact();
        blockEntity.setProfile(profile);
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateShapeDesigner(blockPos, profile));
        onClose2();
    }

    private static int slotColor(String entry) {
        return ShapeBlocks.toStack(entry) != null ? 0xFF373737 : colorOf(entry);
    }

    private static void addIcon(String entry, int x, int y) {
        final ItemStack stack = ShapeBlocks.toStack(entry);
        if (stack != null) {
            ShapeDesignerIcons.add(stack, x, y);
        }
    }

    private static void drawInitial(GraphicsHolder graphicsHolder, String entry, int centerX, int y) {
        if (ShapeBlocks.toStack(entry) == null) {
            graphicsHolder.drawCenteredText(initial(entry), centerX, y, 0xFFFFFFFF);
        }
    }

    private static boolean over(int mouseX, int mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    @Override
    protected void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
        final int left = gridLeft();
        final int top = gridTop();
        final int sideLeft = sideLeft();
        final int listY = listY();

        hoveredEntry = null;
        ShapeDesignerIcons.clear();
        final String property = selectedProperty();
        propertyButton.setLabel(property == null ? "-" : property);
        valueButton.setLabel(property == null ? "-" : ShapeBlocks.propertyValue(brushEntry(), property));
        UiStyle.roundedBox(graphicsHolder, left - 4, top - 4, GRID + 8, GRID + 8, UiStyle.RADIUS, 0xC0101010, UiStyle.PANEL_BORDER);
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final int x = left + column * CELL;
                final int y = top + (ShapeProfile.SIZE - 1 - row) * CELL;
                final String entry = profile.getBlockId(column, row);
                final boolean railCell = column == ShapeProfile.SIZE / 2 && row == ShapeProfile.RAIL_ROW;
                final int border = railCell ? 0xFFFF9900 : column == ShapeProfile.SIZE / 2 ? 0xFF8899AA : 0xFF000000;
                final int inset = railCell ? 3 : 1;
                guiDrawing.drawRectangle(x, y, x + CELL, y + CELL, border);
                guiDrawing.drawRectangle(x + inset, y + inset, x + CELL - inset, y + CELL - inset, entry == null ? 0xFF303030 : slotColor(entry));
                if (entry != null) {
                    addIcon(entry, x + 2, y + 2);
                    if (over(mouseX, mouseY, x, y, CELL)) {
                        hoveredEntry = entry;
                    }
                }
            }
        }

        for (int i = 0; i < ShapeProfile.MAX_PALETTE; i++) {
            final int x = sideLeft + (i % PALETTE_COLUMNS) * SLOT;
            final int y = top + (i / PALETTE_COLUMNS) * SLOT;
            final String entry = i < profile.getPalette().size() ? profile.getPalette().get(i) : null;
            guiDrawing.drawRectangle(x, y, x + SLOT, y + SLOT, i == brush ? 0xFFFFFFFF : 0xFF555555);
            guiDrawing.drawRectangle(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, entry == null ? 0xFF202020 : slotColor(entry));
            if (entry != null) {
                addIcon(entry, x + 1, y + 1);
                if (over(mouseX, mouseY, x, y, SLOT)) {
                    hoveredEntry = entry;
                }
            }
        }

        for (int slot = 0; slot < LIST_COLUMNS * LIST_ROWS; slot++) {
            final int index = scrollRow * LIST_COLUMNS + slot;
            if (index >= filtered.size()) {
                break;
            }
            final String id = filtered.get(index);
            final int x = sideLeft + (slot % LIST_COLUMNS) * SLOT;
            final int y = listY + (slot / LIST_COLUMNS) * SLOT;
            final boolean hovered = over(mouseX, mouseY, x, y, SLOT);
            if (hovered) {
                hoveredEntry = id;
            }
            guiDrawing.drawRectangle(x, y, x + SLOT, y + SLOT, hovered ? 0xFFFFFFFF : 0xFF000000);
            guiDrawing.drawRectangle(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, slotColor(id));
            addIcon(id, x + 1, y + 1);
        }
        guiDrawing.finishDrawingRectangle();

        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final String entry = profile.getBlockId(column, row);
                final int x = left + column * CELL + CELL / 2;
                final int y = top + (ShapeProfile.SIZE - 1 - row) * CELL + 6;
                if (column == ShapeProfile.SIZE / 2 && row == ShapeProfile.RAIL_ROW && entry == null) {
                    graphicsHolder.drawCenteredText("=", x, y, 0xFFFF9900);
                }
                if (entry != null) {
                    drawInitial(graphicsHolder, entry, x, y);
                }
            }
        }
        for (int i = 0; i < profile.getPalette().size(); i++) {
            drawInitial(graphicsHolder, profile.getPalette().get(i), sideLeft + (i % PALETTE_COLUMNS) * SLOT + SLOT / 2, top + (i / PALETTE_COLUMNS) * SLOT + 5);
        }
        for (int slot = 0; slot < LIST_COLUMNS * LIST_ROWS; slot++) {
            final int index = scrollRow * LIST_COLUMNS + slot;
            if (index >= filtered.size()) {
                break;
            }
            drawInitial(graphicsHolder, filtered.get(index), sideLeft + (slot % LIST_COLUMNS) * SLOT + SLOT / 2, listY + (slot / LIST_COLUMNS) * SLOT + 5);
        }

        final String selectedEntry = brushEntry();
        final String selected = selectedEntry == null ? TextHelper.translatable("gui.mrw.shape_designer_eraser").getString() : describe(selectedEntry);
        graphicsHolder.drawText(selected, sideLeft, selectedY(), 0xFFFFFF55, false, GraphicsHolder.getDefaultLight());

        final int infoY = listY + LIST_ROWS * SLOT + 4;
        if (hoveredEntry != null) {
            graphicsHolder.drawText(describe(hoveredEntry), sideLeft, infoY, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(hoveredEntry, sideLeft, infoY + 11, 0xFFAAAAAA, false, GraphicsHolder.getDefaultLight());
        } else {
            graphicsHolder.drawText(filtered.size() + " / " + BlockCatalog.getIds().size(), sideLeft, infoY, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
            if (!message.isEmpty()) {
                graphicsHolder.drawText(message, sideLeft, infoY + 11, 0xFFFF5555, false, GraphicsHolder.getDefaultLight());
            }
        }
        graphicsHolder.drawText(TextHelper.translatable("gui.mrw.shape_designer_rail_marker").getString(), sideLeft, infoY + 22, 0xFFFF9900, false, GraphicsHolder.getDefaultLight());
    }

    private static String describe(String entry) {
        final String detail = ShapeBlocks.detail(entry);
        return ShapeBlocks.getName(entry) + (detail.isEmpty() ? "" : " [" + detail + "]");
    }

    private int[] cellAt(double mouseX, double mouseY) {
        final int column = (int) Math.floor((mouseX - gridLeft()) / CELL);
        final int rowFromTop = (int) Math.floor((mouseY - gridTop()) / CELL);
        if (mouseX < gridLeft() || mouseY < gridTop() || column >= ShapeProfile.SIZE || rowFromTop >= ShapeProfile.SIZE) {
            return null;
        }
        return new int[]{column, ShapeProfile.SIZE - 1 - rowFromTop};
    }

    private void paint(int column, int row, int button) {
        profile.setCell(column, row, button == 1 || brush == ERASER ? ShapeProfile.EMPTY : (byte) brush);
    }

    private void paintLine(int fromColumn, int fromRow, int toColumn, int toRow, int button) {
        int column = fromColumn;
        int row = fromRow;
        final int dx = Math.abs(toColumn - fromColumn);
        final int dy = Math.abs(toRow - fromRow);
        final int stepX = fromColumn < toColumn ? 1 : -1;
        final int stepY = fromRow < toRow ? 1 : -1;
        int error = dx - dy;
        while (true) {
            paint(column, row, button);
            if (column == toColumn && row == toRow) {
                break;
            }
            final int doubled = 2 * error;
            if (doubled > -dy) {
                error -= dy;
                column += stepX;
            }
            if (doubled < dx) {
                error += dx;
                row += stepY;
            }
        }
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        final int[] cell = cellAt(mouseX, mouseY);
        if (cell != null) {
            if (button == 2) {
                final String entry = profile.getBlockId(cell[0], cell[1]);
                brush = entry == null ? ERASER : profile.getPalette().indexOf(entry);
            } else {
                paint(cell[0], cell[1], button);
                lastColumn = cell[0];
                lastRow = cell[1];
            }
            return true;
        }
        if (mouseX >= sideLeft() && mouseX < sideLeft() + SIDE_WIDTH && mouseY >= gridTop() && mouseY < gridTop() + PALETTE_ROWS * SLOT) {
            final int index = (int) Math.floor((mouseY - gridTop()) / SLOT) * PALETTE_COLUMNS + (int) Math.floor((mouseX - sideLeft()) / SLOT);
            if (index < profile.getPalette().size()) {
                brush = index;
            }
            return true;
        }
        if (inList(mouseX, mouseY)) {
            final int slot = (int) Math.floor((mouseY - listY()) / SLOT) * LIST_COLUMNS + (int) Math.floor((mouseX - sideLeft()) / SLOT);
            final int index = scrollRow * LIST_COLUMNS + slot;
            if (index < filtered.size()) {
                addToPalette(filtered.get(index));
            }
            return true;
        }
        return super.mouseClicked2(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 2 && lastColumn >= 0) {
            final int[] cell = cellAt(mouseX, mouseY);
            if (cell != null) {
                paintLine(lastColumn, lastRow, cell[0], cell[1], button);
                lastColumn = cell[0];
                lastRow = cell[1];
            } else {
                lastColumn = -1;
            }
            return true;
        }
        return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased2(double mouseX, double mouseY, int button) {
        lastColumn = -1;
        lastRow = -1;
        return super.mouseReleased2(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
        if (inList(mouseX, mouseY)) {
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled2(mouseX, mouseY, amount);
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= sideLeft() && mouseX < sideLeft() + SIDE_WIDTH && mouseY >= listY() && mouseY < listY() + LIST_ROWS * SLOT;
    }

    private static String initial(String entry) {
        final String id = ShapeBlocks.baseId(entry);
        final int start = id.indexOf(':') + 1;
        return id.substring(start, Math.min(id.length(), start + 1)).toUpperCase();
    }

    private static int colorOf(String entry) {
        final String id = ShapeBlocks.baseId(entry);
        if (ShapeBlocks.AIR.equals(id)) {
            return 0xFF9FD3FF;
        }
        final int hash = id.hashCode();
        return 0xFF000000 | (80 + (hash & 0x7F)) << 16 | (80 + ((hash >> 8) & 0x7F)) << 8 | (80 + ((hash >> 16) & 0x7F));
    }
}