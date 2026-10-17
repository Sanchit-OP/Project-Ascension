package com.ascension.combat.internal.stat;

import java.util.List;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * How much each modifier adds to an attribute's total, for showing where a stat comes from
 * (docs/technical/item-stats.md section 7). Pure arithmetic, so it is unit-tested.
 *
 * <p>Follows vanilla's own order ({@code AttributeInstance#calculateValue}): flat additions first,
 * then additions of a fraction of that sum, then multipliers one after another. A flat modifier's
 * share is its amount; a fraction-of-base modifier's share is that fraction of the flat sum; a
 * multiplier's share is what it adds to the running total when applied. The shares and the base add
 * up to the total exactly, before vanilla clamps it to the attribute's range.
 *
 * <p>Multipliers share by order: two +10% multipliers get slightly different shares depending on
 * which is applied first. The total never depends on it.
 */
public final class ModifierShares {

    private ModifierShares() {
    }

    /** {@code shares[i]} is what {@code modifiers.get(i)} adds to the total above {@code base}. */
    public static double[] of(double base, List<AttributeModifier> modifiers) {
        double[] shares = new double[modifiers.size()];

        double flat = base;
        for (int i = 0; i < modifiers.size(); i++) {
            AttributeModifier modifier = modifiers.get(i);
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                shares[i] = modifier.amount();
                flat += modifier.amount();
            }
        }

        double running = flat;
        for (int i = 0; i < modifiers.size(); i++) {
            AttributeModifier modifier = modifiers.get(i);
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                shares[i] = flat * modifier.amount();
                running += shares[i];
            }
        }

        for (int i = 0; i < modifiers.size(); i++) {
            AttributeModifier modifier = modifiers.get(i);
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                shares[i] = running * modifier.amount();
                running += shares[i];
            }
        }

        return shares;
    }
}
