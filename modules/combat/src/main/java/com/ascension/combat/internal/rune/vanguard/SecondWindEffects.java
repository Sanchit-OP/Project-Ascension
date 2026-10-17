package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import com.ascension.core.api.CombatStateRegistry;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanguard's Second Wind minor (docs/technical/rune-system.md section 4.1): passive regeneration
 * while in combat and below half health &mdash; the path's sustain identity showing up on a minor,
 * not only the keystone.
 *
 * <p>Reuses {@link DurationState} as a cooldown gate ({@code isActive} means "still cooling down,
 * skip a heal tick") rather than a burst window &mdash; the same repurposing
 * {@link com.ascension.combat.internal.armor.RocketbeltEffects} makes of it for its landing-burst
 * delay, just read the opposite way (skip-while-active instead of act-while-active).
 *
 * <p>Its numbers are the rune's data: {@code heal}, {@code interval_seconds}, {@code below_health}
 * (a fraction of max HP).
 */
public final class SecondWindEffects {

    public static final ResourceLocation ID = Runes.id("second_wind");

    private SecondWindEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SecondWindEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        if (!CombatStateRegistry.isInCombat(player)) {
            return;
        }

        if (player.getHealth() >= player.getMaxHealth() * rune.get().value("below_health", 0.5)) {
            return;
        }

        DurationState cooldown = player.getData(RuneAttachments.SECOND_WIND_COOLDOWN);
        long now = player.level().getGameTime();
        if (cooldown.isActive(now)) {
            return;
        }

        player.heal((float) rune.get().value("heal", 1.0));
        cooldown.activateUntil(now + rune.get().ticks("interval_seconds", 1.0));
    }
}
