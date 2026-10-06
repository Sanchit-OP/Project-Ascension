package com.ascension.combat.client.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The weapon-model format's conventions, which tools/generate-combat-models.py writes and
 * tools/render-item-model.py previews with: if these drift apart, the preview stops showing what
 * the game draws. Pure parsing, no game running; says nothing about how the model looks in game
 * (ADR-0008).
 */
class WeaponModelTest {
    private static final float EPS = 1e-4f;

    private static WeaponModel parse(String json) {
        return WeaponModel.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    /** One cube, its south face only, in a part with the given rotation. */
    private static String oneFace(String rotation) {
        return "{\"texture\": \"ascension_combat:textures/x.png\", \"texture_size\": [32, 16],"
                + " \"parts\": [{\"rotation\": " + rotation + ", \"cubes\": [{\"from\": [0, 0, 0], \"to\": [2, 1, 1],"
                + " \"faces\": {\"south\": [4, 2, 8, 6]}}]}]}";
    }

    private static void assertVertex(float[] q, int v, float x, float y, float z) {
        assertEquals(x, q[v * 5], EPS, "x of vertex " + v);
        assertEquals(y, q[v * 5 + 1], EPS, "y of vertex " + v);
        assertEquals(z, q[v * 5 + 2], EPS, "z of vertex " + v);
    }

    @Test
    void southFaceCornersAndTexelUvsFollowVanillaFaceOrder() {
        float[] q = parse(oneFace("[0, 0, 0]")).quads();
        assertEquals(WeaponModel.QUAD_FLOATS, q.length);
        // Vanilla FaceInfo order for south: (minX, maxY), (minX, minY), (maxX, minY), (maxX, maxY).
        assertVertex(q, 0, 0, 1, 1);
        assertVertex(q, 1, 0, 0, 1);
        assertVertex(q, 2, 2, 0, 1);
        assertVertex(q, 3, 2, 1, 1);
        // Texel UVs over the texture size: (u0, v0), (u0, v1), (u1, v1), (u1, v0).
        assertEquals(4 / 32f, q[3], EPS);
        assertEquals(2 / 16f, q[4], EPS);
        assertEquals(4 / 32f, q[8], EPS);
        assertEquals(6 / 16f, q[9], EPS);
        assertEquals(8 / 32f, q[13], EPS);
        assertEquals(6 / 16f, q[14], EPS);
        // The normal points out of the face.
        assertEquals(0, q[20], EPS);
        assertEquals(0, q[21], EPS);
        assertEquals(1, q[22], EPS);
    }

    @Test
    void positiveZRotationTurnsXTowardY() {
        float[] q = parse(oneFace("[0, 0, 90]")).quads();
        // (2, 0, 1) turned 90 degrees about z ends up at (0, 2, 1).
        assertVertex(q, 2, 0, 2, 1);
    }

    @Test
    void xIsAppliedBeforeZ() {
        float[] q = parse(oneFace("[90, 0, 90]")).quads();
        // (0, 1, 1): X first gives (0, -1, 1), then Z gives (1, 0, 1).
        // (Z first would give (-1, 0, 1), then X (-1, -1, 0).)
        assertVertex(q, 0, 1, 0, 1);
    }

    @Test
    void theShippedVeigoModelParses() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/assets/ascension_combat/weapon_models/veigos_blade.json")) {
            assertNotNull(in, "veigos_blade.json is on the classpath");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            WeaponModel model = WeaponModel.parse(json);
            assertNotNull(model.glowTexture());
            float[] q = model.quads();
            assertTrue(q.length > 100 * WeaponModel.QUAD_FLOATS, "a real model, not a stub");
            for (int i = 0; i < q.length; i += WeaponModel.QUAD_FLOATS) {
                float n = q[i + 20] * q[i + 20] + q[i + 21] * q[i + 21] + q[i + 22] * q[i + 22];
                assertEquals(1, n, 1e-3f, "unit normal");
                for (int v = 0; v < 4; v++) {
                    float u = q[i + v * 5 + 3], w = q[i + v * 5 + 4];
                    assertTrue(u >= 0 && u <= 1 && w >= 0 && w <= 1, "UV inside the texture");
                }
            }
        }
    }
}
