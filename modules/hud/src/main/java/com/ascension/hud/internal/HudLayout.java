package com.ascension.hud.internal;

import com.ascension.core.api.client.HudAnchor;
import java.util.Arrays;

/**
 * Turns anchors and sizes into positions. Pure arithmetic, no Minecraft types, so it is checked
 * by unit tests; whether it looks right is checked in a client (ADR-0008).
 *
 * <p>Geometry is vanilla's hotbar, which this HUD does not restyle: 182 by 22 GUI units, centred,
 * flush with the bottom of the screen. Everything is relative to it so the layout holds at any GUI
 * scale and aspect ratio.
 *
 * <p>One instance keeps its scratch space, so {@link #compute} allocates nothing and can run every
 * frame (ADR-0007).
 */
public final class HudLayout {

    public static final int HOTBAR_WIDTH = 182;
    public static final int HOTBAR_HEIGHT = 22;

    /** Space between neighbours. */
    static final int GAP = 4;

    /** Height of the row the {@link HudAnchor#ABOVE_HOTBAR} elements occupy; the centre anchor clears it. */
    static final int ROW_HEIGHT = 14;

    /** Margin from a screen corner. */
    static final int MARGIN = 4;

    private static final int ANCHORS = HudAnchor.values().length;

    /** How far each anchor's stack has grown. */
    private final int[] used = new int[ANCHORS];

    /**
     * Places {@code count} visible elements, in the order given (the stacking order).
     *
     * @param anchors where each wants to sit
     * @param widths  GUI units
     * @param heights GUI units
     * @param outX    receives each element's left edge
     * @param outY    receives each element's top edge
     */
    public void compute(int count, HudAnchor[] anchors, int[] widths, int[] heights,
                        int screenW, int screenH, int[] outX, int[] outY) {
        Arrays.fill(used, 0);
        int left = (screenW - HOTBAR_WIDTH) / 2;
        int right = left + HOTBAR_WIDTH;
        for (int i = 0; i < count; i++) {
            HudAnchor anchor = anchors[i];
            int w = widths[i];
            int h = heights[i];
            int slot = anchor.ordinal();
            int offset = used[slot];
            int x;
            int y;
            switch (anchor) {
                case HOTBAR_LEFT -> {
                    x = left - GAP - w - offset;
                    y = screenH - h;
                    used[slot] += w + GAP;
                }
                case HOTBAR_RIGHT -> {
                    x = right + GAP + offset;
                    y = screenH - h;
                    used[slot] += w + GAP;
                }
                case ABOVE_HOTBAR -> {
                    x = left;
                    y = screenH - HOTBAR_HEIGHT - GAP - h - offset;
                    used[slot] += h + GAP;
                }
                case CENTER_ABOVE_HOTBAR -> {
                    x = (screenW - w) / 2;
                    y = screenH - HOTBAR_HEIGHT - GAP - ROW_HEIGHT - GAP - h - offset;
                    used[slot] += h + GAP;
                }
                case TOP_LEFT -> {
                    x = MARGIN;
                    y = MARGIN + offset;
                    used[slot] += h + GAP;
                }
                case TOP_RIGHT -> {
                    x = screenW - MARGIN - w;
                    y = MARGIN + offset;
                    used[slot] += h + GAP;
                }
                default -> throw new IllegalStateException("Unhandled anchor " + anchor);
            }
            outX[i] = Math.max(0, x);
            outY[i] = Math.max(0, y);
        }
    }
}
