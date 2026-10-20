package com.ascension.worlds.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeltRocksTest {

    @Test
    void sameSeedGivesTheSameBelt() {
        assertArrayEquals(BeltRocks.generate(100, 7L, 320f, 0.15f, 6f, 30f), BeltRocks.generate(100, 7L, 320f, 0.15f, 6f, 30f));
    }

    @Test
    void anotherSeedGivesAnotherBelt() {
        BeltRocks.Rock[] a = BeltRocks.generate(100, 7L, 320f, 0.15f, 6f, 30f);
        BeltRocks.Rock[] b = BeltRocks.generate(100, 8L, 320f, 0.15f, 6f, 30f);
        assertTrue(a[0].x() != b[0].x() || a[0].z() != b[0].z());
    }

    @Test
    void everyRockStaysInsideTheBodyAndItsThickness() {
        BeltRocks.Rock[] rocks = BeltRocks.generate(500, 3L, 320f, 0.15f, 6f, 30f);
        assertEquals(500, rocks.length);
        for (BeltRocks.Rock rock : rocks) {
            double flat = Math.sqrt(rock.x() * rock.x() + rock.z() * rock.z());
            assertTrue(flat + rock.radius() <= 320.01, "inside the disc: " + flat);
            assertTrue(Math.abs(rock.y()) <= 0.15 * 320 + 0.01, "inside the thickness: " + rock.y());
            assertTrue(rock.radius() >= 6f && rock.radius() <= 30f, "size: " + rock.radius());
        }
    }
}
