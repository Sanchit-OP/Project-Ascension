package com.ascension.core.api.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The look of the HUD, handed to every element each frame by the host (ADR-0022): the panel behind
 * a group of things, the typeface, the icons. An element draws with these instead of its own, so
 * the whole HUD stays one family and a different host can restyle all of it.
 *
 * <p>Client only. Implementations must not allocate per call beyond what drawing needs.
 */
public interface HudTheme {

    /** The glass panel with its rim behind a group of things. {@code opacity} is 0 to 1. */
    void panel(GuiGraphics graphics, int x, int y, int w, int h, float opacity);

    /** A thin vertical divider line, {@code length} units tall. */
    void divider(GuiGraphics graphics, int x, int y, int length, float opacity);

    /**
     * An icon in a box of {@code size} units, tinted {@code rgb} (0xRRGGBB).
     */
    void icon(GuiGraphics graphics, HudIcon icon, int x, int y, int size, int rgb, float opacity);

    /**
     * A dark round badge of {@code size} units with a ring round its edge filled {@code fraction}
     * (0 to 1) clockwise from the top, in {@code rgb}. Inside is the caller's to draw.
     */
    void ringBadge(GuiGraphics graphics, int x, int y, int size, float fraction, int rgb, float opacity);

    /**
     * Only a ring, no disc: {@code fraction} of the way round clockwise from the top, in a box of
     * {@code size} units. For a second ring inside a {@link #ringBadge}, such as a tank inside lungs.
     */
    void ring(GuiGraphics graphics, int x, int y, int size, float fraction, int rgb, float opacity);

    /** A soft round glow centred at ({@code cx}, {@code cy}), {@code radius} units across, in {@code rgb}. */
    void glow(GuiGraphics graphics, int cx, int cy, int radius, int rgb, float strength);

    /**
     * Tints the edges of the whole screen, for a warning that must not be missed. Whatever the
     * element's own position, this covers the screen. {@code strength} is 0 to 1.
     */
    void edgeGlow(GuiGraphics graphics, int rgb, float strength);

    /** Text in the HUD's strong face, for numbers that matter. Cache the result; do not call per frame. */
    Component bold(String text);

    /** Text in the HUD's light face, for secondary text. Cache the result; do not call per frame. */
    Component light(String text);
}
