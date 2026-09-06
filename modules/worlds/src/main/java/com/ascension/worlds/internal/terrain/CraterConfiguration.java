package com.ascension.worlds.internal.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A small crater's shape: how big, how deep, how tall the ejecta rim stands.
 *
 * <p>Radius is randomised per placement, the same way vanilla's own {@code DiskConfiguration}
 * randomises a disk's size &mdash; a crater feels wrong if every instance is identical. Large
 * craters use the same idea but aren't configured this way any more: see
 * {@link CraterFeature}'s javadoc for why they moved to a {@code Structure}, which carries its
 * own radius/depth/rim fields on {@link CraterStructure} instead of this record.
 */
public record CraterConfiguration(IntProvider radius, int floorDepth, int rimHeight)
        implements FeatureConfiguration {

    public static final Codec<CraterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    IntProvider.codec(1, 60).fieldOf("radius").forGetter(CraterConfiguration::radius),
                    Codec.intRange(0, 48).fieldOf("floor_depth").forGetter(CraterConfiguration::floorDepth),
                    Codec.intRange(0, 12).fieldOf("rim_height").forGetter(CraterConfiguration::rimHeight))
            .apply(instance, CraterConfiguration::new));
}
