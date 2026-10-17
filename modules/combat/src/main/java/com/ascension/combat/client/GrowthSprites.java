package com.ascension.combat.client;

import com.ascension.combat.AscensionCombat;
import net.minecraft.resources.ResourceLocation;

/**
 * Where everything is in the tree screen's two textures, in pixels. {@code tools/generate-growth-art.py}
 * draws the textures and lays out the sheet; this is its other half, and the two must agree (the
 * script's {@code write_textures} is the one to read).
 */
final class GrowthSprites {

    private GrowthSprites() {
    }

    static final ResourceLocation PANEL = texture("panel.png");
    static final ResourceLocation NODES = texture("nodes.png");

    /** The panel is drawn at this size, one texel to a GUI pixel. */
    static final int PANEL_W = 400;
    static final int PANEL_H = 224;
    static final int SHEET_W = 144;
    static final int SHEET_H = 144;

    /** Sockets: a small node and a notable, in pixels. */
    static final int SMALL = 13;
    static final int NOTABLE = 21;

    /** The marked ring and the hover ring are a little larger than the socket they go round. */
    static final int MARKED_EXTRA = 4;
    static final int HOVER_EXTRA = 6;

    /** The centre emblem. */
    static final int CENTRE_U = 0;
    static final int CENTRE_V = 112;
    static final int CENTRE_SIZE = 31;

    /** Icons are 8 by 8, one column per stat, in three rows: lit, can take, out of reach. */
    static final int GLYPH_U = 32;
    static final int GLYPH_SIZE = 8;
    static final int[] GLYPH_V = {112, 120, 128};

    /** The sheet's row of a theme's sockets, in the order of {@code GrowthNode.Arm}: 21 pixels apart. */
    static int themeV(int theme) {
        return 21 * theme;
    }

    /** Horizontal position of a socket: small chosen, small open, notable chosen, notable open. */
    static int socketU(boolean notable, boolean chosen) {
        return notable ? (chosen ? 26 : 47) : (chosen ? 0 : 13);
    }

    /** Locked sockets share one picture for every theme, in their own row. */
    static final int LOCKED_V = 84;

    static int lockedU(boolean notable) {
        return notable ? 13 : 0;
    }

    /** The marked ring, then the hover ring: left edge on the row at {@link #LOCKED_V}. */
    static int markedU(boolean notable) {
        return notable ? 51 : 34;
    }

    static int hoverU(boolean notable) {
        return notable ? 95 : 76;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "textures/gui/growth/" + name);
    }
}
