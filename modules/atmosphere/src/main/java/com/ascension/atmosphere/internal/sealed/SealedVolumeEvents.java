package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Keeps pressurised volumes in step with the blocks that form them.
 *
 * <p>Recomputation is driven entirely by block changes, never by a schedule (ADR-0007 rule 5),
 * and only for emitters whose volume actually touches the changed position.
 *
 * <p><strong>Known gap, stated rather than hidden.</strong> Minecraft has no general
 * "any block changed" event, and this module may not use mixins (ADR-0003 rule 5). The events
 * below cover what players actually do &mdash; placing, breaking, explosions &mdash; but a wall
 * altered by some other mod's world edit will not be noticed until something else near that
 * volume changes. The failure is safe in the direction that matters: a room reports as sealed
 * slightly too long, rather than a player suffocating in a room that is genuinely fine.
 */
public final class SealedVolumeEvents {

    private SealedVolumeEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SealedVolumeEvents::onPlace);
        gameBus.addListener(SealedVolumeEvents::onBreak);
        gameBus.addListener(SealedVolumeEvents::onExplosion);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        invalidate(event.getLevel(), event.getPos());
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        invalidate(event.getLevel(), event.getPos());
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        if (index.isEmpty()) {
            return;
        }
        // An explosion changes many blocks at once. Recomputing per block would run the same
        // fill dozens of times for one event, so let the first affected position trigger the
        // rebuild and rely on the fill seeing the finished state.
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (index.invalidateAround(level, pos)) {
                return;
            }
        }
    }

    private static void invalidate(LevelAccessor levelAccessor, BlockPos pos) {
        if (!(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        if (index.isEmpty()) {
            return;
        }
        index.invalidateAround(level, pos);
    }
}
