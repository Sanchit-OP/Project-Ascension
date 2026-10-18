package com.ascension.compat.ftbquests.client;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Where a player is on the Planets page. Pure arithmetic, no Minecraft state, so a test can check it.
 *
 * <p>The chart is Earth-centred and north-up: east (+X) is to the right and south (+Z) is down, the way
 * Minecraft's own maps read. Distance from Earth is drawn on a square-root curve, so the near worlds are
 * not crushed against the middle while the far ones still fit; bearing is true. The same formula, in
 * {@code tools/quests/chart.py}, places the planets on the picture, and a test replays the tool's sample
 * points against this one.
 */
public final class ChartMath {

    private ChartMath() {
    }

    /** A point on the picture, and the way the player faces if it is worth drawing (NaN when it is not). */
    public record Marker(double x, double y, double heading) {
        public boolean hasHeading() {
            return !Double.isNaN(heading);
        }
    }

    /**
     * Where a position in space, in blocks from Earth, lands on the picture. A position beyond the chart's
     * range lands on its edge, in the right direction.
     */
    public static double[] project(ChartLayout layout, double blocksX, double blocksZ) {
        double distance = Math.hypot(blocksX, blocksZ);
        if (distance < 1.0E-9) {
            return new double[] {layout.centerX(), layout.centerY()};
        }
        double r = layout.radius() * Math.sqrt(Math.min(distance, layout.rangeBlocks()) / layout.rangeBlocks());
        return new double[] {
                layout.centerX() + r * blocksX / distance,
                layout.centerY() + r * blocksZ / distance
        };
    }

    /**
     * Where the marker goes for a player in {@code dimension} at ({@code x}, {@code z}) facing
     * {@code yawDegrees}, or nothing if that dimension is not on the chart.
     *
     * <p>On a world's surface the marker sits on that world and has no heading (the surface has no place
     * on the chart to point). In space it is at the player's real position and points the way they face.
     */
    public static Optional<Marker> locate(ChartLayout layout, ResourceLocation dimension, double x, double z,
                                          float yawDegrees) {
        if (dimension.equals(layout.space())) {
            double[] at = project(layout, x, z);
            // Minecraft's yaw 0 faces +Z (south, down the chart) and 90 faces -X (west, left).
            double yaw = Math.toRadians(yawDegrees);
            double heading = Math.atan2(Math.cos(yaw), -Math.sin(yaw));
            return Optional.of(new Marker(at[0], at[1], heading));
        }
        for (ChartLayout.Planet planet : layout.planets()) {
            if (planet.surface().equals(dimension)) {
                return Optional.of(new Marker(planet.pixelX(), planet.pixelY(), Double.NaN));
            }
        }
        return Optional.empty();
    }
}
