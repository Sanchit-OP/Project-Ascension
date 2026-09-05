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
                    Codec.INT.optionalFieldOf("lungUnits", AtmosphereTuning.LUNG_CAPACITY)
                            .forGetter(OxygenState::lungUnits),
                    Codec.INT.optionalFieldOf("suffocationTicks", 0)
                            .forGetter(OxygenState::suffocationTicks))
            .apply(instance, OxygenState::new));

    private int lungUnits;
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

    /**
     * Fractional units carried between accounting passes.
     *
     * <p>Units are integers, but drain rarely lands on a whole number once modifiers apply.
     * Rounding every pass would quietly change the effective rate; carrying the remainder keeps
     * the advertised seconds honest. Not serialised, because losing at most one unit across a
     * save is not worth the field.
     */
    private transient float drainCarry;

    public OxygenState() {
        // A new player starts with a full set of lungs, not empty ones.
        this(AtmosphereTuning.LUNG_CAPACITY, 0);
    }

    public OxygenState(int lungUnits, int suffocationTicks) {
        this.lungUnits = lungUnits;
        this.suffocationTicks = suffocationTicks;
    }

    /** The player's own lung reserve, in units. Refills for free in breathable air. */
    public int lungUnits() {
        return lungUnits;
    }

    /**
     * Set the lung reserve.
     *
     * <p>Clamped only at zero. The upper bound is not a constant any more: gear grows lung
     * capacity, so only the tracker knows the current maximum, and clamping here against a
     * fixed value would silently cap an enchanted player at the unenchanted limit.
     */
    public void setLungUnits(int units) {
        this.lungUnits = Math.max(0, units);
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

    public float drainCarry() {
        return drainCarry;
    }

    public void setDrainCarry(float carry) {
        this.drainCarry = carry;
    }

    /** Forget sync bookkeeping so the next pass resends unconditionally. */
    public void invalidateSync() {
        this.lastSynced = null;
    }
}
