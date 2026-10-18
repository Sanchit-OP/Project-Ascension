package com.ascension.compat.ftbquests.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The overlay's pixel arithmetic and the key that decides when it is repainted; neither needs a game. */
class ImageSurfaceTest {

    @Test
    void compositingMatchesTheNativePixelOrder() {
        // Opaque over nothing is the colour itself, with red and blue swapped into the native order (0xAABBGGRR).
        assertEquals(0xFF0000FF, ImageSurface.sourceOver(0xFFFF0000, 0), "opaque red is 0xAABBGGRR = FF0000FF");
        // Nothing over something leaves it alone.
        assertEquals(0xFF336699, ImageSurface.sourceOver(0x00FFFFFF, 0xFF336699));
        // Half-opaque white over opaque black is mid-grey and stays opaque.
        int mixed = ImageSurface.sourceOver(0x80FFFFFF, 0xFF000000);
        assertEquals(0xFF, mixed >>> 24);
        assertEquals(128, mixed & 0xFF, 2);
    }

    @Test
    void theMarkerKeyChangesWithAPixelOrATurnAndNotWithLessThanThat() {
        long here = ChartOverlay.keyOf(new ChartMath.Marker(100.2, 200.1, 0.0));

        assertEquals(here, ChartOverlay.keyOf(new ChartMath.Marker(100.4, 199.9, 0.02)), "inside the same pixel and turn");
        assertTrue(here != ChartOverlay.keyOf(new ChartMath.Marker(101.2, 200.1, 0.0)), "a pixel across");
        assertTrue(here != ChartOverlay.keyOf(new ChartMath.Marker(100.2, 200.1, Math.PI / 2)), "a quarter turn");
        assertTrue(here != ChartOverlay.keyOf(null), "no marker is its own state");
    }
}
