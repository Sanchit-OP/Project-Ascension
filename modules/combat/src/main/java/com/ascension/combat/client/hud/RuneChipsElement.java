package com.ascension.combat.client.hud;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.client.rune.RuneKit;
import com.ascension.combat.internal.rune.hud.RuneHudEntry;
import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import com.ascension.core.api.client.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * What the equipped runes are doing, as round chips right of the hotbar: the rune's own glyph in a
 * disc whose ring is its state (docs/technical/hud.md). Lit when the rune is doing something, dim
 * when idle; stacks and charges carry a number, a cooldown refills its ring until the rune is ready,
 * a window drains its ring until it closes. A full stack or charge glows.
 *
 * <p>Registered through {@code core}'s HUD contract, so combat never depends on the HUD module.
 * Draws from {@link ClientRuneHud}, the cached copy of what the server said when it changed (never
 * a packet per tick); a cooldown's countdown is animated here from the tick it ends at.
 *
 * <p>Runs every frame (ADR-0007): the number text is rebuilt only when a value changes, and no
 * player is stored.
 */
@OnlyIn(Dist.CLIENT)
public final class RuneChipsElement implements HudElement {

    private static final int CHIP = 20;
    private static final int GAP = 4;
    private static final int GLYPH = 12;
    /** Space above the chips for a glow to breathe. */
    private static final int MAX_CHIPS = 12;

    private final Component[] numberText = new Component[MAX_CHIPS];
    private final int[] numberCached = new int[MAX_CHIPS];

    public RuneChipsElement() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < MAX_CHIPS; i++) {
            numberText[i] = null;
            numberCached[i] = Integer.MIN_VALUE;
        }
    }

    @Override
    public HudAnchor anchor() {
        return HudAnchor.HOTBAR_RIGHT;
    }

    @Override
    public int width() {
        int n = Math.min(MAX_CHIPS, ClientRuneHud.size());
        return n == 0 ? 0 : n * CHIP + (n - 1) * GAP;
    }

    @Override
    public int height() {
        return CHIP;
    }

    @Override
    public boolean visible() {
        return ClientRuneHud.size() > 0 && Minecraft.getInstance().player != null;
    }

    @Override
    public void render(GuiGraphics g, HudTheme theme, float partialTick, float opacity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Font font = mc.font;
        long now = player.level().getGameTime();
        int alpha = Math.max(4, Math.min(255, Math.round(opacity * 255.0f)));
        int n = Math.min(MAX_CHIPS, ClientRuneHud.size());

        for (int i = 0; i < n; i++) {
            RuneHudEntry entry = ClientRuneHud.entry(i);
            long total = ClientRuneHud.total(i);
            RunePath path = RunePath.values()[Math.max(0, Math.min(RunePath.values().length - 1, entry.path()))];

            boolean live = RuneChipMath.live(entry.kind(), entry.value(), entry.endTick(), now);
            boolean full = RuneChipMath.full(entry.kind(), entry.value(), entry.max());
            float ring = RuneChipMath.fraction(entry.kind(), entry.value(), entry.max(), entry.endTick(), now, total);
            int number = RuneChipMath.number(entry.kind(), entry.value(), entry.endTick(), now);

            int x = i * (CHIP + GAP);
            int cx = x + CHIP / 2;
            int cy = CHIP / 2;
            int colour = RuneKit.ramp(path, live ? 3 : 2) & 0xFFFFFF;

            if (full) {
                // A slow breathing glow at a full stack.
                float pulse = 0.5f + 0.5f * (float) Math.sin((player.tickCount + partialTick) * 0.25f);
                theme.glow(g, cx, cy, CHIP + 14, colour, (0.25f + 0.25f * pulse) * opacity);
            } else if (live) {
                theme.glow(g, cx, cy, CHIP + 10, colour, 0.2f * opacity);
            }

            theme.ringBadge(g, x, 0, CHIP, ring, colour, live ? opacity : opacity * 0.7f);

            // The rune's own glyph, dimmed when idle.
            int glyph = ClientRuneHud.glyph(i);
            if (glyph >= 0) {
                RuneGlyphs.draw(g, glyph, cx - GLYPH / 2, cy - GLYPH / 2, GLYPH, colour, (live ? 1.0f : 0.5f) * opacity);
            } else {
                // A rune without a smooth glyph yet keeps the orrery's own.
                g.setColor(1.0f, 1.0f, 1.0f, (live ? 1.0f : 0.45f) * opacity);
                RuneKit.blit(g, ClientRuneHud.sprite(i), cx - GLYPH / 2, cy - GLYPH / 2, GLYPH, GLYPH);
                g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            if (number >= 0) {
                Component text = numberText(theme, i, number);
                int tx = x + CHIP - font.width(text) - 1;
                g.drawString(font, text, tx, CHIP - font.lineHeight + 1, (alpha << 24) | 0xFFFFFF, true);
            }
        }
    }

    private Component numberText(HudTheme theme, int index, int number) {
        if (numberCached[index] != number || numberText[index] == null) {
            numberCached[index] = number;
            numberText[index] = theme.bold(Integer.toString(number));
        }
        return numberText[index];
    }
}
