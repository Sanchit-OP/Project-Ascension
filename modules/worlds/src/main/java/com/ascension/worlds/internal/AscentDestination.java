package com.ascension.worlds.internal;

import net.minecraft.world.phys.Vec3;

/**
 * Pure geometry for where an ascending player ends up in space: past the departure planet's own
 * approach shell, along the heading they were already facing. Split out of
 * {@link SpaceMechanics#checkAscent} during the M2.7 refactor pass &mdash; the trig itself takes
 * four numbers and returns a point, with no {@code ServerPlayer}, {@code Level}, or any other
 * Minecraft runtime type involved, which is exactly the arithmetic ADR-0008 says unit tests
 * should be covering instead of "a person standing in a world and eyeballing it".
 *
 * <p>See {@code SpaceMechanics.checkAscent}'s own comment for why the destination is offset past
 * {@code approach_radius} at all, rather than placed at the planet's own coordinate.
 */
final class AscentDestination {

    private AscentDestination() {
    }

    /**
     * @param planetX    the departure planet's own X position in space
     * @param planetZ    the departure planet's own Z position in space
     * @param yawDegrees the player's heading at the moment of ascent, vanilla yaw convention
     * @param offset     distance past the planet's own position to place the destination
     * @return the point in space to arrive at, Y always {@code 0.0} (space's own planetary plane)
     */
    static Vec3 compute(double planetX, double planetZ, double yawDegrees, double offset) {
        double yaw = Math.toRadians(yawDegrees);
        double x = planetX + -Math.sin(yaw) * offset;
        double z = planetZ + Math.cos(yaw) * offset;
        return new Vec3(x, 0.0, z);
    }
}
