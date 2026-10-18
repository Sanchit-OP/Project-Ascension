package com.ascension.combat.client.hud;

import com.ascension.combat.client.ClientGrowth;
import com.ascension.combat.client.GrowthKeybind;
import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import com.ascension.core.api.client.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A small gold note above the health row while the player has unspent skill points: the count and
 * the key that opens the tree. Absent when there is nothing to spend, so it costs nothing to look
 * at (docs/technical/hud.md section 4).
 *
 * <p>Registered through {@code core}'s HUD contract. Reads {@link ClientGrowth}'s cached snapshot;
 * the text is rebuilt only when the count changes.
 */
@OnlyIn(Dist.CLIENT)
public final class SkillPointsElement implements HudElement {

    private static final int WIDTH = 92;
    private static final int HEIGHT = 12;
    private static final int GOLD = 0xFFD870;

    private int cachedPoints = Integer.MIN_VALUE;
    private Component text = Component.empty();
    private Component hint;
    private int hintTick = Integer.MIN_VALUE;

    @Override
    public HudAnchor anchor() {
        return HudAnchor.ABOVE_HOTBAR;
    }

    /** Above the health row, so it stacks after the vitals. */
    @Override
    public int order() {
        return 10;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public boolean visible() {
        return ClientGrowth.get().available() > 0 && Minecraft.getInstance().player != null;
    }

    @Override
    public void render(GuiGraphics g, HudTheme theme, float partialTick, float opacity) {
        int points = ClientGrowth.get().available();
        if (points <= 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        if (points != cachedPoints) {
            cachedPoints = points;
            text = theme.bold(points + (points == 1 ? " skill point" : " skill points"));
        }
        // The key may be rebound, so look again now and then; not every frame, which would allocate.
        int tick = Minecraft.getInstance().player.tickCount;
        if (hint == null || tick - hintTick >= 40 || tick < hintTick) {
            hintTick = tick;
            hint = theme.light("[" + GrowthKeybind.keyName().getString() + "]");
        }

        int alpha = Math.max(4, Math.min(255, Math.round(opacity * 255.0f)));
        theme.panel(g, 0, 0, WIDTH, HEIGHT, opacity);
        g.drawString(font, text, 5, (HEIGHT - font.lineHeight) / 2 + 1, (alpha << 24) | GOLD, true);
        g.drawString(font, hint, WIDTH - font.width(hint) - 5, (HEIGHT - font.lineHeight) / 2 + 1,
                (alpha << 24) | 0x9AA3B4, true);
    }
}
