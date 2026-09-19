package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Tasted Blood's identity (docs/technical/combat-api.md section 6, {@code ORIGINAL}): omnivamp
 * while worn &mdash; heal for a percentage of any damage dealt, not just physical.
 *
 * <p>Same shape as {@link com.ascension.combat.internal.weapons.EndlessHungerEffects}'s own
 * omnivamp, just gated on the boots slot instead of the main hand, and read from {@link
 * LivingDamageEvent.Post} for the same reason: it needs the actual final damage dealt, after
 * every reduction, so armor and magic resist against this wearer aren't rendered pointless.
 */
public final class TastedBloodEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float OMNIVAMP_PERCENT = 0.06f;

    private TastedBloodEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(TastedBloodEffects::onDamagePost);
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getItemBySlot(EquipmentSlot.FEET).is(CombatContent.TASTED_BLOOD.get())) {
            return;
        }

        attacker.heal(event.getNewDamage() * OMNIVAMP_PERCENT);
    }
}
