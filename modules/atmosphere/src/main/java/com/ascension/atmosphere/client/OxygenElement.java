package com.ascension.atmosphere.client;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.OxygenEmblem;
import com.ascension.atmosphere.internal.OxygenEmblem.Mode;
import com.ascension.atmosphere.internal.OxygenLevel;
import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
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
 * Oxygen on the HUD: no bar. A large O2 emblem that rises into view as air runs low, with a ring
 * that drains, going red and pulsing when it is nearly gone, and the screen edge reddening with it.
 * In fine air it is a small quiet badge, and in breathable air with full lungs it is not there.
 *
 * <p>Registered through {@code core}'s HUD contract (ADR-0022), so atmosphere never depends on the
 * HUD module; without a host, {@link OxygenHudLayer} draws its plain bar instead. Reads only
 * {@link ClientOxygenState}, the cached mirror of what the server said (ADR-0007): no world access,
 * and the text is rebuilt only when a value changes.
 */
@OnlyIn(Dist.CLIENT)
public final class OxygenElement implements HudElement {

    private static final int WIDTH = 48;
    private static final int HEIGHT = 56;
    private static final int BIG = 36;
    private static final int SMALL = 16;

    private static final int BLUE = 0x46C8FF;
    private static final int AMBER = 0xFFB23A;
    private static final int RED = 0xFF5A64;
    /** The tank ring: amber, apart from the lungs' blue, so the two read as two things. */
    private static final int AMBER_TANK = 0xFFB23A;

    private Component secondsText = Component.empty();
    private int cachedSeconds = Integer.MIN_VALUE;
    private Component noAirText;
    private Component hazardText = Component.empty();
    private int cachedHazard = Integer.MIN_VALUE;
    private Component pressurisingText = Component.empty();
    private int cachedPressurising = Integer.MIN_VALUE;
    private Component o2;
    private Component two;

    /** Drawn first, so the screen-edge tint lies under the rest of the HUD. */
    @Override
    public int order() {
        return -100;
    }

    @Override
    public HudAnchor anchor() {
        return HudAnchor.CENTER_ABOVE_HOTBAR;
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
        return ClientOxygenState.shouldRender();
    }

    @Override
    public void render(GuiGraphics g, HudTheme theme, float partialTick, float opacity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Font font = mc.font;
        OxygenSyncPayload state = ClientOxygenState.current();
        int seconds = ClientOxygenState.secondsRemaining();
        Mode mode = OxygenEmblem.modeFor(true, state.breathable(), state.suffocating(),
                state.pressurisingSeconds() > 0, state.units(), state.capacity(), seconds);
        if (mode == Mode.HIDDEN) {
            return;
        }
        float share = OxygenEmblem.share(state.units(), state.capacity());

        if (o2 == null) {
            o2 = theme.bold("O");
            two = theme.bold("2");
            noAirText = theme.bold("NO AIR");
        }

        // A slow pulse from the game clock; the player is read for its time and not kept.
        float time = mc.player.tickCount + partialTick;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.35f);

        if (mode == Mode.QUIET) {
            int cx = WIDTH / 2;
            int cy = HEIGHT - SMALL / 2 - 2;
            theme.ringBadge(g, cx - SMALL / 2, cy - SMALL / 2, SMALL, share, BLUE, opacity);
            drawO2(g, font, cx, cy, 0.55f, BLUE, opacity);
            return;
        }

        int colour = mode == Mode.CRITICAL ? RED
                : OxygenLevel.forSecondsRemaining(seconds) == OxygenLevel.LOW ? AMBER : BLUE;
        int cx = WIDTH / 2;
        int cy = BIG / 2 + 2;

        if (mode == Mode.CRITICAL) {
            theme.edgeGlow(g, RED, (0.45f + 0.4f * pulse) * opacity);
        }
        theme.glow(g, cx, cy, BIG + 24, colour, (mode == Mode.CRITICAL ? 0.35f + 0.3f * pulse : 0.2f) * opacity);
        // Outer ring: the lungs. Inner ring, when a tank is carried: the tank.
        int tankCapacity = state.capacity() - state.lungCapacity();
        float lungShare = OxygenEmblem.share(state.lungUnits(), state.lungCapacity());
        theme.ringBadge(g, cx - BIG / 2, cy - BIG / 2, BIG, tankCapacity > 0 ? lungShare : share, colour, opacity);
        if (tankCapacity > 0) {
            int inner = BIG - 12;
            theme.ring(g, cx - inner / 2, cy - inner / 2, inner,
                    OxygenEmblem.share(state.units() - state.lungUnits(), tankCapacity), AMBER_TANK, opacity);
        }
        drawO2(g, font, cx, cy, 1.4f, colour, opacity);

        // The line under the emblem: what the player needs to know right now.
        Component line;
        if (state.pressurisingSeconds() > 0) {
            line = pressurising(theme, state.pressurisingSeconds());
        } else if (state.suffocating()) {
            line = noAirText;
        } else {
            line = seconds(theme, seconds);
        }
        int lineColour = (alpha(opacity) << 24) | colour;
        g.drawString(font, line, cx - font.width(line) / 2, BIG + 5, lineColour, true);

        // Where the air is draining faster than normal, say by how much.
        float baseline = AtmosphereTuning.BASE_UNITS_PER_SECOND;
        if (state.drainPerSecond() > baseline + 0.001f) {
            Component hazard = hazard(theme, Math.round(state.drainPerSecond() / baseline * 10.0f));
            g.drawString(font, hazard, cx + BIG / 2 + 3, cy - font.lineHeight / 2,
                    (alpha(opacity) << 24) | RED, true);
        }
    }

    /** "O" with a small "2" beside it, drawn in the HUD's face at {@code scale}. */
    private void drawO2(GuiGraphics g, Font font, int cx, int cy, float scale, int rgb, float opacity) {
        int colour = (alpha(opacity) << 24) | rgb;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(scale, scale, 1.0f);
        int oWidth = font.width(o2);
        int totalWidth = oWidth + font.width(two) / 2 + 1;
        g.drawString(font, o2, -totalWidth / 2, -font.lineHeight / 2 + 1, colour, false);
        pose.pushPose();
        pose.translate(-totalWidth / 2 + oWidth, font.lineHeight / 2 - 3, 0);
        pose.scale(0.7f, 0.7f, 1.0f);
        g.drawString(font, two, 0, 0, colour, false);
        pose.popPose();
        pose.popPose();
    }

    private Component seconds(HudTheme theme, int seconds) {
        if (seconds != cachedSeconds) {
            cachedSeconds = seconds;
            secondsText = theme.bold(AtmosphereTuning.formatDuration(seconds));
        }
        return secondsText;
    }

    private Component pressurising(HudTheme theme, int seconds) {
        if (seconds != cachedPressurising) {
            cachedPressurising = seconds;
            pressurisingText = theme.bold("PRESSURISING " + seconds + "s");
        }
        return pressurisingText;
    }

    private Component hazard(HudTheme theme, int tenths) {
        if (tenths != cachedHazard) {
            cachedHazard = tenths;
            hazardText = theme.bold("x" + (tenths / 10) + "." + (tenths % 10));
        }
        return hazardText;
    }

    private static int alpha(float opacity) {
        return Math.max(4, Math.min(255, Math.round(opacity * 255.0f)));
    }
}
