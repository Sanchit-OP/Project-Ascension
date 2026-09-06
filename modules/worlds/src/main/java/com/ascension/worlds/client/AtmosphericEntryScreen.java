package com.ascension.worlds.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;

/**
 * What a player sees instead of vanilla's blurred-menu "Downloading Terrain" screen while
 * crossing into or out of {@link com.ascension.worlds.internal.SpaceDimension} &mdash; a warm,
 * flickering re-entry glow standing in for the loading pause that a dimension change cannot
 * avoid (see {@code plans/m2-worlds.md}'s M2.6 lever #4: "Plasma and shaking is what the player
 * expects to see anyway; a vanilla loading screen is not").
 *
 * <p><strong>Still a real loading screen, not a fake one.</strong> Subclassing
 * {@link ReceivingLevelScreen} rather than building an unrelated {@code Screen} keeps its actual
 * job intact: {@code tick()} still closes this the moment the real {@code levelReceived} supplier
 * says the destination's chunks are in, exactly as vanilla's own screen does. Only what gets
 * drawn while that wait happens is different &mdash; nothing about *whether* or *when* it closes.
 *
 * <p>Registered for both directions of a space transition ({@link SpaceClientSetup}) by keying on
 * {@link com.ascension.worlds.internal.SpaceDimension}'s id alone, once as an incoming effect and
 * once as an outgoing one &mdash; so this covers ascent (arriving <em>at</em> space) and descent
 * (leaving space for any planet) without this class or its registration ever needing to know
 * which planet is on the other end. A planet 4&ndash;7 nobody has authored yet gets the same
 * effect automatically, no per-planet Java, per ADR-0004.
 */
public final class AtmosphericEntryScreen extends ReceivingLevelScreen {

    private static final Component ENTRY_TEXT = Component.translatable("multiplayer.downloadingTerrain");

    /**
     * Base colour of the glow band, before flicker. A hot, slightly orange white rather than a
     * pure fire-orange -- reads as atmospheric plasma rather than as fire or lava, which
     * {@code ascension_worlds} otherwise has no reason to associate with space travel.
     */
    private static final int GLOW_RED = 0xFF;
    private static final int GLOW_GREEN = 0xB0;
    private static final int GLOW_BLUE = 0x60;

    /** How much the glow's brightness swings up and down, as a fraction of full strength. */
    private static final float FLICKER_AMPLITUDE = 0.18F;

    /** How fast the flicker cycles, in radians per millisecond. Fast enough to read as unstable
     * plasma rather than a slow, deliberate pulse. */
    private static final double FLICKER_SPEED = 0.006;

    /** Own timestamp rather than the superclass's private one -- see class javadoc. */
    private final long shownAt = System.currentTimeMillis();

    public AtmosphericEntryScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - shownAt;
        float flicker = 1.0F + FLICKER_AMPLITUDE * (float) Math.sin(elapsed * FLICKER_SPEED);

        guiGraphics.fill(0, 0, width, height, 0xFF000000);

        // A horizontal glow band roughly a third of the way down the screen, like looking out at
        // a bright re-entry glow through a viewport, fading to black above and below it. Two
        // gradients rather than one solid band, so the transition in and out is soft instead of
        // a hard-edged stripe.
        int bandCenter = (int) (height * 0.4F);
        int bandHalfHeight = Math.max(1, height / 6);
        int glowColor = glowColor(flicker);

        guiGraphics.fillGradient(0, 0, width, bandCenter, 0x00000000, glowColor);
        guiGraphics.fillGradient(0, bandCenter, width, bandCenter + bandHalfHeight, glowColor, glowColor);
        guiGraphics.fillGradient(0, bandCenter + bandHalfHeight, width, height, glowColor, 0x00000000);
    }

    private static int glowColor(float flicker) {
        int red = clampChannel(GLOW_RED * flicker);
        int green = clampChannel(GLOW_GREEN * flicker);
        int blue = clampChannel(GLOW_BLUE * flicker);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int clampChannel(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately skips ReceivingLevelScreen.render()'s super call: it draws the vanilla
        // "Downloading Terrain" text at a fixed position over whatever renderBackground drew,
        // which read fine over a blurred menu but sits awkwardly over the glow band above. Screen
        // itself still renders normally (this class changes what backdrop appears behind the
        // label, not the label's own accessibility path via the narrator).
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, ENTRY_TEXT, this.width / 2, this.height * 3 / 4, 0xFFFFFF);
    }
}
