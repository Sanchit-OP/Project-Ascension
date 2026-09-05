package com.ascension.atmosphere.client;

import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Oxygen bar and hazard indicator.
 *
 * <p>Hidden entirely in breathable air. A meter that reads "fine" at all times is noise, and
 * Earth is breathable for the whole first act of the campaign.
 *
 * <p>Shows <em>seconds</em>, not units. "40s" is something a player can act on; "1200 units" is
 * not. Units remain the storage format so arithmetic stays exact.
 *
 * <p>Draws from the cached {@link ClientOxygenState} only. No computation, no allocation beyond
 * the text component, nothing that touches world state (ADR-0007 rule 7).
 */
@OnlyIn(Dist.CLIENT)
public final class OxygenHudLayer implements LayeredDraw.Layer {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_Y_OFFSET = 56;

    private static final int COLOUR_FRAME = 0xFF000000;
    private static final int COLOUR_TRACK = 0xFF3A3A3A;
    private static final int COLOUR_OK = 0xFF43C5F0;
    private static final int COLOUR_LOW = 0xFFE0B33A;
    private static final int COLOUR_CRITICAL = 0xFFD84B3A;
    private static final int COLOUR_TEXT = 0xFFFFFFFF;

    /** Below this many seconds the bar turns amber. */
    private static final int SECONDS_LOW = 30;
    /** Below this many seconds the bar turns red. */
    private static final int SECONDS_CRITICAL = 10;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientOxygenState.shouldRender()) {
            return;
        }

        OxygenSyncPayload state = ClientOxygenState.current();
        int seconds = ClientOxygenState.secondsRemaining();

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - BAR_Y_OFFSET;

        float fill = state.capacity() <= 0
                ? 0.0f
                : Math.min(1.0f, (float) state.units() / state.capacity());
        int filled = Math.round(BAR_WIDTH * fill);

        int colour = seconds <= SECONDS_CRITICAL ? COLOUR_CRITICAL
                : seconds <= SECONDS_LOW ? COLOUR_LOW
                : COLOUR_OK;

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOUR_FRAME);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOUR_TRACK);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, colour);
        }

        Component label = state.suffocating()
                ? Component.literal("NO AIR")
                : Component.literal(formatSeconds(seconds));
        int labelWidth = minecraft.font.width(label);
        graphics.drawString(minecraft.font, label,
                (screenWidth - labelWidth) / 2, y - 10, COLOUR_TEXT, true);

        renderHazard(graphics, minecraft, state, x, y);
    }

    /**
     * Minimal hazard indicator: shown only when the surroundings drain faster than baseline.
     *
     * <p>This is what tells a player <em>why</em> their air is going quickly, which matters once
     * different worlds carry different drain multipliers. Deliberately a single glyph rather
     * than a readout panel.
     */
    private void renderHazard(GuiGraphics graphics, Minecraft minecraft,
                              OxygenSyncPayload state, int barX, int barY) {
        float baselineDrain = com.ascension.atmosphere.internal.AtmosphereTuning.BASE_UNITS_PER_SECOND;
        if (state.drainPerSecond() <= baselineDrain + 0.001f) {
            return;
        }
        Component marker = Component.literal(
                String.format("⚠ x%.1f", state.drainPerSecond() / baselineDrain));
        graphics.drawString(minecraft.font, marker,
                barX + BAR_WIDTH + 6, barY - 2, COLOUR_CRITICAL, true);
    }

    private static String formatSeconds(int seconds) {
        if (seconds == Integer.MAX_VALUE) {
            return "--";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}
