package com.ascension.combat.client.hud;

import com.ascension.combat.internal.rune.hud.RuneHudEntry.Kind;

/**
 * What a rune chip shows for an entry at a moment: how full its ring is, whether it is lit, and
 * what number sits on it. Pure arithmetic, so a unit test decides it; how it looks is checked in a
 * client (ADR-0008).
 *
 * <p>The rule (docs/technical/hud.md): a chip is bright when its rune is doing something now, dim
 * when idle. Stacks and charges fill a ring and carry a number, a cooldown refills its ring until
 * the rune is ready, and a window drains its ring until it closes.
 */
final class RuneChipMath {

    private RuneChipMath() {
    }

    /** How much of the ring is drawn, 0 to 1. {@code total} is the length of the cooldown or window, in ticks. */
    static float fraction(Kind kind, int value, int max, long endTick, long now, long total) {
        return switch (kind) {
            case STACKS, CHARGE -> max <= 0 ? 0.0f : clamp((float) value / max);
            case COOLDOWN -> now >= endTick || total <= 0 ? 1.0f : clamp(1.0f - (float) (endTick - now) / total);
            case WINDOW -> now >= endTick || total <= 0 ? 0.0f : clamp((float) (endTick - now) / total);
        };
    }

    /** Whether the rune is doing something right now: lit. A cooldown is lit when it is ready to fire. */
    static boolean live(Kind kind, int value, long endTick, long now) {
        return switch (kind) {
            case STACKS, CHARGE -> value > 0;
            case COOLDOWN -> now >= endTick;
            case WINDOW -> now < endTick;
        };
    }

    /** Whether it is at its most: a full stack or a full charge, for the glow. */
    static boolean full(Kind kind, int value, int max) {
        return (kind == Kind.STACKS || kind == Kind.CHARGE) && max > 0 && value >= max;
    }

    /** The number to put on the chip, or -1 for none. */
    static int number(Kind kind, int value, long endTick, long now) {
        return switch (kind) {
            case STACKS, CHARGE -> value > 0 ? value : -1;
            case WINDOW -> value > 0 && now < endTick ? value : -1;
            case COOLDOWN -> -1;
        };
    }

    private static float clamp(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
