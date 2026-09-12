package com.ascension.combat.internal.mechanics;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * The reduction half of grievous wounds (docs/technical/combat-api.md section 3): any healing
 * routed through {@link LivingEntity#heal(float)} &mdash; which is where vanilla regen, potions,
 * and this project's own future lifesteal/omnivamp on-hit healing will all go through &mdash; is
 * cut while the target carries {@link CombatMobEffects#GRIEVOUS_WOUNDS}.
 */
public final class GrievousWoundsHealing {

    /** Confirmed by Sanchit 2026-09-08: 100% -- no healing at all while affected. */
    private static final float HEAL_REDUCTION_PERCENT = 1.00f;

    private GrievousWoundsHealing() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(GrievousWoundsHealing::onHeal);
    }

    private static void onHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.hasEffect(CombatMobEffects.GRIEVOUS_WOUNDS)) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0f - HEAL_REDUCTION_PERCENT));
    }
}
