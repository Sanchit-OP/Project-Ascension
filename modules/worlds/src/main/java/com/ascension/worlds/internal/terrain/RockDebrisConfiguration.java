package com.ascension.worlds.internal.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A cluster of loose rock pieces scattered around one placement point &mdash; see
 * {@link RockDebrisFeature}'s javadoc for why several small pieces read as debris where one large
 * blob would read as a deliberately placed landmark.
 *
 * @param pieceCount  how many separate rock pieces this cluster scatters
 * @param spread      how far from the placement point (in any direction) a piece's centre may
 *                    land; space has no ground to spread across, so this reaches up/down too
 * @param pieceRadius one piece's rough size before its surface is jittered
 */
public record RockDebrisConfiguration(IntProvider pieceCount, int spread, IntProvider pieceRadius)
        implements FeatureConfiguration {

    public static final Codec<RockDebrisConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    IntProvider.codec(1, 32).fieldOf("piece_count")
                            .forGetter(RockDebrisConfiguration::pieceCount),
                    Codec.intRange(1, 64).fieldOf("spread").forGetter(RockDebrisConfiguration::spread),
                    IntProvider.codec(1, 16).fieldOf("piece_radius")
                            .forGetter(RockDebrisConfiguration::pieceRadius))
            .apply(instance, RockDebrisConfiguration::new));
}
