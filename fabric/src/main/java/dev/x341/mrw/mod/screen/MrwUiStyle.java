package dev.x341.mrw.mod.screen;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public final class MrwUiStyle {

    public static final int DIM = 0xB0101418;
    public static final int PANEL = 0xE01A1E24;
    public static final int PANEL_BORDER = 0xFF3A424C;
    public static final int HEADER = 0xFF0E1115;
    public static final int ACCENT = 0xFFE0B020;
    public static final int SECTION = 0x40FFFFFF;
    public static final int BUTTON = 0xFF2C333C;
    public static final int BUTTON_HOVER = 0xFF3C4753;
    public static final int BUTTON_DISABLED = 0xFF20252B;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFA0A0A0;
    public static final int RADIUS = 3;

    private MrwUiStyle() {
    }

    public static void fillRounded(GraphicsHolder graphicsHolder, int x, int y, int width, int height, int radius, int color) {
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        fill(guiDrawing, x, y, width, height, radius, color);
        guiDrawing.finishDrawingRectangle();
    }

    public static void outlineRounded(GraphicsHolder graphicsHolder, int x, int y, int width, int height, int radius, int color) {
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        outline(guiDrawing, x, y, width, height, radius, color);
        guiDrawing.finishDrawingRectangle();
    }

    public static void roundedBox(GraphicsHolder graphicsHolder, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        fill(guiDrawing, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), fillColor);
        outline(guiDrawing, x, y, width, height, radius, borderColor);
        guiDrawing.finishDrawingRectangle();
    }

    public static void rectangle(GraphicsHolder graphicsHolder, int x1, int y1, int x2, int y2, int color) {
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(x1, y1, x2, y2, color);
        guiDrawing.finishDrawingRectangle();
    }

    public static void text(GraphicsHolder graphicsHolder, String text, int x, int y, int color) {
        graphicsHolder.drawText(text, x, y, color, true, GraphicsHolder.getDefaultLight());
    }

    public static void heading(GraphicsHolder graphicsHolder, String text, int x, int y, int lineWidth) {
        text(graphicsHolder, text, x, y, ACCENT);
        rectangle(graphicsHolder, x, y + 10, x + lineWidth, y + 11, SECTION);
    }

    private static int inset(int radius, int row, int height) {
        final int distance = Math.min(row, height - row - 1);
        if (radius <= 0 || distance >= radius) {
            return 0;
        }
        final double dy = radius - distance - 0.5;
        return (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
    }

    private static void fill(GuiDrawing guiDrawing, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        final int r = Math.min(radius, Math.min(width, height) / 2);
        for (int row = 0; row < r; row++) {
            final int in = inset(r, row, height);
            guiDrawing.drawRectangle(x + in, y + row, x + width - in, y + row + 1, color);
            guiDrawing.drawRectangle(x + in, y + height - 1 - row, x + width - in, y + height - row, color);
        }
        if (height > 2 * r) {
            guiDrawing.drawRectangle(x, y + r, x + width, y + height - r, color);
        }
    }

    private static void outline(GuiDrawing guiDrawing, int x, int y, int width, int height, int radius, int color) {
        if (width < 4 || height < 4) {
            return;
        }
        final int r = Math.min(radius, Math.min(width, height) / 2 - 1);
        for (int row = 0; row < r; row++) {
            final int outer = inset(r, row, height);
            if (row == 0) {
                guiDrawing.drawRectangle(x + outer, y, x + width - outer, y + 1, color);
                guiDrawing.drawRectangle(x + outer, y + height - 1, x + width - outer, y + height, color);
            } else {
                final int inner = 1 + inset(Math.max(0, r - 1), row - 1, height - 2);
                drawRow(guiDrawing, x, y + row, width, outer, inner, color);
                drawRow(guiDrawing, x, y + height - 1 - row, width, outer, inner, color);
            }
        }
        if (height > 2 * r) {
            guiDrawing.drawRectangle(x, y + r, x + 1, y + height - r, color);
            guiDrawing.drawRectangle(x + width - 1, y + r, x + width, y + height - r, color);
        }
    }

    private static void drawRow(GuiDrawing guiDrawing, int x, int y, int width, int outer, int inner, int color) {
        guiDrawing.drawRectangle(x + outer, y, x + Math.max(outer + 1, inner), y + 1, color);
        guiDrawing.drawRectangle(x + width - Math.max(outer + 1, inner), y, x + width - outer, y + 1, color);
    }
}
