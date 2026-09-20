package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;

/**
 * Registers all six single-stat Boots (docs/technical/combat-api.md section 6) as {@link
 * StatBoots} rules. Wizard's Agony and Tasted Blood joined 2026-09-20, once {@link
 * CombatAttributes#ARMOR_PENETRATION_PERCENT}/{@link CombatAttributes#MAGIC_PENETRATION_PERCENT}
 * and {@link CombatAttributes#OMNIVAMP} became real attributes rather than per-item hardcoded
 * percentages &mdash; each used to need its own effects class purely to check its own equipment
 * slot and apply its own private constant; now that the payoff (damage mitigation, on-hit healing)
 * is read centrally off the attribute by {@link com.ascension.combat.internal.mechanics.PhysicalDamage},
 * {@link com.ascension.combat.internal.mechanics.MagicDamage}, and {@link
 * com.ascension.combat.internal.mechanics.OmnivampHealing}, both graduate to the exact same shape
 * as the other four. Wizard's Agony is two separate {@link StatBoots} rules, not one &mdash; Armor
 * Pen and Magic Pen are tracked as two independent attributes (Sanchit's own call), so one item
 * granting both is two contributions, not a shared one.
 */
public final class BootStats {

    /** Rebalanced 2026-09-20 (Sanchit's own numbers, all placeholders per combat-api.md section 3). */
    private static final double MERCURY_TREADS_MAGIC_RESIST = 15.0;

    private static final double NINJA_TABI_ARMOR = 15.0;
    private static final double SWIFTY_MOVE_SPEED_PERCENT = 0.15;
    private static final double BERSERKERS_GREAVES_ATTACK_SPEED_PERCENT = 0.50;
    private static final double WIZARDS_AGONY_PENETRATION_PERCENT = 0.10;
    private static final double TASTED_BLOOD_OMNIVAMP = 0.08;

    private BootStats() {
    }

    public static void register(IEventBus bus) {
        new StatBoots(
                        "mercury_treads_magic_resist",
                        CombatContent.MERCURY_TREADS,
                        EquipmentSlot.FEET,
                        () -> CombatAttributes.MAGIC_RESIST,
                        MERCURY_TREADS_MAGIC_RESIST,
                        AttributeModifier.Operation.ADD_VALUE)
                .register(bus);

        new StatBoots(
                        "ninja_tabi_armor",
                        CombatContent.NINJA_TABI,
                        EquipmentSlot.FEET,
                        () -> CombatAttributes.ARMOR,
                        NINJA_TABI_ARMOR,
                        AttributeModifier.Operation.ADD_VALUE)
                .register(bus);

        new StatBoots(
                        "swifty_move_speed",
                        CombatContent.SWIFTY,
                        EquipmentSlot.FEET,
                        () -> Attributes.MOVEMENT_SPEED,
                        SWIFTY_MOVE_SPEED_PERCENT,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .register(bus);

        new StatBoots(
                        "berserkers_greaves_attack_speed",
                        CombatContent.BERSERKERS_GREAVES,
                        EquipmentSlot.FEET,
                        () -> Attributes.ATTACK_SPEED,
                        BERSERKERS_GREAVES_ATTACK_SPEED_PERCENT,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .register(bus);

        new StatBoots(
                        "wizards_agony_armor_penetration",
                        CombatContent.WIZARDS_AGONY,
                        EquipmentSlot.FEET,
                        () -> CombatAttributes.ARMOR_PENETRATION_PERCENT,
                        WIZARDS_AGONY_PENETRATION_PERCENT,
                        AttributeModifier.Operation.ADD_VALUE)
                .register(bus);

        new StatBoots(
                        "wizards_agony_magic_penetration",
                        CombatContent.WIZARDS_AGONY,
                        EquipmentSlot.FEET,
                        () -> CombatAttributes.MAGIC_PENETRATION_PERCENT,
                        WIZARDS_AGONY_PENETRATION_PERCENT,
                        AttributeModifier.Operation.ADD_VALUE)
                .register(bus);

        new StatBoots(
                        "tasted_blood_omnivamp",
                        CombatContent.TASTED_BLOOD,
                        EquipmentSlot.FEET,
                        () -> CombatAttributes.OMNIVAMP,
                        TASTED_BLOOD_OMNIVAMP,
                        AttributeModifier.Operation.ADD_VALUE)
                .register(bus);
    }
}
