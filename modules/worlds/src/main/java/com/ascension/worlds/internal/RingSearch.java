package com.ascension.worlds.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates integer (dx, dz) offsets in widening square rings, nearest ring first &mdash; the
 * search pattern {@link SpaceMechanics#findSafeLandingSpot} walks to find a safe spot near a
 * remembered landing point. Split out during the M2.7 refactor pass: the ordering itself (ring 0
 * before ring 1 before ring 2, ...) is what makes that search "nearest safe spot" rather than
 * "some safe spot", and that property is plain integer arithmetic with no {@code ServerLevel} or
 * block lookup involved &mdash; worth its own test independent of whether any given block happens
 * to be standable.
 */
final class RingSearch {

    private RingSearch() {
    }

    /** One integer offset from a search's centre point. */
    record Offset(int dx, int dz) {
    }

    /**
     * Every offset within {@code maxRadius} (inclusive), ordered by increasing ring &mdash; all of
     * ring 0 (just the centre), then all of ring 1, and so on. Within a ring, order is not
     * meaningful and not guaranteed beyond "consistent for a given {@code maxRadius}".
     */
    static List<Offset> offsetsUpTo(int maxRadius) {
        List<Offset> offsets = new ArrayList<>();
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                        offsets.add(new Offset(dx, dz));
                    }
                }
            }
        }
        return offsets;
    }
}
