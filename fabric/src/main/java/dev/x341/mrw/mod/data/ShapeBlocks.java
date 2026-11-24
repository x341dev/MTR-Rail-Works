package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.*;

import javax.annotation.Nullable;
import java.util.Locale;

public class ShapeBlocks {

    public static final String AIR = "minecraft:air";

    private ShapeBlocks() {}

    /** "Stone" -> "stone" -> "minecraft:stone"; null if blank or contains spaces/commas. */
    @Nullable
    public static String normalize(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        final String id = raw.trim().toLowerCase();
        if (id.isEmpty() || id.length() > 80 || id.contains(" ") || id.contains(",")) {
            return null;
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    @Nullable
    public static BlockState toState(String id) {
        if (AIR.equals(id)) {
            return Blocks.getAirMapped().getDefaultState();
        }
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putByte("Count", (byte) 1);
        final ItemStack itemStack = ItemStack.fromNbt(tag);
        if (itemStack.isEmpty()) {
            return null;
        }
        final BlockState state = Block.getBlockFromItem(itemStack.getItem()).getDefaultState();
        return state.isAir() ? null : state;
    }

    @Nullable
    public static String fromStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        final String id = itemStack.writeNbt(new CompoundTag()).getString("id");
        return id.isEmpty() || toState(id) == null ? null : id;
    }
}
