package com.ascension.compat.sable.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelHeightAccessor;
import org.junit.jupiter.api.Test;

/**
 * Refiling a saved ship's blocks for a level of another height ({@link PlotSections#restacked}).
 * Read unchanged, a ship carried from Earth into space lost its chest's contents: the headless test
 * of 2026-10-11 that found it is the evidence this works in game; this keeps it from coming back.
 */
class PlotSectionsTest {

    private static final LevelHeightAccessor EARTH = LevelHeightAccessor.create(-64, 384);
    private static final LevelHeightAccessor SPACE = LevelHeightAccessor.create(-512, 1024);
    private static final LevelHeightAccessor SHORT = LevelHeightAccessor.create(0, 256);

    @Test
    void earthToSpaceKeepsEverySectionAtItsHeight() {
        // Section 12 on Earth is y 128..143 (the ship's chest sat at y 129). Space counts from
        // y -512, so the same height is section 40 there.
        CompoundTag plot = plotWithSection(12);
        CompoundTag restacked = PlotSections.restacked(plot, EARTH, SPACE);
        CompoundTag sections = sections(restacked);
        assertTrue(sections.contains("40"));
        assertFalse(sections.contains("12"));
        assertEquals("ship deck", sections.getCompound("40").getString("marker"));
    }

    @Test
    void spaceBackToEarthUndoesIt() {
        CompoundTag restacked = PlotSections.restacked(plotWithSection(40), SPACE, EARTH);
        assertTrue(sections(restacked).contains("12"));
    }

    @Test
    void theOldHeightmapsAreDroppedForSableToRebuild() {
        CompoundTag restacked = PlotSections.restacked(plotWithSection(12), EARTH, SPACE);
        assertFalse(restacked.getCompound("chunks").getCompound("0").contains("heightmaps"));
    }

    @Test
    void sameHeightLevelsAreLeftAlone() {
        // Earth and the Moon share a height range: the Earth-to-Moon trip worked before this fix.
        CompoundTag plot = plotWithSection(12);
        assertSame(plot, PlotSections.restacked(plot, EARTH, EARTH));
        assertTrue(plot.getCompound("chunks").getCompound("0").contains("heightmaps"));
    }

    @Test
    void aShipThatWouldNotFitIsRefused() {
        // Section 2 on Earth is y -32: below the bottom of a level starting at y 0.
        assertNull(PlotSections.restacked(plotWithSection(2), EARTH, SHORT));
    }

    private static CompoundTag plotWithSection(int index) {
        CompoundTag section = new CompoundTag();
        section.putString("marker", "ship deck");
        CompoundTag sections = new CompoundTag();
        sections.put(String.valueOf(index), section);
        CompoundTag chunk = new CompoundTag();
        chunk.put("sections", sections);
        chunk.put("heightmaps", new CompoundTag());
        CompoundTag chunks = new CompoundTag();
        chunks.put("0", chunk);
        CompoundTag plot = new CompoundTag();
        plot.put("chunks", chunks);
        return plot;
    }

    private static CompoundTag sections(CompoundTag plot) {
        return plot.getCompound("chunks").getCompound("0").getCompound("sections");
    }
}
