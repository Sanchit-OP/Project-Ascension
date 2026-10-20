package com.ascension.worlds.client;

import java.util.Random;

/**
 * Where the rocks of an asteroid-belt planet hang, as offsets from the planet's position, and what each looks like: pure
 * arithmetic, no Minecraft or render state, so it is unit-tested ({@code BeltRocksTest}). The same seed always gives the
 * same belt, so every client sees the same rocks in the same places.
 *
 * <p>The shape is a belt, not a blob: rocks keep to bands of a ring with a gap between them and an empty middle, lie in a
 * thin layer that is thinnest where they are densest, run from many tiny rocks to a few large ones, and the very first is
 * the great rock at the middle when the belt asks for one. All inside the planet's {@code body_radius} disc, the volume a
 * ship cannot fly into without being carried down.
 */
final class BeltRocks {

    /**
     * One rock: its offset from the planet's centre, its radius in blocks, which of the atlas's shapes it wears, its
     * tumble (a starting angle and a speed, in radians and radians per second) and a brightness it keeps (0.75 to 1.1).
     */
    record Rock(float x, float y, float z, float radius, int sprite, float phase, float spin, float shade) {
    }

    private BeltRocks() {
    }

    /**
     * How crowded the band is at {@code f}, 0 at the belt's inner edge to 1 at its outer: a broad inner band, a narrow bright
     * outer one, a gap between them, a thin scatter elsewhere. 0 to 1.
     */
    static double density(double f) {
        double inner = Math.exp(-sq((f - 0.40) / 0.14));
        double outer = 0.8 * Math.exp(-sq((f - 0.82) / 0.07));
        double gap = 1.0 - 0.85 * Math.exp(-sq((f - 0.64) / 0.025));
        return Math.min(1.0, (0.10 + inner + outer) * gap);
    }

    private static double sq(double v) {
        return v * v;
    }

    static Rock[] generate(int count, long seed, float bodyRadius, float thickness, float minRadius, float maxRadius,
                           float inner, int sprites, float central) {
        Random random = new Random(seed);
        Rock[] rocks = new Rock[count];
        int index = 0;
        if (central > 0.0F && count > 0) {
            // The great rock at the middle: the Dragon's island seen from far off.
            float radius = Math.min(maxRadius * central, bodyRadius * 0.3F);
            rocks[index++] = new Rock(0.0F, 0.0F, 0.0F, radius, 0, 0.0F, 0.02F, 1.0F);
        }
        int great = Math.max(1, count / 80);
        for (; index < count; index++) {
            boolean big = index >= count - great;
            double reachMax = bodyRadius - maxRadius;
            double f;
            do {
                f = inner + (1.0 - inner) * random.nextDouble();
            } while (random.nextDouble() > density(f));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = f * reachMax;
            // Triangular, and thinner where the band is dense: a flat sheet with a few strays above and below.
            double layer = (random.nextDouble() + random.nextDouble() - 1.0) * thickness * bodyRadius * (0.35 + 0.65 * (1.0 - density(f)));
            // Fourth power: a great many near the smallest size, few large; a handful of great ones set apart.
            double size = big
                    ? maxRadius * (0.7 + 0.3 * random.nextDouble())
                    : minRadius + (maxRadius * 0.45 - minRadius) * Math.pow(random.nextDouble(), 4.0);
            float spin = (float) ((0.04 + 0.4 * random.nextDouble()) * (random.nextBoolean() ? 1.0 : -1.0));
            rocks[index] = new Rock((float) (Math.cos(angle) * reach), (float) layer, (float) (Math.sin(angle) * reach),
                    (float) size, random.nextInt(Math.max(1, sprites)), (float) (random.nextDouble() * Math.PI * 2.0), spin,
                    (float) (0.75 + 0.35 * random.nextDouble()));
        }
        return rocks;
    }
}
