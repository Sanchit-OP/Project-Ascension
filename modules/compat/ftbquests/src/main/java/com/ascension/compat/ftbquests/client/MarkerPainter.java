package com.ascension.compat.ftbquests.client;

/**
 * Draws the "you are here" marker. Brass and cream, never teal (teal belongs to the network: gates and
 * planets).
 *
 * <p>Two looks, because the two places are different. <b>In space</b> the marker is a small thing at the
 * player's real position: a soft glow, a ring with a dark edge so it reads over a bright planet, a bright
 * dot and an arrow for the way they face. <b>On a world's surface</b> the marker sits on a planet's
 * picture, which the world's own sphere (a quest button) covers, so it is drawn as a large ring and halo
 * around the sphere instead of a dot under it.
 *
 * <p>Pure: it draws into whatever {@link Surface} it is given, so a test can use an array and the game uses
 * a texture. It touches only a square around the marker ({@link #bounds}), which is what lets the caller
 * clear and upload just that much instead of the whole picture.
 */
public final class MarkerPainter {

    /** Somewhere to draw. Colours are {@code 0xAARRGGBB}; {@code blend} is source-over. */
    public interface Surface {
        int size();

        void blend(int x, int y, int argb);

        void clear(int x, int y, int width, int height);
    }

    /** A rectangle of pixels. */
    public record Rect(int x, int y, int width, int height) {
    }

    /** How far from the centre anything is drawn in space: the arrow tip plus its edge. */
    static final int SPACE_REACH = 36;

    /** How far on a surface: the halo around the world's ring. */
    static final int SURFACE_REACH = 104;

    // In space.
    private static final double GLOW_RADIUS = 24.0;
    private static final double RING_RADIUS = 12.0;
    private static final double RING_HALF_WIDTH = 1.3;
    private static final double EDGE_HALF_WIDTH = 2.6;
    private static final double DOT_RADIUS = 4.2;
    private static final double ARROW_TIP = 31.0;
    private static final double ARROW_BASE = 18.0;
    private static final double ARROW_HALF_WIDTH = 7.0;

    // On a surface: a ring just outside the planet's sphere (a quest button about 36 px in radius) and its halo.
    private static final double WORLD_RING_RADIUS = 76.0;
    private static final double WORLD_RING_HALF_WIDTH = 1.9;
    private static final double WORLD_EDGE_HALF_WIDTH = 3.6;
    private static final double WORLD_HALO_SPREAD = 26.0;

    private static final int BRASS = 0xF0E0B0;
    private static final int CREAM = 0xFFF6DC;
    private static final int NIGHT = 0x04060C;

    private MarkerPainter() {
    }

    /** The square the marker can touch, clipped to the surface. */
    public static Rect bounds(ChartMath.Marker marker, int size) {
        int reach = marker.hasHeading() ? SPACE_REACH : SURFACE_REACH;
        int x0 = Math.max(0, (int) Math.floor(marker.x()) - reach);
        int y0 = Math.max(0, (int) Math.floor(marker.y()) - reach);
        int x1 = Math.min(size, (int) Math.ceil(marker.x()) + reach + 1);
        int y1 = Math.min(size, (int) Math.ceil(marker.y()) + reach + 1);
        return new Rect(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    /** Draws the marker and returns the rectangle it may have touched. Clear that before drawing again. */
    public static Rect paint(Surface surface, ChartMath.Marker marker) {
        Rect rect = bounds(marker, surface.size());
        boolean inSpace = marker.hasHeading();
        double ux = inSpace ? Math.cos(marker.heading()) : 0.0;
        double uy = inSpace ? Math.sin(marker.heading()) : 0.0;

        for (int py = rect.y(); py < rect.y() + rect.height(); py++) {
            for (int px = rect.x(); px < rect.x() + rect.width(); px++) {
                double dx = px + 0.5 - marker.x();
                double dy = py + 0.5 - marker.y();
                double d = Math.hypot(dx, dy);
                if (inSpace) {
                    paintSpacePixel(surface, px, py, dx, dy, d, ux, uy);
                } else {
                    paintWorldPixel(surface, px, py, d);
                }
            }
        }
        return rect;
    }

    private static void paintSpacePixel(Surface surface, int px, int py, double dx, double dy, double d,
                                        double ux, double uy) {
        if (d < GLOW_RADIUS) {
            double t = 1.0 - d / GLOW_RADIUS;
            put(surface, px, py, BRASS, 0.34 * t * t);
        }
        put(surface, px, py, NIGHT, 0.7 * coverage(Math.abs(d - RING_RADIUS) - EDGE_HALF_WIDTH));
        put(surface, px, py, BRASS, coverage(Math.abs(d - RING_RADIUS) - RING_HALF_WIDTH));
        put(surface, px, py, CREAM, coverage(d - DOT_RADIUS));

        // In the arrow's own frame: along its heading, and across it.
        double along = dx * ux + dy * uy;
        double across = -dx * uy + dy * ux;
        double sd = triangleDistance(along, across);
        put(surface, px, py, NIGHT, 0.7 * coverage(sd - 1.6));
        put(surface, px, py, BRASS, coverage(sd));
    }

    private static void paintWorldPixel(Surface surface, int px, int py, double d) {
        double fromRing = Math.abs(d - WORLD_RING_RADIUS);
        if (fromRing < WORLD_HALO_SPREAD) {
            double t = 1.0 - fromRing / WORLD_HALO_SPREAD;
            put(surface, px, py, BRASS, 0.30 * t * t);
        }
        put(surface, px, py, NIGHT, 0.7 * coverage(fromRing - WORLD_EDGE_HALF_WIDTH));
        put(surface, px, py, BRASS, coverage(fromRing - WORLD_RING_HALF_WIDTH));
    }

    /** Full strength inside the shape, fading across the one pixel of its edge; {@code signedDistance} is negative inside. */
    static double coverage(double signedDistance) {
        return Math.max(0.0, Math.min(1.0, 0.5 - signedDistance));
    }

    /**
     * Distance to the arrow's triangle in its own frame (tip forward), negative inside. A convex shape's
     * largest edge distance is exact inside and a safe underestimate outside, which is all an edge fade needs.
     */
    static double triangleDistance(double along, double across) {
        // Edges from the tip to each base corner, and the base itself.
        double length = Math.hypot(ARROW_TIP - ARROW_BASE, ARROW_HALF_WIDTH);
        double nx = ARROW_HALF_WIDTH / length;
        double ny = (ARROW_TIP - ARROW_BASE) / length;
        double side1 = (along - ARROW_TIP) * nx + across * ny;
        double side2 = (along - ARROW_TIP) * nx - across * ny;
        double base = ARROW_BASE - along;
        return Math.max(Math.max(side1, side2), base);
    }

    private static void put(Surface surface, int x, int y, int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        if (a > 0) {
            surface.blend(x, y, (a << 24) | rgb);
        }
    }
}
