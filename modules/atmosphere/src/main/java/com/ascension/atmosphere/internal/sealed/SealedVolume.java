package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Works out which blocks an emitter can pressurise.
 *
 * <p>A bounded flood fill from the emitter through open space. If the fill reaches its limit
 * without closing, the space is not sealed and the emitter reports failure rather than quietly
 * pressurising an arbitrary blob of open world.
 *
 * <p>Bounded is doing real work here. An unbounded fill placed outdoors would walk every loaded
 * chunk, and per ADR-0007 this runs on block changes, which players generate constantly.
 */
public final class SealedVolume {

    /** Result of a fill attempt. */
    public record Result(boolean sealed, LongOpenHashSet positions) {

        private static final Result UNSEALED = new Result(false, new LongOpenHashSet());

        public static Result unsealed() {
            return UNSEALED;
        }

        public int size() {
            return positions.size();
        }
    }

    private SealedVolume() {
    }

    /**
     * Flood fill outward from {@code origin}.
     *
     * <p>Only reads already-loaded chunks. Forcing chunk loads from a block-change handler is a
     * reliable way to build a lag spike, and a room extending into unloaded chunks is not one we
     * can honestly call sealed.
     */
    public static Result fill(ServerLevel level, BlockPos origin) {
        final int limit = AtmosphereTuning.SEALED_VOLUME_LIMIT;
        final int maxRadius = AtmosphereTuning.SEALED_VOLUME_RADIUS;

        // Two sets, deliberately. `seen` stops the search revisiting anything, walls included.
        // `volume` is only the open space, and is what actually gets pressurised.
        //
        // Collapsing these into one set was the original bug: every wall block the search
        // touched ended up in the volume, so a 3x3x3 room reported 81 blocks -- 27 of interior
        // plus 54 of surrounding shell -- instead of 26.
        LongOpenHashSet seen = new LongOpenHashSet();
        LongOpenHashSet volume = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // The emitter's own block is solid, so it is a search seed but not breathable space.
        seen.add(origin.asLong());
        queue.enqueue(origin.asLong());

        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();

            for (Direction direction : Direction.values()) {
                cursor.set(current).move(direction);

                if (Math.abs(cursor.getX() - origin.getX()) > maxRadius
                        || Math.abs(cursor.getY() - origin.getY()) > maxRadius
                        || Math.abs(cursor.getZ() - origin.getZ()) > maxRadius) {
                    return Result.unsealed();
                }

                long next = cursor.asLong();
                if (!seen.add(next)) {
                    continue;
                }

                // Via the chunk source rather than LevelReader.hasChunk, which is deprecated,
                // and never via getBlockState on an unloaded position -- that would force a
                // chunk load from inside a block-change handler.
                if (!level.getChunkSource().hasChunk(
                        SectionPos.blockToSectionCoord(cursor.getX()),
                        SectionPos.blockToSectionCoord(cursor.getZ()))) {
                    return Result.unsealed();
                }
                if (!isOpen(level, cursor)) {
                    // A wall. Remembered so we do not test it again, but it holds no air.
                    continue;
                }

                volume.add(next);
                // Checked as the frontier grows, not once per dequeue: an open space can enqueue
                // far more than `limit` positions before any of them are processed.
                if (volume.size() > limit) {
                    return Result.unsealed();
                }
                queue.enqueue(next);
            }
        }
        return new Result(true, volume);
    }

    /**
     * Whether air can move through this block.
     *
     * <p>A full cube of collision seals; everything else leaks. Deliberately crude: a player can
     * predict "solid blocks hold air, slabs and fences do not" without a tooltip, and per
     * {@code docs/gameplay/oxygen.md} a model too subtle to communicate is overdesigned.
     */
    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return !state.isCollisionShapeFullBlock(level, pos);
    }
}
