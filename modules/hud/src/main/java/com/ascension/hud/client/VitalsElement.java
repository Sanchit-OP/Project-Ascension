package com.ascension.hud.client;

import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import com.ascension.core.api.client.HudTheme;
import com.ascension.hud.internal.HealthBarModel;
import com.ascension.hud.internal.RegenForecast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The row above the hotbar: level badge, health bar, hunger badge.
 *
 * <p>Replaces vanilla's hearts, food and experience bar (the layers are cancelled in
 * {@link HudClient}). Ships in {@code ascension-hud} because it needs nothing but vanilla state,
 * so the HUD works with only {@code core} present (ADR-0024).
 *
 * <p>Reads the local player's own cached fields each frame and keeps only numbers: no player is
 * stored (ADR-0007). Allocates nothing per frame; the numbers beside the bar are drawn from
 * strings cached until the value changes.
 */
@OnlyIn(Dist.CLIENT)
public final class VitalsElement implements HudElement {

    private static final int WIDTH = 182;
    private static final int HEIGHT = 26;
    private static final int BADGE = 18;
    private static final int BAR_H = 8;
    /** Top of the badge/bar row; the rows above it hold the numbers. */
    private static final int ROW_Y = 8;

    private static final int GREEN = 0x4FD06A;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0x7E8798;
    private static final int RED = 0xE8505A;
    private static final int LEVEL = 0xA6F058;
    private static final int FOOD_OK = 0xFFB04A;
    private static final int FOOD_HEALING = 0x6FE08A;
    private static final int FOOD_LOW = 0xFF5A64;

    private final HealthBarModel model = new HealthBarModel();

    private int cachedHealth = Integer.MIN_VALUE;
    private int cachedMax = Integer.MIN_VALUE;
    private Component healthText = Component.empty();
    private Component maxText = Component.empty();
    private int cachedShield = Integer.MIN_VALUE;
    private Component shieldText = Component.empty();
    private int cachedRegen = Integer.MIN_VALUE;
    private Component regenText = Component.empty();
    private int cachedLevel = Integer.MIN_VALUE;
    private Component levelText = Component.empty();

    /** Forget the remembered red part, on leaving a world. */
    public void reset() {
        model.reset();
    }

    @Override
    public HudAnchor anchor() {
        return HudAnchor.ABOVE_HOTBAR;
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
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isCreative() && !player.isSpectator();
    }

    @Override
    public void render(GuiGraphics g, HudTheme theme, float partialTick, float opacity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Font font = mc.font;

        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
        float forecast = regen == null ? 0.0f : RegenForecast.pending(regen.getDuration(), regen.getAmplifier());
        model.update(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(), forecast,
                player.level().getGameTime());

        int barX = BADGE + 2;
        int barW = WIDTH - 2 * (BADGE + 2);
        int barY = ROW_Y + (BADGE - BAR_H) / 2;

        // Level badge on the left: its ring is the progress to the next level.
        HudSprites.ringBadge(g, 0, ROW_Y, BADGE, player.experienceProgress, LEVEL, opacity);
        drawCentred(g, font, levelText(player.experienceLevel), BADGE / 2, ROW_Y + (BADGE - font.lineHeight) / 2 + 1,
                withAlpha(0xD8FFB0, opacity));

        // The bar: track, then green, white, grey and red parts, then the rim.
        HudSprites.barTrack(g, barX, barY, barW, BAR_H, opacity);
        HudSprites.barFill(g, barX, barY, barW, BAR_H, 0.0f, model.redEnd(), RED, opacity);
        HudSprites.barFill(g, barX, barY, barW, BAR_H, 0.0f, model.greyEnd(), GREY, opacity);
        HudSprites.barFill(g, barX, barY, barW, BAR_H, 0.0f, model.whiteEnd(), WHITE, opacity);
        HudSprites.barFill(g, barX, barY, barW, BAR_H, 0.0f, model.greenEnd(), GREEN, opacity);
        HudSprites.barFrame(g, barX, barY, barW, BAR_H, opacity);

        // Numbers above the bar, so no coloured part is ever under text.
        int health = Math.round(model.current());
        int max = Math.round(model.max());
        updateHealthText(health, max);
        int textY = ROW_Y - font.lineHeight + 1;
        g.drawString(font, healthText, barX + 1, textY, withAlpha(0xFFFFFF, opacity), true);
        g.drawString(font, maxText, barX + 1 + font.width(healthText) + 2, textY, withAlpha(0x9AA3B4, opacity), true);

        int rightX = barX + barW - 1;
        int shield = Math.round(model.shield());
        int pending = Math.round(model.regen());
        if (pending > 0) {
            Component s = regenText(pending);
            rightX -= font.width(s);
            g.drawString(font, s, rightX, textY, withAlpha(GREY, opacity), true);
            rightX -= 4;
        }
        if (shield > 0) {
            Component s = shieldText(shield);
            rightX -= font.width(s);
            g.drawString(font, s, rightX, textY, withAlpha(WHITE, opacity), true);
        }

        // Hunger badge on the right. Healing needs a well-fed player, so the ring says why you are
        // or are not healing: green from 18, red when low.
        int food = player.getFoodData().getFoodLevel();
        int foodColour = food >= 18 ? FOOD_HEALING : food <= 6 ? FOOD_LOW : FOOD_OK;
        HudSprites.ringBadge(g, WIDTH - BADGE, ROW_Y, BADGE, food / 20.0f, foodColour, opacity);
        HudSprites.food(g, WIDTH - BADGE + 3, ROW_Y + 3, BADGE - 6, opacity);
    }

    // --- cached strings: rebuilt only when the number changes ----------------

    private void updateHealthText(int health, int max) {
        if (health != cachedHealth) {
            cachedHealth = health;
            healthText = HudFonts.bold(Integer.toString(health));
        }
        if (max != cachedMax) {
            cachedMax = max;
            maxText = HudFonts.light("/ " + max);
        }
    }

    private Component shieldText(int shield) {
        if (shield != cachedShield) {
            cachedShield = shield;
            shieldText = HudFonts.bold("+" + shield);
        }
        return shieldText;
    }

    private Component regenText(int pending) {
        if (pending != cachedRegen) {
            cachedRegen = pending;
            regenText = HudFonts.bold("+" + pending);
        }
        return regenText;
    }

    private Component levelText(int level) {
        if (level != cachedLevel) {
            cachedLevel = level;
            levelText = HudFonts.bold(Integer.toString(level));
        }
        return levelText;
    }

    private static void drawCentred(GuiGraphics g, Font font, Component text, int cx, int y, int colour) {
        g.drawString(font, text, cx - font.width(text) / 2, y, colour, true);
    }

    /** An opaque RGB colour with the overall opacity as its alpha. */
    private static int withAlpha(int rgb, float opacity) {
        int a = Math.max(4, Math.min(255, Math.round(opacity * 255.0f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
