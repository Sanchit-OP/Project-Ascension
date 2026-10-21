package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * How magic damage (docs/technical/combat-api.md section 2) actually lands: reduced by the
 * target's Magic Resist, never by armor. Every weapon with a magic-damage proc should add its
 * bonus through here instead of folding it into the same pre-mitigation amount a same-type
 * physical bonus would use &mdash; see {@link
 * com.ascension.combat.internal.weapons.SpearOfShojinEffects}'s own javadoc for why armor and
 * magic bonuses can't share one event: adding an unmitigated bonus before vanilla's own armor
 * reduction runs would let armor eat part of a bonus that's supposed to ignore it.
 *
 * <p><b>Penetration, reworked twice on 2026-09-20.</b> Used to special-case Wizard's Agony
 * directly here (its own hardcoded 25%, checked by equipment slot) since armor mitigation has a
 * generic reduction-modifier hook magic resist has none of. Briefly unified with Armor's own
 * penetration into one universal stat, then split back into {@link
 * CombatAttributes#MAGIC_PENETRATION_PERCENT} once Sanchit asked for Armor Pen and Magic Pen to
 * show as separate lines on the character sheet. Wizard's Agony contributes to this attribute
 * passively now instead of being named here specifically.
 */
public final class MagicDamage {

    /** Makes an attacker's magic damage stronger: Attunement, Clarity and Gathering Charge are boosters. */
    @FunctionalInterface
    public interface Booster {
        float multiplier(LivingEntity attacker, LivingEntity target);
    }

    /** Told after magic damage lands, with what actually landed: Siphon and Starfall watch it. */
    @FunctionalInterface
    public interface Observer {
        void dealt(LivingEntity attacker, LivingEntity target, float amount);
    }

    private static final java.util.List<Booster> BOOSTERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.List<Observer> OBSERVERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private MagicDamage() {
    }

    public static void addBooster(Booster booster) {
        BOOSTERS.add(booster);
    }

    public static void addObserver(Observer observer) {
        OBSERVERS.add(observer);
    }

    private static void notifyDealt(Entity attacker, LivingEntity target, float amount) {
        if (attacker instanceof LivingEntity livingAttacker && amount > 0.0f) {
            for (Observer observer : OBSERVERS) {
                observer.dealt(livingAttacker, target, amount);
            }
        }
    }

    /**
     * Applies {@code rawAmount} of magic damage to the current sequence, mitigated by the
     * target's Magic Resist. Must be called from a {@link LivingDamageEvent.Pre} listener, the
     * one point in the sequence where armor has already been applied but health hasn't.
     *
     * @return the amount actually added after mitigation, for callers (a lifesteal or heal-for-
     *         damage-dealt proc, say) that need to know what actually landed.
     */
    public static float apply(LivingDamageEvent.Pre event, float rawAmount) {
        float mitigated = mitigate(rawAmount, event.getEntity(), event.getSource().getEntity());
        event.setNewDamage(event.getNewDamage() + mitigated);
        notifyDealt(event.getSource().getEntity(), event.getEntity(), mitigated);
        return mitigated;
    }

    /**
     * Deals {@code rawAmount} of magic damage as a fresh hit, mitigated by the target's Magic
     * Resist, for a caller with no in-flight {@link LivingDamageEvent.Pre} to augment (Unending
     * Despair's periodic pulse, say, rather than a bonus riding an existing attack).
     *
     * <p>{@code source} must be a damage source in vanilla's own {@code
     * DamageTypeTags#BYPASSES_ARMOR} tag &mdash; {@code minecraft:magic} (confirmed in that tag
     * for 1.21.1) is the natural choice and what every current caller uses. Magic Resist is
     * mitigated here regardless, since it is entirely this module's own attribute and no vanilla
     * damage type applies it on its own.
     *
     * @return the amount actually dealt after mitigation, same as {@link #apply}.
     */
    public static float applyStandalone(LivingEntity target, DamageSource source, float rawAmount) {
        float mitigated = mitigate(rawAmount, target, source.getEntity());
        hurtSettled(target, source, mitigated);
        notifyDealt(source.getEntity(), target, mitigated);
        return mitigated;
    }

    /**
     * Like {@link #applyStandalone}, for a strike that lands by itself while the attacker is also hitting the target: a falling
     * star, a bolt of lightning, a shockwave (2026-10-22). Vanilla gives a target invulnerability frames after every hit, and a
     * second hit inside them only counts for what it adds over the first, so a strike that landed in the half second after a
     * sword swing was mostly swallowed, and the strike's own frames then swallowed the player's next swing. The frames are
     * therefore put to nothing for the strike and given back as they were, so it does its whole damage and costs the swing nothing.
     */
    public static float applyStrike(LivingEntity target, DamageSource source, float rawAmount) {
        float mitigated = mitigate(rawAmount, target, source.getEntity());
        int frames = target.invulnerableTime;
        target.invulnerableTime = 0;
        try {
            hurtSettled(target, source, mitigated);
        } finally {
            target.invulnerableTime = frames;
        }
        notifyDealt(source.getEntity(), target, mitigated);
        return mitigated;
    }

    /**
     * The one hit {@link #hurtSettled} is delivering right now, so {@link PhysicalDamage} can leave it
     * alone. Server thread only, set and cleared inside one call, never kept past it.
     */
    private static DamageSource settled;

    /**
     * Whether {@code source} is a hit whose defence has already been paid.
     *
     * <p>A magic hit of ours (a rune's strike, Unending Despair's pulse) is cut by Magic Resist before it is
     * dealt. Its damage type is magic, so without this the shared defence would cut it a second time
     * when the hit lands. Compared by identity: only the source object being delivered is settled.
     */
    public static boolean isSettled(DamageSource source) {
        return source == settled;
    }

    /**
     * Hurts {@code target} with an amount whose defence is already settled: no further Armor or Magic Resist
     * is applied by {@link PhysicalDamage}. For a caller that has done its own mitigation, or that means a
     * cost rather than an attack (a penalty on the wearer, say).
     */
    public static void hurtSettled(LivingEntity target, DamageSource source, float amount) {
        DamageSource previous = settled;
        settled = source;
        try {
            target.hurt(source, amount);
        } finally {
            settled = previous;
        }
    }

    /**
     * The fraction of a magic hit that gets through {@code target}'s Magic Resist, after the attacker's
     * Magic Penetration. 1.0 when the target has none. The one place this is worked out: weapon procs
     * ({@link #apply}) and incoming magic hits ({@link PhysicalDamage}) both read it.
     */
    public static double passThrough(LivingEntity target, Entity attacker) {
        AttributeInstance magicResist = target.getAttribute(CombatAttributes.MAGIC_RESIST);
        if (magicResist == null) {
            return 1.0;
        }
        return DefenseFormula.multiplier(magicResist.getValue() * (1.0 - penetrationPercent(attacker)));
    }

    private static float mitigate(float rawAmount, LivingEntity target, Entity attacker) {
        float boosted = rawAmount;
        if (attacker instanceof LivingEntity livingAttacker) {
            for (Booster booster : BOOSTERS) {
                boosted *= booster.multiplier(livingAttacker, target);
            }
        }
        return mitigateBoosted(boosted, target, attacker);
    }

    private static float mitigateBoosted(float rawAmount, LivingEntity target, Entity attacker) {
        return (float) (rawAmount * passThrough(target, attacker));
    }

    private static double penetrationPercent(Entity attacker) {
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return 0.0;
        }

        AttributeInstance penetrationPercent = livingAttacker.getAttribute(CombatAttributes.MAGIC_PENETRATION_PERCENT);
        return penetrationPercent == null ? 0.0 : penetrationPercent.getValue();
    }
}
