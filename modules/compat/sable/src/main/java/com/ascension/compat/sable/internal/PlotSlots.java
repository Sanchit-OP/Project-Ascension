package com.ascension.compat.sable.internal;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
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
 * exactly Sable's.
 */
public final class PlotSlots {

    private PlotSlots() {
    }

    /** The first slot free in every level, or null when none is (Sable then refuses, as it would). */
    @Nullable
    public static Vector2i firstFreeEverywhere(SubLevelContainer container, MinecraftServer server) {
        int side = 1 << container.getLogSideLength();
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                if (freeEverywhere(container, server, x, z)) {
                    return new Vector2i(x, z);
                }
            }
        }
        return null;
    }

    private static boolean freeEverywhere(SubLevelContainer container, MinecraftServer server, int x, int z) {
        if (container.getOccupancy().get(container.getIndex(x, z))) {
            return false;
        }
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer other = SubLevelContainer.getContainer(level);
            if (other != null && other != container && other.getLogSideLength() == container.getLogSideLength()
                    && other.getOccupancy().get(other.getIndex(x, z))) {
                return false;
            }
        }
        return true;
    }
}
