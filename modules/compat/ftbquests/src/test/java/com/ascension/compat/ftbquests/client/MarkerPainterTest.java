package com.ascension.compat.ftbquests.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The marker's drawing, on an array instead of a texture. A marker with a heading is in space; one without is on a world. */
class MarkerPainterTest {

    /** A square of ARGB pixels that refuses any write outside itself. */
    private static final class Grid implements MarkerPainter.Surface {
        final int size;
        final int[] pixels;

        Grid(int size) {
            this.size = size;
            this.pixels = new int[size * size];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void blend(int x, int y, int argb) {
            if (x < 0 || y < 0 || x >= size || y >= size) {
                throw new AssertionError("drew outside the picture at " + x + "," + y);
            }
            pixels[y * size + x] = argb;
        }

        @Override
        public void clear(int x, int y, int width, int height) {
            for (int j = y; j < y + height; j++) {
                for (int i = x; i < x + width; i++) {
                    pixels[j * size + i] = 0;
                }
            }
        }

        int alphaAt(int x, int y) {
            return pixels[y * size + x] >>> 24;
        }
    }

    @Test
    void inSpaceTheCentreIsSolidAndTheRingIsAtItsRadius() {
        Grid grid = new Grid(256);

        MarkerPainter.paint(grid, new ChartMath.Marker(128.5, 128.5, 0.0));   // facing east

        assertTrue(grid.alphaAt(128, 128) > 200, "the dot");
        assertTrue(grid.alphaAt(128 - 12, 128) > 150, "the ring, on the side the arrow is not");
        assertEquals(0, grid.alphaAt(128 - 30, 128), "beyond the glow nothing is drawn");
        assertEquals(0, grid.alphaAt(128 + 30, 128 + 30));
    }

    @Test
    void theArrowPointsTheWayTheHeadingSays() {
        Grid east = new Grid(256);
        MarkerPainter.paint(east, new ChartMath.Marker(128.0, 128.0, 0.0));
        assertTrue(east.alphaAt(128 + 25, 128) > 200, "the arrow is ahead");
        assertEquals(0, east.alphaAt(128 - 25, 128), "nothing behind");

        Grid south = new Grid(256);
        MarkerPainter.paint(south, new ChartMath.Marker(128.0, 128.0, Math.PI / 2));   // down the page
        assertTrue(south.alphaAt(128, 128 + 25) > 200);
        assertEquals(0, south.alphaAt(128 + 25, 128));
    }

    @Test
    void onAWorldTheMarkerIsARingAroundItAndNothingUnderneath() {
        Grid grid = new Grid(256);

        MarkerPainter.paint(grid, new ChartMath.Marker(128.0, 128.0, Double.NaN));

        assertEquals(0, grid.alphaAt(128, 128), "the world's own sphere covers the middle, so nothing is drawn there");
        assertEquals(0, grid.alphaAt(128 + 36, 128), "nor on the sphere itself");
        assertTrue(grid.alphaAt(128 + 76, 128) > 200, "the ring, just outside it");
        assertTrue(grid.alphaAt(128, 128 - 76) > 200, "all the way round");
        assertEquals(0, grid.alphaAt(128 + 110, 128), "and nothing beyond its halo");
    }

    @Test
    void aMarkerAtTheEdgeOfThePictureDrawsOnlyInsideIt() {
        Grid grid = new Grid(128);

        MarkerPainter.Rect corner = MarkerPainter.paint(grid, new ChartMath.Marker(1.0, 2.0, 0.5));
        MarkerPainter.Rect far = MarkerPainter.paint(grid, new ChartMath.Marker(127.0, 127.0, 3.0));
        MarkerPainter.Rect world = MarkerPainter.paint(grid, new ChartMath.Marker(120.0, 5.0, Double.NaN));

        assertEquals(0, corner.x());
        assertEquals(0, corner.y());
        assertTrue(far.x() + far.width() <= 128 && far.y() + far.height() <= 128);
        assertTrue(world.x() + world.width() <= 128 && world.y() == 0);
    }

    @Test
    void clearingTheRectangleItReturnedLeavesTheSurfaceBlank() {
        for (double heading : new double[] {1.0, Double.NaN}) {
            Grid grid = new Grid(256);

            MarkerPainter.Rect rect = MarkerPainter.paint(grid, new ChartMath.Marker(120.3, 130.7, heading));
            grid.clear(rect.x(), rect.y(), rect.width(), rect.height());

            for (int pixel : grid.pixels) {
                assertEquals(0, pixel);
            }
        }
    }
}
