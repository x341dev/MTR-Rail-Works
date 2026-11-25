package dev.x341.mrw.mod.data;

import java.util.*;
import java.util.function.Supplier;

public final class BlockCatalog {

    private static Supplier<Collection<String>> supplier = Collections::emptyList;
    private static List<String> cache;

    private BlockCatalog() {}

    public static void setSupplier(Supplier<Collection<String>> newSupplier) {
        supplier = newSupplier;
        cache = null;
    }

    public static List<String> getIds() {
        if (cache == null) {
            final List<String> ids = new ArrayList<>();
            for (final String rawId : supplier.get()) {
                final String id = ShapeBlocks.normalize(rawId);
                if (id != null && !ShapeBlocks.AIR.equals(id) && ShapeBlocks.toState(id) != null) {
                    ids.add(id);
                }
            }
            ids.sort(Comparator.comparing((String id) -> !id.startsWith("minecraft:")).thenComparing(Comparator.naturalOrder()));
            cache = ids;
        }
        return cache;
    }
}
