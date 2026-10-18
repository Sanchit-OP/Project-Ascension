package com.ascension.compat.ftbquests.client;

import com.ascension.compat.ftbquests.AscensionCompatFtbQuests;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The clear picture laid over the Planets page, on which the "you are here" marker is drawn.
 *
 * <p>The quest book shows a picture by asking Minecraft's texture manager for it by name, so a texture
 * registered here under {@link #TEXTURE} is what the book's overlay image draws, with no change to the
 * book. It starts fully transparent; {@link #refresh} paints the marker on it and uploads it.
 *
 * <p>Cost (ADR-0007): one texture, {@code size} x {@code size} x 4 bytes (4 MB at 1024), held for the
 * session. Nothing runs per frame; a check every {@link #POLL_TICKS} ticks compares a few numbers and
 * repaints only when the marker has moved a pixel, changed world or turned. Only the small square around
 * the marker is cleared and redrawn. Nothing here holds a level, a player or an entity: they are read
 * from the client when asked and never kept.
 */
@EventBusSubscriber(modid = AscensionCompatFtbQuests.MOD_ID, value = Dist.CLIENT)
public final class ChartOverlay {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChartOverlay.class);

    /** The name the quest chapter's overlay image refers to; there is no file behind it. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AscensionCompatFtbQuests.MOD_ID, "textures/quests/planets/you_are_here.png");

    static final ResourceLocation LAYOUT = ResourceLocation.fromNamespaceAndPath(
            AscensionCompatFtbQuests.MOD_ID, "quests/chart_layout.json");

    /** Used until the layout says otherwise, so the overlay always exists when the book asks for it. */
    private static final int DEFAULT_SIZE = 1024;

    /** Half a second: the marker cannot move a visible amount faster than a player flies in this time. */
    static final int POLL_TICKS = 10;

    private static ChartLayout layout;
    private static NativeImage image;
    private static DynamicTexture texture;

    private static int ticks;
    private static MarkerPainter.Rect painted;
    private static long lastKey = Long.MIN_VALUE;

    private ChartOverlay() {
    }

    /** Called once from the mod bus: read the layout again whenever resources reload. */
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) ChartOverlay::reload);
    }

    private static void reload(ResourceManager resources) {
        layout = null;
        try (Reader reader = resources.openAsReader(LAYOUT)) {
            layout = ChartLayout.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .getOrThrow(message -> new IOException(message));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Ascension quest book: cannot read {}, the Planets page will have no marker", LAYOUT, e);
        }
        ensureTexture(layout != null ? layout.size() : DEFAULT_SIZE);
        lastKey = Long.MIN_VALUE;
    }

    /** Makes sure our texture is the one registered under {@link #TEXTURE}, at the size the picture is. */
    private static void ensureTexture(int size) {
        Minecraft minecraft = Minecraft.getInstance();
        if (image != null && image.getWidth() != size) {
            release();
        }
        if (texture == null) {
            image = new NativeImage(NativeImage.Format.RGBA, size, size, true);
            texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            painted = null;
        }
        // A resource reload may have dropped it from the manager; put the same one back.
        if (minecraft.getTextureManager().getTexture(TEXTURE, null) != texture) {
            minecraft.getTextureManager().register(TEXTURE, texture);
        }
    }

    /** Lets the texture manager unregister and close it, so it is never closed twice. */
    private static void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
        }
        texture = null;
        image = null;
        painted = null;
    }

    /** The layout the overlay is drawn from, or null if it could not be read. For the dev check. */
    public static ChartLayout layout() {
        return layout;
    }

    /** The overlay's alpha (0 to 255) at a pixel, or -1 if there is no overlay or the pixel is outside it. For the dev check. */
    public static int alphaAt(int x, int y) {
        if (image == null || x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return -1;
        }
        return image.getPixelRGBA(x, y) >>> 24;
    }

    /** Whether the texture the quest book would draw under {@link #TEXTURE} is the one we made. For the dev check. */
    public static boolean isRegistered() {
        return texture != null && Minecraft.getInstance().getTextureManager().getTexture(TEXTURE, null) == texture;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (++ticks % POLL_TICKS == 0) {
            refresh(Minecraft.getInstance());
        }
    }

    /** Repaints the marker if (and only if) where it should be has changed since last time. */
    static void refresh(Minecraft minecraft) {
        if (texture == null || layout == null) {
            return;
        }
        var level = minecraft.level;
        var player = minecraft.player;
        var marker = (level == null || player == null)
                ? Optional.<ChartMath.Marker>empty()
                : ChartMath.locate(layout, level.dimension().location(), player.getX(), player.getZ(), player.getYRot());

        long key = keyOf(marker.orElse(null));
        if (key == lastKey) {
            return;
        }
        lastKey = key;

        if (painted != null) {
            image.fillRect(painted.x(), painted.y(), painted.width(), painted.height(), 0);
            painted = null;
        }
        marker.ifPresent(m -> painted = MarkerPainter.paint(new ImageSurface(image), m));
        texture.upload();
    }

    /** A number that changes exactly when the picture should: the pixel the marker is on and its heading in 15-degree steps. */
    static long keyOf(ChartMath.Marker marker) {
        if (marker == null) {
            return -1L;
        }
        long x = Math.round(marker.x());
        long y = Math.round(marker.y());
        long h = marker.hasHeading() ? Math.floorMod(Math.round(Math.toDegrees(marker.heading()) / 15.0), 24L) : 31L;
        return (x << 40) ^ (y << 16) ^ h;
    }
}
