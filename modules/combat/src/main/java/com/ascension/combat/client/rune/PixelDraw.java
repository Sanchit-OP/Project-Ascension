package com.ascension.combat.client.rune;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Pixel-exact drawing for the pieces that move too freely to be sprites: the spokes of light from
 * a dial's hub to a chosen rune, which point anywhere.
 */
public final class PixelDraw {

    private PixelDraw() {
    }

    /** A one-pixel line between two points (Bresenham), in whatever space {@code graphics} is in. */
    public static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, argb);
            if (x == x1 && y == y1) {
                return;
            }

            int doubled = 2 * error;
            if (doubled >= dy) {
                error += dy;
                x += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y += sy;
            }
        }
    }
}
