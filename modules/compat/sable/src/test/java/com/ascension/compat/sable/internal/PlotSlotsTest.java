package com.ascension.compat.sable.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.BitSet;
import java.util.List;
import java.util.function.IntBinaryOperator;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

/**
 * ADR-0016's slot choice: a new ship's slot must be free in every level, not just its own, or a
 * ship built after another flew away can never follow it.
 */
class PlotSlotsTest {

    private static final int SIDE = 4;

    /** Any one-to-one numbering will do; the search must use the one it is given. */
    private static final IntBinaryOperator INDEX = (x, z) -> x * SIDE + z;

    @Test
    void anEmptyServerGivesSablesOwnFirstSlot() {
        assertEquals(new Vector2i(0, 0), PlotSlots.firstFree(SIDE, INDEX, List.of(new BitSet(), new BitSet())));
    }

    @Test
    void aSlotHeldInAnotherLevelIsSkipped() {
        // Ship A was built in slot 0 on Earth and flew to the Moon: Earth's slot 0 is free again,
        // the Moon's is not. The next ship built on Earth must not get slot 0.
        BitSet earth = new BitSet();
        BitSet moon = bits(0);
        assertEquals(new Vector2i(0, 1), PlotSlots.firstFree(SIDE, INDEX, List.of(earth, moon)));
    }

    @Test
    void slotsHeldAcrossSeveralLevelsAreAllSkipped() {
        assertEquals(new Vector2i(0, 3), PlotSlots.firstFree(SIDE, INDEX, List.of(bits(0), bits(1), bits(2))));
    }

    @Test
    void searchesZBeforeMovingOnInX() {
        assertEquals(new Vector2i(1, 0), PlotSlots.firstFree(SIDE, INDEX, List.of(bits(0, 1, 2, 3))));
    }

    @Test
    void aFullServerHasNoSlot() {
        BitSet all = new BitSet();
        all.set(0, SIDE * SIDE);
        assertNull(PlotSlots.firstFree(SIDE, INDEX, List.of(all)));
    }

    private static BitSet bits(int... set) {
        BitSet bits = new BitSet();
        for (int bit : set) {
            bits.set(bit);
        }
        return bits;
    }
}
