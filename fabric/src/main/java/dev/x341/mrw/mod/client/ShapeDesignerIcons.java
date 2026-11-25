package dev.x341.mrw.mod.client;

import org.mtr.mapping.holder.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ShapeDesignerIcons {

    public static final class Icon {
        public final ItemStack stack;
        public final int x;
        public final int y;

        private Icon(ItemStack stack, int x, int y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }

    private static final List<Icon> ICONS = new ArrayList<>();

    private ShapeDesignerIcons() {}

    public static void clear() {
        ICONS.clear();
    }

    public static void add(ItemStack stack, int x, int y) {
        ICONS.add(new Icon(stack, x, y));
    }

    public static List<Icon> get() {
        return ICONS;
    }
}
