package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Replaces vanilla's own armor-mitigation formula with this module's own curve (docs/technical/combat-api.md
 * section 3) for every hit, universally &mdash; not gated on any weapon, the same way vanilla's own
 * armor mitigation applies to every physical hit regardless of what dealt it. See {@link
 * CombatAttributes#ARMOR}'s javadoc for why this reads a second, uncapped attribute rather than
 * vanilla's own {@code Attributes.ARMOR}.
 *
 * <p>{@link DamageContainer#addModifier} only ever fires when vanilla itself decides an ARMOR
 * reduction applies at all (i.e. the damage source isn't tagged {@code bypasses_armor}), which is
 * exactly the gate this module wants too &mdash; magic and true damage already skip this
 * category entirely on vanilla's own say-so, with no extra check needed here.
 *
 * <p>Registered at {@link EventPriority#HIGHEST} so this modifier is inserted into the {@code
 * ARMOR} reduction's function list before any per-weapon lethality or penetration modifier
 * (Wraithblade, Edge of Night, Wizard's Agony, Mortal Reminder) &mdash; those all subtract from or
 * scale whatever reduction already exists, so this module's own base curve has to be computed
 * first for that composition to mean anything. Reads {@code container.getNewDamage()} as the raw
 * damage to mitigate rather than the vanilla-computed {@code reductionIn} it's handed (which this
 * class ignores entirely) &mdash; at this point in the sequence nothing has subtracted from it yet,
 * so it reflects the fully-resolved pre-mitigation amount, bonus procs included.
 */
public final class PhysicalDamage {

    private PhysicalDamage() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGHEST, PhysicalDamage::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        AttributeInstance armor = target.getAttribute(CombatAttributes.ARMOR);
        if (armor == null) {
            return;
        }

        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) ->
                        container.getNewDamage() * (float) (1.0 - DefenseFormula.multiplier(armor.getValue())));
    }
}
