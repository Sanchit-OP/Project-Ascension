package com.ascension.worlds.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * How a planet looks from space: the same face it shows in another world's sky, drawn as a sphere
 * at its real place and size ({@link SpaceSkyRenderer}). Pure appearance, so a client resource
 * ({@code assets/<namespace>/ascension_space/*.json}, loaded by {@link SpaceBodies}), as sky bodies
 * are; a planet with none is drawn as a disc in its {@code color}. Format: {@code worlds-api.md},
 * "Space bodies".
 *
 * @param planet  the planet's id in the {@code ascension_worlds:planet} registry
 * @param texture its face, drawn as a sphere: the image's inscribed circle is the disc, as for a
 *                {@link SkyBody}
 * @param lit     whether the sun shades it into a phase
 * @param glow    an atmosphere halo's colour ({@code 0xRRGGBB}) around the lit limb, if any
 * @param belt    if present the planet is an asteroid belt, not a sphere: its {@code texture} is the rock every
 *                piece of the belt wears, and the rocks are scattered through the planet's body (Sanchit,
 *                2026-10-20: the End is not a planet-shaped planet)
 * @param emissive the body's own light, if any: a mostly transparent texture added unshaded over the
 *                face and weighted to the night side, so a burning world's lava glows where the sun
 *                is not (proposed by the Planet 3 design session, 2026-10-11)
 */
record SpaceBody(ResourceLocation planet, ResourceLocation texture, boolean lit, Optional<Integer> glow,
                 Optional<ResourceLocation> emissive, Optional<Belt> belt) {

    /**
     * An asteroid belt (Sanchit, 2026-10-20: the End is not a planet-shaped planet). A wide, thin, flat band of many
     * irregular rocks through the planet's body, with dust and a bright core. Each rock is a camera-facing sprite cut from
     * the body's {@code texture}, an atlas of {@code sprites} irregular shapes (four to a row), tumbling slowly.
     *
     * @param count     rocks, the central one included
     * @param seed      the belt's seed: the same one gives the same belt on every client
     * @param thickness half the belt's height, as a fraction of the body's radius
     * @param minRadius the smallest rock's radius, in blocks; most rocks are near it
     * @param maxRadius the largest ordinary rock's radius, in blocks; a few reach it
     * @param inner     the fraction of the radius left empty at the middle, where the core shines
     * @param sprites   how many rock shapes the texture holds
     * @param central   the middle rock's radius as a multiple of {@code maxRadius}; 0 for none (the Dragon's island)
     * @param dust      a soft ring texture added over the belt's plane, if any: v runs inner to outer, u around
     * @param dustColour the dust's tint
     * @param coreTexture a soft round glow drawn at the middle, if any
     * @param coreColour the core glow's tint
     */
    record Belt(int count, long seed, float thickness, float minRadius, float maxRadius, float inner, int sprites,
                float central, Optional<ResourceLocation> dust, int dustColour, Optional<ResourceLocation> coreTexture,
                int coreColour) {

        static final Codec<Belt> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        Codec.intRange(1, 3000).optionalFieldOf("count", 140).forGetter(Belt::count),
                        Codec.LONG.optionalFieldOf("seed", 1L).forGetter(Belt::seed),
                        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("thickness", 0.15F).forGetter(Belt::thickness),
                        Codec.floatRange(0.5F, 200.0F).optionalFieldOf("min_radius", 6.0F).forGetter(Belt::minRadius),
                        Codec.floatRange(0.5F, 200.0F).optionalFieldOf("max_radius", 34.0F).forGetter(Belt::maxRadius),
                        Codec.floatRange(0.0F, 0.9F).optionalFieldOf("inner", 0.0F).forGetter(Belt::inner),
                        Codec.intRange(1, 64).optionalFieldOf("sprites", 1).forGetter(Belt::sprites),
                        Codec.floatRange(0.0F, 10.0F).optionalFieldOf("central", 0.0F).forGetter(Belt::central),
                        ResourceLocation.CODEC.optionalFieldOf("dust").forGetter(Belt::dust),
                        Codec.INT.optionalFieldOf("dust_colour", 0x8A78B8).forGetter(Belt::dustColour),
                        ResourceLocation.CODEC.optionalFieldOf("core_texture").forGetter(Belt::coreTexture),
                        Codec.INT.optionalFieldOf("core_colour", 0xC8B0F0).forGetter(Belt::coreColour))
                .apply(instance, Belt::new));
    }

    static final Codec<SpaceBody> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("planet").forGetter(SpaceBody::planet),
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(SpaceBody::texture),
                    Codec.BOOL.optionalFieldOf("lit", true).forGetter(SpaceBody::lit),
                    Codec.INT.optionalFieldOf("glow").forGetter(SpaceBody::glow),
                    ResourceLocation.CODEC.optionalFieldOf("emissive").forGetter(SpaceBody::emissive),
                    Belt.CODEC.optionalFieldOf("belt").forGetter(SpaceBody::belt))
            .apply(instance, SpaceBody::new));
}
