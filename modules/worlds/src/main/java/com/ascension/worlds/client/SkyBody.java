package com.ascension.worlds.client;

import com.ascension.worlds.internal.GateProgress;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
 * @param gateLight   a light on the body's face that appears with the first lit gate and brightens
 *                    with each one after, if any: the Archive's pylon seen from Earth
 */
record SkyBody(
        ResourceLocation texture, float yaw, float elevation, float angularSize, boolean lit, Optional<Integer> glow,
        Motion motion, boolean additive, Optional<GateLight> gateLight) {

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
                    Codec.BOOL.optionalFieldOf("additive", false).forGetter(SkyBody::additive),
                    GateLight.CODEC.optionalFieldOf("gate_light").forGetter(SkyBody::gateLight))
            .apply(instance, SkyBody::new));

    /**
     * A point of light on a body's face, there once a gate is lit ({@code docs/lore/the-mark.md}: the
     * first glyph on the Archive's pylon begins to glow, and that is the light seen from Earth).
     * Drawn additively and unshaded, so it shines on the night side too, and brighter with every
     * gate, as more of the pylon's glyphs glow.
     *
     * @param texture the light's picture, drawn additively: alpha carries its falloff
     * @param x       where on the face, -1 (left edge) to 1 (right), as seen with the body upright
     * @param y       where on the face, -1 (bottom) to 1 (top)
     * @param size    its width as a fraction of the body's width
     */
    record GateLight(ResourceLocation texture, float x, float y, float size) {

        static final Codec<GateLight> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        ResourceLocation.CODEC.fieldOf("texture").forGetter(GateLight::texture),
                        Codec.floatRange(-1.0F, 1.0F).fieldOf("x").forGetter(GateLight::x),
                        Codec.floatRange(-1.0F, 1.0F).fieldOf("y").forGetter(GateLight::y),
                        Codec.floatRange(0.01F, 1.0F).fieldOf("size").forGetter(GateLight::size))
                .apply(instance, GateLight::new));

        /** How bright the first gate's light is; every gate after adds an even share of the rest. */
        static final float FIRST_GATE = 0.45F;

        /** 0 with no gate lit, {@link #FIRST_GATE} with one, full once every glyph of the mark is. */
        static float alpha(int stage) {
            if (stage <= 0) {
                return 0.0F;
            }
            int steps = GateProgress.MAX_STAGE - 1;
            return FIRST_GATE + (1.0F - FIRST_GATE) * (Math.min(stage, GateProgress.MAX_STAGE) - 1) / steps;
        }
    }

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
     * One {@code ascension_sky} file: bodies for one dimension, and optionally its haze. Several
     * files may name the same dimension, from any namespace, and their bodies add up, so a mod can
     * put its own planet in our Moon's sky without touching our file. Haze does not add up: the
     * last file loaded with one wins.
     */
    record SkyFile(ResourceLocation dimension, List<SkyBody> bodies, Optional<Haze> haze) {

        static final Codec<SkyFile> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        ResourceLocation.CODEC.fieldOf("dimension").forGetter(SkyFile::dimension),
                        SkyBody.CODEC.listOf().optionalFieldOf("bodies", List.of()).forGetter(SkyFile::bodies),
                        Haze.CODEC.optionalFieldOf("haze").forGetter(SkyFile::haze))
                .apply(instance, SkyFile::new));
    }

    /**
     * How far a dimension's air lets you see, in place of its own fog: built for the Nether opened
     * to the sky, whose vanilla fog closes in at about 96 blocks, so a ship coming down from 216
     * would see nothing of the ground. Applied by {@link SkyHaze}, never past the render distance,
     * and not while the camera is under water, in lava or blinded.
     *
     * @param start  where the haze begins, in blocks
     * @param end    where nothing shows through it any more, in blocks
     * @param colour the haze's colour ({@code 0xRRGGBB}), if it overrides the biome's fog colour.
     *               In a world with no sky of its own (the Nether) this is also the colour overhead
     */
    record Haze(float start, float end, Optional<Integer> colour) {

        static final Codec<Haze> CODEC = RecordCodecBuilder.<Haze>create(instance -> instance
                        .group(
                                Codec.floatRange(0.0F, 4096.0F).fieldOf("start").forGetter(Haze::start),
                                Codec.floatRange(1.0F, 4096.0F).fieldOf("end").forGetter(Haze::end),
                                Codec.INT.optionalFieldOf("color").forGetter(Haze::colour))
                        .apply(instance, Haze::new))
                .validate(haze -> haze.end() > haze.start()
                        ? DataResult.success(haze)
                        : DataResult.error(() -> "haze end must be beyond its start"));
    }
}
