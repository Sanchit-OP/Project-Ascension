package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.Reentry;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneTargets;
import com.ascension.combat.internal.rune.Runes;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Reaver's Overkill minor (docs/technical/rune-system.md section 4.2): the damage a kill didn't
 * need splashes onto the nearest enemy.
 *
 * <p>Read in {@link LivingDamageEvent.Pre}, where the damage is final and the health is still what
 * it was, so "beyond what the kill needed" is plain subtraction. The splash goes only to
 * {@link RuneTargets#isFoe foes}, never a pet or a villager or another player, and it is a normal hit by
 * the player (so armour counts); {@link #SPLASH} stops it from splashing again in turn.
 *
 * <p>Its numbers are the rune's data: {@code radius} (blocks) and {@code max_damage}.
 */
public final class OverkillEffects {

    public static final ResourceLocation ID = Runes.id("overkill");

    /** Guards the splash from splashing in turn. */
    private static final Reentry SPLASH = new Reentry();

    private OverkillEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(OverkillEffects::onDamagePre);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (SPLASH.active() || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty() || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        float splash = splash(event.getNewDamage(), victim.getHealth() + victim.getAbsorptionAmount(),
                (float) rune.get().value("max_damage", 6.0));
        if (splash <= 0.0f) {
            return;
        }

        double radius = rune.get().value("radius", 4.0);
        RuneTargets.foes(level, attacker, victim.getBoundingBox().inflate(radius))
                .stream()
                .filter(e -> e != victim)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim)))
                .ifPresent(next -> SPLASH.run(() -> next.hurt(level.damageSources().playerAttack(attacker), splash)));
    }

    /** The damage that spills over: what the hit did beyond what was left, up to {@code max}; 0 if the hit didn't kill. */
    static float splash(float damage, float remaining, float max) {
        return Math.min(max, Math.max(0.0f, damage - remaining));
    }
}
