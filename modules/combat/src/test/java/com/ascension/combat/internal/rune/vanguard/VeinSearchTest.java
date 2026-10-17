package com.ascension.combat.internal.rune.vanguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** Vein Breaker's search (docs/technical/rune-system.md section 4.1). Pure logic; says nothing about the game (ADR-0008). */
class VeinSearchTest {

    private static final BlockPos START = new BlockPos(0, 64, 0);

    private static Set<BlockPos> line(int length) {
        Set<BlockPos> blocks = new HashSet<>();
        for (int i = 1; i <= length; i++) {
            blocks.add(START.east(i));
        }
        return blocks;
    }

    @Test
    void findsTheConnectedBlocksButNotTheStart() {
        List<BlockPos> found = VeinSearch.connected(START, line(3)::contains, 32);
        assertEquals(3, found.size());
        assertFalse(found.contains(START));
    }

    @Test
    void stopsAtTheLimit() {
        assertEquals(5, VeinSearch.connected(START, line(20)::contains, 5).size());
        assertEquals(0, VeinSearch.connected(START, line(20)::contains, 0).size());
    }

    @Test
    void leavesBlocksThatAreNotConnected() {
        Set<BlockPos> blocks = line(2);
        blocks.add(START.east(5));
        assertEquals(2, VeinSearch.connected(START, blocks::contains, 32).size());
    }

    @Test
    void followsDiagonalsSoNoHalfVeinIsLeftBehind() {
        Set<BlockPos> blocks = new HashSet<>();
        blocks.add(START.offset(1, 1, 1));
        blocks.add(START.offset(2, 2, 2));
        assertEquals(2, VeinSearch.connected(START, blocks::contains, 32).size());
    }

    @Test
    void returnsTheNearestFirstSoALimitKeepsTheCloseOnes() {
        List<BlockPos> found = VeinSearch.connected(START, line(10)::contains, 3);
        assertTrue(found.contains(START.east(1)));
        assertTrue(found.contains(START.east(2)));
        assertTrue(found.contains(START.east(3)));
    }
}
