package com.ascension.worlds.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Something hanging in a dimension's sky: Earth seen from the Moon is the first. Pure appearance,
 * so it is a client resource ({@code assets/<namespace>/ascension_sky/*.json}, loaded by
 * {@link SkyBodies}), not part of the server's {@code Planet} record: a resource pack can restyle
 * it, the server never hears of it, and the {@code api} record keeps its shape. Format and
 * examples: {@code docs/technical/worlds-api.md}, "Sky bodies".
 *
 * <p>A body is fixed in the sky. That is right for the Moon (tidally locked: Earth never rises or
 * sets there) and is all v1 needs; a body that moves with the day would be a new field.
 *
 * @param texture     the body's face, drawn as a sphere: the image's inscribed circle is the disc
 * @param yaw         compass heading, Minecraft's convention (0 south, 90 west, 180 north, 270 east)
 * @param elevation   degrees above the horizon
 * @param angularSize degrees across. Real Earth from the Moon is about 2; vanilla's own moon is
 *                    drawn at about 11, and a sky body is a sight first and an almanac second
 * @param lit         whether the sun shades it, which draws its phase over the day. A star or a
 *                    glowing thing would be {@code false}
 * @param glow        an atmosphere halo's colour ({@code 0xRRGGBB}) around the lit limb, if any
 */
record SkyBody(
        ResourceLocation texture, float yaw, float elevation, float angularSize, boolean lit, Optional<Integer> glow) {

    static final Codec<SkyBody> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(SkyBody::texture),
                    Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(SkyBody::yaw),
                    Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("elevation", 45.0F)
                            .forGetter(SkyBody::elevation),
                    Codec.floatRange(0.1F, 90.0F).fieldOf("angular_size").forGetter(SkyBody::angularSize),
                    Codec.BOOL.optionalFieldOf("lit", true).forGetter(SkyBody::lit),
                    Codec.INT.optionalFieldOf("glow").forGetter(SkyBody::glow))
            .apply(instance, SkyBody::new));

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
