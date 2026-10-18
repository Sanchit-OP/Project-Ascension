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
}
