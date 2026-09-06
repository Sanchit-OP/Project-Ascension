package com.ascension.worlds.api;

import com.ascension.core.api.WorldEnvironment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A world you can travel to, and where it is.
 *
 * <p><strong>A planet references a dimension. It does not define one.</strong> Minecraft already
 * has a data-driven dimension system &mdash; {@code dimension}, {@code dimension_type}, biomes,
 * noise settings &mdash; which datapacks author well and every worldgen tool already understands.
 * Competing with that would put this module in the worldgen business and break those tools. So
 * {@link #surface()} points at a dimension somebody else's JSON created.
 *
 * <p><strong>And this describes where a world is and what it is like, not what happens on it.</strong>
 * {@code docs/progression/planets.md} lists required fields per planet &mdash; boss, dungeon,
 * resource gate, movement mechanic &mdash; and that is an authoring checklist, not a schema. A
 * boss is an entity in a structure. Putting those here would make one record that everything
 * depends on and nothing can change.
 *
 * <p>Design record: {@code docs/technical/worlds-api.md}.
 *
 * @param surface     the dimension a player lands in
 * @param space       where the planet sits in interplanetary space
 * @param environment what the air is like there; defaults to {@link WorldEnvironment#EARTHLIKE}
 * @param order       sort order for listings; ties broken by id
 * @param color       packed {@code 0xRRGGBB} tint for the placeholder body rendered in space;
 *                    defaults to {@code 0xFFFFFF} (no tint). Cosmetic only, read by
 *                    {@code SpaceSkyRenderer} &mdash; the point is telling two placeholder discs
 *                    apart at a glance before real per-planet art exists.
 */
public record Planet(
        ResourceKey<Level> surface,
        SpacePosition space,
        WorldEnvironment environment,
        int order,
        int color) {

    public static final Codec<Planet> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("surface")
                            .forGetter(Planet::surface),
                    SpacePosition.CODEC.fieldOf("space").forGetter(Planet::space),
                    WorldEnvironment.CODEC.optionalFieldOf("environment", WorldEnvironment.EARTHLIKE)
                            .forGetter(Planet::environment),
                    Codec.INT.optionalFieldOf("order", 0).forGetter(Planet::order),
                    Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Planet::color))
            .apply(instance, Planet::new));

    /**
     * Where a planet sits in the shared interplanetary space dimension, and how big it is there.
     *
     * <p><strong>Two-dimensional, and that is a hard limit rather than a simplification.</strong>
     * A dimension's height caps in the low thousands while X and Z run to nearly thirty million,
     * so Y cannot carry interplanetary distance. Planets therefore lie on a plane. Which turns
     * out to be a gift: a plane can be drawn on a chart, and
     * {@code ADR-0010} made navigation a real design problem that a chart is the answer to.
     *
     * <p>Note this constrains only the <em>layout</em>. Space itself is an ordinary dimension and
     * a ship moves through it in three dimensions like anything else.
     *
     * @param x              position along X in the space dimension
     * @param z              position along Z in the space dimension
     * @param bodyRadius     apparent size of the body, and the volume you cannot fly into
     * @param approachRadius distance within which descent to {@link #surface()} is possible
     */
    public record SpacePosition(int x, int z, int bodyRadius, int approachRadius) {

        private static final Codec<List<Integer>> POSITION_CODEC = Codec.INT.listOf().validate(
                list -> list.size() == 2
                        ? DataResult.success(list)
                        : DataResult.error(() ->
                                "position must be exactly [x, z], got " + list.size() + " values"));

        public static final Codec<SpacePosition> CODEC = RecordCodecBuilder
                .<SpacePosition>create(instance -> instance
                        .group(
                                POSITION_CODEC.fieldOf("position")
                                        .forGetter(p -> List.of(p.x(), p.z())),
                                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("body_radius")
                                        .forGetter(SpacePosition::bodyRadius),
                                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("approach_radius")
                                        .forGetter(SpacePosition::approachRadius))
                        .apply(instance, (position, body, approach) ->
                                new SpacePosition(position.get(0), position.get(1), body, approach)))
                .validate(SpacePosition::validate);

        /**
         * The approach shell must be wider than the body.
         *
         * <p>Not a tidiness check. The shell is what a player aims at, and it has to be reachable
         * before the body they cannot fly into. A shell inside the body would mean aiming at a
         * planet, arriving, and bouncing off it &mdash; which reads as a broken mod rather than a
         * misconfigured datapack, so it fails at load with a message instead.
         */
        private static DataResult<SpacePosition> validate(SpacePosition position) {
            if (position.approachRadius() <= position.bodyRadius()) {
                return DataResult.error(() -> "approach_radius (" + position.approachRadius()
                        + ") must be greater than body_radius (" + position.bodyRadius()
                        + "); otherwise the planet cannot be approached");
            }
            return DataResult.success(position);
        }

        /** Squared distance from this planet to a point in space. Squared, to avoid the sqrt. */
        public long distanceSquaredTo(int otherX, int otherZ) {
            long dx = (long) otherX - x;
            long dz = (long) otherZ - z;
            return dx * dx + dz * dz;
        }
    }
}
