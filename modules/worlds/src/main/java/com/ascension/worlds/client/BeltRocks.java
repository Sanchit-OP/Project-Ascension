package com.ascension.worlds.client;

import java.util.Random;

/**
 * Where the rocks of an asteroid-belt planet hang, as offsets from the planet's position: pure
 * arithmetic, no Minecraft or render state, so it is unit-tested ({@code BeltRocksTest}). The same
 * seed always gives the same belt, so every client sees the same rocks in the same places.
 *
 * <p>The rocks fill a flat disc of the planet's {@code body_radius} (the volume a ship cannot fly
 * into without being carried down), thick by {@code thickness} of that radius above and below the
 * plane. They are spread evenly over the disc's area, not piled at the middle.
 */
final class BeltRocks {

    /** One rock: its offset from the planet's centre and its radius, in blocks. */
    record Rock(float x, float y, float z, float radius) {
    }

    private BeltRocks() {
    }

    /**
     * @param count      how many rocks
     * @param seed       the belt's seed
     * @param bodyRadius the planet's body radius: the disc the rocks fill
     * @param thickness  half the belt's height, as a fraction of {@code bodyRadius}
     * @param minRadius  the smallest rock's radius, in blocks
     * @param maxRadius  the largest rock's radius, in blocks
     */
    static Rock[] generate(int count, long seed, float bodyRadius, float thickness, float minRadius, float maxRadius) {
        Random random = new Random(seed);
        Rock[] rocks = new Rock[count];
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            // Square root: an even spread over the disc's area, not a crowd at the centre.
            double reach = Math.sqrt(random.nextDouble()) * (bodyRadius - maxRadius);
            double height = (random.nextDouble() * 2.0 - 1.0) * thickness * bodyRadius;
            // Cubed: mostly small rocks, a few big ones.
            double size = minRadius + (maxRadius - minRadius) * Math.pow(random.nextDouble(), 3.0);
            rocks[i] = new Rock((float) (Math.cos(angle) * reach), (float) height, (float) (Math.sin(angle) * reach),
                    (float) size);
        }
        return rocks;
    }
}
