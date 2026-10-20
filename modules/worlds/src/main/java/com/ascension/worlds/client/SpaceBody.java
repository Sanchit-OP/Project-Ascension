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
     * An asteroid belt: {@code count} rocks from {@code seed} through the planet's body, each between
     * {@code minRadius} and {@code maxRadius} blocks across, in a layer {@code thickness} of the body's radius either
     * side of its plane.
     */
    record Belt(int count, long seed, float thickness, float minRadius, float maxRadius) {

        static final Codec<Belt> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        Codec.intRange(1, 1000).optionalFieldOf("count", 140).forGetter(Belt::count),
                        Codec.LONG.optionalFieldOf("seed", 1L).forGetter(Belt::seed),
                        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("thickness", 0.15F).forGetter(Belt::thickness),
                        Codec.floatRange(1.0F, 200.0F).optionalFieldOf("min_radius", 6.0F).forGetter(Belt::minRadius),
                        Codec.floatRange(1.0F, 200.0F).optionalFieldOf("max_radius", 34.0F).forGetter(Belt::maxRadius))
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
