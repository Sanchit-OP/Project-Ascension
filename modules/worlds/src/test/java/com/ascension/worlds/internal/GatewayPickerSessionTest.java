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
    void gameTimeGoingBackwardsIsNotAFreshSession() {
        // A level's game time can be reset by /time or a world swap; a session from the "future"
        // must not count as valid forever.
        GatewayPickerSession session = new GatewayPickerSession();
        session.open(EARTH, 5000);
        assertFalse(session.isValidIn(EARTH, 4000));
    }
}
