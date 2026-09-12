package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The combat-state tracker described in docs/technical/combat-api.md section 4: a player counts
 * as "in combat" for a rolling window after dealing or receiving damage. Lives entirely in
 * {@code ascension-combat} per ADR-0011 (the event listener is behaviour, which {@code core} may
 * never hold, no matter how small), and registers itself as {@code core}'s single {@code
 * CombatStateSource} so {@code ascension-atmosphere} can eventually read it without a direct
 * Tier 1 dependency. Combat's own items that need this (The Protean Mask, Edge of Night, Unending
 * Despair) read it the same way everyone else does, through {@link CombatStateRegistry}, rather
 * than duplicating the window logic locally &mdash; there is exactly one definition of "in
 * combat" in the whole game.
 *
 * <p>Not stood up until now because nothing needed it (see the note this replaces in {@code
 * AscensionCombat}'s own javadoc) &mdash; now that armor items actually consume it, standing it up
 * is answering a real question, not building for a hypothetical.
 *
 * <p>Window length (5s, matching Heartwood of Pallas's own corruption window) is the catalog's own
 * proposed starting point, not confirmed &mdash; open question, combat-api.md section 9. Reuses
 * {@link DurationState} for the per-player window rather than a new class: "in combat until this
 * game tick" is exactly that shape.
 */
public final class CombatStateTracking {

    /** Placeholder -- combat-api.md sections 3 and 9: proposed, not confirmed. */
    private static final long COMBAT_WINDOW_TICKS = 100L;

    private CombatStateTracking() {
    }

    public static void register(IEventBus bus) {
        CombatStateRegistry.register(CombatStateTracking::isInCombat);
        bus.addListener(CombatStateTracking::onIncomingDamage);
    }

    private static boolean isInCombat(ServerPlayer player) {
        return player.getData(CombatAttachments.COMBAT_STATE).isActive(player.level().getGameTime());
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target) {
            markInCombat(target);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            markInCombat(attacker);
        }
    }

    private static void markInCombat(ServerPlayer player) {
        player.getData(CombatAttachments.COMBAT_STATE)
                .activateUntil(player.level().getGameTime() + COMBAT_WINDOW_TICKS);
    }
}
