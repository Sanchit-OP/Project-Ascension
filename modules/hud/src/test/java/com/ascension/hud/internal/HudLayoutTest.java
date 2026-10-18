package com.ascension.hud.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.core.api.client.HudAnchor;
import org.junit.jupiter.api.Test;

class HudLayoutTest {

    private final HudLayout layout = new HudLayout();
    private final int[] x = new int[8];
    private final int[] y = new int[8];

    private void place(HudAnchor[] anchors, int[] w, int[] h, int screenW, int screenH) {
        layout.compute(anchors.length, anchors, w, h, screenW, screenH, x, y);
    }

    @Test
    void statBlockSitsJustLeftOfTheHotbarAndBottomAligned() {
        // 480x270 is a 1920x1080 window at GUI scale 4. Hotbar spans 149..331.
        place(new HudAnchor[] {HudAnchor.HOTBAR_LEFT}, new int[] {66}, new int[] {36}, 480, 270);
        assertEquals(149 - HudLayout.GAP - 66, x[0]);
        assertEquals(270 - 36, y[0]);
    }

    @Test
    void rightAnchorSitsJustRightOfTheHotbar() {
        place(new HudAnchor[] {HudAnchor.HOTBAR_RIGHT}, new int[] {20}, new int[] {20}, 480, 270);
        assertEquals(331 + HudLayout.GAP, x[0]);
    }

    @Test
    void sharedAnchorStacksAwayFromTheHotbar() {
        place(new HudAnchor[] {HudAnchor.HOTBAR_RIGHT, HudAnchor.HOTBAR_RIGHT},
                new int[] {20, 20}, new int[] {20, 20}, 480, 270);
        assertEquals(x[0] + 20 + HudLayout.GAP, x[1]);
    }

    @Test
    void aboveRowStartsAtTheHotbarLeftAndStacksUp() {
        place(new HudAnchor[] {HudAnchor.ABOVE_HOTBAR, HudAnchor.ABOVE_HOTBAR},
                new int[] {100, 100}, new int[] {8, 8}, 480, 270);
        assertEquals(149, x[0]);
        assertEquals(270 - 22 - HudLayout.GAP - 8, y[0]);
        assertEquals(y[0] - 8 - HudLayout.GAP, y[1]);
    }

    @Test
    void centreAnchorClearsTheRowAboveTheHotbar() {
        place(new HudAnchor[] {HudAnchor.CENTER_ABOVE_HOTBAR}, new int[] {40}, new int[] {40}, 480, 270);
        assertEquals(220, x[0]);
        int bottom = y[0] + 40;
        assertEquals(270 - 22 - HudLayout.GAP - HudLayout.ROW_HEIGHT - HudLayout.GAP, bottom);
    }

    @Test
    void anchorsDoNotShareAStack() {
        place(new HudAnchor[] {HudAnchor.HOTBAR_LEFT, HudAnchor.HOTBAR_RIGHT},
                new int[] {50, 20}, new int[] {20, 20}, 480, 270);
        assertEquals(331 + HudLayout.GAP, x[1]);
    }

    @Test
    void positionsNeverGoOffTheLeftOrTop() {
        place(new HudAnchor[] {HudAnchor.HOTBAR_LEFT}, new int[] {200}, new int[] {40}, 200, 30);
        assertEquals(0, x[0]);
        assertEquals(0, y[0]);
    }

    @Test
    void layoutIsRepeatable() {
        HudAnchor[] a = {HudAnchor.TOP_LEFT, HudAnchor.TOP_LEFT};
        place(a, new int[] {10, 10}, new int[] {10, 10}, 480, 270);
        int first = y[1];
        place(a, new int[] {10, 10}, new int[] {10, 10}, 480, 270);
        assertEquals(first, y[1]);
    }
}
