package com.ascension.worlds.internal.gateway;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The gateway frame contract (docs/technical/gateway-frame-format.md): every rule a frame must
 * pass to load, and that a frame built in the world is recognised whichever way it faces.
 */
class GatewayFrameTest {

    /** A 5x6 Nether-style ring, 3x4 opening, keystone in the bottom border. */
    private static final String RING = """
            {
              "key": {
                "#": { "block": "minecraft:crying_obsidian" },
                "K": { "block": "ascension_worlds:gateway_keystone" }
              },
              "keystone": "K",
              "field": "O",
              "layers": [
                ["##K##"], ["#OOO#"], ["#OOO#"], ["#OOO#"], ["#OOO#"], ["#####"]
              ]
            }""";

    private static DataResult<GatewayFrame> parse(String json) {
        return GatewayFrame.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static GatewayFrame ring() {
        return parse(RING).getOrThrow();
    }

    private static void assertRejected(String json, String reasonFragment) {
        DataResult<GatewayFrame> result = parse(json);
        assertTrue(result.error().isPresent(), "should not load: " + reasonFragment);
        String message = result.error().get().message();
        assertTrue(message.contains(reasonFragment), "expected '" + reasonFragment + "' in: " + message);
    }

    private static String withLayers(String layers) {
        return RING.replaceFirst("(?s)\"layers\": \\[.*\\]", "\"layers\": [" + layers + "]");
    }

    // --- loading ------------------------------------------------------------

    @Test
    void theReferenceRingLoads() {
        GatewayFrame frame = ring();
        assertEquals(30, frame.cells().size(), "every non-space cell, field included");
        assertEquals('K', frame.keystone().symbol());
    }

    @Test
    void spacesAreNotCells() {
        GatewayFrame frame = parse(withLayers("[\" ##K## \"], [\" #OOO# \"], [\" #OOO# \"], [\" #OOO# \"], [\" ##### \"]")).getOrThrow();
        assertTrue(frame.cells().stream().noneMatch(c -> c.x() == 0 || c.x() == 6));
    }

    @Test
    void rejectsUnknownCharacters() {
        assertRejected(withLayers("[\"##K##\"], [\"#OOO#\"], [\"#OXO#\"], [\"#OOO#\"], [\"#####\"]"), "unknown character 'X'");
    }

    @Test
    void rejectsTwoKeystones() {
        assertRejected(withLayers("[\"#KK##\"], [\"#OOO#\"], [\"#OOO#\"], [\"#OOO#\"], [\"#####\"]"), "exactly one keystone");
    }

    @Test
    void rejectsAFieldThatIsNotARectangle() {
        assertRejected(withLayers("[\"##K##\"], [\"#OOO#\"], [\"#O#O#\"], [\"#OOO#\"], [\"#####\"]"), "solid rectangle");
    }

    @Test
    void rejectsAFieldThatIsTooSmall() {
        assertRejected(withLayers("[\"#K#\"], [\"#O#\"], [\"#O#\"], [\"#O#\"], [\"###\"]"), "at least 2 wide");
    }

    @Test
    void rejectsAFieldThatIsNotEnclosed() {
        assertRejected(withLayers("[\"##K##\"], [\" OOO#\"], [\"#OOO#\"], [\"#OOO#\"], [\"#####\"]"), "not enclosed");
    }

    @Test
    void rejectsAFieldSpreadAcrossDepth() {
        assertRejected(withLayers("[\"##K##\", \"#####\"], [\"#OOO#\", \"#OOO#\"], [\"#OOO#\", \"#OOO#\"], [\"#OOO#\", \"#OOO#\"], [\"#####\", \"#####\"]"),
                "one row");
    }

    @Test
    void rejectsATagWithoutAPlaceBlock() {
        assertRejected(RING.replace("{ \"block\": \"minecraft:crying_obsidian\" }", "{ \"tag\": \"ascension_worlds:frame\" }"),
                "also needs \"place\"");
    }

    @Test
    void rejectsAnOversizedFrame() {
        assertRejected(withLayers("[\"##K##############\"], [\"#OOO#\"], [\"#OOO#\"], [\"#OOO#\"], [\"#####\"]"), "wider than");
    }

    // --- rotation -----------------------------------------------------------

    @Test
    void southFacingKeepsXAndRunsDepthNorth() {
        GatewayFrame.Placement placement = ring().placeOriginAt(100, 64, 200, GatewayFrame.Facing.SOUTH);
        assertArrayEquals(new int[] {104, 64, 200}, placement.world(4, 0, 0));
        assertArrayEquals(new int[] {100, 64, 199}, placement.world(0, 0, 1), "back is north (-z)");
    }

    @Test
    void theArrivalSpotIsInFrontOfTheFieldsBottomCentre() {
        // Field spans x 1..3, y 1..4, z 0 -> bottom centre (2, 1, 0); in front of a south-facing frame is +z.
        GatewayFrame.Placement placement = ring().placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH);
        assertArrayEquals(new int[] {2, 65, 1}, placement.arrivalSpot());
        GatewayFrame.Placement east = ring().placeOriginAt(0, 64, 0, GatewayFrame.Facing.EAST);
        assertArrayEquals(new int[] {1, 65, -2}, east.arrivalSpot(), "in front of an east-facing frame is +x");
    }

    @Test
    void placeArrivalAtPutsTheArrivalSpotExactlyThere() {
        for (GatewayFrame.Facing facing : GatewayFrame.Facing.values()) {
            GatewayFrame.Placement placement = ring().placeArrivalAt(-40, 70, 13, facing);
            assertArrayEquals(new int[] {-40, 70, 13}, placement.arrivalSpot(), "facing " + facing);
        }
    }

