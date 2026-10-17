package com.ascension.combat.internal.rune.aetherwright;

import net.minecraft.world.phys.Vec3;

/**
 * Starfall's bookkeeping for one player: when it may be called again, and the star that has been
 * called and has not landed yet (docs/technical/rune-system.md section 4.3).
 *
 * <p>The star waits as a place and a game tick, never an entity: the player's next tick after that
 * tick is what lands it, so nothing is scheduled and nothing is held that could leak (ADR-0007).
 * Pure numbers: unit-tested ({@code StarfallStateTest}); not serialised.
 */
public final class StarfallState {

    private long cooldownUntil;
    private long strikeAt = -1L;
    private Vec3 where;

    /** Whether a star may be called now: the cooldown is over and none is already falling. */
    public boolean ready(long now) {
        return now >= cooldownUntil && strikeAt < 0L;
    }

    /** Calls a star down on {@code where}, landing {@code delay} ticks from now; the cooldown starts at once. */
    public void call(long now, Vec3 where, long delay, long cooldown) {
        this.where = where;
        this.strikeAt = now + delay;
        this.cooldownUntil = now + cooldown;
    }

    /** Takes {@code ticks} off the cooldown; a magic hit does this. */
    public void shorten(long ticks) {
        cooldownUntil -= ticks;
    }

    public boolean falling() {
        return strikeAt >= 0L;
    }

    /** Whether the star that is falling is due to land. */
    public boolean due(long now) {
        return strikeAt >= 0L && now >= strikeAt;
    }

    /** Where the star will land, or null if none is falling. */
    public Vec3 where() {
        return where;
    }

    /** Ticks until it lands, for drawing it fall; 0 if none is. */
    public long remaining(long now) {
        return strikeAt < 0L ? 0L : Math.max(0L, strikeAt - now);
    }

    /** Lands the star: forgets it, and says where it fell. */
    public Vec3 land() {
        Vec3 landed = where;
        where = null;
        strikeAt = -1L;
        return landed;
    }
}
