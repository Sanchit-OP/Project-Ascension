package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The airship-only rule (ADR-0015): {@code vehicleOnly} closes the on-foot road to space, but never
 * before a vehicle exists to replace it, and never for creative or spectator players.
 */
class AscentCarriersTest {

    @Test
    void vehicleOnlyWithAVehicleStopsASurvivalPlayer() {
        assertFalse(AscentCarriers.onFootAllowed(true, true, false));
    }

    @Test
    void vehicleOnlyWithNoVehicleInstalledLeavesSpaceReachable() {
        assertTrue(AscentCarriers.onFootAllowed(true, false, false));
    }

    @Test
    void creativePlayersAreNeverStopped() {
        assertTrue(AscentCarriers.onFootAllowed(true, true, true));
    }

    @Test
    void turningTheSettingOffOpensTheOnFootRoad() {
        assertTrue(AscentCarriers.onFootAllowed(false, true, false));
    }
}
