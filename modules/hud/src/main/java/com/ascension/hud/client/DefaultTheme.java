package com.ascension.hud.client;

import com.ascension.core.api.client.HudIcon;
import com.ascension.core.api.client.HudTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** The HUD's own look: glass panels with a gold rim, Barlow, our icons. Stateless. */
@OnlyIn(Dist.CLIENT)
final class DefaultTheme implements HudTheme {

    static final DefaultTheme INSTANCE = new DefaultTheme();

    /** Where the element being drawn has been moved to, so a full-screen effect can undo it. */
    private int originX;
    private int originY;

    private DefaultTheme() {
    }

    /** Set by the layer before each element draws. */
    void origin(int x, int y) {
        originX = x;
        originY = y;
    }

    @Override
    public void ringBadge(GuiGraphics graphics, int x, int y, int size, float fraction, int rgb, float opacity) {
        HudSprites.ringBadge(graphics, x, y, size, fraction, rgb, opacity);
    }

    @Override
    public void glow(GuiGraphics graphics, int cx, int cy, int radius, int rgb, float strength) {
        HudSprites.glow(graphics, cx, cy, radius, rgb, strength);
    }

    @Override
    public void edgeGlow(GuiGraphics graphics, int rgb, float strength) {
        HudSprites.edge(graphics, -originX, -originY, graphics.guiWidth(), graphics.guiHeight(), rgb, strength);
    }

    @Override
    public void panel(GuiGraphics graphics, int x, int y, int w, int h, float opacity) {
        HudSprites.panel(graphics, x, y, w, h, opacity);
    }

    @Override
    public void divider(GuiGraphics graphics, int x, int y, int length, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(70 * opacity)));
        graphics.fill(x, y, x + 1, y + length, (alpha << 24) | 0xC9A45A);
    }

    @Override
    public void icon(GuiGraphics graphics, HudIcon icon, int x, int y, int size, int rgb, float opacity) {
        HudSprites.icon(graphics, icon.ordinal(), x, y, size, rgb, opacity);
    }

    @Override
    public Component bold(String text) {
        return HudFonts.bold(text);
    }

    @Override
    public Component light(String text) {
        return HudFonts.light(text);
    }
}
