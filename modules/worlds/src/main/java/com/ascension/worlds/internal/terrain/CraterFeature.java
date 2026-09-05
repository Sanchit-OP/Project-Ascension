package com.ascension.worlds.internal.terrain;

import com.mojang.serialization.Codec;
import com.ascension.worlds.internal.WorldsContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A bowl carved into the surface, with a raised rim of loose ejecta around it.
 *
 * <p>The one piece of real engineering M2.3's terrain needed. Vanilla's density-function
 * worldgen can shape rolling hills through pure JSON, but a ring-shaped rim around a bowl is not
 * expressible as noise &mdash; noise has no notion of "this shape, centred here, this size". A
 * crater needs code once; after that, every crater on the Moon (or a future airless world) is
 * one more {@link CraterConfiguration} and a {@code placed_feature} choosing how common it is.
 * Small and common, large and rare, are two configured features pointing at this same class, not
 * two classes (see {@code worldgen/configured_feature/small_crater.json} and
 * {@code large_crater.json}).
 *
 * <p>Runs at the {@code LOCAL_MODIFICATIONS} step, after terrain shape but before the ore feature
 * at {@code UNDERGROUND_ORES} &mdash; so ore placement correctly sees a crater's carved-out
 * interior as already-air, and never replaces stone that a crater already removed.
 *
 * <p>Column-by-column, not a 3D scan: the height at every position in range is read once from
 * the heightmap, and only the blocks that actually change are touched. A radius-32 crater is at
 * most a 65&times;65 column footprint, each column touching at most a few dozen blocks
 * vertically &mdash; a one-time cost paid during chunk generation, nothing that runs per tick
 * (ADR-0007 rule 3 is about ongoing cost; a single generation-time pass is not what it guards
 * against).
 */
public final class CraterFeature extends Feature<CraterConfiguration> {

    public CraterFeature(Codec<CraterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterConfiguration> context) {
        CraterConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int radius = config.radius().sample(random);
        int floorDepth = config.floorDepth();
        int rimHeight = config.rimHeight();
        // The rim tapers off over a band just outside the bowl, roughly a sixth of the crater's
        // size -- wide enough to read as a rim rather than a wall, narrow enough not to blur two
        // nearby craters into one shape.
        int rimBand = Math.max(1, radius / 6);
        int searchRadius = radius + rimBand;

        boolean changed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState regolith = WorldsContent.MOON_REGOLITH.get().defaultBlockState();

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > searchRadius) {
                    continue;
                }

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                if (distance <= radius) {
                    // Inside the bowl: a parabolic dip, deepest at the centre, meeting
                    // undisturbed ground exactly at the rim.
                    double falloff = 1.0 - (distance * distance) / (radius * radius);
                    int depthHere = (int) Math.round(floorDepth * falloff);
                    for (int y = surfaceY; y > surfaceY - depthHere; y--) {
                        cursor.set(x, y, z);
                        if (level.getBlockState(cursor).isAir()) {
                            // Already open -- carving further would break into whatever left it
                            // that way, rather than shaping the crater.
                            break;
                        }
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                        changed = true;
                    }
                } else {
                    // The rim band: ejecta piled highest at the bowl's edge, tapering to nothing
                    // at the band's outer edge.
                    double bandPosition = (distance - radius) / rimBand;
                    int riseHere = (int) Math.round(rimHeight * (1.0 - bandPosition));
                    for (int i = 1; i <= riseHere; i++) {
                        cursor.set(x, surfaceY + i, z);
                        level.setBlock(cursor, regolith, 3);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }
}
