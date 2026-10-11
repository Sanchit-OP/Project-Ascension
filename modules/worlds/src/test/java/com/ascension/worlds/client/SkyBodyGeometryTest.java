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
    void moonSitsOppositeTheSun() {
        assertVector(0, -1, 0, SkyBodyGeometry.moonDirection(0.0));  // noon, underfoot
        assertVector(1, 0, 0, SkyBodyGeometry.moonDirection(0.25));  // rising in the east at sunset
        assertVector(0, 1, 0, SkyBodyGeometry.moonDirection(0.5));   // midnight, overhead
    }

    @Test
    void arcBasisNeverFlipsAcrossTheZenith() {
        double[][] beforeMidnight = SkyBodyGeometry.arcBasis(SkyBodyGeometry.moonDirection(0.49));
        double[][] afterMidnight = SkyBodyGeometry.arcBasis(SkyBodyGeometry.moonDirection(0.51));
        assertTrue(SkyBodyGeometry.dot(beforeMidnight[0], afterMidnight[0]) > 0.99, "right holds");
        assertTrue(SkyBodyGeometry.dot(beforeMidnight[1], afterMidnight[1]) > 0.99, "up holds");
    }

    @Test
    void arcBasisIsUprightAtMoonriseAndMatchesTheFixedOnesHandedness() {
        double[] east = SkyBodyGeometry.moonDirection(0.25);
        double[][] arc = SkyBodyGeometry.arcBasis(east);
        double[][] fixed = SkyBodyGeometry.basis(east);
        assertVector(fixed[0][0], fixed[0][1], fixed[0][2], arc[0]);
        assertVector(0, 1, 0, arc[1]);
    }

    @Test
    void moonPhasesLightTheDiscLikeVanillasBrightness() {
        double[] direction = SkyBodyGeometry.moonDirection(0.5);
        double[][] basis = SkyBodyGeometry.arcBasis(direction);
        double full = phaseCentre(0, direction, basis);
        double gibbous = phaseCentre(1, direction, basis);
        double quarter = phaseCentre(2, direction, basis);
        double fresh = phaseCentre(4, direction, basis);
        assertEquals(1.0, full, EPSILON);
        assertTrue(gibbous > quarter && gibbous < full);
        assertEquals(SkyBodyGeometry.NIGHT_SIDE, quarter, EPSILON, "a quarter's terminator crosses the centre");
        assertEquals(SkyBodyGeometry.NIGHT_SIDE, fresh, EPSILON);
        assertEquals(phaseCentre(1, direction, basis), phaseCentre(7, direction, basis), EPSILON,
                "waning and waxing gibbous are equally bright");
        assertEquals(phaseCentre(0, direction, basis), phaseCentre(8, direction, basis), EPSILON, "phase wraps");
    }

    @Test
    void waningIsLitOnTheLeftWaxingOnTheRight() {
        double[] direction = SkyBodyGeometry.moonDirection(0.5);
        double[][] basis = SkyBodyGeometry.arcBasis(direction);
        double[] lastQuarter = SkyBodyGeometry.phaseLight(2, direction, basis[0]);
        double[] firstQuarter = SkyBodyGeometry.phaseLight(6, direction, basis[0]);
        assertTrue(edge(-0.9, direction, basis, lastQuarter) > edge(0.9, direction, basis, lastQuarter));
        assertTrue(edge(0.9, direction, basis, firstQuarter) > edge(-0.9, direction, basis, firstQuarter));
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
            assertEquals(SkyBody.Motion.FIXED, earth.motion());
            assertTrue(!earth.additive(), "seen from an airless world, Earth covers the stars");
        }
    }

    /** The Overworld's sky file parses, rides vanilla's moon, and the blank vanilla moon ships. */
    @Test
    void shippedOverworldSkyReplacesVanillasMoon() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/ascension_worlds/ascension_sky/overworld.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            SkyBody.SkyFile sky = SkyBody.SkyFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals("minecraft:overworld", sky.dimension().toString());
            SkyBody moon = sky.bodies().get(0);
            assertEquals(SkyBody.Motion.VANILLA_MOON, moon.motion());
            assertTrue(moon.additive(), "under an atmosphere the sky shows through the dark side");
            assertTrue(getClass().getResource("/assets/ascension_worlds/textures/environment/moon.png") != null);
            SkyBody.GateLight light = moon.gateLight().orElseThrow();
            assertEquals("ascension_worlds:textures/environment/moon_signal.png", light.texture().toString());
            assertTrue(light.x() < 0 && light.y() > 0, "a little up and left of centre (lore-art-design.md, section 9)");
            assertTrue(getClass().getResource("/assets/ascension_worlds/textures/environment/moon_signal.png") != null);
        }
        try (var blank = getClass().getResourceAsStream("/assets/minecraft/textures/environment/moon_phases.png")) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(blank);
            assertEquals(128, image.getWidth());
            assertEquals(64, image.getHeight());
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    assertEquals(0, image.getRGB(x, y) >>> 24, "every texel transparent");
                }
            }
        }
    }

    @Test
    void theGateLightIsDarkUntilAGateIsLitThenBrightensToFull() {
        assertEquals(0.0F, SkyBody.GateLight.alpha(0));
        assertEquals(SkyBody.GateLight.FIRST_GATE, SkyBody.GateLight.alpha(1), 1.0e-6);
        assertEquals(1.0F, SkyBody.GateLight.alpha(7), 1.0e-6);
        assertEquals(1.0F, SkyBody.GateLight.alpha(12), 1.0e-6, "no brighter past the seventh glyph");
        for (int stage = 1; stage < 7; stage++) {
            assertTrue(SkyBody.GateLight.alpha(stage + 1) > SkyBody.GateLight.alpha(stage), "brighter with gate " + (stage + 1));
        }
    }

    @Test
    void aFarPlanetsOutlineIsItsRadius() {
        assertEquals(384.0, SkyBodyGeometry.silhouetteHalfSize(384.0, 100_000.0), 0.01);
    }

    @Test
    void upCloseTheOutlineIsWiderThanTheRadius() {
        // From twice the radius away, sight lines graze the sphere at 30 degrees off centre:
        // a disc at the centre must be 2/sqrt(3) radii wide to cover it.
        assertEquals(2.0 / Math.sqrt(3.0), SkyBodyGeometry.silhouetteHalfSize(1.0, 2.0), EPSILON);
    }

    @Test
    void atTheSurfaceTheOutlineIsCapped() {
        assertEquals(SkyBodyGeometry.MAX_SILHOUETTE, SkyBodyGeometry.silhouetteHalfSize(1.0, 1.0), EPSILON);
        assertEquals(SkyBodyGeometry.MAX_SILHOUETTE, SkyBodyGeometry.silhouetteHalfSize(1.0, 0.5), EPSILON);
    }

    private static double centre(double[] direction, double[][] basis, double[] sun) {
        return SkyBodyGeometry.brightness(0, 0, direction, basis[0], basis[1], sun);
    }

    private static double phaseCentre(int phase, double[] direction, double[][] basis) {
        return centre(direction, basis, SkyBodyGeometry.phaseLight(phase, direction, basis[0]));
    }

    private static double edge(double u, double[] direction, double[][] basis, double[] light) {
        return SkyBodyGeometry.brightness(u, 0, direction, basis[0], basis[1], light);
    }

    private static void assertVector(double x, double y, double z, double[] actual) {
        assertEquals(x, actual[0], EPSILON, "x");
        assertEquals(y, actual[1], EPSILON, "y");
        assertEquals(z, actual[2], EPSILON, "z");
    }

    @Test
    void aPackedColourUnpacksToItsThreeChannels() {
        float[] rgb = SkyBodyGeometry.rgb(0xFF8000);
        assertEquals(1.0, rgb[0], 1e-6);
        assertEquals(128 / 255.0, rgb[1], 1e-6);
        assertEquals(0.0, rgb[2], 1e-6);
    }
}
