package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.api.rune.RunePath;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every rune file we ship, read as a whole (docs/technical/rune-system.md sections 3, 4 and 9):
 * each reads, each path that has any runes has its full set, every rune has its name, sentence and
 * numbers' words in the lang file, its icon's sprites exist, and no two share a place. Pure
 * decoding of the shipped resources; says nothing about the game (ADR-0008).
 */
class ShippedRunesTest {

    private static final String RUNE_DIR = "/data/ascension_combat/ascension_combat/rune";

    private record Shipped(String id, RuneDefinition rune) {
    }

    private static List<Shipped> shipped() throws IOException, URISyntaxException {
        List<Shipped> out = new ArrayList<>();
        Path dir = Path.of(ShippedRunesTest.class.getResource(RUNE_DIR).toURI());
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json")).sorted()::iterator) {
                String name = file.getFileName().toString();
                try (var in = Files.newBufferedReader(file)) {
                    RuneDefinition rune = RuneDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(in))
                            .getOrThrow(message -> new AssertionError(name + ": " + message));
                    out.add(new Shipped(name.substring(0, name.length() - 5), rune));
                }
            }
        }
        return out;
    }

    private static JsonObject lang() throws IOException {
        try (var in = new InputStreamReader(ShippedRunesTest.class.getResourceAsStream("/assets/ascension_combat/lang/en_us.json"))) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static boolean resourceExists(String path) {
        return ShippedRunesTest.class.getResource(path) != null;
    }

    @Test
    void everyRuneFileReads() throws Exception {
        assertTrue(shipped().size() >= 18, "at least Vanguard and the shards");
    }

    @Test
    void everyPathThatHasRunesHasItsWholeSet() throws Exception {
        Map<RunePath, List<RuneDefinition>> byPath = new HashMap<>();
        for (Shipped s : shipped()) {
            s.rune().path().ifPresent(p -> byPath.computeIfAbsent(p, k -> new ArrayList<>()).add(s.rune()));
        }

        for (var entry : byPath.entrySet()) {
            String path = entry.getKey().getSerializedName();
            List<RuneDefinition> runes = entry.getValue();
            long keystones = runes.stream().filter(r -> r.slot() == RuneSlot.KEYSTONE).count();
            long world = runes.stream().filter(r -> r.slot() == RuneSlot.KEYSTONE && r.world()).count();
            long minors = runes.stream().filter(r -> r.slot() == RuneSlot.MINOR).count();
            long growing = runes.stream().filter(RuneDefinition::grows).count();
            assertEquals(3, keystones, path + " has three keystones");
            assertEquals(1, world, path + " has one world keystone");
            assertEquals(6, minors, path + " has six minors");
            assertEquals(1, growing, path + " has one growing rune");
            for (int row = 0; row < RuneDefinition.MINOR_ROWS; row++) {
                int r = row;
                assertEquals(3, runes.stream().filter(x -> x.slot() == RuneSlot.MINOR && x.row() == r).count(), path + " row " + row);
            }
        }
    }

    @Test
    void noTwoRunesShareAPlace() throws Exception {
        Set<String> places = new HashSet<>();
        for (Shipped s : shipped()) {
            RuneDefinition r = s.rune();
            String place = r.path().map(RunePath::getSerializedName).orElse("shard") + "/" + r.slot() + "/"
                    + (r.world() ? "world" : r.row()) + "/" + r.position();
            assertTrue(places.add(place), s.id() + " shares the place " + place);
        }
    }

    @Test
    void theEffectIsTheFileName() throws Exception {
        for (Shipped s : shipped()) {
            if (s.rune().slot() != RuneSlot.SHARD) {
                assertEquals("ascension_combat:" + s.id(), s.rune().effect().toString(), s.id());
            }
        }
    }

    @Test
    void everyRuneHasItsWordsInTheLangFile() throws Exception {
        JsonObject lang = lang();
        for (Shipped s : shipped()) {
            String key = "rune.ascension_combat." + s.id();
            assertTrue(lang.has(key), key);
            assertTrue(lang.has(key + ".desc"), key + ".desc");
            if (s.rune().slot() == RuneSlot.KEYSTONE) {
                assertTrue(lang.has(key + ".blurb"), key + ".blurb");
            }
            for (RuneNumberLine line : s.rune().numbers()) {
                assertTrue(lang.has(line.text()), line.text());
                int slots = line.text() == null ? 0 : lang.get(line.text()).getAsString().split("%s", -1).length - 1;
                assertEquals(line.parts().size(), slots, line.text() + " has one %s for each number");
            }
        }
    }

    @Test
    void everyRunesIconSpritesExist() throws Exception {
        for (Shipped s : shipped()) {
            String path = s.rune().icon().getPath();
            assertTrue(resourceExists("/assets/ascension_combat/textures/gui/sprites/" + path + ".png"), path);
            assertTrue(resourceExists("/assets/ascension_combat/textures/gui/sprites/" + path + "_open.png"), path + "_open");
        }
    }
}
