package com.ascension.combat.internal.rune.hud;

import com.ascension.combat.internal.net.RuneHudSyncPayload;
import com.ascension.combat.internal.rune.RuneAttachments;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells a player's client what their equipped runes are doing, when it changes (ADR-0007).
 *
 * <p>Every few ticks it reads the equipped runes' state, compares it with what the player was last
 * told, and sends the list only if it differs. A cooldown does not change while it counts down (the
 * entry carries the tick it ends at), a stack changes about once a second, so a quiet player costs
 * a handful of comparisons and no packets. Only the owning player is ever sent anything: a second
 * player's HUD shows only their own runes.
 *
 * <p>What was last sent lives on the player ({@link RuneAttachments#HUD_SENT}), not in a static
 * map, so nothing outlives the player and a respawn simply starts again.
 */
public final class RuneHudSync {

    /** How often the state is checked: every other tick is quick enough to see and half the work. */
    private static final int CHECK_EVERY_TICKS = 2;

    private RuneHudSync() {
    }

    public static void register(IEventBus gameBus) {
        // Last in the tick, so every rune's own handler has already updated its state: the chips
        // then show this tick's state, never the one before it.
        gameBus.addListener(EventPriority.LOWEST, RuneHudSync::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_EVERY_TICKS != 0) {
            return;
        }
        List<RuneHudEntry> now = RuneHudProbes.read(player, player.level().getGameTime());
        List<RuneHudEntry> sent = player.getData(RuneAttachments.HUD_SENT);
        if (!differs(sent, now)) {
            return;
        }
        player.setData(RuneAttachments.HUD_SENT, List.copyOf(now));
        PacketDistributor.sendToPlayer(player, new RuneHudSyncPayload(now));
    }

    /** Whether the two lists differ in any way a player could see. Pure, so a test can decide it. */
    static boolean differs(List<RuneHudEntry> sent, List<RuneHudEntry> now) {
        return !sent.equals(now);
    }
}
