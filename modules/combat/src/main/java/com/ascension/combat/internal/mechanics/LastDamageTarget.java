package com.ascension.combat.internal.mechanics;

import java.util.UUID;

/**
 * "The last entity this player dealt damage to, and when" (docs/technical/combat-api.md section
 * 6, Galeforce's homing target selection), attached to the wielder. Stores only a {@link UUID}
 * and a game-time timestamp, never a held entity reference (ADR-0007 rule 1) &mdash; resolving it
 * against the player's current level is left to the caller, which also naturally handles the
 * remembered target having left that level in the meantime (a lookup on the wrong level just
 * finds nothing).
 */
public final class LastDamageTarget {

    private UUID targetId;
    private long recordedAtGameTime;

    public void record(UUID targetId, long now) {
        this.targetId = targetId;
        this.recordedAtGameTime = now;
    }

    public UUID targetId() {
        return targetId;
    }

    public boolean isValid(long now, long windowTicks) {
        return targetId != null && now - recordedAtGameTime <= windowTicks;
    }
}
