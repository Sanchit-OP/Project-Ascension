package com.ascension.combat.client.hud;

import com.ascension.combat.AscensionCombat;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The smooth rune glyphs the HUD chips use, one white cell per rune on a single sheet, tinted at
 * draw time (made by {@code tools/generate-hud-rune-glyphs.py}, whose {@code NAMES} list this one
 * mirrors). A rune not on the sheet has no index and the chip falls back to the orrery's own glyph.
 */
@OnlyIn(Dist.CLIENT)
final class RuneGlyphs {

    /** In the order of the sheet's cells. Keep in step with the generator's NAMES. */
    private static final List<String> NAMES = List.of(
            "conquerors_resolve", "relentless_strikes", "momentum", "cadence", "arc", "second_wind",
            "pack_leader", "starfall", "brace", "drop_in", "riposte", "killing_surge", "vanish", "plating");

    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "textures/gui/hud/rune_glyphs.png");
    private static final int CELL = 64;

    private RuneGlyphs() {
    }

    /** The cell of the rune's glyph, or -1 if it has none on the sheet. */
    static int indexOf(String runeName) {
        return NAMES.indexOf(runeName);
    }

    /** Draws cell {@code index} in a box of {@code size} units, tinted {@code rgb} at {@code alpha} (0 to 1). */
    static void draw(GuiGraphics g, int index, int x, int y, int size, int rgb, float alpha) {
        RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, alpha);
        g.blit(SHEET, x, y, size, size, index * CELL, 0, CELL, CELL, CELL * NAMES.size(), CELL);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
