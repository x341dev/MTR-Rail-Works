package dev.x341.mrw.mod.ui.components;

import dev.x341.mrw.mod.ui.UiStyle;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.function.IntFunction;

public class StepSlider extends UiComponent {

    private final int steps;
    private final IntFunction<String> labelProvider;
    private int index;
    private boolean dragging;

    public StepSlider(int x, int y, int width, int height, int steps, int index, IntFunction<String> labelProvider) {
        super(x, y, width, height);
        this.steps = steps;
        this.index = Math.max(0, Math.min(steps - 1, index));
        this.labelProvider = labelProvider;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
        final boolean hovered = isEnabled() && (dragging || contains(mouseX, mouseY));
        UiStyle.roundedBox(graphicsHolder, x, y, width, height, UiStyle.RADIUS, isEnabled() ? UiStyle.BUTTON : UiStyle.BUTTON_DISABLED, hovered ? UiStyle.ACCENT : UiStyle.PANEL_BORDER);
        final int fillWidth = Math.min(width - 2, 2 + (width - 4) * (index + 1) / steps);
        UiStyle.fillRounded(graphicsHolder, x + 1, y + 1, fillWidth, height - 2, UiStyle.RADIUS - 1, UiStyle.SLIDER_FILL);
        graphicsHolder.drawCenteredText(labelProvider.apply(index), x + width / 2, y + (height - 8) / 2, isEnabled() ? UiStyle.TEXT : UiStyle.TEXT_DIM);
    }

    private void update(double mouseX) {
        index = Math.max(0, Math.min(steps - 1, (int) Math.floor((mouseX - x) / width * steps)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isEnabled() && button == 0 && contains(mouseX, mouseY)) {
            dragging = true;
            update(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging) {
            update(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        final boolean wasDragging = dragging;
        dragging = false;
        return wasDragging;
    }
}
