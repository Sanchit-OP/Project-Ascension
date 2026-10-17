package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reaver's Finishing Blow minor (docs/technical/rune-system.md section 4.2): more damage to
 * enemies that are nearly dead. Reads max HP through the stat system, like every rune that
 * compares health.
 *
 * <p>Its numbers are the rune's data: {@code below_health} (a fraction) and {@code bonus}.
 */
public final class FinishingBlowEffects {

    public static final ResourceLocation ID = Runes.id("finishing_blow");

    private FinishingBlowEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(FinishingBlowEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }

        LivingEntity victim = event.getEntity();
        double max = StatReader.value(victim, CombatStats.HEALTH);
        if (isNearlyDead(victim.getHealth(), max, rune.get().value("below_health", 0.4))) {
            event.setAmount(event.getAmount() * (1.0f + (float) rune.get().value("bonus", 0.12)));
        }
    }

    /** Whether {@code health} of {@code max} is below the {@code threshold} fraction. */
    static boolean isNearlyDead(double health, double max, double threshold) {
        return max > 0.0 && health / max < threshold;
    }
}
