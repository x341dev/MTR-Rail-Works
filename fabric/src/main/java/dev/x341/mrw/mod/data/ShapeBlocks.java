package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Blocks;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.SlabBlock;
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
        final Map<String, String> defaults = propertiesOf(defaultState);
        BlockState best = null;
        int bestScore = -1;
        for (final BlockState state : statesOf(baseId, defaultState)) {
            final Map<String, String> candidate = propertiesOf(state);
            if (!candidate.entrySet().containsAll(wanted.entrySet())) {
                continue;
            }
            int score = 0;
            for (final Map.Entry<String, String> property : defaults.entrySet()) {
                if (!wanted.containsKey(property.getKey()) && property.getValue().equals(candidate.get(property.getKey()))) {
                    score++;
                }
            }
            if (score > bestScore) {
                best = state;
                bestScore = score;
            }
        }
        return best;
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
        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, String> property : properties(entry).entrySet()) {
            parts.add(property.getKey() + "=" + property.getValue());
        }
        if (isAutoSlab(entry)) {
            parts.add("type=auto");
        }
        return String.join(", ", parts);
    }

    private static boolean isSlabState(@Nullable BlockState state) {
        return state != null && SlabBlock.isInstance(state.getBlock());
    }

    public static boolean isAutoSlab(String entry) {
        return isSlabState(toState(entry)) && !properties(entry).containsKey("type");
    }

    public static List<String> propertyKeys(String entry) {
        return new ArrayList<>(fullProperties(entry).keySet());
    }

    public static List<String> propertyValues(String entry, String key) {
        final List<String> values = new ArrayList<>();
        final String baseId = baseId(entry);
        final BlockState defaultState = defaultState(baseId);
        if (defaultState == null) {
            return values;
        }
        if ("type".equals(key) && isSlabState(defaultState)) {
            return new ArrayList<>(Arrays.asList("auto", "bottom", "top", "double"));
        }
        for (final BlockState state : statesOf(baseId, defaultState)) {
            final String value = propertiesOf(state).get(key);
            if (value != null && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    public static String propertyValue(String entry, String key) {
        if ("type".equals(key) && isSlabState(toState(entry))) {
            return properties(entry).getOrDefault("type", "auto");
        }
        return fullProperties(entry).getOrDefault(key, "");
    }

    @Nullable
    public static String withProperty(String entry, String key, String value) {
        final String baseId = baseId(entry);
        final BlockState defaultState = defaultState(baseId);
        final BlockState current = toState(entry);
        if (defaultState == null || current == null) {
            return null;
        }
        final boolean slabType = "type".equals(key) && isSlabState(current);
        final boolean auto = slabType && "auto".equals(value);
        final Map<String, String> wanted = new LinkedHashMap<>(propertiesOf(current));
        wanted.put(key, auto ? "bottom" : value);
        BlockState best = null;
        int bestScore = -1;
        for (final BlockState state : statesOf(baseId, defaultState)) {
            final Map<String, String> candidate = propertiesOf(state);
            if (!wanted.get(key).equals(candidate.get(key))) {
                continue;
            }
            int score = 0;
            for (final Map.Entry<String, String> property : wanted.entrySet()) {
                if (property.getValue().equals(candidate.get(property.getKey()))) {
                    score++;
                }
            }
            if (score > bestScore) {
                best = state;
                bestScore = score;
            }
        }
        if (best == null) {
            return null;
        }
        final boolean keepType = isSlabState(current) && !auto && (slabType || properties(entry).containsKey("type"));
        return entryOf(baseId, defaultState, propertiesOf(best), keepType);
    }

    @Nullable
    public static String canonicalEntry(@Nullable String raw) {
        final String entry = normalizeEntry(raw);
        if (entry == null || AIR.equals(baseId(entry))) {
            return null;
        }
        final BlockState state = toState(entry);
        if (state == null) {
            return null;
        }
        final String baseId = baseId(entry);
        final BlockState defaultState = defaultState(baseId);
        return defaultState == null ? null : entryOf(baseId, defaultState, propertiesOf(state), isSlabState(state));
    }

    private static String entryOf(String baseId, BlockState defaultState, Map<String, String> properties, boolean keepType) {
        final Map<String, String> defaults = propertiesOf(defaultState);
        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, String> property : properties.entrySet()) {
            if (!property.getValue().equals(defaults.get(property.getKey())) || (keepType && "type".equals(property.getKey()))) {
                parts.add(property.getKey() + "=" + property.getValue());
            }
        }
        return parts.isEmpty() ? baseId : baseId + "[" + String.join(",", parts) + "]";
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
