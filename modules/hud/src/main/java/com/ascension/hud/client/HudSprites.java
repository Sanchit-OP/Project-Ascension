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
        barRow(g, BAR_TRACK, x, y, w, h, 1.0f, 1.0f, 1.0f, opacity);
    }

    /** The gold rim, drawn last over everything. */
    static void barFrame(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        barRow(g, BAR_FRAME, x, y, w, h, 1.0f, 1.0f, 1.0f, opacity);
    }

    /**
     * A coloured part of the bar between two fractions of its width. The fill is drawn full width
     * and clipped, so the left end stays rounded and the cut edge is straight.
     */
    static void barFill(GuiGraphics g, int x, int y, int w, int h, float from, float to,
                        int rgb, float opacity) {
        int x0 = x + Math.round(w * from);
        int x1 = x + Math.round(w * to);
        if (x1 <= x0) {
            return;
        }
        g.enableScissor(x0, y, x1, y + h);
        barRow(g, BAR_FILL, x, y, w, h,
                ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, opacity);
        g.disableScissor();
    }

    private static void barRow(GuiGraphics g, int row, int x, int y, int w, int h,
                               float r, float gr, float b, float a) {
        g.setColor(r, gr, b, a);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        int v = row * BAR_ROW;
        g.blit(BAR, x, y, CAP_UNITS, h, 0, v, BAR_CAP, BAR_ROW, BAR_SHEET_W, BAR_SHEET_H);
        int mid = w - 2 * CAP_UNITS;
        if (mid > 0) {
            g.blit(BAR, x + CAP_UNITS, y, mid, h, BAR_CAP, v, BAR_MID, BAR_ROW, BAR_SHEET_W, BAR_SHEET_H);
        }
        g.blit(BAR, x + w - CAP_UNITS, y, CAP_UNITS, h, BAR_CAP + BAR_MID, v, BAR_CAP, BAR_ROW,
                BAR_SHEET_W, BAR_SHEET_H);
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
