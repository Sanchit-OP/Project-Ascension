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
 * node, every node can be reached, no two nodes sit on one spot, and the web really is a web: no dead
 * ends, a way in to every theme, and the themes joined to one another (growth.md section 5). Catches a
 * typo in a node file at build time rather than in a player's hands. It does not read the numbers:
 * those are Sanchit's to change (growth.md section 8), and a test that pinned them would only get in
 * his way. The server log's {@code GrowthDataCheck} says the same things for packs that add nodes.
 */
class GrowthTreeDataTest {

    private static final String NAMESPACE = "ascension_combat";
    private static final Set<String> THEMES = Set.of("power", "fury", "guard", "wayfarer");

    /** Every node file: its id and parsed JSON. */
    private record NodeFile(ResourceLocation id, JsonObject json) {

        List<ResourceLocation> links() {
            List<ResourceLocation> links = new ArrayList<>();
            if (json.has("links")) {
                json.getAsJsonArray("links").forEach(link -> links.add(ResourceLocation.parse(link.getAsString())));
            }
            return links;
        }

        boolean start() {
            return json.has("start") && json.get("start").getAsBoolean();
        }

        String arm() {
            return json.get("arm").getAsString();
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
                            json.getAsJsonObject()));
                }
            }
        }
    }

    private static Map<ResourceLocation, List<ResourceLocation>> links() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        NODES.forEach(node -> links.put(node.id(), node.links()));
        return links;
    }

    private static Map<ResourceLocation, NodeFile> byId() {
        Map<ResourceLocation, NodeFile> byId = new LinkedHashMap<>();
        NODES.forEach(node -> byId.put(node.id(), node));
        return byId;
    }

    @Test
    void thereAreNodes() {
        assertFalse(NODES.isEmpty());
    }

    @Test
    void everyLinkLandsOnANode() {
        Set<ResourceLocation> ids = byId().keySet();
        for (NodeFile node : NODES) {
            for (ResourceLocation link : node.links()) {
                assertTrue(ids.contains(link), node.id() + " links to " + link + ", which is not a node");
            }
        }
    }

    @Test
    void everyNodeCanBeReached() {
        Set<ResourceLocation> starts = new HashSet<>();
        NODES.stream().filter(NodeFile::start).forEach(node -> starts.add(node.id()));
        assertEquals(Set.of(), GrowthRules.unreachable(starts, links()), "nodes no chain of links joins to a start");
    }

    @Test
    void aPlayerCanStartInAnyTheme() {
        Set<String> themesWithAStart = new HashSet<>();
        NODES.stream().filter(NodeFile::start).forEach(node -> themesWithAStart.add(node.arm()));
        assertEquals(THEMES, themesWithAStart, "a theme has no start node, so it can only be reached through another");
    }

    @Test
    void thereAreNoDeadEnds() {
        Map<ResourceLocation, Set<ResourceLocation>> near = GrowthRules.neighbours(links());
        for (NodeFile node : NODES) {
            int neighbours = near.get(node.id()).size() + (node.start() ? 1 : 0);
            assertTrue(neighbours >= 2, node.id() + " is a dead end: it touches " + neighbours);
        }
    }

    @Test
    void everyThemeIsJoinedToAtLeastTwoOthers() {
        Map<ResourceLocation, NodeFile> byId = byId();
        Map<String, Set<String>> joined = new LinkedHashMap<>();
        THEMES.forEach(theme -> joined.put(theme, new HashSet<>()));
        for (NodeFile node : NODES) {
            for (ResourceLocation link : node.links()) {
                String other = byId.get(link).arm();
                if (!other.equals(node.arm())) {
                    joined.get(node.arm()).add(other);
                    joined.get(other).add(node.arm());
                }
            }
        }
        joined.forEach((theme, others) ->
                assertTrue(others.size() >= 2, theme + " touches only " + others + ": the themes are corridors"));
    }

    @Test
    void everyNodeHasAKnownTheme() {
        for (NodeFile node : NODES) {
            assertTrue(THEMES.contains(node.arm()), node.id() + " has the theme " + node.arm());
        }
    }

    @Test
    void noTwoNodesShareASpot() {
        Set<String> spots = new HashSet<>();
        for (NodeFile node : NODES) {
            var position = node.json().getAsJsonArray("position");
            String spot = "%.2f,%.2f".formatted(position.get(0).getAsDouble(), position.get(1).getAsDouble());
            assertTrue(spots.add(spot), node.id() + " is on the same spot as another node: " + spot);
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
