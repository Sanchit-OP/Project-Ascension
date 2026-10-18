package com.ascension.hud.client;

import com.ascension.hud.AscensionHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Draws the HUD's smooth sprites (made by {@code tools/generate-hud-art.py}).
 *
 * <p>They are plain textures with the {@code blur} flag, not atlas sprites, so the game filters
 * them smoothly at any GUI scale instead of showing hard texels. Every draw tints with
 * {@link GuiGraphics#setColor} and restores white afterwards, and goes through blending: vanilla
 * fills and text switch it off, so a translucent sprite must turn it on first.
 */
@OnlyIn(Dist.CLIENT)
final class HudSprites {

    private static final ResourceLocation BAR = texture("bar.png");
    private static final ResourceLocation BADGE = texture("badge.png");
    private static final ResourceLocation RING = texture("ring.png");
    private static final ResourceLocation FOOD = texture("food.png");
    private static final ResourceLocation PANEL = texture("panel.png");
    private static final ResourceLocation GLOW = texture("glow.png");
    private static final ResourceLocation EDGE = texture("edge.png");
    private static final ResourceLocation ICONS = texture("icons.png");

    /** Bar sheet: three rows of 32 texels, each [left cap 16 | middle 8 | right cap 16]. */
    private static final int BAR_SHEET_W = 40;
    private static final int BAR_SHEET_H = 96;
    private static final int BAR_ROW = 32;
    private static final int BAR_CAP = 16;
    private static final int BAR_MID = 8;

    private static final int BAR_TRACK = 0;
    private static final int BAR_FILL = 1;
    private static final int BAR_FRAME = 2;

    /** Ring sheet: 8 by 8 frames of 72 texels, filling clockwise from the top. */
    private static final int BADGE_TEXELS = 72;
    private static final int RING_FRAMES = 64;
    private static final int RING_GRID = 8;

    /** Cap width in GUI units: 16 texels at 4 texels per unit. */
    static final int CAP_UNITS = 4;

    private HudSprites() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "textures/gui/hud/" + name);
    }

    // --- the bar -------------------------------------------------------------

    /** The dark track, full width. */
    static void barTrack(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        barRow(g, BAR_TRACK, x, y, w, h, x, x + w, 1.0f, 1.0f, 1.0f, opacity);
    }

    /** The gold rim, drawn last over everything. */
    static void barFrame(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        barRow(g, BAR_FRAME, x, y, w, h, x, x + w, 1.0f, 1.0f, 1.0f, opacity);
    }

    /**
     * A coloured part of the bar between two fractions of its width. The fill is drawn full width
     * but cropped, so the left end stays rounded and the cut edge is straight.
     *
     * <p>Cropped by drawing less of the sprite, not with a scissor: in 1.21.1 the scissor ignores
     * the pose, and every element is drawn translated into place, so a scissor lands in the wrong
     * part of the screen and hides the fill entirely.
     */
    static void barFill(GuiGraphics g, int x, int y, int w, int h, float from, float to,
                        int rgb, float opacity) {
        int x0 = x + Math.round(w * from);
        int x1 = x + Math.round(w * to);
        if (x1 <= x0) {
            return;
        }
        barRow(g, BAR_FILL, x, y, w, h, x0, x1,
                ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, opacity);
    }

    /**
     * Draws one row of the bar sheet across {@code [x, x + w)}, showing only {@code [clip0, clip1)}.
     *
     * <p>The two caps are 4 texels to the unit, so cropping them is exact. The middle is the same
     * from left to right, so it is simply stretched over whatever is visible of it.
     */
    private static void barRow(GuiGraphics g, int row, int x, int y, int w, int h, int clip0, int clip1,
                               float r, float gr, float b, float a) {
        g.setColor(r, gr, b, a);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        int v = row * BAR_ROW;

        // Left cap.
        int lx0 = Math.max(x, clip0);
        int lx1 = Math.min(x + CAP_UNITS, clip1);
        if (lx1 > lx0) {
            g.blit(BAR, lx0, y, lx1 - lx0, h, (lx0 - x) * 4, v, (lx1 - lx0) * 4, BAR_ROW,
                    BAR_SHEET_W, BAR_SHEET_H);
        }
        // Middle.
        int mx0 = Math.max(x + CAP_UNITS, clip0);
        int mx1 = Math.min(x + w - CAP_UNITS, clip1);
        if (mx1 > mx0) {
            g.blit(BAR, mx0, y, mx1 - mx0, h, BAR_CAP, v, BAR_MID, BAR_ROW, BAR_SHEET_W, BAR_SHEET_H);
        }
        // Right cap.
        int rx = x + w - CAP_UNITS;
        int rx0 = Math.max(rx, clip0);
        int rx1 = Math.min(x + w, clip1);
        if (rx1 > rx0) {
            g.blit(BAR, rx0, y, rx1 - rx0, h, BAR_CAP + BAR_MID + (rx0 - rx) * 4, v, (rx1 - rx0) * 4, BAR_ROW,
                    BAR_SHEET_W, BAR_SHEET_H);
        }
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // --- badges ---------------------------------------------------------------

    /** A dark disc with a progress ring in {@code rgb}: {@code fraction} of the way round, clockwise from the top. */
    static void ringBadge(GuiGraphics g, int x, int y, int size, float fraction, int rgb, float opacity) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1.0f, 1.0f, 1.0f, opacity);
        g.blit(BADGE, x, y, size, size, 0, 0, BADGE_TEXELS, BADGE_TEXELS, BADGE_TEXELS, BADGE_TEXELS);
        // The faint full ring behind the progress.
        ringFrame(g, x, y, size, RING_FRAMES - 1, 1.0f, 1.0f, 1.0f, 0.11f * opacity);
        int frame = Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * (RING_FRAMES - 1));
        if (frame > 0) {
            ringFrame(g, x, y, size, frame, ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f, opacity);
        }
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void ringFrame(GuiGraphics g, int x, int y, int size, int frame,
                                  float r, float gr, float b, float a) {
        g.setColor(r, gr, b, a);
        int sheet = BADGE_TEXELS * RING_GRID;
        g.blit(RING, x, y, size, size, (frame % RING_GRID) * BADGE_TEXELS, (frame / RING_GRID) * BADGE_TEXELS,
                BADGE_TEXELS, BADGE_TEXELS, sheet, sheet);
    }

    /** The drumstick, centred in a box of {@code size} units. */
    static void food(GuiGraphics g, int x, int y, int size, float opacity) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1.0f, 1.0f, 1.0f, opacity);
        g.blit(FOOD, x, y, size, size, 0, 0, 48, 48, 48, 48);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // --- panel and icons -------------------------------------------------------

    /** Panel sheet: 40 by 40 texels, corners 16 and a middle of 8, so a nine-slice at 4 texels to the unit. */
    private static final int PANEL_SHEET = 40;
    private static final int PANEL_CORNER = 16;
    private static final int PANEL_MID = 8;

    /** Icon sheet: six icons of 48 texels in a row. */
    private static final int ICON_TEXELS = 48;
    private static final int ICON_COUNT = 6;

    /**
     * The glass panel, stretched to any size from a nine-slice. The corners are cropped when the
     * panel is smaller than two corners, so a tiny panel is still a rounded shape, never a hole.
     */
    static void panel(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        if (w <= 0 || h <= 0) {
            return;
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1.0f, 1.0f, 1.0f, opacity);
        int c = Math.min(CAP_UNITS, Math.min(w, h) / 2);
        int tc = c * 4;
        int[] xs = {x, x + c, x + w - c, x + w};
        int[] ys = {y, y + c, y + h - c, y + h};
        // The corners use only as much of the sprite as fits (tc texels), the middle all of its 8.
        int[] srcW = {tc, PANEL_MID, tc};
        int[] srcU = {0, PANEL_CORNER, PANEL_SHEET - tc};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int dx = xs[col];
                int dw = xs[col + 1] - xs[col];
                int dy = ys[row];
                int dh = ys[row + 1] - ys[row];
                if (dw <= 0 || dh <= 0) {
                    continue;
                }
                g.blit(PANEL, dx, dy, dw, dh, srcU[col], srcU[row], srcW[col], srcW[row], PANEL_SHEET, PANEL_SHEET);
            }
        }
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** One icon from the sheet, tinted {@code rgb}. {@code index} is the icon's ordinal. */
    static void icon(GuiGraphics g, int index, int x, int y, int size, int rgb, float opacity) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, opacity);
        g.blit(ICONS, x, y, size, size, index * ICON_TEXELS, 0, ICON_TEXELS, ICON_TEXELS,
                ICON_TEXELS * ICON_COUNT, ICON_TEXELS);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** Only the ring, with its faint full track behind it, at {@code size} units. */
    static void ring(GuiGraphics g, int x, int y, int size, float fraction, int rgb, float opacity) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        ringFrame(g, x, y, size, RING_FRAMES - 1, 1.0f, 1.0f, 1.0f, 0.11f * opacity);
        int frame = Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * (RING_FRAMES - 1));
        if (frame > 0) {
            ringFrame(g, x, y, size, frame, ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f, opacity);
        }
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** A soft round glow in {@code rgb}, {@code size} units across, centred on ({@code cx}, {@code cy}). */
    static void glow(GuiGraphics g, int cx, int cy, int size, int rgb, float strength) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f,
                Math.max(0.0f, Math.min(1.0f, strength)));
        g.blit(GLOW, cx - size / 2, cy - size / 2, size, size, 0, 0, 128, 128, 128, 128);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** The screen-edge tint, stretched over a screen of {@code w} by {@code h} units whose top-left is at ({@code x}, {@code y}). */
    static void edge(GuiGraphics g, int x, int y, int w, int h, int rgb, float strength) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f,
                Math.max(0.0f, Math.min(1.0f, strength)));
        g.blit(EDGE, x, y, w, h, 0, 0, 256, 256, 256, 256);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
