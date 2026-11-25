package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class ShapeBlocks {

    public static final String AIR = "minecraft:air";

    
    private static final Set<String> DENIED = new HashSet<String>(Arrays.asList(
            "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw",
            "minecraft:barrier", "minecraft:light", "minecraft:bedrock", "minecraft:spawner",
            "minecraft:moving_piston", "minecraft:piston_head",
            "minecraft:end_portal", "minecraft:end_gateway", "minecraft:nether_portal"
    ));

    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    @Nullable
    private static Function<String, BlockState> resolver;

    private ShapeBlocks() {}

    public static void setResolver(Function<String, BlockState> newResolver) {
        resolver = newResolver;
        STACKS.clear();
    }

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
        if (DENIED.contains(id)) {
            return null;
        }
        if (resolver != null) {
            final BlockState state = resolver.apply(id);
            return state == null || state.isAir() ? null : state;
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
    public static ItemStack toStack(String id) {
        if (STACKS.containsKey(id)) {
            return STACKS.get(id);
        }
        ItemStack stack = null;
        final BlockState state = toState(id);
        if (state != null) {
            final ItemStack candidate = new ItemStack(new ItemConvertible(Item.fromBlock(state.getBlock()).data));
            if (!candidate.isEmpty()) {
                stack = candidate;
            }
        }
        STACKS.put(id, stack);
        return stack;
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
