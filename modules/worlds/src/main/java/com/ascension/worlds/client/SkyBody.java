package com.ascension.worlds.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Something hanging in a dimension's sky: Earth seen from the Moon, and the Moon seen from Earth.
 * Pure appearance, so it is a client resource ({@code assets/<namespace>/ascension_sky/*.json},
 * loaded by {@link SkyBodies}), not part of the server's {@code Planet} record: a resource pack can
 * restyle it, the server never hears of it, and the {@code api} record keeps its shape. Format and
 * examples: {@code docs/technical/worlds-api.md}, "Sky bodies".
 *
 * @param texture     the body's face, drawn as a sphere: the image's inscribed circle is the disc
 * @param yaw         compass heading, Minecraft's convention (0 south, 90 west, 180 north, 270 east).
 *                    Only for a {@link Motion#FIXED} body
 * @param elevation   degrees above the horizon. Only for a {@link Motion#FIXED} body
 * @param angularSize degrees across. Real Earth from the Moon is about 2; vanilla's own moon is
 *                    drawn at about 11, and a sky body is a sight first and an almanac second
 * @param lit         whether it is shaded into a phase: by the sun's real direction for a fixed body,
 *                    by the world's moon phase for one riding vanilla's moon. A star or a glowing
 *                    thing would be {@code false}
 * @param glow        an atmosphere halo's colour ({@code 0xRRGGBB}) around the lit limb, if any
 * @param motion      how it moves across the sky
 * @param additive    drawn by adding light, the way vanilla draws its sun and moon: right when the
 *                    viewer stands under an atmosphere, whose sky shows through a body's dark side.
 *                    Off, the body covers what is behind it, which is right from an airless world
 */
record SkyBody(
        ResourceLocation texture, float yaw, float elevation, float angularSize, boolean lit, Optional<Integer> glow,
        Motion motion, boolean additive) {

    static final Codec<SkyBody> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(SkyBody::texture),
                    Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(SkyBody::yaw),
                    Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("elevation", 45.0F)
                            .forGetter(SkyBody::elevation),
                    Codec.floatRange(0.1F, 90.0F).fieldOf("angular_size").forGetter(SkyBody::angularSize),
                    Codec.BOOL.optionalFieldOf("lit", true).forGetter(SkyBody::lit),
                    Codec.INT.optionalFieldOf("glow").forGetter(SkyBody::glow),
                    Motion.CODEC.optionalFieldOf("motion", Motion.FIXED).forGetter(SkyBody::motion),
                    Codec.BOOL.optionalFieldOf("additive", false).forGetter(SkyBody::additive))
            .apply(instance, SkyBody::new));

    /** How a body moves across its sky. */
    enum Motion implements StringRepresentable {
        /**
         * Never moves, at its {@code yaw} and {@code elevation}. Right for a tidally locked moon's
         * view of its planet: Earth never rises or sets over the Moon.
         */
        FIXED("fixed"),
        /**
         * Exactly where vanilla draws its own moon, rising and setting opposite the sun, and in the
         * world's moon phase ({@code Level.getMoonPhase}), so what the sky shows still matches
         * everything in the game that reads that phase. For a body that replaces vanilla's moon.
         */
        VANILLA_MOON("vanilla_moon");

        static final Codec<Motion> CODEC = StringRepresentable.fromEnum(Motion::values);

        private final String name;

        Motion(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * One {@code ascension_sky} file: bodies for one dimension. Several files may name the same
     * dimension, from any namespace, and their bodies add up, so a mod can put its own planet in
     * our Moon's sky without touching our file.
     */
    record SkyFile(ResourceLocation dimension, List<SkyBody> bodies) {

        static final Codec<SkyFile> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        ResourceLocation.CODEC.fieldOf("dimension").forGetter(SkyFile::dimension),
                        SkyBody.CODEC.listOf().fieldOf("bodies").forGetter(SkyFile::bodies))
                .apply(instance, SkyFile::new));
    }
}
