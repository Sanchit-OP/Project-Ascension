package com.ascension.compat.sable.internal;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntBinaryOperator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

/**
 * Picks a storage slot for a new ship that no ship holds in any dimension (ADR-0016).
 *
 * <p>Sable keeps each ship's blocks in a numbered slot (its plot) of the level it is in, and gives a
 * new ship the first slot free in that level. A ship carries its slot number with it when it
 * crosses to another level, so two ships built on different worlds with the same number cannot
 * both be in one place. Choosing slots free everywhere at the start keeps every ship's number its
 * own, wherever it flies.
 *
 * <p>Reads Sable's own occupancy record for each level, which counts parked, unloaded ships too.
 * Same search order as Sable's (x, then z), so with only one level holding ships the choice is
 * exactly Sable's. Runs only when a ship is made.
 */
public final class PlotSlots {

    private PlotSlots() {
    }

    /** The first slot free in every level, or null when none is (Sable then refuses, as it would). */
    @Nullable
    public static Vector2i firstFreeEverywhere(SubLevelContainer container, MinecraftServer server) {
        List<BitSet> taken = new ArrayList<>();
        taken.add(container.getOccupancy());
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer other = SubLevelContainer.getContainer(level);
            if (other != null && other != container && other.getLogSideLength() == container.getLogSideLength()) {
                taken.add(other.getOccupancy());
            }
        }
        return firstFree(1 << container.getLogSideLength(), container::getIndex, taken);
    }

    /**
     * The first slot (x, then z, each below {@code side}) whose index, by Sable's own numbering
     * {@code index}, is clear in every one of {@code taken}. Null when there is none.
     */
    @Nullable
    static Vector2i firstFree(int side, IntBinaryOperator index, List<BitSet> taken) {
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                int slot = index.applyAsInt(x, z);
                if (taken.stream().noneMatch(occupancy -> occupancy.get(slot))) {
                    return new Vector2i(x, z);
                }
            }
        }
        return null;
    }
}
