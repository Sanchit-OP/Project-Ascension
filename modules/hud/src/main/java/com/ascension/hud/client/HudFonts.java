package com.ascension.hud.client;

import com.ascension.hud.AscensionHud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The HUD's typeface: Barlow (SIL Open Font Licence 1.1, see {@code font/OFL-Barlow.txt}), loaded
 * through Minecraft's own font system, so the numbers are smooth instead of pixel-blocky. Falls
 * back to the default font for any glyph Barlow lacks.
 *
 * <p>Components are built once per changed value by the element and cached; nothing here is
 * called per frame with new strings.
 */
@OnlyIn(Dist.CLIENT)
final class HudFonts {

    private static final Style BOLD = Style.EMPTY.withFont(
            ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "hud"));
    private static final Style LIGHT = Style.EMPTY.withFont(
            ResourceLocation.fromNamespaceAndPath(AscensionHud.MOD_ID, "hud_light"));

    private HudFonts() {
    }

    /** Barlow SemiBold, for the numbers that matter. */
    static Component bold(String text) {
        return Component.literal(text).withStyle(BOLD);
    }

    /** Barlow Regular, for secondary text such as a maximum. */
    static Component light(String text) {
        return Component.literal(text).withStyle(LIGHT);
    }
}
