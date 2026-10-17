package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How stats read as text, in one place: the character sheet, {@code /ascension stats}, and later
 * the League-style tooltips all show numbers the same way.
 *
 * <ul>
 *   <li>Points read as whole numbers, or with one decimal: 79, 9.3.</li>
 *   <li>Percent stats read as percentages: Omnivamp 0.08 reads 8%.</li>
 *   <li>Speeds read as the speed, to three decimals: 1.6 attacks a second, a walk of 0.1.</li>
 * </ul>
 *
 * Trailing zeros are dropped, so 62.0 reads 62.
 */
public final class StatText {

    private StatText() {
    }

    /** A stat's value, in the attribute's own units, as a player reads it. */
    public static String value(CombatStat stat, double value) {
        return switch (stat.kind()) {
            case POINTS -> number(value, 1);
            case PERCENT -> number(value * 100.0, 1) + "%";
            case SPEED -> number(value, 3);
        };
    }

    /** {@link #value}, with a plus sign when positive: one source's share of a total. */
    public static String signedValue(CombatStat stat, double value) {
        return signed(value, value(stat, value));
    }

    /** A stat as data writes it: a speed or percent bonus as {@code +20%}, points as {@code +15}. */
    public static String written(CombatStat stat, double written) {
        String number = number(written, 1);
        return signed(written, stat.kind() == CombatStat.Kind.POINTS ? number : number + "%");
    }

    /** {@code value} to at most {@code decimals} places, without trailing zeros. */
    public static String number(double value, int decimals) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0).toPlainString() : rounded.toPlainString();
    }

    private static String signed(double value, String text) {
        return value > 0.0 ? "+" + text : text;
    }
}
