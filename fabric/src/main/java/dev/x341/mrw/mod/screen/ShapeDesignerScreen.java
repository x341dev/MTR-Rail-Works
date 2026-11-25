package dev.x341.mrw.mod.screen;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.client.ShapeDesignerIcons;
import dev.x341.mrw.mod.data.BlockCatalog;
import dev.x341.mrw.mod.data.ShapeBlocks;
import dev.x341.mrw.mod.data.ShapeProfile;
import dev.x341.mrw.mod.packet.PacketUpdateShapeDesigner;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;

import java.util.ArrayList;
import java.util.List;

public class ShapeDesignerScreen extends ScreenExtension {

    private static final int CELL = 20;
    private static final int GRID = CELL * ShapeProfile.SIZE;
    private static final int COLUMN_GAP = 12;
    private static final int SLOT = 18;
    private static final int LIST_COLUMNS = 8;
    private static final int LIST_ROWS = 8;
    private static final int SIDE_WIDTH = SLOT * LIST_COLUMNS;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTENT_HEIGHT = GRID + 8 + BUTTON_HEIGHT + 3 + BUTTON_HEIGHT;
    private static final int ERASER = -1;

    private final BlockPos blockPos;
    private final BlockEntityShapeDesigner blockEntity;
    private final ShapeProfile profile;
    private final List<String> filtered = new ArrayList<>();

    private int brush = ERASER;
    private int scrollRow = 0;
    private String message = "";
    private String hoveredId = null;
    private TextFieldWidgetExtension searchField;

    public ShapeDesignerScreen(BlockPos blockPos, BlockEntityShapeDesigner blockEntity) {
        super(TextHelper.translatable("gui.mrw.shape_designer_title"));
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;
        final CompoundTag tag = new CompoundTag();
        blockEntity.getProfile().writeTag(tag);
        profile = ShapeProfile.readTag(tag);
    }

    private int gridLeft() {
        return (getWidthMapped() - (GRID + COLUMN_GAP + SIDE_WIDTH)) / 2;
    }

    private int gridTop() {
        return (getHeightMapped() - CONTENT_HEIGHT) / 2 + 8;
    }

    private int sideLeft() {
        return gridLeft() + GRID + COLUMN_GAP;
    }

    private int searchY() {
        return gridTop() + SLOT + 20;
    }

    private int listY() {
        return searchY() + 22;
    }

    @Override
    protected void init2() {
        searchField = new TextFieldWidgetExtension(sideLeft(), searchY(), SIDE_WIDTH, 18, 40, TextCase.LOWER, "[^a-z0-9_:./ -]", TextHelper.translatable("gui.mrw.shape_designer_search").getString());
        searchField.setChangedListener2(this::applyFilter);
        addChild(new ClickableWidget(searchField));
        applyFilter("");

        final int buttonsY = gridTop() + GRID + 8;
        final int smallWidth = (GRID - 2 * 3) / 3;
        addButton(gridLeft(), buttonsY, GRID, "gui.mrw.shape_designer_hand", () -> {
            final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
            final String id = player == null ? null : ShapeBlocks.fromStack(player.getMainHandStack());
            if (id == null) {
                message = TextHelper.translatable("gui.mrw.shape_designer_hand_invalid").getString();
            } else {
                addToPalette(id);
            }
        });
        final int row2Y = buttonsY + BUTTON_HEIGHT + 3;
        addButton(gridLeft(), row2Y, smallWidth, "gui.mrw.shape_designer_air", () -> addToPalette(ShapeBlocks.AIR));
        addButton(gridLeft() + smallWidth + 3, row2Y, smallWidth, "gui.mrw.shape_designer_clear", () -> {
            profile.clear();
            brush = ERASER;
        });
        addButton(gridLeft() + 2 * (smallWidth + 3), row2Y, smallWidth, "gui.mrw.shape_designer_save", this::saveAndClose);
    }

    private void addButton(int x, int y, int width, String translationKey, Runnable onPress) {
        addChild(new ClickableWidget(new ButtonWidgetExtension(x, y, width, BUTTON_HEIGHT, TextHelper.translatable(translationKey), button -> onPress.run())));
    }

    private void applyFilter(String text) {
        final String query = text.trim();
        filtered.clear();
        for (final String id : BlockCatalog.getIds()) {
            if (query.isEmpty() || id.contains(query)) {
                filtered.add(id);
            }
        }
        scrollRow = 0;
    }

    private int maxScrollRow() {
        return Math.max(0, (filtered.size() + LIST_COLUMNS - 1) / LIST_COLUMNS - LIST_ROWS);
    }

    private void addToPalette(String id) {
        if (ShapeBlocks.toState(id) == null) {
            message = TextHelper.translatable("gui.mrw.shape_designer_invalid_block").getString();
            return;
        }
        final int index = profile.getOrAddPaletteIndex(id);
        if (index < 0) {
            message = TextHelper.translatable("gui.mrw.shape_designer_palette_full").getString();
        } else {
            brush = index;
            message = "";
        }
    }

