package com.ascension.worlds.internal.terrain;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A handful of small, irregular rock pieces scattered loosely around one point in the vacuum
 * &mdash; the "rocks and debris" that make a long interplanetary flight something to occasionally
 * steer around, rather than a featureless void 99%+ of the time (that 99%+ comes from how rarely
 * this feature is placed at all &mdash; see {@code worldgen/placed_feature/rock_debris.json}'s
 * {@code rarity_filter} &mdash; not from anything in this class).
 *
 * <p>A cluster of several small pieces, not one large asteroid: {@code piece_count} independent
 * blobs placed at random offsets within {@code spread} blocks of the origin, each with its own
 * randomised centre and radius. One smooth sphere would read as a single deliberate landmark;
 * several ragged little pieces scattered around one point read as debris &mdash; wreckage, or a
 * field of rubble &mdash; which is the actual goal here.
 *
 * <p>Each piece's surface is jittered per-block (the inclusion test multiplies the piece's radius
 * by a random 0.6&ndash;1.0 factor at every position, rather than testing a fixed radius) for the
 * same reason: a perfect sphere reads as a placed object, an uneven one reads as a rock.
 *
 * <p>Placed via {@code minecraft:height_range} in the placed-feature JSON, not a heightmap &mdash;
 * the space dimension has no surface for a heightmap to find, so placement is a plain 3D scatter
 * through the dimension's build volume instead of anything column-based.
 */
public final class RockDebrisFeature extends Feature<RockDebrisConfiguration> {

    private static final BlockState[] ROCK_STATES = {
            Blocks.STONE.defaultBlockState(),
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.BASALT.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.COBBLED_DEEPSLATE.defaultBlockState()
    };

    public RockDebrisFeature(Codec<RockDebrisConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RockDebrisConfiguration> context) {
        RockDebrisConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int pieceCount = config.pieceCount().sample(random);
        int spread = config.spread();
        boolean changed = false;

        for (int i = 0; i < pieceCount; i++) {
            BlockPos center = origin.offset(
                    random.nextInt(spread * 2 + 1) - spread,
                    random.nextInt(spread * 2 + 1) - spread,
                    random.nextInt(spread * 2 + 1) - spread);
            int radius = config.pieceRadius().sample(random);
            if (placePiece(level, random, center, radius)) {
                changed = true;
            }
        }

        return changed;
    }

    private boolean placePiece(WorldGenLevel level, RandomSource random, BlockPos center, int radius) {
        boolean changed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = Math.sqrt((double) (dx * dx + dy * dy + dz * dz));
                    double jitteredRadius = radius * (0.6 + random.nextDouble() * 0.4);
                    if (distance > jitteredRadius) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    level.setBlock(cursor, ROCK_STATES[random.nextInt(ROCK_STATES.length)], 3);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
