package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * The reduction half of grievous wounds (docs/technical/combat-api.md section 3): any healing
 * routed through {@link LivingEntity#heal(float)} &mdash; which is where vanilla regen, potions,
 * and this project's own future lifesteal/omnivamp on-hit healing will all go through &mdash; is
 * cut while the target carries {@link CombatAttachments#GRIEVOUS_WOUNDS_STATE}.
 *
 * <p>Checks the attachment, not {@link LivingEntity#hasEffect}. Confirmed live 2026-09-21 against
 * both the vanilla Wither (its own {@code addEffect} unconditionally returns {@code false}) and
 * L_Ender's Cataclysm's bosses (their shared base class only admits effects tagged
 * {@code cataclysm:effective_for_bosses}): the exact targets this mechanic exists to matter
 * against are the ones most likely to reject an ordinary {@code MobEffectInstance} outright, so
 * the vanilla effect can only ever be cosmetic here.
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
        if (!target.getData(CombatAttachments.GRIEVOUS_WOUNDS_STATE).isActive(target.level().getGameTime())) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0f - HEAL_REDUCTION_PERCENT));
    }
}
