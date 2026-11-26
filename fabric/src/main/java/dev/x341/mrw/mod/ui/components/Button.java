package dev.x341.mrw.mod.ui.components;

import dev.x341.mrw.mod.ui.UiStyle;
import org.mtr.mapping.mapper.GraphicsHolder;

public class Button extends UiComponent {

    private String label;
    private final Runnable onPress;

    public Button(int x, int y, int width, int height, String label, Runnable onPress) {
        super(x, y, width, height);
        this.label = label;
        this.onPress = onPress;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
        final boolean hovered = isEnabled() && contains(mouseX, mouseY);
        final int fill = !isEnabled() ? UiStyle.BUTTON_DISABLED : hovered ? UiStyle.BUTTON_HOVER : UiStyle.BUTTON;
        UiStyle.roundedBox(graphicsHolder, x, y, width, height, UiStyle.RADIUS, fill, hovered ? UiStyle.ACCENT : UiStyle.PANEL_BORDER);
        graphicsHolder.drawCenteredText(label, x + width / 2, y + (height - 8) / 2, isEnabled() ? UiStyle.TEXT : UiStyle.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isEnabled() && button == 0 && contains(mouseX, mouseY)) {
            onPress.run();
            return true;
        }
        return false;
    }
}
