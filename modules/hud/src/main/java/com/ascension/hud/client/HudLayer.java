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

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
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

        int count = 0;
        for (int i = 0; i < total; i++) {
            HudElement element = elements.get(i);
            if (!element.visible()) {
                continue;
            }
            shown[count] = element;
            anchors[count] = element.anchor();
            widths[count] = element.width();
            heights[count] = element.height();
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
            shown[i].render(graphics, partialTick, opacity);
            graphics.pose().popPose();
            shown[i] = null;
        }
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
