package dev.x341.mrw.mod.screen;

import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public class MrwButton extends ClickableWidgetExtension {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Runnable onPress;
    private String label;
    private boolean enabled = true;

    public MrwButton(int x, int y, int width, int height, String label, Runnable onPress) {
        super(x, y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onPress = onPress;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        final boolean hovered = enabled && contains(mouseX, mouseY);
        final int fill = !enabled ? MrwUiStyle.BUTTON_DISABLED : hovered ? MrwUiStyle.BUTTON_HOVER : MrwUiStyle.BUTTON;
        MrwUiStyle.roundedBox(graphicsHolder, x, y, width, height, MrwUiStyle.RADIUS, fill, hovered ? MrwUiStyle.ACCENT : MrwUiStyle.PANEL_BORDER);
        graphicsHolder.drawCenteredText(label, x + width / 2, y + (height - 8) / 2, enabled ? MrwUiStyle.TEXT : MrwUiStyle.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
         if (enabled && button == 0 && contains(mouseX, mouseY)) {
             onPress.run();
             return true;
         }
         return false;
    }
}