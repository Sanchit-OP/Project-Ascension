package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
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

    /**
     * Last payload sent to this player, for change detection.
     *
     * <p>Deliberately not in the codec: it is transient sync bookkeeping, not saved state, and
     * a reconnecting client must be resynced from scratch rather than trusted to remember.
     * Living on the attachment means it dies with the player instead of accumulating in a
     * server-side map keyed by player (ADR-0007 rule 1).
     */
    private transient OxygenSyncPayload lastSynced;

    /** Server tick of the last send, for the low-frequency reconcile. */
    private transient long lastSyncTick;

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

    public OxygenSyncPayload lastSynced() {
        return lastSynced;
    }

    public long lastSyncTick() {
        return lastSyncTick;
    }

    public void recordSync(OxygenSyncPayload payload, long tick) {
        this.lastSynced = payload;
        this.lastSyncTick = tick;
    }

    /** Forget sync bookkeeping so the next pass resends unconditionally. */
    public void invalidateSync() {
        this.lastSynced = null;
    }
}
