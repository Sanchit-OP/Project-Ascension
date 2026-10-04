package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.CombatMobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Mortal Reminder's identity (docs/technical/combat-api.md section 6, the anti-tank family):
 * ignores a percentage of the target's armor, and applies grievous wounds on hit, refreshed by
 * every subsequent hit.
 *
 * <p>Checks {@code event.getSource().getWeaponItem()} rather than the shooter's main hand, unlike
 * {@link VeigosBladeEffects} and {@link DariusAxeEffects} &mdash; those fire on melee hits, which
 * are instantaneous, so "what's in the attacker's hand right now" and "what hit them" are the same
 * thing. An arrow is in flight for a while after release, and {@code AbstractArrow} already
 * captures the bow that fired it independently of what the shooter is holding by the time it
 * lands, so that is the correct thing to check for a ranged weapon.
 *
 * <p>Armor ignore is a {@link DamageContainer.Reduction#ARMOR} modifier registered on {@link
 * LivingIncomingDamageEvent} &mdash; per {@link DamageContainer}'s own javadoc, this is the only
 * point in the damage sequence where a reduction modifier still has any effect.
 */
public final class MortalReminderEffects {

    /** Bumped by Sanchit 2026-09-21: 50% -> 60% armor ignore. */
    private static final float ARMOR_IGNORE_PERCENT = 0.60f;

    private static final int GRIEVOUS_WOUNDS_DURATION_TICKS = 60;

    private MortalReminderEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(MortalReminderEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || !weapon.is(CombatContent.MORTAL_REMINDER.get())) {
            return;
        }

        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) -> reductionIn * (1.0f - ARMOR_IGNORE_PERCENT));

        LivingEntity target = event.getEntity();
        // The real gate: an attachment, not the effect below. addEffect is routinely a no-op on
        // exactly the targets this item cares most about -- see CombatAttachments.GRIEVOUS_WOUNDS_STATE.
        target.getData(CombatAttachments.GRIEVOUS_WOUNDS_STATE)
                .activateUntil(target.level().getGameTime() + GRIEVOUS_WOUNDS_DURATION_TICKS);
        // Cosmetic only from here: gives a status icon on targets that actually accept effects.
        target.addEffect(new MobEffectInstance(
                CombatMobEffects.GRIEVOUS_WOUNDS, GRIEVOUS_WOUNDS_DURATION_TICKS, 0));
    }
}
