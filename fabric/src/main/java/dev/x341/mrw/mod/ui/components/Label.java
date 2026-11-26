package dev.x341.mrw.mod.ui.components;

import dev.x341.mrw.mod.ui.UiStyle;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.function.Supplier;

public class Label extends UiComponent {

    private final Supplier<String> text;
    private final boolean centered;

    public Label(int x, int y, boolean centered, Supplier<String> text) {
        super(x, y, 0, 8);
        this.centered = centered;
        this.text = text;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
        final int color = isEnabled() ? UiStyle.TEXT : UiStyle.TEXT_DIM;
        if (centered) {
            graphicsHolder.drawCenteredText(text.get(), x, y, color);
        } else {
            UiStyle.text(graphicsHolder, text.get(), x, y, color);
        }
    }
}
