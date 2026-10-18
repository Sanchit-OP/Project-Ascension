package com.ascension.core.api.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One thing on the player's screen: a gauge, a stat block, a badge.
 *
 * <p>The contract of ADR-0022. A module that owns some state (oxygen, runes, stats) registers an
 * element through {@link HudRegistry}; whichever HUD host is installed decides where it goes, how
 * big the frame is and whether it is shown at all. The element only knows how to draw itself.
 *
 * <p><strong>Client only.</strong> Register from client code, and never from a class a dedicated
 * server loads.
 *
 * <p><strong>Runs every frame (ADR-0007).</strong> Implementations must read cached client state
 * only: no world access, no scans, no allocation in {@link #render}, and no long-lived reference
 * to a {@code Level}, {@code Player} or {@code Entity}.
 */
public interface HudElement {

    /** Where the element wants to sit. Read each frame; may change, e.g. with a config. */
    HudAnchor anchor();

    /** Width in GUI units. Called each frame, so it must be a plain field read. */
    int width();

    /** Height in GUI units. Called each frame, so it must be a plain field read. */
    int height();

    /**
     * Whether to draw this frame. An element that has nothing to say returns false and takes no
     * room, which is how oxygen disappears in breathable air.
     */
    boolean visible();

    /**
     * Draw in local coordinates: {@code (0, 0)} is the element's top-left and it occupies
     * {@link #width()} by {@link #height()}. The host has already moved the pose there.
     *
     * @param theme   the host's panel, typeface and icons; draw with these so the HUD stays one family
     * @param opacity 0 to 1, the host's overall opacity setting; multiply it into alpha
     */
    void render(GuiGraphics graphics, HudTheme theme, float partialTick, float opacity);

    /** Sorts elements sharing an anchor; lower is nearer the hotbar. Ties break on id. */
    default int order() {
        return 0;
    }
}
