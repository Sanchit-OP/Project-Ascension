package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** What opens each planet's portal. */
class GatewayRulesTest {

    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");
    private static final ResourceLocation PLANET_3 = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "planet_3");

    @Test
    void earthTakesARelayCoil() {
        assertEquals(GatewayRules.Requirement.RELAY_COIL, GatewayRules.requirementFor(GatewayRules.EARTH));
    }

    @Test
    void everyOtherPlanetTakesItsOwnKey() {
        assertEquals(GatewayRules.Requirement.PLANET_KEY, GatewayRules.requirementFor(MOON));
        assertEquals(GatewayRules.Requirement.PLANET_KEY, GatewayRules.requirementFor(PLANET_3),
                "a new planet needs no new branch -- its boss's key names it");
    }

    @Test
    void aKeyOpensOnlyThePlanetItNames() {
        assertTrue(GatewayRules.keyOpens(MOON, MOON));
        assertFalse(GatewayRules.keyOpens(MOON, PLANET_3), "a Moon key must not open a later planet");
    }

    @Test
    void anUntaggedKeyOpensNothing() {
        assertFalse(GatewayRules.keyOpens(null, MOON));
    }
}
