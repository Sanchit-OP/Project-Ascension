package com.ascension.compat.sable.internal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelHeightAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A saved ship's blocks, as Sable files them: per chunk, per section, each section numbered up from
 * the bottom of the level it was saved in. Plain saved data, no Sable classes, so it is tested on
 * its own ({@code PlotSectionsTest}).
 */
final class PlotSections {

    private PlotSections() {
    }

    /**
     * Sable files a ship's blocks by section number, counted up from the bottom of the level they
     * were saved in. Earth and the Moon start at y -64, space at y -512, so the same number is a
     * different height on each side: read unchanged, a ship's blocks sank 448 blocks inside its own
     * storage, away from its chests' contents, which are saved by height. So every section is
     * renumbered to keep its height, and the saved heightmaps, sized for the old level's height,
     * are dropped for Sable to rebuild. Edits {@code plot} in place. Null if a section would land
     * outside {@code to}.
     */
    @Nullable
    static CompoundTag restacked(CompoundTag plot, LevelHeightAccessor from, LevelHeightAccessor to) {
        int shift = from.getMinSection() - to.getMinSection();
        if (shift == 0 && from.getSectionsCount() == to.getSectionsCount()) {
            return plot;
        }
        CompoundTag chunks = plot.getCompound("chunks");
        for (String key : chunks.getAllKeys()) {
            CompoundTag chunk = chunks.getCompound(key);
            CompoundTag sections = chunk.getCompound("sections");
            CompoundTag renumbered = new CompoundTag();
            for (String index : sections.getAllKeys()) {
                int section = Integer.parseInt(index) + shift;
                if (section < 0 || section >= to.getSectionsCount()) {
                    return null;
                }
                renumbered.put(String.valueOf(section), sections.get(index));
            }
            chunk.put("sections", renumbered);
            chunk.remove("heightmaps");
        }
        return plot;
    }
}
