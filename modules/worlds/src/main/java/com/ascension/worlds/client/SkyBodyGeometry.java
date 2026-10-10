package com.ascension.worlds.client;

/**
 * The arithmetic behind a {@link SkyBody}: where it sits on the sky, how its disc is oriented, and
 * how brightly each point of it is lit, by the sun or by a moon phase. Plain {@code double[3]} vectors, no Minecraft or render
 * state, so it is unit-tested ({@code SkyBodyGeometryTest}) rather than checked by squinting at
 * the sky.
 *
 * <p>World axes are Minecraft's: +X east, +Y up, +Z south.
 */
final class SkyBodyGeometry {

    /**
     * How bright the unlit side of a body is, as a fraction of full. Not zero: a black disc reads
     * as a hole in the sky, and real Earth's night side is never quite black from the Moon.
     */
    static final double NIGHT_SIDE = 0.07;

    /**
     * The unit vector from the viewer toward a body.
     *
     * @param yawDegrees       compass heading, Minecraft's convention: 0 south, 90 west,
     *                         180 north, 270 east
     * @param elevationDegrees angle above the horizon, -90 to 90
     */
    static double[] direction(double yawDegrees, double elevationDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double elevation = Math.toRadians(elevationDegrees);
        double horizontal = Math.cos(elevation);
        return new double[] {-Math.sin(yaw) * horizontal, Math.sin(elevation), Math.cos(yaw) * horizontal};
    }

    /**
     * The unit vector toward the sun at a point in the day, matching where vanilla's sky renderer
     * draws it ({@code LevelRenderer.renderSky}: rotate {@code (0, 1, 0)} about X by the day
     * fraction, then about Y by -90 degrees). The sun crosses the sky from east to west in the X/Y
     * plane.
     *
     * @param timeOfDay {@code Level.getTimeOfDay}, 0 to 1
     */
    static double[] sunDirection(double timeOfDay) {
        double angle = timeOfDay * Math.PI * 2.0;
        return new double[] {-Math.sin(angle), Math.cos(angle), 0.0};
    }

    /**
     * The unit vector toward vanilla's moon: straight opposite the sun, where
     * {@code LevelRenderer.renderSky} draws it.
     *
     * @param timeOfDay {@code Level.getTimeOfDay}, 0 to 1
     */
    static double[] moonDirection(double timeOfDay) {
        double[] sun = sunDirection(timeOfDay);
        return new double[] {-sun[0], -sun[1], -sun[2]};
    }

    /**
     * The disc's axes for a body travelling the sun's arc, which lies in the X/Y plane: "right" is
     * always south, square to the arc, and "up" follows the body round. It rises upright in the
     * east and sets upside down in the west, turning with the sky the way vanilla's moon does.
     * {@link #basis}'s keep-upright rule would instead flip the disc over as it crossed the zenith.
     *
     * @return {@code {right, up}}
     */
    static double[][] arcBasis(double[] direction) {
        double[] right = {0.0, 0.0, 1.0};
        double[] up = normalize(cross(right, direction));
        return new double[][] {right, up};
    }

    /**
     * Where the light must come from to show vanilla's moon phase on a body, whatever the real
     * sun says: phase 0 is full (lit from behind the viewer), 4 is new (lit from behind the body),
     * and between them the light swings round to the side. Waning (1 to 3) is lit on the left,
     * waxing (5 to 7) on the right, as seen from the northern hemisphere.
     *
     * @param phase {@code Level.getMoonPhase}, 0 to 7
     */
    static double[] phaseLight(int phase, double[] direction, double[] right) {
        double angle = Math.floorMod(phase, 8) * Math.PI / 4.0;
        double towardViewer = Math.cos(angle);
        double side = -Math.sin(angle);
        return new double[] {
            -direction[0] * towardViewer + right[0] * side,
            -direction[1] * towardViewer + right[1] * side,
            -direction[2] * towardViewer + right[2] * side,
        };
    }

    /**
     * The disc's own right and up axes, facing the viewer, kept upright: "up" is as close to world
     * up as it can be, so a body never rolls as the camera turns. For a body straight overhead or
     * underfoot, where world up is useless, it falls back to east as "right".
     *
     * @return {@code {right, up}}
     */
    static double[][] basis(double[] direction) {
        double[] right = cross(direction, new double[] {0.0, 1.0, 0.0});
        if (length(right) < 1.0e-6) {
            right = new double[] {1.0, 0.0, 0.0};
        }
        right = normalize(right);
        double[] up = normalize(cross(right, direction));
        return new double[][] {right, up};
    }

    /**
     * How much sunlight falls on a point of the body's visible face, treating it as a sphere.
     * {@code (u, v)} is the point on the disc, each -1 to 1, along {@code right} and {@code up};
     * the sphere's surface normal there is rebuilt from it. Lambert: 1 facing the sun, 0 on the
     * night side, and the terminator falls wherever the normal turns away from the sun.
     *
     * @return 0 to 1
     */
    static double daylight(double u, double v, double[] direction, double[] right, double[] up, double[] sun) {
        double toward = Math.sqrt(Math.max(0.0, 1.0 - u * u - v * v));
        double[] normal = {
            u * right[0] + v * up[0] - toward * direction[0],
            u * right[1] + v * up[1] - toward * direction[1],
            u * right[2] + v * up[2] - toward * direction[2],
        };
        return Math.max(0.0, dot(normal, sun));
    }

    /**
     * How bright to draw that point: {@link #daylight}, floored at {@link #NIGHT_SIDE}. This is
     * what draws the phase.
     *
     * @return {@link #NIGHT_SIDE} to 1
     */
    static double brightness(double u, double v, double[] direction, double[] right, double[] up, double[] sun) {
        return NIGHT_SIDE + (1.0 - NIGHT_SIDE) * daylight(u, v, direction, right, up, sun);
    }

    /** Half the disc's width at {@code distance}, for a body {@code angularSizeDegrees} across. */
    static double halfSize(double angularSizeDegrees, double distance) {
        return distance * Math.tan(Math.toRadians(angularSizeDegrees) / 2.0);
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0],
        };
    }

    private static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    private static double[] normalize(double[] a) {
        double length = length(a);
        return new double[] {a[0] / length, a[1] / length, a[2] / length};
    }

    private SkyBodyGeometry() {
    }
}
