package com.ascension.combat.internal.rune.vanguard;

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
 * Vanguard's Giant Slayer minor (docs/technical/rune-system.md section 4.1): you deal more damage
 * to an enemy the more max HP it has than you. Bosses above all.
 *
 * <p>Reads max HP through the stat system ({@link StatReader}, never the attribute), so armour,
 * growth and a mob's own data all count the same way they do on the character sheet.
 *
 * <p>Its numbers are the rune's data: nothing below {@code min_ratio} (enemy HP over yours), then
 * {@code min_bonus} at that ratio, rising in a straight line to {@code max_bonus} at {@code
 * full_ratio} and no further.
 */
public final class GiantSlayerEffects {

    public static final ResourceLocation ID = Runes.id("giant_slayer");

    private GiantSlayerEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(GiantSlayerEffects::onIncomingDamage);
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
        double mine = StatReader.value(attacker, CombatStats.HEALTH);
        double theirs = StatReader.value(victim, CombatStats.HEALTH);
        if (mine <= 0.0) {
            return;
        }

        RuneDefinition values = rune.get();
        double bonus = bonus(theirs / mine, values.value("min_ratio", 1.1), values.value("min_bonus", 0.05),
                values.value("full_ratio", 2.0), values.value("max_bonus", 0.15));
        if (bonus > 0.0) {
            event.setAmount(event.getAmount() * (1.0f + (float) bonus));
        }
    }

    /**
     * The extra damage, as a fraction, against an enemy with {@code ratio} times your max HP: none
     * below {@code minRatio}, {@code minBonus} at it, a straight line up to {@code maxBonus} at
     * {@code fullRatio}, and that for anything bigger.
     */
    static double bonus(double ratio, double minRatio, double minBonus, double fullRatio, double maxBonus) {
        if (ratio < minRatio) {
            return 0.0;
        }

        if (ratio >= fullRatio || fullRatio <= minRatio) {
            return maxBonus;
        }

        return minBonus + (maxBonus - minBonus) * (ratio - minRatio) / (fullRatio - minRatio);
    }
}
