package com.ascension.worlds.internal.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The gateway data actually shipped in this jar &mdash; the design session's frame and Moon site
 * &mdash; held to the contract. A datapack registry entry that fails to parse stops a world from
 * loading, so a design edit that breaks the contract should fail the build, not a player's save.
 */
class ShippedGatewayDataTest {

    private static final String FRAME = "/data/ascension_worlds/ascension_worlds/gateway_frame/gateway.json";
    private static final String MOON_SITE = "/data/ascension_worlds/ascension_worlds/gateway_site/gateway_moon_site.json";

    private static <T> T load(String path, Codec<T> codec) {
        try (InputStream in = ShippedGatewayDataTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing from the jar: " + path);
            JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return codec.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String key(int[] at) {
        return at[0] + "," + at[1] + "," + at[2];
    }

    @Test
    void theFrameLoadsAndIsRecognisedWhicheverWayItIsBuilt() {
        GatewayFrame frame = load(FRAME, GatewayFrame.CODEC);
        for (GatewayFrame.Facing facing : GatewayFrame.Facing.values()) {
            GatewayFrame.Placement built = frame.placeOriginAt(0, 64, 0, facing);
            java.util.Map<String, Character> blocks = new java.util.HashMap<>();
            for (GatewayFrame.Cell cell : frame.cells()) {
                if (cell.symbol() != frame.fieldSymbol()) {
                    blocks.put(key(built.world(cell.x(), cell.y(), cell.z())), cell.symbol());
                }
            }
            int[] keystone = built.keystone();
            assertTrue(frame.match(keystone[0], keystone[1], keystone[2], (symbol, x, y, z) -> {
                Character here = blocks.get(x + "," + y + "," + z);
                return symbol == frame.fieldSymbol() ? here == null : here != null && here == symbol;
            }).isPresent(), "shipped frame not recognised facing " + facing);
        }
    }

    @Test
    void theMoonSiteLoadsAndNeverTouchesAFrameCell() {
        GatewayFrame frame = load(FRAME, GatewayFrame.CODEC);
        GatewaySite site = load(MOON_SITE, GatewaySite.CODEC);
        GatewayFrame.Placement placement = frame.placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH);

        Set<String> frameCells = new HashSet<>();
        for (GatewayFrame.Cell cell : frame.cells()) {
            frameCells.add(key(placement.world(cell.x(), cell.y(), cell.z())));
        }
        for (GatewayFrame.Cell cell : site.cells()) {
            assertFalse(frameCells.contains(key(site.world(placement, cell))),
                    "site cell " + cell + " lands on a frame cell");
        }
    }

    @Test
    void nothingInTheSiteBlocksWhereArrivalsStand() {
        GatewayFrame frame = load(FRAME, GatewayFrame.CODEC);
        GatewaySite site = load(MOON_SITE, GatewaySite.CODEC);
        GatewayFrame.Placement placement = frame.placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH);
        int[] arrival = placement.arrivalSpot();

        Set<String> siteCells = new HashSet<>();
        site.cells().forEach(cell -> siteCells.add(key(site.world(placement, cell))));
        // The two blocks a player occupies at the arrival spot (the landing search also tries one
        // lower, onto the reveal floor; the design keeps that clear too).
        for (int dy = -1; dy <= 1; dy++) {
            int[] at = {arrival[0], arrival[1] + dy, arrival[2]};
            assertFalse(siteCells.contains(key(at)), "site places a block at the arrival spot " + key(at));
        }
    }

    @Test
    void theSiteSitsWhereTheDesignDocSaysItDoes() {
        GatewaySite site = load(MOON_SITE, GatewaySite.CODEC);
        assertEquals(7, site.frameOffset().getX());
        assertEquals(2, site.frameOffset().getY(), "plaza floor at site y=1 is ground level, under the frame's y=0");
        assertEquals(6, site.frameOffset().getZ());
    }
}
