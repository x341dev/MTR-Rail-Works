package dev.x341.mrw.mod.ui;

import dev.x341.mrw.mod.ui.components.Button;
import dev.x341.mrw.mod.ui.components.Label;
import dev.x341.mrw.mod.ui.components.StepSlider;
import dev.x341.mrw.mod.ui.components.Toggle;
import dev.x341.mrw.mod.ui.components.UiComponent;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class ScreenBase extends ScreenExtension {

    protected static final int HEADER_HEIGHT = 24;
    protected static final int PADDING = 10;
    protected static final int COMPONENT_HEIGHT = 20;

    private final String title;
    private final int panelWidth;
    private final int panelHeight;
    private final List<UiComponent> components = new ArrayList<>();

    protected ScreenBase(String titleTranslationKey, int panelWidth, int panelHeight) {
        super(TextHelper.translatable(titleTranslationKey));
        this.title = TextHelper.translatable(titleTranslationKey).getString();
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    protected final int panelWidth() {
        return panelWidth;
    }

    protected final int panelHeight() {
        return panelHeight;
    }

    protected final int panelLeft() {
        return (getWidthMapped() - panelWidth) / 2;
    }

    protected final int panelTop() {
        return (getHeightMapped() - panelHeight) / 2;
    }

    protected final int contentTop() {
        return panelTop() + HEADER_HEIGHT + PADDING;
    }

    protected final int centeredX(int width) {
        return panelLeft() + (panelWidth - width) / 2;
    }

    @Override
    protected final void init2() {
        components.clear();
        initContent();
    }

    protected abstract void initContent();

    protected void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
    }

    protected final <T extends UiComponent> T add(T component) {
        components.add(component);
        return component;
    }

    protected final Button addButton(int x, int y, int width, String label, Runnable onPress) {
        return add(new Button(x, y, width, COMPONENT_HEIGHT, label, onPress));
    }

    protected final Toggle addToggle(int x, int y, int width, String label, boolean checked, Consumer<Boolean> onChange) {
        return add(new Toggle(x, y, width, COMPONENT_HEIGHT, label, checked, onChange));
    }

    protected final StepSlider addSlider(int x, int y, int width, int steps, int index, IntFunction<String> labelProvider) {
        return add(new StepSlider(x, y, width, COMPONENT_HEIGHT, steps, index, labelProvider));
    }

    protected final Label addLabel(int x, int y, boolean centered, Supplier<String> text) {
        return add(new Label(x, y, centered, text));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        UiStyle.rectangle(graphicsHolder, 0, 0, getWidthMapped(), getHeightMapped(), UiStyle.DIM);
        final int left = panelLeft();
        final int top = panelTop();
        UiStyle.roundedBox(graphicsHolder, left, top, panelWidth, panelHeight, UiStyle.RADIUS + 1, UiStyle.PANEL, UiStyle.PANEL_BORDER);
        UiStyle.roundedBox(graphicsHolder, left + 1, top + 1, panelWidth - 2, HEADER_HEIGHT - 1, UiStyle.RADIUS, UiStyle.HEADER, UiStyle.HEADER);
        UiStyle.rectangle(graphicsHolder, left + 2, top + HEADER_HEIGHT, left + panelWidth - 2, top + HEADER_HEIGHT + 1, UiStyle.ACCENT);
        UiStyle.text(graphicsHolder, title, left + PADDING, top + 8, UiStyle.TEXT);
        renderContent(graphicsHolder, mouseX, mouseY);
        for (final UiComponent component : components) {
            component.render(graphicsHolder, mouseX, mouseY);
        }
        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        for (final UiComponent component : components) {
            if (component.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked2(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (final UiComponent component : components) {
            if (component.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased2(double mouseX, double mouseY, int button) {
        boolean handled = false;
        for (final UiComponent component : components) {
            handled |= component.mouseReleased(mouseX, mouseY, button);
        }
        return handled || super.mouseReleased2(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
