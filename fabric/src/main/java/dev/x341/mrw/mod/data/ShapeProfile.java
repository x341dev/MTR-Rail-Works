package dev.x341.mrw.mod.data;

import org.mtr.mapping.holder.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShapeProfile {

    public static final int SIZE = 9;
    public static final int MAX_PALETTE = 8;
    public static final byte EMPTY = -1;

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
}