    @Test
    void placeCentredAtCentresTheFootprint() {
        // The ring is 5 wide, 1 deep: centred on x=100 it spans 98..102, whichever way it faces.
        GatewayFrame.Placement south = ring().placeCentredAt(100, 64, 50, GatewayFrame.Facing.SOUTH);
        int[] bounds = south.bounds();
        assertEquals(98, bounds[0]);
        assertEquals(102, bounds[3]);
        assertEquals(50, bounds[2]);
        assertEquals(64, bounds[1], "bottom layer at the given y");

        int[] east = ring().placeCentredAt(100, 64, 50, GatewayFrame.Facing.EAST).bounds();
        assertEquals(48, east[2]);
        assertEquals(52, east[5], "an east-facing ring runs along z instead");
    }

    @Test
    void boundsCoverEveryCellAndNothingMore() {
        int[] bounds = ring().placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH).bounds();
        assertArrayEquals(new int[] {0, 64, 0, 4, 69, 0}, bounds);
    }

    @Test
    void fieldAxisFollowsTheFacing() {
        assertTrue(GatewayFrame.Facing.SOUTH.fieldAlongX());
        assertTrue(GatewayFrame.Facing.NORTH.fieldAlongX());
        assertFalse(GatewayFrame.Facing.EAST.fieldAlongX());
        assertFalse(GatewayFrame.Facing.WEST.fieldAlongX());
    }

    // --- matching against a world -------------------------------------------

    /** A sparse fake world: position -> symbol it satisfies. Air satisfies the field. */
    private static final class FakeWorld implements GatewayFrame.CellTest {
        private final Map<String, Character> blocks = new HashMap<>();

        void put(int[] at, char symbol) {
            blocks.put(at[0] + "," + at[1] + "," + at[2], symbol);
        }

        @Override
        public boolean matches(char symbol, int x, int y, int z) {
            Character here = blocks.get(x + "," + y + "," + z);
            return symbol == 'O' ? here == null : here != null && here == symbol;
        }
    }

    private static FakeWorld build(GatewayFrame.Placement placement, GatewayFrame frame) {
        FakeWorld world = new FakeWorld();
        for (GatewayFrame.Cell cell : frame.cells()) {
            if (cell.symbol() != frame.fieldSymbol()) {
                world.put(placement.world(cell.x(), cell.y(), cell.z()), cell.symbol());
            }
        }
        return world;
    }

    @Test
    void recognisesAFrameBuiltFacingAnyDirection() {
        GatewayFrame frame = ring();
        for (GatewayFrame.Facing facing : GatewayFrame.Facing.values()) {
            GatewayFrame.Placement built = frame.placeOriginAt(37, 70, -12, facing);
            FakeWorld world = build(built, frame);
            int[] keystone = built.keystone();

            Optional<GatewayFrame.Placement> found = frame.match(keystone[0], keystone[1], keystone[2], world);
            assertTrue(found.isPresent(), "built facing " + facing + " not recognised");
            // A one-deep, left-right symmetric ring reads identically from either side, so the
            // opposite facing is an equally correct answer; what must agree is the opening itself.
            assertEquals(fieldSet(built), fieldSet(found.get()), "built facing " + facing);
            assertEquals(facing.fieldAlongX(), found.get().facing().fieldAlongX());
        }
    }

    private static java.util.Set<String> fieldSet(GatewayFrame.Placement placement) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (int[] p : placement.fieldPositions()) {
            set.add(p[0] + "," + p[1] + "," + p[2]);
        }
        return set;
    }

    @Test
    void anAsymmetricFrameIsOnlyRecognisedTheWayItWasBuilt() {
        // Keystone off-centre: the mirror image is a different frame, and must not match.
        GatewayFrame frame = parse(withLayers("[\"#K###\"], [\"#OOO#\"], [\"#OOO#\"], [\"#OOO#\"], [\"#####\"]")).getOrThrow();
        for (GatewayFrame.Facing facing : GatewayFrame.Facing.values()) {
            GatewayFrame.Placement built = frame.placeOriginAt(5, 64, 5, facing);
            int[] keystone = built.keystone();
            Optional<GatewayFrame.Placement> found = frame.match(keystone[0], keystone[1], keystone[2], build(built, frame));
            assertTrue(found.isPresent());
            assertEquals(facing, found.get().facing());
            assertArrayEquals(built.arrivalSpot(), found.get().arrivalSpot());
        }
    }

    @Test
    void aMissingFrameBlockIsNotAFrame() {
        GatewayFrame frame = ring();
        GatewayFrame.Placement built = frame.placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH);
        FakeWorld world = build(built, frame);
        world.blocks.remove(String.join(",", List.of("4", "69", "0"))); // top-right corner
        int[] keystone = built.keystone();
        assertTrue(frame.match(keystone[0], keystone[1], keystone[2], world).isEmpty());
    }

    @Test
    void aBlockedOpeningIsNotAFrame() {
        GatewayFrame frame = ring();
        GatewayFrame.Placement built = frame.placeOriginAt(0, 64, 0, GatewayFrame.Facing.SOUTH);
        FakeWorld world = build(built, frame);
        world.put(new int[] {2, 66, 0}, '#'); // something standing in the opening
        int[] keystone = built.keystone();
        assertTrue(frame.match(keystone[0], keystone[1], keystone[2], world).isEmpty());
    }

    @Test
    void theFieldIsTwelveCellsInOnePlane() {
        GatewayFrame.Placement placement = ring().placeOriginAt(0, 64, 0, GatewayFrame.Facing.WEST);
        List<int[]> field = placement.fieldPositions();
        assertEquals(12, field.size());
        assertTrue(field.stream().allMatch(p -> p[0] == 0), "a west-facing frame's field lies in one x plane");
    }
}
