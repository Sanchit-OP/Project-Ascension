package com.ascension.worlds.internal.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A crater's shape: how big, how deep, how tall the ejecta rim stands.
 *
 * <p>Radius is randomised per placement, the same way vanilla's own {@code DiskConfiguration}
 * randomises a disk's size &mdash; a crater feels wrong if every instance is identical. Depth and
 * rim height stay fixed per configuration; two configured features with different numbers
 * (small and common, large and rare) are how "most craters are small dents, a few are basins"
 * happens, without a second {@link CraterFeature} class.
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
