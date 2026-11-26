package dev.x341.mrw;

import dev.x341.mrw.mod.client.ShapeDesignerIcons;
import dev.x341.mrw.mod.screen.ShapeDesignerScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;

public final class ShapeDesignerIconRenderer {

    private ShapeDesignerIconRenderer() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ShapeDesignerScreen) {
                ScreenEvents.afterRender(screen).register((renderedScreen, graphics, mouseX, mouseY, tickDelta) -> {
                    for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                        #if MC_VERSION >= "12000"
                        graphics.drawItem(icon.stack.data, icon.x, icon.y);
                        #elif MC_VERSION >= "11903"
                        MinecraftClient.getInstance().getItemRenderer().renderInGui(graphics, icon.stack.data, icon.x, icon.y);
                        #else
                        MinecraftClient.getInstance().getItemRenderer().renderInGui(icon.stack.data, icon.x, icon.y);
                        #endif
                    }
                });
            }
        });
    }
}