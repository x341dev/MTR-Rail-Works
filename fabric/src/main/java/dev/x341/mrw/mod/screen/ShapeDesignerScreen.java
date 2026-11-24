package dev.x341.mrw.mod.screen;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import dev.x341.mrw.mod.client.InitClient;
import dev.x341.mrw.mod.data.ShapeBlocks;
import dev.x341.mrw.mod.data.ShapeProfile;
import dev.x341.mrw.mod.packet.PacketUpdateShapeDesigner;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.tool.TextCase;

import java.util.Locale;

public class ShapeDesignerScreen extends ScreenExtension {

    private static final int CELL = 20;
    private static final int GRID = CELL * ShapeProfile.EMPTY   ;
    private static final int PALETTE_WIDTH = 150;
    private static final int PALETTE_ROW = 14;
    private static final int GAP = 16;
    private static final int ERASER = -1;

    private final BlockPos blockPos;
    private final BlockEntityShapeDesigner blockEntity;
    private final ShapeProfile profile;

    private int brush = ERASER;
    private String message = "";
    private TextFieldWidgetExtension idField;

    public ShapeDesignerScreen(BlockPos blockPos, BlockEntityShapeDesigner blockEntity) {
        super(TextHelper.translatable("gui.mrw.shape_designer_title"));
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;

        final CompoundTag tag = new CompoundTag();
        blockEntity.getProfile().writeTag(tag);
        profile = ShapeProfile.readTag(tag);
    }

    private int gridLeft() {
        return (getWidthMapped() - (GRID + GAP + PALETTE_WIDTH)) / 2;
    }

    private int gridTop() {
        return (getHeightMapped() - GRID) / 2;
    }

    private int paletteLeft() {
        return gridLeft() + GRID + GAP;
    }

    @Override
    protected void init2() {
        final int x = paletteLeft();
        final int fieldY = gridTop() + (ShapeProfile.MAX_PALETTE + 2) * PALETTE_ROW + 6;

        idField = new TextFieldWidgetExtension(x, fieldY, PALETTE_WIDTH, 18, 80, TextCase.DEFAULT, "minecraft:stone", "");
        addChild(new ClickableWidget(idField));

        addButton(x, fieldY + 22, 72, "gui.mrw.shape_designer_add", () -> addToPalette(ShapeBlocks.normalize(idField.getText2())));
        addButton(x + 76, fieldY + 22, 74, "gui.mrw.shape_designer_air", () -> addToPalette(ShapeBlocks.AIR));
        addButton(x, fieldY + 44, PALETTE_WIDTH, "gui.mrw.shape_designer_hand", () -> {
            final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
            final String id = player == null ? null : ShapeBlocks.fromStack(player.getMainHandStack());
            if (id == null) {
                message = TextHelper.translatable("gui.mrw.shape_designer_hand_invalid").getString();
            } else {
                addToPalette(id);
            }
        });
        addButton(x, fieldY + 66, 72, "gui.mrw.shape_designer_clear", () -> {
            profile.clear();
            brush = ERASER;
        });
        addButton(x + 76, fieldY + 66, 74, "gui.mrw.shape_designer_save", this::saveAndClose);
    }

    private void addButton(int x, int y, int width, String translationKey, Runnable onPress) {
        addChild(new ClickableWidget(new ButtonWidgetExtension(x, y, width, 20, TextHelper.translatable(translationKey), button -> onPress.run())));
    }

    private void addToPalette(String id) {
        if (id == null || ShapeBlocks.toState(id) == null) {
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

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        renderBackground(graphicsHolder);
        final int left = gridLeft();
        final int top = gridTop();
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(left - 4, top - 4, left + GRID + 4, top + GRID + 4, 0xC0101010);
        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final int x = left + column * CELL;
                final int y = top + (ShapeProfile.SIZE - 1 - row) * CELL;
                final String id = profile.getBlockId(column, row);

                final int border = column == ShapeProfile.SIZE / 2 ? 0xFF8899AA : 0xFF000000;
                guiDrawing.drawRectangle(x, y, x + CELL, y + CELL, border);
                guiDrawing.drawRectangle(x + 1, y + 1, x + CELL - 1, y + CELL - 1, id == null ? 0xFF303030 : colorOf(id));
            }
        }
        for (int i = 0; i < profile.getPalette().size(); i++) {
            final int y = paletteRowY(i);
            guiDrawing.drawRectangle(paletteLeft() - 2, y - 1, paletteLeft() + PALETTE_WIDTH, y + PALETTE_ROW - 1, i == brush ? 0xFFFFFFFF : 0x80000000);
            guiDrawing.drawRectangle(paletteLeft(), y, paletteLeft() + 10, y + 10, colorOf(profile.getPalette().get(i)));
        }
        guiDrawing.finishDrawingRectangle();

        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final String id = profile.getBlockId(column, row);
                if (id != null) {
                    graphicsHolder.drawCenteredText(id.substring(id.indexOf(':') + 1, id.indexOf(':') + 2).toUpperCase(), left + column * CELL + CELL / 2, top + (ShapeProfile.SIZE - 1 - row) * CELL + 6, 0xFFFFFFFF);
                }
            }
        }
        graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mrw.shape_designer_title"), left + GRID / 2, top - 16, 0xFFFFFFFF);
        for (int i = 0; i < profile.getPalette().size(); i++) {
            graphicsHolder.drawText(profile.getPalette().get(i), paletteLeft() + 14, paletteRowY(i) + 1, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
        }
        if (!message.isEmpty()) {
            graphicsHolder.drawText(message, paletteLeft(), top + GRID + 8, 0xFFFF5555, false, GraphicsHolder.getDefaultLight());
        }

        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    private int paletteRowY(int index) {
        return gridTop() + index * PALETTE_ROW;
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        final int column = (int) Math.floor((mouseX - gridLeft()) / CELL);
        final int rowFromTop = (int) Math.floor((mouseY - gridTop()) / CELL);
        if (mouseX >= gridLeft() && mouseY >= gridTop() && column < ShapeProfile.SIZE && rowFromTop < ShapeProfile.SIZE) {
            final int row = ShapeProfile.SIZE - 1 - rowFromTop;
            profile.setCell(column, row, button == 1 || brush == ERASER ? ShapeProfile.EMPTY : (byte) brush);
            return true;
        }
        if (mouseX >= paletteLeft() - 2 && mouseX < paletteLeft() + PALETTE_WIDTH) {
            final int index = (int) Math.floor((mouseY - gridTop()) / PALETTE_ROW);
            if (mouseY >= gridTop() && index >= 0 && index < profile.getPalette().size()) {
                brush = index;
                return true;
            }
        }
        return super.mouseClicked2(mouseX, mouseY, button);
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
