package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The one shared payoff for {@link CombatAttributes#OMNIVAMP} (docs/technical/combat-api.md
 * section 3): heals the attacker for a percentage of any damage they deal, of either type, added
 * 2026-09-20 when Lifesteal and Omnivamp were collapsed into one real attribute instead of three
 * separate per-item heal listeners (Endless Hunger, Tasted Blood, Sterak's Harness) each
 * hardcoding its own percentage.
 *
 * <p>Reads {@link LivingDamageEvent.Post#getNewDamage}, not {@code .Pre} &mdash; the same reason
 * every one of those three items already read {@code .Post} on its own: healing off a
 * pre-mitigation number would make the target's Armor and Magic Resist worse than useless against
 * this weapon. Each item now only has to keep {@link CombatAttributes#OMNIVAMP} current (a flat
 * contribution while worn, a dynamic one recomputed per tick, or a temporary window &mdash;
 * whichever shape that item's own identity needs); this is the one place that actually turns the
 * number into healing, so two different Omnivamp sources stack into one real total instead of two
 * independent heals nobody can see summed.
 */
public final class OmnivampHealing {

    private OmnivampHealing() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(OmnivampHealing::onDamagePost);
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        AttributeInstance omnivamp = attacker.getAttribute(CombatAttributes.OMNIVAMP);
        if (omnivamp == null || omnivamp.getValue() <= 0.0) {
            return;
        }

        attacker.heal((float) (event.getNewDamage() * omnivamp.getValue()));
    }
}
