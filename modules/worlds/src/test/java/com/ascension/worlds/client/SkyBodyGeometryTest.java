package com.ascension.worlds.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SkyBodyGeometryTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void yawFollowsMinecraftsCompass() {
        assertVector(0, 0, 1, SkyBodyGeometry.direction(0, 0));    // south
        assertVector(-1, 0, 0, SkyBodyGeometry.direction(90, 0));  // west
        assertVector(0, 0, -1, SkyBodyGeometry.direction(180, 0)); // north
        assertVector(1, 0, 0, SkyBodyGeometry.direction(270, 0));  // east
        assertVector(0, 1, 0, SkyBodyGeometry.direction(123, 90)); // overhead, whatever the yaw
    }

    @Test
    void sunMatchesVanillasSky() {
        assertVector(0, 1, 0, SkyBodyGeometry.sunDirection(0.0));    // noon, overhead
        assertVector(-1, 0, 0, SkyBodyGeometry.sunDirection(0.25));  // setting in the west
        assertVector(0, -1, 0, SkyBodyGeometry.sunDirection(0.5));   // midnight, underfoot
        assertVector(1, 0, 0, SkyBodyGeometry.sunDirection(0.75));   // rising in the east
    }

    @Test
    void discStaysUprightAndFacesTheViewer() {
        double[] direction = SkyBodyGeometry.direction(170, 48);
        double[][] basis = SkyBodyGeometry.basis(direction);
        double[] right = basis[0];
        double[] up = basis[1];
        assertEquals(0.0, right[1], EPSILON, "right has no vertical component: no roll");
        assertTrue(up[1] > 0, "up points skyward");
        assertEquals(0.0, SkyBodyGeometry.dot(right, direction), EPSILON);
        assertEquals(0.0, SkyBodyGeometry.dot(up, direction), EPSILON);
        assertEquals(1.0, SkyBodyGeometry.dot(right, right), EPSILON);
        assertEquals(1.0, SkyBodyGeometry.dot(up, up), EPSILON);
    }

    @Test
    void bodyOverheadStillGetsABasis() {
        double[][] basis = SkyBodyGeometry.basis(new double[] {0, 1, 0});
        assertVector(1, 0, 0, basis[0]);
        assertEquals(0.0, SkyBodyGeometry.dot(basis[1], new double[] {0, 1, 0}), EPSILON);
    }

    @Test
    void fullWhenTheSunIsBehindTheViewerNewWhenBehindTheBody() {
        double[] direction = SkyBodyGeometry.direction(0, 0);
        double[][] basis = SkyBodyGeometry.basis(direction);
        double[] behindViewer = {0, 0, -1};
        double[] behindBody = {0, 0, 1};
        assertEquals(1.0, centre(direction, basis, behindViewer), EPSILON);
        assertEquals(SkyBodyGeometry.NIGHT_SIDE, centre(direction, basis, behindBody), EPSILON);
    }

    @Test
    void halfLitAtQuarterPhase() {
        double[] direction = SkyBodyGeometry.direction(0, 0);
        double[][] basis = SkyBodyGeometry.basis(direction);
        double[] sunToTheRight = basis[0];
        double litEdge = SkyBodyGeometry.brightness(0.9, 0, direction, basis[0], basis[1], sunToTheRight);
        double darkEdge = SkyBodyGeometry.brightness(-0.9, 0, direction, basis[0], basis[1], sunToTheRight);
        assertTrue(litEdge > 0.9);
        assertEquals(SkyBodyGeometry.NIGHT_SIDE, darkEdge, EPSILON);
    }

    @Test
    void daylightIsTheUnflooredBrightness() {
        double[] direction = SkyBodyGeometry.direction(0, 0);
        double[][] basis = SkyBodyGeometry.basis(direction);
        double[] behindBody = {0, 0, 1};
        assertEquals(0.0, SkyBodyGeometry.daylight(0, 0, direction, basis[0], basis[1], behindBody), EPSILON);
        double[] sun = SkyBodyGeometry.sunDirection(0.1);
        double day = SkyBodyGeometry.daylight(0.3, -0.2, direction, basis[0], basis[1], sun);
        assertEquals(SkyBodyGeometry.NIGHT_SIDE + (1 - SkyBodyGeometry.NIGHT_SIDE) * day,
                SkyBodyGeometry.brightness(0.3, -0.2, direction, basis[0], basis[1], sun), EPSILON);
    }

    @Test
    void halfSizeMatchesTheAngle() {
        assertEquals(100.0, SkyBodyGeometry.halfSize(90, 100), 1.0e-6);
        assertEquals(100 * Math.tan(Math.toRadians(6)), SkyBodyGeometry.halfSize(12, 100), 1.0e-9);
    }

    /** The shipped Moon sky file parses, and puts Earth where the gateway plaza looks. */
    @Test
    void shippedMoonSkyParses() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/ascension_worlds/ascension_sky/moon.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            SkyBody.SkyFile sky = SkyBody.SkyFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals("ascension_worlds:moon", sky.dimension().toString());
            assertEquals(1, sky.bodies().size());
            SkyBody earth = sky.bodies().get(0);
            assertEquals("ascension_worlds:textures/environment/earth.png", earth.texture().toString());
            assertTrue(earth.lit());
            assertTrue(earth.glow().isPresent());
            assertTrue(getClass().getResource("/assets/ascension_worlds/textures/environment/earth.png") != null);
        }
    }

    private static double centre(double[] direction, double[][] basis, double[] sun) {
        return SkyBodyGeometry.brightness(0, 0, direction, basis[0], basis[1], sun);
    }

    private static void assertVector(double x, double y, double z, double[] actual) {
        assertEquals(x, actual[0], EPSILON, "x");
        assertEquals(y, actual[1], EPSILON, "y");
        assertEquals(z, actual[2], EPSILON, "z");
    }
}
