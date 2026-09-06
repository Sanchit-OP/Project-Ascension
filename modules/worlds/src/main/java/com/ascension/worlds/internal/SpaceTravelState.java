package com.ascension.worlds.internal;

import net.minecraft.resources.ResourceLocation;

/**
 * Per-player runtime state for navigating {@link SpaceDimension}.
 *
 * <p>Exists purely so {@link SpaceMechanics} can tell "just entered a planet's approach shell"
 * from "still inside it" and fire the arrival message once per entry rather than every tick.
 * Not meaningful game state on its own, which is why it is not serialised (see
 * {@link SpaceAttachments#TRAVEL_STATE}).
 */
public final class SpaceTravelState {

    private ResourceLocation nearPlanet;

    /** {@code null} if the player is not currently inside any planet's approach shell. */
    public ResourceLocation nearPlanet() {
        return nearPlanet;
    }

    public void setNearPlanet(ResourceLocation nearPlanet) {
        this.nearPlanet = nearPlanet;
    }
}
