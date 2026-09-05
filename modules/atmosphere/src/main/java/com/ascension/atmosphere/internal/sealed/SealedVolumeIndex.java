package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Which positions are currently pressurised in one level.
 *
 * <p>Lives on a level attachment, so it is created per {@code ServerLevel} and freed when that
 * level unloads. A static map keyed by dimension would leak the contents of every world the
 * server ever loaded, which is exactly what ADR-0007 rule 3 exists to prevent.
 *
 * <p>Lookups are the hot path &mdash; every breathing player, every accounting pass &mdash; so
 * the union of all volumes is kept flattened into one set and queried in O(1).
 *
 * <p><strong>Emitters are tracked separately from their volumes.</strong> That separation is the
 * whole point: an emitter whose room is currently broken must still be reconsidered when the
 * wall is repaired. An earlier version dropped unsealed emitters from the map entirely, so a
 * broken room could never recover and the only cure was to break and replace the emitter.
 */
public final class SealedVolumeIndex {

    /** Every emitter in this level, sealed or not. */
    private final LongOpenHashSet emitters = new LongOpenHashSet();

    /** Emitter position to the volume it pressurises. Only sealed emitters appear here. */
    private final Long2ObjectOpenHashMap<LongOpenHashSet> volumes = new Long2ObjectOpenHashMap<>();

    /** Flattened union of every volume, for O(1) lookup. */
    private final LongOpenHashSet pressurised = new LongOpenHashSet();

    /** True when this position is inside any sealed volume. */
    public boolean isPressurised(BlockPos pos) {
        return !pressurised.isEmpty() && pressurised.contains(pos.asLong());
    }

    /** True when this level has no emitters at all, so queries can abstain immediately. */
    public boolean isEmpty() {
        return emitters.isEmpty();
    }

    /**
     * Start tracking an emitter and compute its volume.
     *
     * <p>Called when one is placed and again whenever its chunk loads, so the index survives
     * chunk unloads and server restarts without being serialised.
     */
    public SealedVolume.Result addEmitter(ServerLevel level, BlockPos emitter) {
        emitters.add(emitter.asLong());
        return update(level, emitter);
    }

    /** Stop tracking an emitter entirely. */
    public void removeEmitter(BlockPos emitter) {
        emitters.remove(emitter.asLong());
        if (volumes.remove(emitter.asLong()) != null) {
            rebuildUnion();
        }
    }

    /**
     * Recompute one emitter's volume.
     *
     * @return that emitter's own result, not the state of the level as a whole
     */
    public SealedVolume.Result update(ServerLevel level, BlockPos emitter) {
        SealedVolume.Result result = SealedVolume.fill(level, emitter);
        if (result.sealed()) {
            volumes.put(emitter.asLong(), result.positions());
        } else {
            volumes.remove(emitter.asLong());
        }
        rebuildUnion();
        return result;
    }

    /** Volume owned by one emitter, or zero if it is not sealed. */
    public int volumeSize(BlockPos emitter) {
        LongOpenHashSet volume = volumes.get(emitter.asLong());
        return volume == null ? 0 : volume.size();
    }

    /**
     * Recompute every emitter that could care about a changed position.
     *
     * <p>Relevance is measured from the emitter itself, not from its volume, because an emitter
     * with no volume is exactly the case that must be re-checked when a wall is repaired. A
     * block changed across the world costs one distance comparison per emitter.
     *
     * @return emitters that were sealed before this change and are not any more
     */
    public List<BlockPos> invalidateAround(ServerLevel level, BlockPos changed) {
        if (emitters.isEmpty()) {
            return List.of();
        }
        // Copied first: update() mutates `volumes` while we iterate.
        long[] candidates = emitters.toLongArray();
        List<BlockPos> broken = new ArrayList<>();

        for (long emitterPos : candidates) {
            BlockPos emitter = BlockPos.of(emitterPos);
            if (!withinReach(emitter, changed)) {
                continue;
            }
            boolean wasSealed = volumes.containsKey(emitterPos);
            boolean nowSealed = update(level, emitter).sealed();
            if (wasSealed && !nowSealed) {
                broken.add(emitter);
            }
        }
        return broken;
    }

    /**
     * Whether a change is close enough to possibly affect this emitter.
     *
     * <p>One block of slack past the fill radius, so a wall at the very edge of a
     * maximum-sized room still counts.
     */
    private static boolean withinReach(BlockPos emitter, BlockPos changed) {
        int reach = AtmosphereTuning.SEALED_VOLUME_RADIUS + 1;
        return Math.abs(emitter.getX() - changed.getX()) <= reach
                && Math.abs(emitter.getY() - changed.getY()) <= reach
                && Math.abs(emitter.getZ() - changed.getZ()) <= reach;
    }

    private void rebuildUnion() {
        pressurised.clear();
        for (LongOpenHashSet volume : volumes.values()) {
            pressurised.addAll(volume);
        }
    }

    /** Emitters currently tracked, sealed or not. */
    public int emitterCount() {
        return emitters.size();
    }

    /** Emitters that currently hold a sealed volume. */
    public int sealedCount() {
        return volumes.size();
    }

    public int pressurisedCount() {
        return pressurised.size();
    }
}
