package com.ascension.combat.client.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The "hold" section's parsing and easing, which tools/render-item-model.py's hold_pose() copies
 * for the previews. Pure arithmetic; how it looks on a player is for the live test (ADR-0008).
 */
class HoldAnimationTest {
    private static final float EPS = 1e-4f;

    private static HoldAnimation parse(String json) {
        return HoldAnimation.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    private static final String TWO_KEYS = "{\"grip\": [8, 10, 8], \"idle\": {\"right_arm\": [15, 0, 8]},"
            + " \"attacks\": [[{\"t\": 0, \"right_arm\": [0, 0, 0], \"left_arm\": [0, 0, 0], \"body_twist\": 0},"
            + " {\"t\": 1, \"right_arm\": [-90, 40, 0], \"left_arm\": [-80, 0, 0], \"body_twist\": 30,"
            + " \"item\": [0, 0, 90]}]]}";

    @Test
    void idleIsOneHandedWithoutALeftArm() {
        HoldAnimation hold = parse(TWO_KEYS);
        assertFalse(hold.idle.twoHanded());
        assertEquals(15, hold.idle.rightArm()[0], EPS);
        assertEquals(1f, hold.walkSwing, EPS); // the default keeps all of vanilla's walking swing
    }

    @Test
    void keyframesEaseWithSmoothstep() {
        HoldAnimation.Pose mid = parse(TWO_KEYS).attack(0, 0.5f);
        // smoothstep(0.5) is 0.5: halfway.
        assertEquals(-45, mid.rightArm()[0], EPS);
        assertEquals(15, mid.bodyTwist(), EPS);
        HoldAnimation.Pose quarter = parse(TWO_KEYS).attack(0, 0.25f);
        // smoothstep(0.25) = 0.15625, so the arm has moved 0.15625 of the way.
        assertEquals(-90 * 0.15625f, quarter.rightArm()[0], EPS);
        assertTrue(quarter.twoHanded());
    }

    @Test
    void progressOutsideTheKeysHoldsTheEnds() {
        HoldAnimation hold = parse(TWO_KEYS);
        assertEquals(0, hold.attack(0, -1f).rightArm()[0], EPS);
        assertEquals(90, hold.attack(0, 2f).item()[2], EPS);
    }

    @Test
    void theShippedVeigoHoldParses() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/assets/ascension_combat/weapon_models/veigos_blade.json")) {
            assertNotNull(in);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            HoldAnimation hold = WeaponModel.parse(json).hold();
            assertNotNull(hold, "Veigo's Blade has a hold section");
            assertEquals(3, hold.attackCount(), "two slashes and a stab");
            assertTrue(hold.attack(0, 0.45f).twoHanded(), "attacks are two-handed");
            FirstPersonHold fp = WeaponModel.parse(json).firstPerson();
            assertNotNull(fp, "Veigo's Blade has a first-person hold");
            assertEquals(hold.attackCount(), fp.attackCount(), "the same attacks in both views");
            assertFalse(fp.idle.twoHands(), "idle in one hand");
            assertTrue(fp.attack(2, 0.45f).twoHands(), "the stab in both");
            // The off hand's grip sits below the main hand's, far enough apart for two fists.
            assertTrue(fp.rightHand[1] - fp.leftHand[1] >= 4, "four pixels or more between the grips");
        }
    }
}
