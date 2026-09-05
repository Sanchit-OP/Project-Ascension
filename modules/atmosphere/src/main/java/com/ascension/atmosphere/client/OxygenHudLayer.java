package com.ascension.atmosphere.client;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Oxygen bar and hazard indicator, drawn in the vanilla air-bubble slot.
 *
 * <p>Position is deliberately the same place vanilla shows air: the right-hand status column
 * above the hotbar. Registering above {@code VanillaGuiLayers.AIR_LEVEL} only controls draw
 * order, not layout, so the coordinates here do the actual work.
 *
 * <p>Hidden entirely in breathable air. A meter that always reads "fine" is noise, and Earth is
 * breathable for the whole first act of the campaign.
 *
 * <p>Shows <em>seconds</em>, not units. "40s" is something a player can act on; "1200 units" is
 * not. Units remain the storage format so arithmetic stays exact.
 *
 * <p>Draws from the cached {@link ClientOxygenState} only. No computation, no world access
 * (ADR-0007 rule 7).
 */
@OnlyIn(Dist.CLIENT)
public final class OxygenHudLayer implements LayeredDraw.Layer {

    /** Half the hotbar width; vanilla anchors both status columns to this. */
    private static final int HOTBAR_HALF_WIDTH = 91;

    /** Vanilla draws the air-bubble row this far above the bottom of the screen. */
    private static final int AIR_ROW_FROM_BOTTOM = 49;

    /** Lift clear of vanilla bubbles when the player is also short of breath underwater. */
    private static final int UNDERWATER_LIFT = 10;

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;

    private static final int COLOUR_FRAME = 0xFF000000;
    private static final int COLOUR_TRACK = 0xFF3A3A3A;
    private static final int COLOUR_OK = 0xFF43C5F0;
    private static final int COLOUR_LOW = 0xFFE0B33A;
    private static final int COLOUR_CRITICAL = 0xFFD84B3A;
    private static final int COLOUR_TEXT = 0xFFFFFFFF;

    private static final int SECONDS_LOW = 30;
    private static final int SECONDS_CRITICAL = 10;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientOxygenState.shouldRender()) {
            return;
        }

        OxygenSyncPayload state = ClientOxygenState.current();
        int seconds = ClientOxygenState.secondsRemaining();

        int right = graphics.guiWidth() / 2 + HOTBAR_HALF_WIDTH;
        int barX = right - BAR_WIDTH;
        int barY = graphics.guiHeight() - AIR_ROW_FROM_BOTTOM;

        // Vanilla only draws bubbles while the player is actually short of air, so shift up
        // only then rather than permanently leaving a gap.
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            barY -= UNDERWATER_LIFT;
        }

        float fill = state.capacity() <= 0
                ? 0.0f
                : Math.min(1.0f, (float) state.units() / state.capacity());
        int filled = Math.round(BAR_WIDTH * fill);

        int colour = seconds <= SECONDS_CRITICAL ? COLOUR_CRITICAL
                : seconds <= SECONDS_LOW ? COLOUR_LOW
                : COLOUR_OK;

        graphics.fill(barX - 1, barY - 1, right + 1, barY + BAR_HEIGHT + 1, COLOUR_FRAME);
        graphics.fill(barX, barY, right, barY + BAR_HEIGHT, COLOUR_TRACK);
        if (filled > 0) {
            graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, colour);
        }

        // Right-aligned above the bar, so the digits stay put as the text width changes.
        Component label = state.suffocating()
                ? Component.literal("NO AIR")
                : Component.literal(formatSeconds(seconds));
        graphics.drawString(minecraft.font, label,
                right - minecraft.font.width(label), barY - 10, COLOUR_TEXT, true);

        renderHazard(graphics, minecraft, state, barX, barY);
    }

    /**
     * Minimal hazard indicator, left of the bar. Shown only when the surroundings drain faster
     * than baseline &mdash; that is what tells a player <em>why</em> their air is going quickly
     * once different worlds carry different multipliers.
     */
    private void renderHazard(GuiGraphics graphics, Minecraft minecraft,
                              OxygenSyncPayload state, int barX, int barY) {
        float baseline = AtmosphereTuning.BASE_UNITS_PER_SECOND;
        if (state.drainPerSecond() <= baseline + 0.001f) {
            return;
        }
        Component marker = Component.literal(
                String.format("x%.1f", state.drainPerSecond() / baseline));
        graphics.drawString(minecraft.font, marker,
                barX - minecraft.font.width(marker) - 5, barY - 1, COLOUR_CRITICAL, true);
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
