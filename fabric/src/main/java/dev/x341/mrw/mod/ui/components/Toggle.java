package dev.x341.mrw.mod.ui.components;

import dev.x341.mrw.mod.ui.UiStyle;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.function.Consumer;

public class Toggle extends UiComponent{

    private final String label;
    private final Consumer<Boolean> onChange;
    private boolean checked;

    public Toggle(int x, int y, int width, int height, String label, boolean checked, Consumer<Boolean> onChange) {
        super(x, y, width, height);
        this.label = label;
        this.checked = checked;
        this.onChange = onChange;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
        final boolean hovered = isEnabled() && contains(mouseX, mouseY);
        final int fill = !isEnabled() ? UiStyle.BUTTON_DISABLED : checked ? UiStyle.CHECKED : hovered ? UiStyle.BUTTON_HOVER : UiStyle.BUTTON;
        final int border = isEnabled() && (checked || hovered) ? UiStyle.ACCENT : UiStyle.PANEL_BORDER;
        UiStyle.roundedBox(graphicsHolder, x, y, width, height, UiStyle.RADIUS, fill, border);
        graphicsHolder.drawCenteredText(label, x + width / 2, y + (height - 8) / 2, isEnabled() ? UiStyle.TEXT : UiStyle.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isEnabled() && button == 0 && contains(mouseX, mouseY)) {
            checked = !checked;
            onChange.accept(checked);
            return true;
        }
        return false;
    }
}
