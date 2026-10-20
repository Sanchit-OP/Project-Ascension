package com.ascension.worlds.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeltRocksTest {

    private static BeltRocks.Rock[] belt(long seed) {
        return BeltRocks.generate(800, seed, 320f, 0.08f, 1.5f, 30f, 0.2f, 8, 1.4f);
    }

    @Test
    void sameSeedGivesTheSameBelt() {
        assertArrayEquals(belt(7L), belt(7L));
    }

    @Test
    void anotherSeedGivesAnotherBelt() {
        assertTrue(belt(7L)[5].x() != belt(8L)[5].x());
    }

    @Test
    void everyRockStaysInsideTheBodyAndItsThickness() {
        BeltRocks.Rock[] rocks = belt(3L);
        assertEquals(800, rocks.length);
        for (BeltRocks.Rock rock : rocks) {
            double flat = Math.sqrt(rock.x() * rock.x() + rock.z() * rock.z());
            assertTrue(flat + rock.radius() <= 320.01, "inside the disc: " + flat);
            assertTrue(Math.abs(rock.y()) <= 0.08 * 320 + 0.01, "inside the thickness: " + rock.y());
            assertTrue(rock.sprite() >= 0 && rock.sprite() < 8, "sprite: " + rock.sprite());
        }
    }

    @Test
    void theMiddleIsTheGreatRockAndIsEmptyAroundIt() {
        BeltRocks.Rock[] rocks = belt(3L);
        assertEquals(0f, rocks[0].x());
        assertEquals(42f, rocks[0].radius(), 0.001f);
        for (int i = 1; i < rocks.length; i++) {
            double flat = Math.sqrt(rocks[i].x() * rocks[i].x() + rocks[i].z() * rocks[i].z());
            assertTrue(flat >= 0.2 * (320 - 30) - 0.01, "clear of the inner edge: " + flat);
        }
    }

    @Test
    void mostRocksAreSmallAndFewAreLarge() {
        BeltRocks.Rock[] rocks = belt(3L);
        int small = 0;
        int large = 0;
        for (BeltRocks.Rock rock : rocks) {
            if (rock.radius() < 4f) {
                small++;
            }
            if (rock.radius() > 15f) {
                large++;
            }
        }
        assertTrue(small > rocks.length / 2, "many small: " + small);
        assertTrue(large > 0 && large < 40, "a few large: " + large);
    }

    @Test
    void theGapBetweenTheBandsIsThinnerThanTheBands() {
        assertTrue(BeltRocks.density(0.64) < BeltRocks.density(0.40) * 0.5);
        assertTrue(BeltRocks.density(0.64) < BeltRocks.density(0.82) * 0.5);
    }
}
