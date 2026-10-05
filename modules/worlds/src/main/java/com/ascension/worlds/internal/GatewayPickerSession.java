package com.ascension.worlds.internal;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A player's gateway state that has to be remembered between ticks: proof they just opened the
 * destination screen at a lit portal, and whether they were already standing in a field.
 *
 * <p>The proof is what {@code GatewayDestinations.tryTravel} demands before moving anyone. Travel
 * requests arrive from the client (and from a typeable command), so without it, asking from
 * anywhere would skip the whole spaceflight loop the gateway is meant to be a <em>reward</em> for.
 * A session is opened only by a lit portal, is tied to the dimension that portal is in, expires
 * after {@link #LIFETIME_TICKS}, and is spent by one trip.
 *
 * <p>Held as a non-serialized player attachment ({@link SpaceAttachments#GATEWAY_PICKER_SESSION}):
 * nothing here outlives a logout, and no static map holds a player (ADR-0007 rule 1).
 */
public final class GatewayPickerSession {

    /** One minute to pick a destination before the screen has to be reopened. */
    static final long LIFETIME_TICKS = 20L * 60;

    @Nullable
    private ResourceLocation openedIn;
    private long openedAt;

    /** Game time of the last tick spent in a field; far in the past until the first. */
    private long lastTickInField = -10;

    /**
     * Called every tick the player stands in a gateway field (possibly several times a tick, once
     * per field block they overlap). True only on the first tick of a new entry, so the screen
     * opens once when they step in, not twenty times a second while they stand there.
     */
    boolean stepIntoField(long gameTime) {
        // The second test: game time can jump backwards (/time set, a world swap).
        boolean entering = gameTime - lastTickInField > 1 || gameTime < lastTickInField;
        lastTickInField = gameTime;
        return entering;
    }

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
