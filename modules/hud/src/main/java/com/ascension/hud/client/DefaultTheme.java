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

    private DefaultTheme() {
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
