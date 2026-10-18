package com.ascension.compat.ftbquests.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Where a player lands on the Planets page: the projection and the marker's place (docs/progression/quests.md section 5). */
class ChartMathTest {

    private static final ResourceLocation SPACE = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "space");
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");
    private static final ResourceLocation ELSEWHERE = ResourceLocation.fromNamespaceAndPath("somemod", "somewhere");

    private static final ChartLayout LAYOUT = new ChartLayout(1024, 512, 512, 480, 60000, 24, SPACE, List.of(
            new ChartLayout.Planet(OVERWORLD, 0, 0, 512, 512),
            new ChartLayout.Planet(MOON, 4000, 0, 512 + 480 * Math.sqrt(4000.0 / 60000.0), 512)));

    private static final double EPS = 1.0E-9;

    @Test
    void earthIsTheMiddle() {
        double[] at = ChartMath.project(LAYOUT, 0, 0);

        assertEquals(512, at[0], EPS);
        assertEquals(512, at[1], EPS);
    }

    @Test
    void theRangeLandsOnTheEdgeAndAQuarterOfItHalfway() {
        double[] edge = ChartMath.project(LAYOUT, 60000, 0);
        double[] quarter = ChartMath.project(LAYOUT, 0, 15000);

        assertEquals(512 + 480, edge[0], EPS);
        assertEquals(512, edge[1], EPS);
        assertEquals(512, quarter[0], EPS);
        assertEquals(512 + 240, quarter[1], EPS, "a square-root curve: a quarter of the distance is half the radius");
    }

    @Test
    void northIsUpAndEastIsRight() {
        double[] east = ChartMath.project(LAYOUT, 1000, 0);
        double[] north = ChartMath.project(LAYOUT, 0, -1000);

        assertTrue(east[0] > 512 && Math.abs(east[1] - 512) < EPS, "+X goes right");
        assertTrue(north[1] < 512 && Math.abs(north[0] - 512) < EPS, "-Z goes up the page");
    }

    @Test
    void positionsBeyondTheRangeStayOnTheEdgeInTheSameDirection() {
        double[] far = ChartMath.project(LAYOUT, 0, 500000);

        assertEquals(512, far[0], EPS);
        assertEquals(512 + 480, far[1], EPS);
    }

    @Test
    void nearerIsAlwaysCloserToTheMiddle() {
        double last = 0;
        for (int d = 100; d <= 50000; d += 700) {   // inside the range: past it the chart is deliberately flat
            double[] at = ChartMath.project(LAYOUT, d, d / 2.0);
            double r = Math.hypot(at[0] - 512, at[1] - 512);
            assertTrue(r > last, "the chart must not fold back on itself at " + d);
            last = r;
        }
    }

    @Test
    void standingOnAWorldPutsTheMarkerOnThatWorldWithNothingToPointAt() {
        Optional<ChartMath.Marker> marker = ChartMath.locate(LAYOUT, MOON, 123, -456, 90f);

        assertTrue(marker.isPresent());
        assertEquals(LAYOUT.planets().get(1).pixelX(), marker.get().x(), EPS);
        assertEquals(512, marker.get().y(), EPS);
        assertFalse(marker.get().hasHeading(), "the surface has no place on the chart to point");
    }

    @Test
    void aDimensionNotOnTheChartHasNoMarker() {
        assertTrue(ChartMath.locate(LAYOUT, ELSEWHERE, 0, 0, 0f).isEmpty());
    }

    @Test
    void inSpaceTheMarkerIsAtThePlayersRealPositionAndFacesTheWayTheyFace() {
        // Minecraft's yaw: 0 faces +Z (south), 90 faces -X (west), -90 faces +X (east), 180 faces -Z (north).
        assertEquals(Math.PI / 2, ChartMath.locate(LAYOUT, SPACE, 4000, 0, 0f).orElseThrow().heading(), 1.0E-6);
        assertEquals(Math.PI, ChartMath.locate(LAYOUT, SPACE, 4000, 0, 90f).orElseThrow().heading(), 1.0E-6);
        assertEquals(0.0, ChartMath.locate(LAYOUT, SPACE, 4000, 0, -90f).orElseThrow().heading(), 1.0E-6);
        assertEquals(-Math.PI / 2, ChartMath.locate(LAYOUT, SPACE, 4000, 0, 180f).orElseThrow().heading(), 1.0E-6);

        ChartMath.Marker at = ChartMath.locate(LAYOUT, SPACE, 4000, 0, 0f).orElseThrow();
        assertEquals(LAYOUT.planets().get(1).pixelX(), at.x(), 1.0E-6, "flying to the Moon's position puts you on the Moon");
    }
}
