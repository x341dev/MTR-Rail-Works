package dev.x341.mrw.mod.screen;

import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;

public abstract class MrwScreenBase extends ScreenExtension {

    protected static final int HEADER_HEIGHT = 24;
    protected static final int PADDING = 10;
    protected static final int BUTTON_HEIGHT = 20;

    private final String title;
    private final int panelWidth;
    private final int panelHeight;

    protected MrwScreenBase(String titleTranslationKey, int panelWidth, int panelHeight) {
        super(TextHelper.translatable(titleTranslationKey));
        this.title = titleTranslationKey;
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

    @Override
    protected void init2() {
        initContent();
    }

    protected abstract void initContent();

    protected abstract void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY);

    protected final MrwButton addButton(int x, int y, int width, String label, Runnable onPress) {
        final MrwButton button = new MrwButton(x, y, width, BUTTON_HEIGHT, label, onPress);
        addChild(new ClickableWidget(button));
        return button;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        MrwUiStyle.rectangle(graphicsHolder, 0, 0, getWidthMapped(), getHeightMapped(), MrwUiStyle.DIM);
        final int left = panelLeft();
        final int top = panelTop();
        MrwUiStyle.roundedBox(graphicsHolder, left, top, panelWidth, panelHeight, MrwUiStyle.RADIUS + 1, MrwUiStyle.PANEL, MrwUiStyle.PANEL_BORDER);
        MrwUiStyle.roundedBox(graphicsHolder, left + 1, top + 1, panelWidth - 2, HEADER_HEIGHT - 1, MrwUiStyle.RADIUS, MrwUiStyle.HEADER, MrwUiStyle.HEADER);
        MrwUiStyle.rectangle(graphicsHolder, left + 2, top + HEADER_HEIGHT, left + panelWidth - 2, top + HEADER_HEIGHT + 1, MrwUiStyle.ACCENT);
        MrwUiStyle.text(graphicsHolder, title, left + PADDING, top + 8, MrwUiStyle.TEXT);
        renderContent(graphicsHolder, mouseX, mouseY);
        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
