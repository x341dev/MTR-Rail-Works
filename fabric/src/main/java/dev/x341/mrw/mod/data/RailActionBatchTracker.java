package dev.x341.mrw.mod.data;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Not thread-safe by design: every call site (RailActionMrw construction,
 * RailActionModuleMrw#tick, RailActionMixin) runs on the server main thread, so BATCHES is
 * effectively confined to it. Do not call from an async context without adding synchronization.
 */
public final class RailActionBatchTracker {

    private static final Map<UUID, int[]> BATCHES = new HashMap<>();

    private RailActionBatchTracker() {
    }

    public static void onEnqueue(UUID uuid) {
        final int[] batch = BATCHES.get(uuid);
        if (batch == null || batch[0] >= batch[1]) {
            BATCHES.put(uuid, new int[]{0, 1});
        } else {
            batch[1]++;
        }
    }

    public static void onComplete(UUID uuid) {
        final int[] batch = BATCHES.get(uuid);
        if (batch != null) {
            batch[0]++;
            if (batch[0] >= batch[1]) {
                BATCHES.remove(uuid);
            }
        }
    }

    /** Called when a queued rail action is cancelled before completing. */
    public static void onRemove(UUID uuid) {
        final int[] batch = BATCHES.get(uuid);
        if (batch != null) {
            batch[1]--;
            if (batch[0] >= batch[1]) {
                BATCHES.remove(uuid);
            }
        }
    }

    /** Returns {completed, total} for the player's active batch, or null if none. */
    @Nullable
    public static int[] get(UUID uuid) {
        return BATCHES.get(uuid);
    }
}
