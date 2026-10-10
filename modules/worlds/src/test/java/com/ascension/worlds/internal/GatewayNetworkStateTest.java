package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The network-wide "which portals are on, and where" state, and the one travel rule both the
 * destination screen and the travel command apply.
 */
class GatewayNetworkStateTest {

    private static final ResourceLocation EARTH = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");
    private static final ResourceLocation PLANET_3 = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "planet_3");

    private static final BlockPos EARTH_PORTAL = new BlockPos(296, 70, 9);
    private static final BlockPos MOON_PORTAL = new BlockPos(-152, 24, 9);

    @Test
    void startsWithNothingActive() {
        GatewayNetworkState state = new GatewayNetworkState();
        assertFalse(state.isActive(EARTH));
        assertTrue(state.activePlanets().isEmpty());
        assertNull(state.portalAt(EARTH));
    }

    @Test
    void activateReportsOnlyTheFirstTime() {
        GatewayNetworkState state = new GatewayNetworkState();
        assertTrue(state.activate(MOON, MOON_PORTAL));
        assertFalse(state.activate(MOON, MOON_PORTAL), "re-activating must not report a change");
        assertTrue(state.isActive(MOON));
    }

    @Test
    void remembersWhereEachPortalIs() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH, EARTH_PORTAL);
        state.activate(MOON, MOON_PORTAL);
        assertEquals(EARTH_PORTAL, state.portalAt(EARTH));
        assertEquals(MOON_PORTAL, state.portalAt(MOON));
    }

    @Test
    void reactivatingElsewhereMovesTheLandingPoint() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(MOON, MOON_PORTAL);
        BlockPos placed = new BlockPos(0, 30, 0);
        state.activate(MOON, placed);
        assertEquals(placed, state.portalAt(MOON), "the most recently activated portal is where travel lands");
    }

    @Test
    void travelNeedsBothEndsActive() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH, EARTH_PORTAL);
        assertFalse(state.canTravel(EARTH, MOON), "destination inactive");
        assertFalse(state.canTravel(MOON, EARTH), "origin inactive");

        state.activate(MOON, MOON_PORTAL);
        assertTrue(state.canTravel(EARTH, MOON));
        assertTrue(state.canTravel(MOON, EARTH));
        assertFalse(state.canTravel(EARTH, PLANET_3), "a planet never activated stays unreachable");
    }

    @Test
    void neverTravelsToItself() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH, EARTH_PORTAL);
        assertFalse(state.canTravel(EARTH, EARTH));
    }

    @Test
    void survivesASaveAndLoad() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH, EARTH_PORTAL);
        state.activate(MOON, MOON_PORTAL);

        JsonElement saved = GatewayNetworkState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        GatewayNetworkState loaded = GatewayNetworkState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();

        assertEquals(Set.of(EARTH, MOON), loaded.activePlanets());
        assertEquals(EARTH_PORTAL, loaded.portalAt(EARTH));
        assertEquals(MOON_PORTAL, loaded.portalAt(MOON));
    }

    @Test
    void loadsTheOriginalFormatWithoutPositions() {
        // What a world saved before portal positions were tracked holds: a bare list of ids.
        JsonElement legacy = JsonParser.parseString("[\"minecraft:overworld\", \"ascension_worlds:moon\"]");
        GatewayNetworkState loaded = GatewayNetworkState.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertEquals(Set.of(EARTH, MOON), loaded.activePlanets());
        assertTrue(loaded.canTravel(EARTH, MOON));
        assertNull(loaded.portalAt(MOON), "no position known -- travel falls back to the ordinary landing");
    }

    @Test
    void exposedSetIsACopy() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(EARTH, EARTH_PORTAL);
        Set<ResourceLocation> view = state.activePlanets();
        state.activate(MOON, MOON_PORTAL);
        assertFalse(view.contains(MOON), "a caller's snapshot must not change under it");
    }

    @Test
    void debugLightingWithoutAPortalLeavesNoLandingPoint() {
        GatewayNetworkState state = new GatewayNetworkState();
        assertTrue(state.activateWithoutPortal(PLANET_3));
        assertFalse(state.activateWithoutPortal(PLANET_3), "lighting twice must not report a change");
        assertTrue(state.isActive(PLANET_3));
        assertNull(state.portalAt(PLANET_3), "travel there falls back to the ordinary landing");
    }

    @Test
    void deactivatingPutsTheGateOutAndForgetsItsPortal() {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activate(MOON, MOON_PORTAL);
        assertTrue(state.deactivate(MOON));
        assertFalse(state.isActive(MOON));
        assertNull(state.portalAt(MOON));
        assertFalse(state.deactivate(MOON), "a dark gate cannot go dark again");
    }
}
