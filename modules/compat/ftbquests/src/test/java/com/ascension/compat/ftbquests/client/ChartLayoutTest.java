package com.ascension.compat.ftbquests.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The layout file the generator wrote, read the way the game reads it, and the generator's own sample
 * points replayed against the Java projection: the two implementations of one formula cannot drift apart
 * without this failing.
 */
class ChartLayoutTest {

    private static JsonObject json;
    private static ChartLayout layout;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = ChartLayoutTest.class.getResourceAsStream(
                "/assets/ascension_compat_ftbquests/quests/chart_layout.json")) {
            assertNotNull(in, "tools/generate-quests.py has not written the chart layout");
            json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        layout = ChartLayout.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new);
    }

    @Test
    void theGameCanReadIt() {
        assertTrue(layout.size() >= 256);
        assertTrue(layout.radius() > 0 && layout.radius() < layout.size() / 2.0);
        assertEquals(ResourceLocation.fromNamespaceAndPath("ascension_worlds", "space"), layout.space());
        assertFalse(layout.planets().isEmpty());
    }

    @Test
    void earthIsOnTheChartAtItsMiddle() {
        ChartLayout.Planet earth = layout.planets().stream()
                .filter(p -> p.surface().equals(ResourceLocation.withDefaultNamespace("overworld")))
                .findFirst().orElseThrow();

        assertEquals(layout.centerX(), earth.pixelX(), 1.0E-6);
        assertEquals(layout.centerY(), earth.pixelY(), 1.0E-6);
    }

    @Test
    void everyPlanetIsWhereTheProjectionPutsIt() {
        for (ChartLayout.Planet planet : layout.planets()) {
            double[] at = ChartMath.project(layout, planet.blocksX(), planet.blocksZ());

            assertEquals(planet.pixelX(), at[0], 1.0E-3, planet.surface() + " x");
            assertEquals(planet.pixelY(), at[1], 1.0E-3, planet.surface() + " y");
        }
    }

    @Test
    void theGeneratorsSamplePointsAgreeWithTheJavaProjection() {
        JsonArray golden = json.getAsJsonArray("golden");
        assertTrue(golden.size() >= 8, "the tool should write a spread of sample points");

        for (JsonElement entry : golden) {
            JsonArray row = entry.getAsJsonArray();
            double[] at = ChartMath.project(layout, row.get(0).getAsDouble(), row.get(1).getAsDouble());

            assertEquals(row.get(2).getAsDouble(), at[0], 1.0E-3, "x for " + row);
            assertEquals(row.get(3).getAsDouble(), at[1], 1.0E-3, "y for " + row);
        }
    }

}
