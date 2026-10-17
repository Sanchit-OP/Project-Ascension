package com.ascension.combat.api.stat;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Where one stat's total comes from, for display (docs/technical/item-stats.md section 7).
 *
 * <p>{@code parts} starts with the entity's own base value, then each worn or held item by name,
 * then each {@link StatSources registered source}, then "Other". Amounts are in the attribute's own
 * units, as {@link StatReader#value} gives them. They add up to the total before vanilla clamps it
 * to the attribute's range, so in the rare clamped case they add up to a little more than {@code
 * total}.
 *
 * @param total the stat's value, as {@link StatReader#value} gives it
 * @param parts each source's share of it
 */
public record StatBreakdown(double total, List<Part> parts) {

    /** One source's share: an item, a registered source such as growth, or the base value. */
    public record Part(Component source, double amount) {
    }
}
