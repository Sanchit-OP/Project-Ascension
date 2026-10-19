package com.ascension.combat.client.hud;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.CombatClientConfig;
import com.ascension.combat.internal.mechanics.MagicHitPulse;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The faint tint at the screen edge when a magic hit lands on the player (docs/technical/damage-kinds.md).
 *
 * <p>Drawn as nested frames of flat fills, thinnest at the very edge and fading inward, in the colour
 * of the Magic Resist stat so the character sheet and the hit speak the same colour. It needs no texture. It
 * lasts half a second and is at most a fifth opaque at the edge (see {@link MagicHitPulse}); the player can
 * turn it down or off in {@code ascension_combat-client.toml}. Draws nothing, and does no work, unless a
 * pulse is live. Its own layer, so it works with or without the HUD module.
 */
@OnlyIn(Dist.CLIENT)
public final class MagicHitLayer implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientMagicHit.any()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) {
            return;
        }
        float age = mc.level.getGameTime() - ClientMagicHit.startedAt() + deltaTracker.getGameTimeDeltaPartialTick(false);
        float setting = CombatClientConfig.INSTANCE.magicHitTint.get().floatValue();
        float alpha = MagicHitPulse.alpha(age, ClientMagicHit.strength(), setting);
        if (alpha <= 0.0f) {
            return;
        }

        int rgb = CombatStats.MAGIC_RESIST.color() & 0xFFFFFF;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int thickness = Math.max(8, Math.round(height * MagicHitPulse.THICKNESS_FRACTION));

        // Band i is the frame between two nested rectangles, so no pixel is painted twice.
        for (int band = 0; band < MagicHitPulse.BANDS; band++) {
            int from = thickness * band / MagicHitPulse.BANDS;
            int to = thickness * (band + 1) / MagicHitPulse.BANDS;
            int a = Math.round(alpha * MagicHitPulse.falloff(band) * 255.0f);
            if (to <= from || a <= 0) {
                continue;
            }
            int argb = (a << 24) | rgb;
            graphics.fill(from, from, width - from, to, argb);                  // top
            graphics.fill(from, height - to, width - from, height - from, argb); // bottom
            graphics.fill(from, to, to, height - to, argb);                      // left
            graphics.fill(width - to, to, width - from, height - to, argb);      // right
        }
    }
}
