package com.ascension.hud.client;

import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import com.ascension.core.api.client.HudRegistry;
import com.ascension.hud.internal.HudConfig;
import com.ascension.hud.internal.HudLayout;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Draws every registered element where {@link HudLayout} puts it.
 *
 * <p>Runs every frame (ADR-0007): the scratch arrays are sized once and reused, so a frame
 * allocates nothing unless elements were added, and it reads only what each element already has
 * cached.
 */
@OnlyIn(Dist.CLIENT)
public final class HudLayer implements LayeredDraw.Layer {

    private final HudLayout layout = new HudLayout();

    private HudElement[] shown = new HudElement[0];
    private HudAnchor[] anchors = new HudAnchor[0];
    private int[] widths = new int[0];
    private int[] heights = new int[0];
    private int[] xs = new int[0];
    private int[] ys = new int[0];

    /** Which elements the config hides; recomputed only when its list changes. */
    private boolean[] hidden = new boolean[0];
    private java.util.List<? extends String> hiddenSource;
    private int hiddenFor = -1;

    /**
     * Whether the player has hidden the health row. Then vanilla's hearts must stay, or there would
     * be no health on screen at all; {@link HudClient} reads this before cancelling them.
     */
    static volatile boolean vitalsHidden;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        long started = HudFrameCost.begin();
        try {
            draw(graphics, deltaTracker);
        } finally {
            HudFrameCost.end(started);
        }
    }

    private void draw(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || !HudConfig.INSTANCE.enabled.getAsBoolean()) {
            return;
        }
        List<HudElement> elements = HudRegistry.elements();
        int total = elements.size();
        if (total == 0) {
            return;
        }
        ensureCapacity(total);

        refreshHidden(total);
        float scale = HudConfig.INSTANCE.scale.get().floatValue();

        int count = 0;
        for (int i = 0; i < total; i++) {
            HudElement element = elements.get(i);
            if (hidden[i] || !element.visible()) {
                continue;
            }
            shown[count] = element;
            anchors[count] = element.anchor();
            // Laid out at its scaled size, so a larger HUD still lines up along the hotbar.
            widths[count] = Math.round(element.width() * scale);
            heights[count] = Math.round(element.height() * scale);
            count++;
        }
        if (count == 0) {
            return;
        }

        layout.compute(count, anchors, widths, heights,
                graphics.guiWidth(), graphics.guiHeight(), xs, ys);

        float opacity = HudConfig.INSTANCE.opacity.get().floatValue();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        for (int i = 0; i < count; i++) {
            graphics.pose().pushPose();
            graphics.pose().translate(xs[i], ys[i], 0);
            graphics.pose().scale(scale, scale, 1.0f);
            DefaultTheme.INSTANCE.origin(xs[i], ys[i], scale);
            shown[i].render(graphics, DefaultTheme.INSTANCE, partialTick, opacity);
            graphics.pose().popPose();
            shown[i] = null;
        }
    }

    /** Rebuilds the hidden flags if the config list, or the set of elements, changed. */
    private void refreshHidden(int total) {
        java.util.List<? extends String> list = HudConfig.INSTANCE.hiddenElements.get();
        if (list == hiddenSource && total == hiddenFor) {
            return;
        }
        if (hidden.length < total) {
            hidden = new boolean[total];
        }
        com.ascension.hud.internal.HudVisibility.compute(HudRegistry.ids(), list, hidden);
        hiddenSource = list;
        hiddenFor = total;
        int vitals = HudRegistry.ids().indexOf(HudClient.VITALS_ID);
        vitalsHidden = vitals >= 0 && hidden[vitals];
    }

    private void ensureCapacity(int n) {
        if (shown.length >= n) {
            return;
        }
        shown = new HudElement[n];
        anchors = new HudAnchor[n];
        widths = new int[n];
        heights = new int[n];
        xs = new int[n];
        ys = new int[n];
    }
}
