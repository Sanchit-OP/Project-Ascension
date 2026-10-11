package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The search order behind {@code SpaceMechanics.findSafeLandingSpot} and a seeking gateway ruin:
 * nearest ring first, so a spot two away is always returned before one four away, never the reverse.
 */
class RingSearchTest {

    @Test
    void radiusZeroIsJustTheCentre() {
        List<RingSearch.Offset> offsets = RingSearch.offsetsUpTo(0);
        assertEquals(List.of(new RingSearch.Offset(0, 0)), offsets);
    }

    @Test
    void countsMatchTheKnownRingSizes() {
        // Ring 0 has 1 cell, ring 1 has 8, ring 2 has 16 -- the perimeter of a (2r+1) square.
        assertEquals(1, RingSearch.offsetsUpTo(0).size());
        assertEquals(1 + 8, RingSearch.offsetsUpTo(1).size());
        assertEquals(1 + 8 + 16, RingSearch.offsetsUpTo(2).size());
    }

    @Test
    void everyOffsetIsWithinItsDeclaredRadius() {
        for (RingSearch.Offset offset : RingSearch.offsetsUpTo(4)) {
            int chebyshevDistance = Math.max(Math.abs(offset.dx()), Math.abs(offset.dz()));
            assertTrue(chebyshevDistance <= 4, "offset " + offset + " exceeds the requested radius");
        }
    }

    @Test
    void nearerRingsComeBeforeFartherOnes() {
        List<RingSearch.Offset> offsets = RingSearch.offsetsUpTo(3);
        int previousRing = -1;
        for (RingSearch.Offset offset : offsets) {
            int ring = Math.max(Math.abs(offset.dx()), Math.abs(offset.dz()));
            assertTrue(ring >= previousRing, "ring " + ring + " appeared after ring " + previousRing);
            previousRing = ring;
        }
    }

    @Test
    void noOffsetIsRepeated() {
        List<RingSearch.Offset> offsets = RingSearch.offsetsUpTo(3);
        assertEquals(offsets.size(), offsets.stream().distinct().count());
    }

    @Test
    void firstReturnsTheNearestAcceptedOffset() {
        // Accept anything on ring 2 or further out: the answer must be on ring 2.
        Optional<RingSearch.Offset> found = RingSearch.first(5,
                offset -> Math.max(Math.abs(offset.dx()), Math.abs(offset.dz())) >= 2);
        assertTrue(found.isPresent());
        assertEquals(2, Math.max(Math.abs(found.get().dx()), Math.abs(found.get().dz())));
    }

    @Test
    void firstStopsAtTheFirstMatch() {
        AtomicInteger asked = new AtomicInteger();
        RingSearch.first(50, offset -> asked.incrementAndGet() == 3);
        assertEquals(3, asked.get());
    }

    @Test
    void firstFindsNothingWhenNothingQualifies() {
        assertEquals(Optional.empty(), RingSearch.first(3, offset -> false));
    }

    @Test
    void firstWalksTheSameOrderAsTheList() {
        List<RingSearch.Offset> all = RingSearch.offsetsUpTo(4);
        RingSearch.Offset tenth = all.get(10);
        assertEquals(Optional.of(tenth), RingSearch.first(4, tenth::equals));
    }
}
