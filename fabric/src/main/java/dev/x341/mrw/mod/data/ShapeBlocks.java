package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Blocks;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ShapeBlocks {

    public static final String AIR = "minecraft:air";
    private static final int MAX_STATES_SCANNED = 4096;

    private static final Set<String> DENIED = new HashSet<>(Arrays.asList(
            "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw",
            "minecraft:barrier", "minecraft:light", "minecraft:bedrock", "minecraft:spawner",
            "minecraft:moving_piston", "minecraft:piston_head",
            "minecraft:end_portal", "minecraft:end_gateway", "minecraft:nether_portal"
    ));

    private static final Map<String, ItemStack> STACKS = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, List<BlockState>> STATES = new HashMap<>();

    @Nullable
    private static Function<String, BlockState> resolver;

    private ShapeBlocks() {
    }

    public static void setResolver(Function<String, BlockState> newResolver) {
        resolver = newResolver;
        STACKS.clear();
        NAMES.clear();
        STATES.clear();
    }

    @Nullable
    public static String normalize(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        final String id = raw.trim().toLowerCase();
        if (id.isEmpty() || id.length() > 80 || id.contains(" ") || id.contains(",") || id.contains("[")) {
            return null;
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    @Nullable
    public static String normalizeEntry(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        final String entry = raw.trim().toLowerCase();
        final int bracket = entry.indexOf('[');
        final String id = normalize(bracket < 0 ? entry : entry.substring(0, bracket));
        final String properties = bracket < 0 ? "" : entry.substring(bracket);
        if (id == null || entry.length() > 240) {
            return null;
        }
        if (!properties.isEmpty() && !properties.matches("\\[[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*]")) {
            return null;
        }
        return id + properties;
    }

    public static String baseId(String entry) {
        final int bracket = entry.indexOf('[');
        return bracket < 0 ? entry : entry.substring(0, bracket);
    }

    public static Map<String, String> properties(String entry) {
        final Map<String, String> map = new LinkedHashMap<>();
        final int bracket = entry.indexOf('[');
        if (bracket >= 0 && entry.endsWith("]")) {
            for (final String pair : entry.substring(bracket + 1, entry.length() - 1).split(",")) {
                final String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }
        }
        return map;
    }

    @Nullable
    private static BlockState defaultState(String baseId) {
        if (AIR.equals(baseId)) {
            return Blocks.getAirMapped().getDefaultState();
        }
        if (DENIED.contains(baseId)) {
            return null;
        }
        if (resolver != null) {
            final BlockState state = resolver.apply(baseId);
            return state == null || state.isAir() ? null : state;
        }
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", baseId);
        tag.putByte("Count", (byte) 1);
        final ItemStack itemStack = ItemStack.fromNbt(tag);
        if (itemStack.isEmpty()) {
            return null;
        }
        final BlockState state = Block.getBlockFromItem(itemStack.getItem()).getDefaultState();
        return state.isAir() ? null : state;
    }

    @Nullable
    public static BlockState toState(String entry) {
        final String baseId = baseId(entry);
        final BlockState defaultState = defaultState(baseId);
        if (defaultState == null) {
            return null;
        }
        final Map<String, String> wanted = properties(entry);
        if (wanted.isEmpty()) {
            return defaultState;
        }
        for (final BlockState state : statesOf(baseId, defaultState)) {
            if (propertiesOf(state).entrySet().containsAll(wanted.entrySet())) {
                return state;
            }
        }
        return null;
    }

    private static List<BlockState> statesOf(String baseId, BlockState defaultState) {
        final List<BlockState> cached = STATES.get(baseId);
        if (cached != null) {
            return cached;
        }
        final List<BlockState> list = new ArrayList<>();
        list.add(defaultState);
        try {
            final Block block = defaultState.getBlock();
            final int rawId = Block.getRawIdFromState(defaultState);
            for (int id = rawId - 1; id >= 0 && rawId - id <= MAX_STATES_SCANNED; id--) {
                final BlockState state = Block.getStateFromRawId(id);
                if (state == null || !state.isOf(block)) {
                    break;
                }
                list.add(state);
            }
            for (int id = rawId + 1; id - rawId <= MAX_STATES_SCANNED; id++) {
                final BlockState state = Block.getStateFromRawId(id);
                if (state == null || !state.isOf(block)) {
                    break;
                }
                list.add(state);
            }
        } catch (Exception ignored) {
        }
        STATES.put(baseId, list);
        return list;
    }

    private static Map<String, String> propertiesOf(BlockState state) {
        final Map<String, String> map = new LinkedHashMap<>();
        final String text = String.valueOf(state.data);
        final int bracket = text.lastIndexOf('[');
        if (bracket >= 0 && text.endsWith("]")) {
            for (final String pair : text.substring(bracket + 1, text.length() - 1).split(",")) {
                final String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return map;
    }

    public static Map<String, String> fullProperties(String entry) {
        final BlockState state = toState(entry);
        return state == null ? new LinkedHashMap<>() : propertiesOf(state);
    }

    public static String detail(String entry) {
        return String.join(", ", properties(entry).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new));
    }

    @Nullable
    public static String rotate(String entry, char axis) {
        final String baseId = baseId(entry);
        final BlockState defaultState = defaultState(baseId);
        final BlockState current = toState(entry);
        if (defaultState == null || current == null) {
            return null;
        }
        final Map<String, String> from = propertiesOf(current);
        final Map<String, String> full = new LinkedHashMap<>();
        final Map<String, String> flipOnly = new LinkedHashMap<>();
        for (final Map.Entry<String, String> property : from.entrySet()) {
            full.put(property.getKey(), turn(property.getKey(), property.getValue(), axis, true));
            flipOnly.put(property.getKey(), turn(property.getKey(), property.getValue(), axis, false));
        }
        for (final Map<String, String> candidate : Arrays.asList(full, flipOnly)) {
            if (candidate.equals(from)) {
                continue;
            }
            for (final BlockState state : statesOf(baseId, defaultState)) {
                if (propertiesOf(state).equals(candidate)) {
                    return entryOf(baseId, defaultState, candidate);
                }
            }
        }
        return null;
    }

    private static String entryOf(String baseId, BlockState defaultState, Map<String, String> properties) {
        if (properties.equals(propertiesOf(defaultState))) {
            return baseId;
        }
        final Map<String, String> defaults = propertiesOf(defaultState);
        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, String> property : properties.entrySet()) {
            if (!property.getValue().equals(defaults.get(property.getKey()))) {
                parts.add(property.getKey() + "=" + property.getValue());
            }
        }
        return baseId + "[" + String.join(",", parts) + "]";
    }

    private static String turn(String key, String value, char axis, boolean full) {
        final boolean upsideDown = (axis == 'x' || axis == 'z') && ("half".equals(key) || "type".equals(key));
        if (upsideDown) {
            return "top".equals(value) ? "bottom" : "bottom".equals(value) ? "top" : value;
        }
        if (!full) {
            return value;
        }
        switch (key) {
            case "facing":
                return cycle(value, axis == 'y' ? new String[]{"north", "east", "south", "west"} : axis == 'x' ? new String[]{"north", "up", "south", "down"} : new String[]{"east", "up", "west", "down"});
            case "axis":
                return axis == 'y' ? swap(value, "x", "z") : axis == 'x' ? swap(value, "y", "z") : swap(value, "x", "y");
            case "rotation":
                if (axis == 'y') {
                    try {
                        return String.valueOf((Integer.parseInt(value) + 4) % 16);
                    } catch (NumberFormatException ignored) {
                    }
                }
                return value;
            default:
                return value;
        }
    }

    private static String cycle(String value, String[] order) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(value)) {
                return order[(i + 1) % order.length];
            }
        }
        return value;
    }

    private static String swap(String value, String a, String b) {
        return a.equals(value) ? b : b.equals(value) ? a : value;
    }

    public static String getName(String entry) {
        final String baseId = baseId(entry);
        final String cached = NAMES.get(baseId);
        if (cached != null) {
            return cached;
        }
        String name = baseId;
        try {
            final BlockState state = defaultState(baseId);
            if (state != null) {
                final String text = state.getBlock().getName().getString();
                if (!text.isEmpty()) {
                    name = text;
                }
            }
        } catch (Exception ignored) {
        }
        NAMES.put(baseId, name);
        return name;
    }

    @Nullable
    public static ItemStack toStack(String entry) {
        final String baseId = baseId(entry);
        if (STACKS.containsKey(baseId)) {
            return STACKS.get(baseId);
        }
        ItemStack stack = null;
        final BlockState state = defaultState(baseId);
        if (state != null) {
            final ItemStack candidate = new ItemStack(new ItemConvertible(Item.fromBlock(state.getBlock()).data));
            if (!candidate.isEmpty()) {
                stack = candidate;
            }
        }
        STACKS.put(baseId, stack);
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
