package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The network-wide "which portals are on" set, and the one travel rule both the destination list
 * and the travel command apply.
 */
class GatewayNetworkStateTest {

    private static final ResourceLocation EARTH = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");
    private static final ResourceLocation PLANET_3 = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "planet_3");

    @Test
    void startsWithNothingActive() {
        GatewayNetworkState state = new GatewayNetworkState();
        assertFalse(state.isActive(EARTH));
        assertTrue(state.activePlanets().isEmpty());
    }

    @Test
    void activateReportsOnlyTheFirstTime() {
        GatewayNetworkState state = new GatewayNetworkState();
        assertTrue(state.activate(MOON));
        assertFalse(state.activate(MOON), "re-activating must not report a change");
        assertTrue(state.isActive(MOON));
    }

    @Test
    void travelNeedsBothEndsActive() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH);
        assertFalse(state.canTravel(EARTH, MOON), "destination inactive");
        assertFalse(state.canTravel(MOON, EARTH), "origin inactive");

        state.activate(MOON);
        assertTrue(state.canTravel(EARTH, MOON));
        assertTrue(state.canTravel(MOON, EARTH));
        assertFalse(state.canTravel(EARTH, PLANET_3), "a planet never activated stays unreachable");
    }

    @Test
    void neverTravelsToItself() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH);
        assertFalse(state.canTravel(EARTH, EARTH));
    }

    @Test
    void survivesASaveAndLoad() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH);
        state.activate(MOON);

        JsonElement saved = GatewayNetworkState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        GatewayNetworkState loaded = GatewayNetworkState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();

        assertEquals(Set.of(EARTH, MOON), loaded.activePlanets());
        assertTrue(loaded.canTravel(EARTH, MOON));
    }

    @Test
    void exposedSetIsACopy() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH);
        Set<ResourceLocation> view = state.activePlanets();
        state.activate(MOON);
        assertFalse(view.contains(MOON), "a caller's snapshot must not change under it");
    }
}
