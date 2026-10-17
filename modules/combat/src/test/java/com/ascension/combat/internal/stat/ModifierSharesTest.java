package com.ascension.combat.internal.stat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import org.junit.jupiter.api.Test;

/**
 * Each modifier's share of a stat's total, for the character sheet's sources. Pure arithmetic;
 * whether the sheet shows the right items in game is a client check (ADR-0008).
 */
class ModifierSharesTest {

    private static AttributeModifier modifier(String id, double amount, Operation operation) {
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", id), amount, operation);
    }

    /** What vanilla's AttributeInstance#calculateValue gives, without its clamp. */
    private static double vanillaTotal(double base, List<AttributeModifier> modifiers) {
        double flat = base;
        for (AttributeModifier m : modifiers) {
            if (m.operation() == Operation.ADD_VALUE) {
                flat += m.amount();
            }
        }
        double total = flat;
        for (AttributeModifier m : modifiers) {
            if (m.operation() == Operation.ADD_MULTIPLIED_BASE) {
                total += flat * m.amount();
            }
        }
        for (AttributeModifier m : modifiers) {
            if (m.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
                total *= 1.0 + m.amount();
            }
        }
        return total;
    }

    @Test
    void flatModifiersShareTheirOwnAmount() {
        // A player in a diamond chestplate and helmet: 20 base Health, +5 and +5.
        double[] shares = ModifierShares.of(20.0, List.of(
                modifier("chest", 5.0, Operation.ADD_VALUE),
                modifier("head", 5.0, Operation.ADD_VALUE)));

        assertArrayEquals(new double[] {5.0, 5.0}, shares, 1e-12);
    }

    @Test
    void percentOfBaseSharesAFractionOfTheFlatSum() {
        // A sword's attack speed: 4.0 base, -2.4 from the sword, then +20% and +30% from two items.
        double[] shares = ModifierShares.of(4.0, List.of(
                modifier("sword", -2.4, Operation.ADD_VALUE),
                modifier("boots", 0.2, Operation.ADD_MULTIPLIED_BASE),
                modifier("rune", 0.3, Operation.ADD_MULTIPLIED_BASE)));

        assertEquals(-2.4, shares[0], 1e-12);
        assertEquals(0.32, shares[1], 1e-12);
        assertEquals(0.48, shares[2], 1e-12);
    }

    @Test
    void baseAndSharesAddUpToTheTotalWithEveryOperationMixed() {
        List<AttributeModifier> modifiers = List.of(
                modifier("multiplier", 0.15, Operation.ADD_MULTIPLIED_TOTAL),
                modifier("flat", 7.0, Operation.ADD_VALUE),
                modifier("percent_of_base", 0.5, Operation.ADD_MULTIPLIED_BASE),
                modifier("another_multiplier", -0.1, Operation.ADD_MULTIPLIED_TOTAL),
                modifier("negative_flat", -2.0, Operation.ADD_VALUE));

        double[] shares = ModifierShares.of(1.0, modifiers);
        double sum = 1.0;
        for (double share : shares) {
            sum += share;
        }

        assertEquals(vanillaTotal(1.0, modifiers), sum, 1e-9);
    }

    @Test
    void noModifiersMeansNoShares() {
        assertEquals(0, ModifierShares.of(20.0, List.of()).length);
    }
}
