package dev.x341.mrw;

import dev.x341.mrw.mod.client.ShapeDesignerIcons;
import dev.x341.mrw.mod.screen.ShapeDesignerScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
#if MC_VERSION >= "11902"
import net.minecraftforge.client.event.ScreenEvent;
#elif MC_VERSION >= "11802"
import net.minecraftforge.client.event.ScreenEvent;
#else
import net.minecraftforge.client.event.GuiScreenEvent;
#endif

public final class ShapeDesignerIconHandler {

    #if MC_VERSION >= "12000"
    @SubscribeEvent
    public void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ShapeDesignerScreen) {
            for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                event.getGuiGraphics().renderItem(icon.stack.data, icon.x, icon.y);
            }
        }
    }
    #elif MC_VERSION >= "11904"
    @SubscribeEvent
    public void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ShapeDesignerScreen) {
            for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                Minecraft.getInstance().getItemRenderer().renderGuiItem(event.getPoseStack(), icon.stack.data, icon.x, icon.y);
            }
        }
    }
    #elif MC_VERSION >= "11902"
    @SubscribeEvent
    public void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ShapeDesignerScreen) {
            for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                Minecraft.getInstance().getItemRenderer().renderGuiItem(icon.stack.data, icon.x, icon.y);
            }
        }
    }
    #elif MC_VERSION >= "11802"
    @SubscribeEvent
    public void onRender(ScreenEvent.DrawScreenEvent.Post event) {
        if (event.getScreen() instanceof ShapeDesignerScreen) {
            for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                Minecraft.getInstance().getItemRenderer().renderGuiItem(icon.stack.data, icon.x, icon.y);
            }
        }
    }
    #else
    @SubscribeEvent
    public void onRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() instanceof ShapeDesignerScreen) {
            for (final ShapeDesignerIcons.Icon icon : ShapeDesignerIcons.get()) {
                Minecraft.getInstance().getItemRenderer().renderGuiItem(icon.stack.data, icon.x, icon.y);
            }
        }
    }
    #endif
}