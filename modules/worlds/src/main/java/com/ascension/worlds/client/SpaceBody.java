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
 * @param emissive the body's own light, if any: a mostly transparent texture added unshaded over the
 *                face and weighted to the night side, so a burning world's lava glows where the sun
 *                is not (proposed by the Planet 3 design session, 2026-10-11)
 */
record SpaceBody(ResourceLocation planet, ResourceLocation texture, boolean lit, Optional<Integer> glow,
                 Optional<ResourceLocation> emissive) {

    static final Codec<SpaceBody> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("planet").forGetter(SpaceBody::planet),
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(SpaceBody::texture),
                    Codec.BOOL.optionalFieldOf("lit", true).forGetter(SpaceBody::lit),
                    Codec.INT.optionalFieldOf("glow").forGetter(SpaceBody::glow),
                    ResourceLocation.CODEC.optionalFieldOf("emissive").forGetter(SpaceBody::emissive))
            .apply(instance, SpaceBody::new));
}
