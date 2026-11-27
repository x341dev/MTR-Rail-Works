package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShapeProfile {

    public static final int SIZE = 9;
    public static final int MAX_PALETTE = 32;
    public static final byte EMPTY = -1;
    public static final int RAIL_ROW = 1;

    private static final String KEY_PALETTE_SIZE = "mrw_shape_palette_size";
    private static final String KEY_PALETTE_PREFIX = "mrw_shape_palette_";
    private static final String KEY_CELLS = "mrw_shape_cells";

    private final List<String> palette = new ArrayList<>();
    private final byte[] cells = new byte[SIZE * SIZE];

    public ShapeProfile() {
        Arrays.fill(cells, EMPTY);
    }

    /** Row 0 is the lowest row; column 0 is the leftmost. */
    public static int index(int column, int row) {
        return row * SIZE + column;
    }

    public byte getCell(int column, int row) {
        return cells[index(column, row)];
    }

    public void setCell(int column, int row, byte paletteIndex) {
        cells[index(column, row)] = paletteIndex;
    }

    public void shift(int deltaColumn, int deltaRow) {
        final byte[] old = cells.clone();
        Arrays.fill(cells, EMPTY);
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                final int newColumn = column + deltaColumn;
                final int newRow = row + deltaRow;
                if (newColumn >= 0 && newColumn < SIZE && newRow >= 0 && newRow < SIZE) {
                    cells[index(newColumn, newRow)] = old[index(column, row)];
                }
            }
        }
    }

    public List<String> getPalette() {
        return palette;
    }

    public boolean isEmpty() {
        for (final byte cell : cells) {
            if (cell != EMPTY) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        palette.clear();
        Arrays.fill(cells, EMPTY);
    }

    public void compact() {
        final int[] newIndex = new int[palette.size()];
        final List<String> kept = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            boolean used = false;
            for (final byte cell : cells) {
                used |= cell == i;
            }
            newIndex[i] = used ? kept.size() : -1;
            if (used) {
                kept.add(palette.get(i));
            }
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] = cells[i] >= 0 && cells[i] < newIndex.length ? (byte) newIndex[cells[i]] : EMPTY;
        }
        palette.clear();
        palette.addAll(kept);
    }

    public int getOrAddPaletteIndex(String blockId) {
        final int existing = palette.indexOf(blockId);
        if (existing >= 0) {
            return existing;
        }
        if (palette.size() >= MAX_PALETTE) {
            return -1;
        }
        palette.add(blockId);
        return palette.size() - 1;
    }

    public String getBlockId(int column, int row) {
        final int paletteIndex = getCell(column, row);
        return paletteIndex >= 0 && paletteIndex < palette.size() ? palette.get(paletteIndex) : null;
    }

    public void writeTag(CompoundTag tag) {
        tag.putInt(KEY_PALETTE_SIZE, palette.size());
        for (int i = 0; i < palette.size(); i++) {
            tag.putString(KEY_PALETTE_PREFIX + i, palette.get(i));
        }
        tag.putByteArray(KEY_CELLS, cells.clone());
    }

    public static ShapeProfile readTag(CompoundTag tag) {
        final ShapeProfile profile = new ShapeProfile();
        final int paletteSize = Math.max(0, Math.min(tag.getInt(KEY_PALETTE_SIZE), MAX_PALETTE));
        for (int i = 0; i < paletteSize; i++) {
            profile.palette.add(tag.getString(KEY_PALETTE_PREFIX + i));
        }
        final byte[] savedCells = tag.getByteArray(KEY_CELLS);
        if (savedCells.length == profile.cells.length) {
            for (int i = 0; i < savedCells.length; i++) {
                profile.cells[i] = savedCells[i] >= 0 && savedCells[i] < paletteSize ? savedCells[i] : EMPTY;
            }
        }
        return profile;
    }

    public static boolean isPresent(CompoundTag tag) {
        return tag.contains(KEY_CELLS);
    }

    public static void removeTag(CompoundTag tag) {
        tag.remove(KEY_PALETTE_SIZE);
        for (int i = 0; i < MAX_PALETTE; i++) {
            tag.remove(KEY_PALETTE_PREFIX + i);
        }
        tag.remove(KEY_CELLS);
    }
}
