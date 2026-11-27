package dev.x341.mrw.mod.data;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.CommandBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WorldEditShapeImporter {

    private static final String PKG = "com.sk89q.worldedit.";

    private WorldEditShapeImporter() {
    }

    public static boolean isAvailable() {
        try {
            Class.forName(PKG + "WorldEdit");
            Class.forName(PKG + "session.SessionManager");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int run(CommandBuilder.ContextHandler context) {
        final ServerPlayerEntity player = context.getServerPlayer();
        final ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem().data instanceof ItemRailWorker)) {
            context.sendFailure("command.mrw.worldedit_hold_rail_worker");
            return 0;
        }

        final int[] bounds = new int[6];
        final String[] blocks;
        try {
            final Object clipboard = findClipboard(player.getName().getString());
            if (clipboard == null) {
                context.sendFailure("command.mrw.worldedit_no_clipboard");
                return 0;
            }
            final Class<?> clipboardClass = Class.forName(PKG + "extent.clipboard.Clipboard");
            final Class<?> vectorClass = Class.forName(PKG + "math.BlockVector3");
            final Object min = clipboardClass.getMethod("getMinimumPoint").invoke(clipboard);
            final Object max = clipboardClass.getMethod("getMaximumPoint").invoke(clipboard);
            final Method getX = vectorClass.getMethod("getX");
            final Method getY = vectorClass.getMethod("getY");
            final Method getZ = vectorClass.getMethod("getZ");
            bounds[0] = (int) getX.invoke(min);
            bounds[1] = (int) getY.invoke(min);
            bounds[2] = (int) getZ.invoke(min);
            bounds[3] = (int) getX.invoke(max);
            bounds[4] = (int) getY.invoke(max);
            bounds[5] = (int) getZ.invoke(max);

            final int width = bounds[3] - bounds[0] + 1;
            final int height = bounds[4] - bounds[1] + 1;
            final int length = bounds[5] - bounds[2] + 1;
            final boolean alongX = length == 1 && width <= ShapeProfile.SIZE;
            final boolean alongZ = !alongX && width == 1 && length <= ShapeProfile.SIZE;
            if (height > ShapeProfile.SIZE || !(alongX || alongZ)) {
                context.sendFailure("command.mrw.worldedit_bad_size", width, height, length);
                return 0;
            }

            final int columns = alongX ? width : length;
            final int offset = (ShapeProfile.SIZE - columns) / 2;
            final Method at = vectorClass.getMethod("at", int.class, int.class, int.class);
            final Method getFullBlock = clipboardClass.getMethod("getFullBlock", vectorClass);
            final Method getAsString = Class.forName(PKG + "world.block.BlockStateHolder").getMethod("getAsString");

            final ShapeProfile profile = new ShapeProfile();
            int placed = 0;
            int skipped = 0;
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < height; row++) {
                    final int x = bounds[0] + (alongX ? column : 0);
                    final int z = bounds[2] + (alongX ? 0 : column);
                    final Object block = getFullBlock.invoke(clipboard, at.invoke(null, x, bounds[1] + row, z));
                    final String raw = String.valueOf(getAsString.invoke(block));
                    final int brace = raw.indexOf('{');
                    final String id = ShapeBlocks.baseId(brace < 0 ? raw : raw.substring(0, brace));
                    final boolean air = id.endsWith("air");
                    final String entry = air ? ShapeBlocks.AIR : ShapeBlocks.canonicalEntry(brace < 0 ? raw : raw.substring(0, brace));
                    if (entry == null) {
                        if (!air) {
                            skipped++;
                        }
                        continue;
                    }
                    final int index = profile.getOrAddPaletteIndex(entry);
                    if (index < 0) {
                        context.sendFailure("command.mrw.worldedit_palette_full", ShapeProfile.MAX_PALETTE);
                        return 0;
                    }
                    profile.setCell(offset + column, row, (byte) index);
                    placed++;
                }
            }
            if (placed == 0) {
                context.sendFailure("command.mrw.worldedit_empty");
                return 0;
            }

            final CompoundTag tag = stack.getOrCreateTag();
            ShapeProfile.removeTag(tag);
            profile.writeTag(tag);
            context.sendSuccess("command.mrw.worldedit_success", false, placed, profile.getPalette().size(), skipped);
            return 1;
        } catch (Throwable throwable) {
            Init.LOGGER.error("[MRW] WorldEdit shape import failed", throwable instanceof InvocationTargetException && throwable.getCause() != null ? throwable.getCause() : throwable);
            context.sendFailure("command.mrw.worldedit_error");
            return 0;
        }
    }

    private static Object findClipboard(String playerName) throws Exception {
        final Object worldEdit = Class.forName(PKG + "WorldEdit").getMethod("getInstance").invoke(null);
        final Object sessionManager = Class.forName(PKG + "WorldEdit").getMethod("getSessionManager").invoke(worldEdit);
        final Object session = Class.forName(PKG + "session.SessionManager").getMethod("findByName", String.class).invoke(sessionManager, playerName);
        if (session == null) {
            return null;
        }
        final Object holder;
        try {
            holder = Class.forName(PKG + "LocalSession").getMethod("getClipboard").invoke(session);
        } catch (InvocationTargetException e) {
            return null;
        }
        return holder == null ? null : Class.forName(PKG + "session.ClipboardHolder").getMethod("getClipboard").invoke(holder);
    }
}
