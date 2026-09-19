package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
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
 * <p>Also where Wizard's Agony's magic-penetration half lives, rather than in that item's own
 * effects class: penetration is fundamentally a mitigation-math concern, and every magic-damage
 * consumer (five weapons and counting) needs to honour it identically, so it has to sit at the
 * one choke point they all already go through rather than be threaded as a parameter into each
 * of their call sites. This is a deliberate, narrow exception to shared mechanics staying
 * item-agnostic &mdash; the armor-penetration half of the same item is ordinary per-item logic
 * in {@code WizardsAgonyEffects}, since armor mitigation already has a generic modifier hook
 * ({@code DamageContainer.Reduction}) that magic resist has no equivalent of.
 */
public final class MagicDamage {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final double WIZARDS_AGONY_MAGIC_PENETRATION_PERCENT = 0.25;

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
        float mitigated = mitigate(rawAmount, event.getEntity(), event.getSource().getEntity());
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
        float mitigated = mitigate(rawAmount, target, source.getEntity());
        target.hurt(source, mitigated);
        return mitigated;
    }

    private static float mitigate(float rawAmount, LivingEntity target, Entity attacker) {
        AttributeInstance magicResist = target.getAttribute(CombatAttributes.MAGIC_RESIST);
        if (magicResist == null) {
            return rawAmount;
        }

        double resist = magicResist.getValue() * (1.0 - magicPenetrationPercent(attacker));
        return (float) (rawAmount * DefenseFormula.multiplier(resist));
    }

    private static double magicPenetrationPercent(Entity attacker) {
        if (attacker instanceof Player player
                && player.getItemBySlot(EquipmentSlot.FEET).is(CombatContent.WIZARDS_AGONY.get())) {
            return WIZARDS_AGONY_MAGIC_PENETRATION_PERCENT;
        }
        return 0.0;
    }
}
