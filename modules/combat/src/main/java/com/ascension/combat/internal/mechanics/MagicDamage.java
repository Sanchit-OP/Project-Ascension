package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.damagesource.DamageSource;
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
 */
public final class MagicDamage {

    /** Placeholder formula, same shape as League's own: 100 magic resist halves the damage. */
    private static final double MAGIC_RESIST_SCALE = 100.0;

    private MagicDamage() {
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
        float mitigated = mitigate(rawAmount, event.getEntity());
        event.setNewDamage(event.getNewDamage() + mitigated);
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
        float mitigated = mitigate(rawAmount, target);
        target.hurt(source, mitigated);
        return mitigated;
    }

    private static float mitigate(float rawAmount, LivingEntity target) {
        AttributeInstance magicResist = target.getAttribute(CombatAttributes.MAGIC_RESIST);
        if (magicResist == null) {
            return rawAmount;
        }

        double resist = magicResist.getValue();
        return (float) (rawAmount / (1.0 + resist / MAGIC_RESIST_SCALE));
    }
}
