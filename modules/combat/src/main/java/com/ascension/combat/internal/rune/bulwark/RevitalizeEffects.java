package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Bulwark's Revitalize minor (docs/technical/rune-system.md section 4.4): heals and shields you
 * receive are stronger, more so when you are low. Heals are vanilla's own {@link LivingHealEvent},
 * read early ({@link EventPriority#HIGH}) so Pack Leader's pets share the stronger heal; shields go
 * through the shared {@link com.ascension.combat.internal.mechanics.Shield} door, which asks
 * {@link #shieldBoost}.
 *
 * <p>The design also says "give", but nothing in the game yet heals or shields <em>another</em>
 * player from a source this can tell apart; when something does, it joins here.
 *
 * <p>Its numbers are the rune's data: {@code bonus}, {@code low_bonus}, {@code low_health} (a
 * fraction).
 */
public final class RevitalizeEffects {

    public static final ResourceLocation ID = Runes.id("revitalize");

    private RevitalizeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, RevitalizeEffects::onHeal);
    }

    private static void onHeal(LivingHealEvent event) {
        if (event.getAmount() > 0.0f && event.getEntity() instanceof Player player) {
            event.setAmount(event.getAmount() * multiplier(player));
        }
    }

    /** The {@link com.ascension.combat.internal.mechanics.Shield.Booster} for shields a player is granted. */
    public static float shieldBoost(LivingEntity entity, float amount) {
        return entity instanceof Player player ? amount * multiplier(player) : amount;
    }

    private static float multiplier(Player player) {
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return 1.0f;
        }

        return multiplier(player.getHealth(), StatReader.value(player, CombatStats.HEALTH), rune.get().value("bonus", 0.10),
                rune.get().value("low_bonus", 0.20), rune.get().value("low_health", 0.40));
    }

    /** The multiplier for what is received: the {@code lowBonus} when health is under {@code lowHealth} of max, else {@code bonus}. */
    static float multiplier(double health, double max, double bonus, double lowBonus, double lowHealth) {
        boolean low = max > 0.0 && health / max < lowHealth;
        return 1.0f + (float) (low ? lowBonus : bonus);
    }
}
