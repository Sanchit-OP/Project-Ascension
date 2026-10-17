package com.ascension.combat.internal.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The tree's own data files, read as plain JSON from this module's resources: every link lands on a
 * node, every node can be reached, no two nodes of an arm sit on one spot. Catches a typo in a node
 * file at build time rather than in a player's hands. It does not read the numbers: those are
 * Sanchit's to change (growth.md section 8), and a test that pinned them would only get in his way.
 * The server log's {@code GrowthDataCheck} says the same things for packs that add nodes.
 */
class GrowthTreeDataTest {

    private static final String NAMESPACE = "ascension_combat";

    /** Every node file: its id, folder (the arm it should name) and parsed JSON. */
    private record NodeFile(ResourceLocation id, String folder, JsonObject json) {

        List<ResourceLocation> links() {
            List<ResourceLocation> links = new ArrayList<>();
            if (json.has("links")) {
                json.getAsJsonArray("links").forEach(link -> links.add(ResourceLocation.parse(link.getAsString())));
            }
            return links;
        }
    }

    private static final List<NodeFile> NODES = new ArrayList<>();

    @BeforeAll
    static void readNodes() throws IOException, URISyntaxException {
        var url = GrowthTreeDataTest.class.getClassLoader()
                .getResource("data/ascension_combat/ascension_combat/growth_node");
        assertNotNull(url, "the growth_node folder is on the classpath");
        Path root = Path.of(url.toURI());
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                Path relative = root.relativize(file);
                String folder = relative.getParent().toString().replace('\\', '/');
                String name = relative.getFileName().toString().replace(".json", "");
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    NODES.add(new NodeFile(
                            ResourceLocation.fromNamespaceAndPath(NAMESPACE, folder + "/" + name),
                            folder, json.getAsJsonObject()));
                }
            }
        }
    }

    @Test
    void thereAreNodes() {
        assertFalse(NODES.isEmpty());
    }

    @Test
    void everyLinkLandsOnANode() {
        Set<ResourceLocation> ids = new HashSet<>();
        NODES.forEach(node -> ids.add(node.id()));
        for (NodeFile node : NODES) {
            for (ResourceLocation link : node.links()) {
                assertTrue(ids.contains(link), node.id() + " links to " + link + ", which is not a node");
            }
        }
    }

    @Test
    void everyNodeCanBeReached() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        NODES.forEach(node -> links.put(node.id(), node.links()));
        assertEquals(Set.of(), GrowthRules.unreachable(links), "nodes no chain of links leads to");
    }

    @Test
    void everyArmHasAWayIn() {
        Set<String> arms = new HashSet<>();
        NODES.forEach(node -> arms.add(node.folder()));
        for (String arm : arms) {
            assertTrue(NODES.stream().anyMatch(node -> node.folder().equals(arm) && node.links().isEmpty()),
                    arm + " has no node that touches the centre");
        }
    }

    @Test
    void aNodeSitsInTheFolderOfItsArm() {
        for (NodeFile node : NODES) {
            assertEquals(node.folder(), node.json().get("arm").getAsString(), node.id() + " names another arm");
        }
    }

    @Test
    void noTwoNodesOfAnArmShareASpot() {
        Set<String> spots = new HashSet<>();
        for (NodeFile node : NODES) {
            var position = node.json().getAsJsonArray("position");
            String spot = node.folder() + " " + position.get(0).getAsInt() + "," + position.get(1).getAsInt();
            assertTrue(spots.add(spot), node.id() + " is on the same spot as another node of its arm: " + spot);
        }
    }

    @Test
    void everyNodeGivesSomethingAndANotableHasAName() {
        for (NodeFile node : NODES) {
            JsonObject json = node.json();
            assertTrue(json.has("stats") && json.getAsJsonArray("stats").size() > 0, node.id() + " gives nothing");
            json.getAsJsonArray("stats").forEach(stat -> {
                String attribute = stat.getAsJsonObject().get("attribute").getAsString();
                assertNotNull(ResourceLocation.tryParse(attribute), node.id() + " names a bad attribute id " + attribute);
            });
            if (json.has("kind") && json.get("kind").getAsString().equals("notable")) {
                assertTrue(json.has("name"), node.id() + " is a notable with no name");
            }
        }
    }
}
