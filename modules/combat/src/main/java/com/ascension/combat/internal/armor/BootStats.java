package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;

/**
 * Registers the four single-stat Boots (docs/technical/combat-api.md section 6) as {@link
 * StatBoots} rules. Wizard's Agony and Tasted Blood aren't here: penetration and omnivamp both
 * need real per-hit logic, not just an attribute modifier, so they get their own effects classes.
 */
public final class BootStats {

    /** Placeholders -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final double MERCURY_TREADS_MAGIC_RESIST = 4.0;

    private static final double NINJA_TABI_ARMOR = 4.0;
    private static final double SWIFTY_MOVE_SPEED_PERCENT = 0.15;
    private static final double BERSERKERS_GREAVES_ATTACK_SPEED_PERCENT = 0.15;

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
    }
}
