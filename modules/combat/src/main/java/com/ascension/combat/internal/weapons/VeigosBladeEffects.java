package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Veigo's Blade's on-hit identity (docs/technical/combat-api.md section 6, the anti-tank family):
 * bonus physical damage as a percentage of the target's <em>current</em> HP.
 *
 * <p>Deliberately not an override on a custom {@code Item} subclass. NeoForge's damage pipeline
 * already hands every hit through {@link LivingIncomingDamageEvent} before mitigation runs, which
 * is the one place "which weapon, how much damage, against whom" are all available together at
 * once &mdash; checking the attacker's main hand here needs no bespoke item class.
 *
 * <p>The active (steal a burst of the target's move speed while slowing them) is not built yet.
 * combat-api.md scopes it as a follow-up once there is an "active item" mechanism to hang it on
 * &mdash; the same shape M1.7 shipped the tank before M1.8 added the valve.
 */
public final class VeigosBladeEffects {

    /**
     * Bonus damage as a fraction of the target's current HP. Placeholder &mdash; per
     * combat-api.md section 3, nothing here is balanced; Sanchit tunes by playing.
     */
    private static final float CURRENT_HP_PERCENT = 0.04f;

    private VeigosBladeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(VeigosBladeEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Nothing to add to a hit that would not have landed anyway -- a fully blocked or
        // otherwise negated attack should not still deal bonus damage.
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.VEIGOS_BLADE.get())) {
            return;
        }

        LivingEntity target = event.getEntity();
        float bonus = target.getHealth()
                * (CURRENT_HP_PERCENT + AbilityScaling.VEIGO_CURRENT_HP_PER_AD * AbilityScaling.attackDamage(attacker));
        event.setAmount(event.getAmount() + bonus);
    }
}
