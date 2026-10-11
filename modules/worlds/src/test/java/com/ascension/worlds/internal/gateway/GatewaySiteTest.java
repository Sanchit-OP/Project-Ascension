package com.ascension.worlds.internal.gateway;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/** A gateway site's own rules, and that its cells turn with the frame they surround. */
class GatewaySiteTest {

    private static final String RING = """
            {
              "key": { "#": { "block": "minecraft:obsidian" }, "K": { "block": "ascension_worlds:gateway_keystone" } },
              "keystone": "K", "field": "O",
              "layers": [ ["#K##"], ["#OO#"], ["#OO#"], ["#OO#"], ["####"] ]
            }""";

    private static DataResult<GatewaySite> parse(String layers) {
        String json = """
                { "key": { "p": { "block": "minecraft:stone_bricks" } },
                  "frame_offset": [1, 1, 1],
                  "layers": [%s] }""".formatted(layers);
        return GatewaySite.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    @Test
    void loadsAndSkipsSpaces() {
        GatewaySite site = parse("[\"ppp\", \"p p\", \"ppp\"]").getOrThrow();
        assertEquals(8, site.cells().size());
        assertEquals(1, site.frameOffset().getX());
    }

    @Test
    void rejectsUnknownCharactersAndSaysItIsASite() {
        DataResult<GatewaySite> result = parse("[\"pXp\"]");
        assertTrue(result.error().isPresent());
        assertTrue(result.error().get().message().contains("unknown site character 'X'"),
                result.error().get().message());
    }

    @Test
    void cellsAreExpressedRelativeToTheFrameAndTurnWithIt() {
        GatewaySite site = parse("[\"p\"]").getOrThrow();
        GatewayFrame frame = GatewayFrame.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(RING)).getOrThrow();
        // Site (0,0,0) with frame_offset (1,1,1) is frame-local (-1,-1,-1).
        GatewayFrame.Cell cell = site.cells().get(0);

        GatewayFrame.Placement south = frame.placeOriginAt(10, 64, 10, GatewayFrame.Facing.SOUTH);
        assertArrayEquals(new int[] {9, 63, 11}, site.world(south, cell), "back is north, so in front is +z");

        GatewayFrame.Placement east = frame.placeOriginAt(10, 64, 10, GatewayFrame.Facing.EAST);
        assertArrayEquals(east.world(-1, -1, -1), site.world(east, cell));
    }

    @Test
    void aSiteWithoutAPaletteBuildsTheFrameAsUsual() {
        assertTrue(parse("[\"p\"]").getOrThrow().framePalette().isEmpty());
    }

    @Test
    void readsAFramePaletteBySymbol() {
        String json = """
                { "key": { "p": { "block": "minecraft:stone_bricks" } },
                  "frame_offset": [0, 0, 0],
                  "frame_palette": { "D": "minecraft:polished_blackstone_bricks" },
                  "layers": [["p"]] }""";
        GatewaySite site = GatewaySite.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals("minecraft:polished_blackstone_bricks", site.framePalette().get('D').toString());
    }

    @Test
    void rejectsAPaletteKeyThatIsNotOneSymbol() {
        String json = """
                { "key": { "p": { "block": "minecraft:stone_bricks" } },
                  "frame_offset": [0, 0, 0],
                  "frame_palette": { "DD": "minecraft:polished_blackstone_bricks" },
                  "layers": [["p"]] }""";
        DataResult<GatewaySite> result = GatewaySite.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.error().isPresent());
        assertTrue(result.error().get().message().contains("frame_palette"), result.error().get().message());
    }
}
