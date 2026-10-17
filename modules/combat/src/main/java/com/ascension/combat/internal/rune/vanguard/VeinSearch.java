package com.ascension.combat.internal.rune.vanguard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * Finds the blocks connected to a start block, for Vein Breaker (docs/technical/rune-system.md
 * section 4.1). Pure search: it is handed a question ("is this position part of the vein?"), not a
 * level, so it is unit-tested ({@code VeinSearchTest}) and walks at most {@code limit} blocks
 * once (ADR-0007: no scans).
 *
 * <p>Blocks touch if they share a corner, not only a face: ore veins and the logs of a tree
 * connect diagonally, and a search by faces would leave half a vein behind.
 */
final class VeinSearch {

    private VeinSearch() {
    }

    /**
     * @param start where the player mined; not in the result
     * @param inVein whether a position is a block of the same kind, and loaded
     * @param limit the most blocks to return
     * @return the connected blocks, nearest first, at most {@code limit}
     */
    static List<BlockPos> connected(BlockPos start, Predicate<BlockPos> inVein, int limit) {
        List<BlockPos> found = new ArrayList<>();
        if (limit <= 0) {
            return found;
        }

        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos from = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = from.offset(dx, dy, dz);
                        if (found.size() < limit && seen.add(next) && inVein.test(next)) {
                            found.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }
}
