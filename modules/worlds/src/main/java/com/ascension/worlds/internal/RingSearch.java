package com.ascension.worlds.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Enumerates integer (dx, dz) offsets in widening square rings, nearest ring first &mdash; the
 * search pattern {@link SpaceMechanics#findSafeLandingSpot} walks to find a safe spot near a
 * remembered landing point, and a seeking gateway ruin walks to find its ground
 * ({@code GatewayFrameStructure}). Split out during the M2.7 refactor pass: the ordering itself
 * (ring 0 before ring 1 before ring 2, ...) is what makes a search "nearest" rather than "some",
 * and that property is plain integer arithmetic with no {@code ServerLevel} or block lookup
 * involved &mdash; worth its own test independent of whether any given spot happens to qualify.
 */
public final class RingSearch {

    private RingSearch() {
    }

    /** One integer offset from a search's centre point. */
    public record Offset(int dx, int dz) {
    }

    /**
     * Every offset within {@code maxRadius} (inclusive), ordered by increasing ring &mdash; all of
     * ring 0 (just the centre), then all of ring 1, and so on. Within a ring, order is not
     * meaningful and not guaranteed beyond "consistent for a given {@code maxRadius}".
     */
    static List<Offset> offsetsUpTo(int maxRadius) {
        List<Offset> offsets = new ArrayList<>();
        first(maxRadius, offset -> {
            offsets.add(offset);
            return false;
        });
        return offsets;
    }

    /**
     * The first offset within {@code maxRadius} that {@code accept} takes, walking the same order
     * as {@link #offsetsUpTo}, without building the list: a search that finds its spot in the
     * second ring never visits the hundredth.
     */
    public static Optional<Offset> first(int maxRadius, Predicate<Offset> accept) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                // On the ring's left and right edges every dz is on the ring; between them, only
                // the top and bottom rows are.
                int step = Math.abs(dx) == radius ? 1 : Math.max(1, 2 * radius);
                for (int dz = -radius; dz <= radius; dz += step) {
                    Offset offset = new Offset(dx, dz);
                    if (accept.test(offset)) {
                        return Optional.of(offset);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
