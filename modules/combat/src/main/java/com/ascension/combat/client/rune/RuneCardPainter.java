package com.ascension.combat.client.rune;

import com.ascension.combat.api.rune.RunePath;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Paints a hover card (docs/technical/rune-system.md section 10): a nine-sliced frame in a path's
 * colour with a bold title, a tag in the path's light colour, and blocks of text, the first the
 * plain sentence and the last, when there are three or more, the dimmer fine print. It knows
 * nothing about runes: the screen decides what a card says, and this puts it on the screen beside
 * the thing hovered, kept inside the window. Growth's tree draws its cards the same way.
 */
public final class RuneCardPainter {

    /** The card's width in GUI pixels; text wraps 14 pixels inside it. */
    public static final int WIDTH = 168;
    public static final int TEXT_WIDTH = WIDTH - 14;

    private RuneCardPainter() {
    }

    /** The height a card with these blocks needs. */
    public static int height(List<List<FormattedCharSequence>> blocks) {
        int h = 6 + 10 + 12;
        for (List<FormattedCharSequence> block : blocks) {
            h += block.size() * 10 + 4;
        }
        return h;
    }

    /**
     * Draws the card beside {@code (anchorX, anchorY)} (the middle of what was hovered, in screen
     * pixels), to its right unless that would leave the window, then to its left.
     *
     * @param scale how big the panel the anchor sits on is drawn, so the card keeps its distance
     */
    public static void paint(GuiGraphics g, Font font, int screenWidth, int screenHeight, double anchorX, double anchorY,
            double scale, RunePath path, Component title, Component tag, List<List<FormattedCharSequence>> blocks) {
        int h = height(blocks);
        int x = (int) (anchorX + 18 * scale);
        if (x + WIDTH + 4 > screenWidth) {
            x = (int) (anchorX - 18 * scale) - WIDTH;
        }
        x = Math.max(4, x);
        int y = Math.max(4, Math.min(screenHeight - h - 4, (int) (anchorY - 6 * scale)));

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x + 2, y + 2, x + WIDTH + 2, y + h + 2, 0x6E000000);
        RuneKit.blit(g, RuneKit.card(path), x, y, WIDTH, h);
        int yy = y + 6;
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), x + 7, yy, RuneKit.TEXT, true);
        yy += 10;
        g.drawString(font, tag, x + 7, yy, RuneKit.ramp(path, 3), true);
        yy += 12;
        for (int i = 0; i < blocks.size(); i++) {
            boolean finePrint = i == blocks.size() - 1 && blocks.size() > 2;
            for (FormattedCharSequence line : blocks.get(i)) {
                g.drawString(font, line, x + 7, yy, finePrint ? RuneKit.TEXT_DIM : RuneKit.TEXT_BODY, true);
                yy += 10;
            }
            yy += 4;
        }
        g.pose().popPose();
    }
}
