package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereTuning;

/**
 * What a room costs to hold, as plain arithmetic (no world, so it is unit tested).
 *
 * <p>The one rule: 1 mB of air per second per 16 blocks of room, never less than 1 mB a second.
 */
public final class AirDemand {

    private AirDemand() {
    }

    /** mB per second needed to hold a room of this many blocks. */
    public static int mbPerSecond(int roomBlocks) {
        if (roomBlocks <= 0) {
            return 0;
        }
        return Math.max(1, (roomBlocks + AtmosphereTuning.ROOM_BLOCKS_PER_MB_SECOND - 1)
                / AtmosphereTuning.ROOM_BLOCKS_PER_MB_SECOND);
    }
}
