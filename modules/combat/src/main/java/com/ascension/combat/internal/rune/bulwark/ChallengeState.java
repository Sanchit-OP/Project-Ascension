package com.ascension.combat.internal.rune.bulwark;

import java.util.UUID;

/**
 * On a mob a player has hit with Challenge (docs/technical/rune-system.md section 4.4): who
 * challenged it, and until when it keeps attacking them instead of anyone else. The challenger is
 * a {@link UUID}, found again when needed, never held (ADR-0007 rule 1). Not serialised.
 */
public final class ChallengeState {

    private UUID challenger;
    private long until;

    public void challenge(UUID challenger, long until) {
        this.challenger = challenger;
        this.until = until;
    }

    public boolean active(long now) {
        return challenger != null && now < until;
    }

    /** Who challenged it; only meaningful while {@link #active}. */
    public UUID challenger() {
        return challenger;
    }
}
