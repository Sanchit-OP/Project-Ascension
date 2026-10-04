package com.ascension.worlds.internal;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Proof that a player just opened the destination list on an active gateway portal &mdash; what
 * {@code /ascension worlds gateway travel} checks before teleporting anyone.
 *
 * <p>The picker is a clickable chat message whose lines run that command, so the command is a
 * public entry point any player can type. Without this, typing it from anywhere would skip the
 * whole spaceflight loop the gateway is meant to be a <em>reward</em> for. A session is opened
 * only by clicking an active portal, is tied to the dimension that portal is in, expires after
 * {@link #LIFETIME_TICKS}, and is spent by one trip.
 *
 * <p>Held as a non-serialized player attachment ({@link SpaceAttachments#GATEWAY_PICKER_SESSION}):
 * nothing here outlives a logout, and no static map holds a player (ADR-0007 rule 1).
 */
public final class GatewayPickerSession {

    /** One minute to pick a line from the list before it has to be reopened. */
    static final long LIFETIME_TICKS = 20L * 60;

    @Nullable
    private ResourceLocation openedIn;
    private long openedAt;

    void open(ResourceLocation portalDimension, long gameTime) {
        this.openedIn = portalDimension;
        this.openedAt = gameTime;
    }

    boolean isValidIn(ResourceLocation currentDimension, long gameTime) {
        return openedIn != null
                && openedIn.equals(currentDimension)
                && gameTime >= openedAt
                && gameTime - openedAt <= LIFETIME_TICKS;
    }

    void close() {
        this.openedIn = null;
    }
}
