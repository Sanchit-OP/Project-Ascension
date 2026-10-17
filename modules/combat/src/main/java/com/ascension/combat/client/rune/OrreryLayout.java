package com.ascension.combat.client.rune;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits on the orrery (docs/technical/rune-system.md section 10), in panel pixels:
 * the 400 x 224 panel, one texel to a GUI pixel. These are the mockup's own numbers, from
 * {@code tools/generate-rune-art.py}, which drew the dial sprites to match them: change one here
 * and the sprites' rings and arcs no longer line up with the sockets until the script's constant
 * changes too.
 *
 * <p>Pure geometry, no game: unit-tested ({@code OrreryLayoutTest}).
 */
public final class OrreryLayout {

    public static final int PANEL_W = 400;
    public static final int PANEL_H = 224;

    /** The primary dial: centre, hub, keystone ring, minor ring and rim radii. */
    public static final int P_CX = 98;
    public static final int P_CY = 122;
    public static final int P_HUB = 14;
    public static final int P_KEY = 37;
    public static final int P_MINOR = 70;

    /** The secondary dial. */
    public static final int S_CX = 248;
    public static final int S_CY = 76;
    public static final int S_HUB = 11;
    public static final int S_MINOR = 36;

    /** Socket sizes: keystone, minor (and path tab), shard moon. */
    public static final int KEY_SOCKET = 30;
    public static final int MINOR_SOCKET = 22;
    public static final int SHARD_SOCKET = 16;

    /** Glyph sizes inside them. */
    public static final int KEY_GLYPH = 20;
    public static final int MINOR_GLYPH = 14;
    public static final int SHARD_GLYPH = 8;

    /**
     * Degrees on the dial, 0 to the right, clockwise on screen. The two combat keystones sit above
     * the hub, the world keystone below it.
     */
    private static final int[] KEY_ANGLES = {210, 330, 90};
    private static final int WORLD_KEY = 2;

    /** Row 1 of the minors on the left half of the outer ring, row 2 on the right. */
    private static final int[][] PRIMARY_ROWS = {{150, 180, 210}, {330, 0, 30}};
    private static final int[][] SECONDARY_ROWS = {{140, 180, 220}, {320, 0, 40}};

    /** The four path tabs in the primary dial's corners (top-left of each socket), in path order. */
    private static final int[][] CORNERS = {{10, 24}, {164, 24}, {10, 198}, {164, 198}};

    /** The three secondary tabs, left to right, beside the secondary dial. */
    public static final int SECONDARY_TAB_X = 310;
    public static final int SECONDARY_TAB_Y = 50;
    public static final int SECONDARY_TAB_STEP = 24;

    /** The shards: three rows of three moons. */
    public static final int SHARD_LABEL_X = 202;
    public static final int SHARD_LABEL_Y = 134;
    public static final int SHARD_MOON_Y = SHARD_LABEL_Y + 11;
    public static final int SHARD_ROW_STEP = 64;
    public static final int SHARD_MOON_STEP = 17;

    /** The keystone box along the bottom right. */
    public static final int BLURB_X0 = 198;
    public static final int BLURB_Y0 = 176;
    public static final int BLURB_X1 = PANEL_W - 8;
    public static final int BLURB_Y1 = PANEL_H - 6;

    /** A point in panel pixels. */
    public record Point(int x, int y) {
    }

    private OrreryLayout() {
    }

    private static Point polar(int cx, int cy, int radius, double degrees) {
        double a = Math.toRadians(degrees);
        return new Point((int) Math.round(cx + radius * Math.cos(a)), (int) Math.round(cy + radius * Math.sin(a)));
    }

    /**
     * The centre of a keystone socket on the primary dial. {@code combatIndex} 0 and 1 are the two
     * combat keystones; {@code world} puts the world keystone below the hub. {@code turn} is the
     * dial's turn in degrees while it spins to a new path, 0 at rest.
     */
    public static Point primaryKeystone(int combatIndex, boolean world, double turn) {
        return polar(P_CX, P_CY, P_KEY, KEY_ANGLES[world ? WORLD_KEY : Math.min(combatIndex, 1)] + turn);
    }

    /** The centre of a minor's socket on the primary dial: row 0 or 1, place 0 to 2 in the row. */
    public static Point primaryMinor(int row, int place, double turn) {
        return polar(P_CX, P_CY, P_MINOR, PRIMARY_ROWS[row][place] + turn);
    }

    public static Point secondaryMinor(int row, int place) {
        return polar(S_CX, S_CY, S_MINOR, SECONDARY_ROWS[row][place]);
    }

    /** The centre of the {@code index}th path tab round the primary dial (paths in their fixed order). */
    public static Point primaryTab(int index) {
        return new Point(CORNERS[index][0] + MINOR_SOCKET / 2, CORNERS[index][1] + MINOR_SOCKET / 2);
    }

    /** The centre of the {@code index}th secondary tab (the three paths that aren't the primary). */
    public static Point secondaryTab(int index) {
        return new Point(SECONDARY_TAB_X + index * SECONDARY_TAB_STEP + MINOR_SOCKET / 2,
                SECONDARY_TAB_Y + MINOR_SOCKET / 2);
    }

    /** The centre of a shard moon: its row (0 to 2) and place in the row (0 to 2). */
    public static Point shardMoon(int row, int place) {
        return new Point(SHARD_LABEL_X + row * SHARD_ROW_STEP + place * SHARD_MOON_STEP + SHARD_SOCKET / 2,
                SHARD_MOON_Y + SHARD_SOCKET / 2);
    }

    /** The centre of a shard row's name, under its moons. */
    public static Point shardRowLabel(int row) {
        return new Point(SHARD_LABEL_X + row * SHARD_ROW_STEP + 25, SHARD_LABEL_Y + 29);
    }

    /**
     * The pixels of a growth ring round a minor's socket, as offsets from the socket's centre with
     * the angle each lies at, clockwise from the top in degrees: a growing rune fills the ring from
     * the top as it grows. Radius 13.4, a pixel and a fifth wide, as the mockup's.
     */
    public static List<RingPixel> growthRing() {
        List<RingPixel> out = new ArrayList<>();
        double outer = 13.4;
        double inner = outer - 1.2;
        for (int y = -15; y <= 15; y++) {
            for (int x = -15; x <= 15; x++) {
                double dx = x + 0.5;
                double dy = y + 0.5;
                double d = Math.hypot(dx, dy);
                if (d <= outer && d > inner) {
                    double angle = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0) % 360.0;
                    out.add(new RingPixel(x, y, angle));
                }
            }
        }
        return out;
    }

    /** One pixel of the growth ring. */
    public record RingPixel(int dx, int dy, double angle) {
    }

    /** Whether {@code (x, y)} is inside the circle of {@code radius} round {@code centre}. */
    public static boolean inside(Point centre, double radius, double x, double y) {
        return Math.hypot(x - centre.x(), y - centre.y()) <= radius;
    }
}
