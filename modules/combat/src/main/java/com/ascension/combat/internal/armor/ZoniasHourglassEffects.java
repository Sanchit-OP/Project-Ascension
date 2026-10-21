package com.ascension.combat.internal.armor;

import com.ascension.combat.api.WornStacks;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.Stasis;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Zonia's Hourglass's identity (docs/technical/combat-api.md section 6): stasis ({@link Stasis},
 * section 5) for a short duration, on a cooldown.
 *
 * <p>Triggered by {@link com.ascension.combat.internal.net.ActivateArmorAbilityPayload}, the same
 * shared key {@link GargoyleStoneplateEffects} and {@link RocketbeltEffects} use &mdash; the third
 * real consumer, and the first off-hand one, confirming the key was never really "armor-only,"
 * just "whatever's equipped with an active."
 *
 * <p>Unlike {@link GuardianWingEffects}, exiting stasis here does nothing but turn it off: no
 * revive, no health change. Zonia's Hourglass is pure defensive stasis, not a death-prevention
 * payoff.
 */
public final class ZoniasHourglassEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final long STASIS_DURATION_TICKS = 60L;

    private static final int COOLDOWN_TICKS = 600;

    private ZoniasHourglassEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ZoniasHourglassEffects::onPlayerTick);
    }

    public static void tryActivate(ServerPlayer player) {
        if (!WornStacks.isWorn(player, CombatContent.ZONIAS_HOURGLASS.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.ZONIAS_HOURGLASS.get())) {
            return;
        }

        long now = player.level().getGameTime();
        player.getData(CombatAttachments.ZONIAS_HOURGLASS_STASIS).activateUntil(now + STASIS_DURATION_TICKS);
        Stasis.enter(player);
        Stasis.syncToClient(player, true);
        player.getCooldowns().addCooldown(CombatContent.ZONIAS_HOURGLASS.get(), ItemAbilities.cooldown(player, COOLDOWN_TICKS));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState stasis = player.getData(CombatAttachments.ZONIAS_HOURGLASS_STASIS);
        long now = player.level().getGameTime();

        if (stasis.isActive(now)) {
            Stasis.tick(player);
            return;
        }

        if (stasis.hasExpired(now)) {
            stasis.clear();
            Stasis.exit(player);
            Stasis.syncToClient(player, false);
        }
    }
}