    private void saveAndClose() {
        blockEntity.setProfile(profile);
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateShapeDesigner(blockPos, profile));
        onClose2();
    }

    private static int slotColor(String id) {
        return ShapeBlocks.toStack(id) != null ? 0xFF373737 : colorOf(id);
    }

    private static void addIcon(String id, int x, int y) {
        final ItemStack stack = ShapeBlocks.toStack(id);
        if (stack != null) {
            ShapeDesignerIcons.add(stack, x, y);
        }
    }

    private static void drawInitial(GraphicsHolder graphicsHolder, String id, int centerX, int y) {
        if (ShapeBlocks.toStack(id) == null) {
            graphicsHolder.drawCenteredText(initial(id), centerX, y, 0xFFFFFFFF);
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        renderBackground(graphicsHolder);
        final int left = gridLeft();
        final int top = gridTop();
        final int sideLeft = sideLeft();
        final int listY = listY();

        hoveredId = null;
        ShapeDesignerIcons.clear();
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(left - 4, top - 4, left + GRID + 4, top + GRID + 4, 0xC0101010);
        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final int x = left + column * CELL;
                final int y = top + (ShapeProfile.SIZE - 1 - row) * CELL;
                final String id = profile.getBlockId(column, row);
                // The centre column (the rail's column) gets a lighter border
                guiDrawing.drawRectangle(x, y, x + CELL, y + CELL, column == ShapeProfile.SIZE / 2 ? 0xFF8899AA : 0xFF000000);
                guiDrawing.drawRectangle(x + 1, y + 1, x + CELL - 1, y + CELL - 1, id == null ? 0xFF303030 : slotColor(id));
                if (id != null) {
                    addIcon(id, x + 2, y + 2);
                }
            }
        }

        for (int i = 0; i < ShapeProfile.MAX_PALETTE; i++) {
            final int x = sideLeft + i * SLOT;
            final String id = i < profile.getPalette().size() ? profile.getPalette().get(i) : null;
            guiDrawing.drawRectangle(x, top, x + SLOT, top + SLOT, i == brush ? 0xFFFFFFFF : 0xFF555555);
            guiDrawing.drawRectangle(x + 1, top + 1, x + SLOT - 1, top + SLOT - 1, id == null ? 0xFF202020 : slotColor(id));
            if (id != null) {
                addIcon(id, x + 1, top + 1);
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
            final boolean hovered = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
            if (hovered) {
                hoveredId = id;
            }
            guiDrawing.drawRectangle(x, y, x + SLOT, y + SLOT, hovered ? 0xFFFFFFFF : 0xFF000000);
            guiDrawing.drawRectangle(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, slotColor(id));
            addIcon(id, x + 1, y + 1);
        }
        guiDrawing.finishDrawingRectangle();

        graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.shape_designer_title"), left + GRID / 2, top - 16, 0xFFFFFFFF);
        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final String id = profile.getBlockId(column, row);
                if (id != null) {
                    drawInitial(graphicsHolder, id, left + column * CELL + CELL / 2, top + (ShapeProfile.SIZE - 1 - row) * CELL + 6);
                }
            }
        }
        for (int i = 0; i < profile.getPalette().size(); i++) {
            drawInitial(graphicsHolder, profile.getPalette().get(i), sideLeft + i * SLOT + SLOT / 2, top + 5);
        }
        for (int slot = 0; slot < LIST_COLUMNS * LIST_ROWS; slot++) {
            final int index = scrollRow * LIST_COLUMNS + slot;
            if (index >= filtered.size()) {
                break;
            }
            drawInitial(graphicsHolder, filtered.get(index), sideLeft + (slot % LIST_COLUMNS) * SLOT + SLOT / 2, listY + (slot / LIST_COLUMNS) * SLOT + 5);
        }

        final String selected = brush >= 0 && brush < profile.getPalette().size() ? profile.getPalette().get(brush) : TextHelper.translatable("gui.mrw.shape_designer_eraser").getString();
        graphicsHolder.drawText(selected, sideLeft, top + SLOT + 6, 0xFFFFFF55, false, GraphicsHolder.getDefaultLight());
        final int infoY = listY + LIST_ROWS * SLOT + 4;
        graphicsHolder.drawText(hoveredId != null ? hoveredId : filtered.size() + " / " + BlockCatalog.getIds().size(), sideLeft, infoY, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
        if (!message.isEmpty()) {
            graphicsHolder.drawText(message, sideLeft, infoY + 12, 0xFFFF5555, false, GraphicsHolder.getDefaultLight());
        }

        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        final int column = (int) Math.floor((mouseX - gridLeft()) / CELL);
        final int rowFromTop = (int) Math.floor((mouseY - gridTop()) / CELL);
        if (mouseX >= gridLeft() && mouseY >= gridTop() && column < ShapeProfile.SIZE && rowFromTop < ShapeProfile.SIZE) {
            profile.setCell(column, ShapeProfile.SIZE - 1 - rowFromTop, button == 1 || brush == ERASER ? ShapeProfile.EMPTY : (byte) brush);
            return true;
        }
        if (mouseX >= sideLeft() && mouseX < sideLeft() + SIDE_WIDTH && mouseY >= gridTop() && mouseY < gridTop() + SLOT) {
            final int index = (int) Math.floor((mouseX - sideLeft()) / SLOT);
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

    private static String initial(String id) {
        final int start = id.indexOf(':') + 1;
        return id.substring(start, Math.min(id.length(), start + 1)).toUpperCase();
    }

    private static int colorOf(String id) {
        if (ShapeBlocks.AIR.equals(id)) {
            return 0xFF9FD3FF;
        }
        final int hash = id.hashCode();
        return 0xFF000000 | (80 + (hash & 0x7F)) << 16 | (80 + ((hash >> 8) & 0x7F)) << 8 | (80 + ((hash >> 16) & 0x7F));
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}