package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Wizard's Agony's identity (docs/technical/combat-api.md section 6, {@code ORIGINAL}): armor and
 * magic penetration while worn.
 *
 * <p>Only the armor half lives here. Armor mitigation already has a generic per-hit modifier hook
 * ({@link DamageContainer.Reduction}), the same one {@link
 * com.ascension.combat.internal.weapons.MortalReminderEffects} and {@link
 * com.ascension.combat.internal.weapons.WraithbladeEffects}'s passive use, so this item's
 * percentage version fits the same shape. Magic resist has no equivalent generic hook &mdash; it
 * is entirely this module's own attribute, checked only by whichever code deals magic damage in
 * the first place &mdash; so the magic-penetration half lives in {@link
 * com.ascension.combat.internal.mechanics.MagicDamage} instead, the one place every magic-damage
 * consumer already goes through.
 */
public final class WizardsAgonyEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float ARMOR_PENETRATION_PERCENT = 0.25f;

    private WizardsAgonyEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(WizardsAgonyEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getItemBySlot(EquipmentSlot.FEET).is(CombatContent.WIZARDS_AGONY.get())) {
            return;
        }

        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) -> reductionIn * (1.0f - ARMOR_PENETRATION_PERCENT));
    }
}
