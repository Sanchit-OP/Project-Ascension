package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneCatalog;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanguard's Second Wind minor (docs/technical/combat-api.md section 7): passive regeneration
 * while in combat and below half health &mdash; the path's sustain identity showing up on a minor,
 * not only the keystone.
 *
 * <p>Reuses {@link DurationState} as a cooldown gate ({@code isActive} means "still cooling down,
 * skip a heal tick") rather than a burst window &mdash; the same repurposing
 * {@link com.ascension.combat.internal.armor.RocketbeltEffects} makes of it for its landing-burst
 * delay, just read the opposite way (skip-while-active instead of act-while-active).
 */
public final class SecondWindEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float HEAL_PER_PULSE = 1.0f;

    private static final long HEAL_INTERVAL_TICKS = 20L;
    private static final float HEALTH_THRESHOLD_PERCENT = 0.5f;

    private SecondWindEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SecondWindEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getData(CombatAttachments.RUNE_PAGE).hasMinor(RuneCatalog.VANGUARD_SECOND_WIND.id())) {
            return;
        }

        if (!CombatStateRegistry.isInCombat(player)) {
            return;
        }

        if (player.getHealth() >= player.getMaxHealth() * HEALTH_THRESHOLD_PERCENT) {
            return;
        }

        DurationState cooldown = player.getData(CombatAttachments.SECOND_WIND_COOLDOWN);
        long now = player.level().getGameTime();
        if (cooldown.isActive(now)) {
            return;
        }

        player.heal(HEAL_PER_PULSE);
        cooldown.activateUntil(now + HEAL_INTERVAL_TICKS);
    }
}
