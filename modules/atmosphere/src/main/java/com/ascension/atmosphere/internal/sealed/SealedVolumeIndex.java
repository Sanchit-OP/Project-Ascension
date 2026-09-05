package com.ascension.atmosphere.internal.sealed;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Which positions are currently pressurised in one level.
 *
 * <p>Lives on a level attachment, so it is created per {@code ServerLevel} and freed when that
 * level unloads. A static map keyed by dimension would leak the contents of every world the
 * server ever loaded, which is exactly what ADR-0007 rule 3 exists to prevent.
 *
 * <p>Lookups are the hot path &mdash; every breathing player hits this every accounting pass
 * &mdash; so the union of all emitter volumes is kept flattened into one set and queried in O(1)
 * rather than walking emitters.
 */
public final class SealedVolumeIndex {

    /** Emitter position to the volume it currently pressurises. */
    private final Long2ObjectOpenHashMap<LongOpenHashSet> volumes = new Long2ObjectOpenHashMap<>();

    /** Flattened union of every volume, for O(1) lookup. */
    private final LongOpenHashSet pressurised = new LongOpenHashSet();

    /** True when this position is inside any sealed volume. */
    public boolean isPressurised(BlockPos pos) {
        return !pressurised.isEmpty() && pressurised.contains(pos.asLong());
    }

    public boolean isEmpty() {
        return volumes.isEmpty();
    }

    /**
     * Recompute one emitter's volume and fold it back into the union.
     *
     * @return whether the emitter found a sealed space
     */
    public boolean update(ServerLevel level, BlockPos emitter) {
        SealedVolume.Result result = SealedVolume.fill(level, emitter);
        if (result.sealed()) {
            volumes.put(emitter.asLong(), result.positions());
        } else {
            volumes.remove(emitter.asLong());
        }
        rebuildUnion();
        return result.sealed();
    }

    public void remove(BlockPos emitter) {
        if (volumes.remove(emitter.asLong()) != null) {
            rebuildUnion();
        }
    }

    /**
     * Recompute every emitter whose volume contains, or is near, a changed position.
     *
     * <p>Only emitters that actually care are touched. A block broken on the far side of the
     * world costs a bounding-box comparison per emitter and nothing more.
     *
     * @return true if anything changed
     */
    public boolean invalidateAround(ServerLevel level, BlockPos changed) {
        if (volumes.isEmpty()) {
            return false;
        }
        long[] affected = volumes.keySet().toLongArray();
        boolean changedAny = false;

        for (long emitterPos : affected) {
            BlockPos emitter = BlockPos.of(emitterPos);
            LongOpenHashSet volume = volumes.get(emitterPos);

            // A change matters if it is inside the volume, or directly against its shell -- the
            // wall you just broke is not itself part of the pressurised space.
            boolean relevant = volume.contains(changed.asLong())
                    || touchesVolume(volume, changed);
            if (!relevant) {
                continue;
            }
            update(level, emitter);
            changedAny = true;
        }
        return changedAny;
    }

    private static boolean touchesVolume(LongOpenHashSet volume, BlockPos changed) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (var direction : net.minecraft.core.Direction.values()) {
            cursor.set(changed).move(direction);
            if (volume.contains(cursor.asLong())) {
                return true;
            }
        }
        return false;
    }

    private void rebuildUnion() {
        pressurised.clear();
        for (LongOpenHashSet volume : volumes.values()) {
            pressurised.addAll(volume);
        }
    }

    /** Emitter positions currently tracked, for diagnostics. */
    public int emitterCount() {
        return volumes.size();
    }

    public int pressurisedCount() {
        return pressurised.size();
    }
}
