package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The proof-of-portal the gateway travel command demands from non-operators. Every way it should
 * refuse matters as much as the way it should allow: each refusal is a route around spaceflight.
 */
class GatewayPickerSessionTest {

    private static final ResourceLocation EARTH = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");

    @Test
    void noSessionUntilAPortalOpensOne() {
        assertFalse(new GatewayPickerSession().isValidIn(EARTH, 100));
    }

    @Test
    void validWhereItWasOpenedWithinItsLifetime() {
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 1000);
        assertTrue(session.isValidIn(EARTH, 1000));
        assertTrue(session.isValidIn(EARTH, 1000 + GatewayPickerSession.LIFETIME_TICKS));
    }

    @Test
    void expires() {
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 1000);
        assertFalse(session.isValidIn(EARTH, 1000 + GatewayPickerSession.LIFETIME_TICKS + 1));
    }

    @Test
    void doesNotFollowThePlayerToAnotherDimension() {
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 1000);
        assertFalse(session.isValidIn(MOON, 1001));
    }

    @Test
    void oneTripPerOpening() {
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 1000);
        session.close();
        assertFalse(session.isValidIn(EARTH, 1001));
    }

    @Test
    void steppingIntoAFieldCountsOncePerEntry() {
        GatewayPickerSession session = new GatewayPickerSession();
        assertTrue(session.stepIntoField(100), "first tick in the field opens the screen");
        assertFalse(session.stepIntoField(100), "a second field block overlapped the same tick");
        for (long tick = 101; tick <= 150; tick++) {
            assertFalse(session.stepIntoField(tick), "still standing in it at tick " + tick);
        }
        // Tick 151: stepped out (no call). Tick 152: back in.
        assertTrue(session.stepIntoField(152), "stepped out for a tick and back in");
    }

    @Test
    void theVeryFirstTickOfAWorldStillCountsAsAnEntry() {
        assertTrue(new GatewayPickerSession().stepIntoField(0));
    }

    @Test
    void steppingInAfterGameTimeWasResetCountsAsAnEntry() {
        GatewayPickerSession session = new GatewayPickerSession();
        session.stepIntoField(5000);
        assertTrue(session.stepIntoField(10));
    }

    @Test
    void gameTimeGoingBackwardsIsNotAFreshSession() {
        // A level's game time can be reset by /time or a world swap; a session from the "future"
        // must not count as valid forever.
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 5000);
        assertFalse(session.isValidIn(EARTH, 4000));
    }
}
