package com.ascension.atmosphere.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's own oxygen reserve, stored as a data attachment.
 *
 * <p>Attached to the player rather than kept in a side map, so it serialises with them and
 * cannot outlive them. A static {@code Map<Player, ...>} here would be the exact leak ADR-0007
 * rule 1 forbids.
 *
 * <p>Units, not seconds. Integer arithmetic stays exact across save and load; the conversion to
 * a readable duration happens once, at display time.
 */
public final class OxygenState {

    public static final Codec<OxygenState> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.INT.fieldOf("units").forGetter(OxygenState::units),
                    Codec.INT.fieldOf("suffocationTicks").forGetter(OxygenState::suffocationTicks))
            .apply(instance, OxygenState::new));

    private int units;
    private int suffocationTicks;

    public OxygenState() {
        this(0, 0);
    }

    public OxygenState(int units, int suffocationTicks) {
        this.units = units;
        this.suffocationTicks = suffocationTicks;
    }

    public int units() {
        return units;
    }

    public void setUnits(int units) {
        this.units = Math.max(0, units);
    }

    /** Ticks spent inside the failure window. Zero whenever the player can breathe. */
    public int suffocationTicks() {
        return suffocationTicks;
    }

    public void setSuffocationTicks(int ticks) {
        this.suffocationTicks = Math.max(0, ticks);
    }
}
