package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Keeps pressurised volumes in step with the blocks that form them.
 *
 * <p>Recomputation is driven entirely by block changes, never by a schedule (ADR-0007 rule 5),
 * and only for emitters close enough to care.
 *
 * <p><strong>Every recompute is deferred to later in the tick.</strong> {@code BreakEvent} and
 * {@code EntityPlaceEvent} are both cancellable, which means they fire <em>before</em> the world
 * actually changes. Filling from inside the handler reads the old world, so the volume was
 * always one edit behind: breaking a wall block reported the room still sealed, and it only
 * appeared to break on the <em>next</em> edit. Repair had the mirror-image problem, which is why
 * a single hole seemed to need patching twice before it took effect.
 *
 * <p><strong>Known gap, stated rather than hidden.</strong> Minecraft has no general "any block
 * changed" event and Tier 1 may not use mixins (ADR-0003 rule 5). Place, break and explosions
 * are covered; a wall altered by another mod's world edit is not noticed until something else
 * near that volume changes. The failure direction is safe: a room reads as sealed slightly too
 * long, rather than a player suffocating in a room that is genuinely fine.
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
        scheduleRecompute(event.getLevel(), event.getPos());
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        scheduleRecompute(event.getLevel(), event.getPos());
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        List<BlockPos> affected = event.getAffectedBlocks();
        if (!affected.isEmpty()) {
            // One deferred recompute is enough for the whole blast: by the time it runs, every
            // one of those block changes has already landed.
            scheduleRecompute(event.getLevel(), affected.get(0));
        }
    }

    /**
     * Queue the recompute so it runs after the block change has actually been applied.
     *
     * <p>{@code MinecraftServer.execute} runs the task on the server thread at the next
     * opportunity, which is the earliest point the world reflects the edit.
     */
    private static void scheduleRecompute(LevelAccessor levelAccessor, BlockPos pos) {
        if (!(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        if (level.getData(AtmosphereAttachments.SEALED_VOLUMES).isEmpty()) {
            return;
        }
        BlockPos frozen = pos.immutable();
        level.getServer().execute(() -> {
            SealedVolumeIndex.Changes changes = level
                    .getData(AtmosphereAttachments.SEALED_VOLUMES)
                    .invalidateAround(level, frozen);
            announce(level, changes);
        });
    }

    /**
     * Tell nearby players what happened to their room.
     *
     * <p>Both directions are announced. Reporting only failure meant a player who patched a hole
     * got no confirmation, and could not tell a working repair from a wasted one.
     *
     * <p>Sent to everyone near the emitter, not only whoever swung the pickaxe, because losing or
     * regaining pressure affects the whole room.
     */
    private static void announce(ServerLevel level, SealedVolumeIndex.Changes changes) {
        if (changes.isEmpty()) {
            return;
        }
        notifyNear(level, changes.broken(),
                Component.literal("Pressure lost — this space is no longer sealed")
                        .withStyle(ChatFormatting.RED));
        notifyNear(level, changes.restored(),
                Component.literal("Pressure restored")
                        .withStyle(ChatFormatting.GREEN));
    }

    private static void notifyNear(ServerLevel level, List<BlockPos> emitters, Component message) {
        if (emitters.isEmpty()) {
            return;
        }
        int reach = AtmosphereTuning.SEALED_VOLUME_RADIUS * 2;
        for (BlockPos emitter : emitters) {
            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().distManhattan(emitter) <= reach) {
                    player.displayClientMessage(message, true);
                }
            }
        }
    }
}
