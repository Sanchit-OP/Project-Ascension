package com.ascension.core.api.client;

import com.ascension.core.internal.client.HudElements;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Add something to the HUD, or say that you are the HUD.
 *
 * <p>The meeting point ADR-0024 describes: modules that own state register a {@link HudElement},
 * a HUD host (today {@code ascension-hud}) reads the list and lays it out, and neither needs to
 * know the other exists. Any third-party mod may do either.
 *
 * <p>Register during client setup. The registry closes once mod loading completes, so the host
 * sees a stable, sorted list; registering afterwards throws.
 *
 * <p><strong>Client only.</strong> Call from client code; a dedicated server never loads this.
 */
public final class HudRegistry {

    private HudRegistry() {
    }

    /**
     * Contribute an element.
     *
     * @param id stable identifier, conventionally {@code yourmod:what_it_shows}
     * @throws IllegalStateException    if mod loading has already completed
     * @throws IllegalArgumentException if {@code id} is already registered
     */
    public static void register(ResourceLocation id, HudElement element) {
        HudElements.get().add(id, element);
    }

    /**
     * Say that a HUD host is installed and will draw the registered elements.
     *
     * <p>A module that ships its own small fallback drawing for the same thing (atmosphere's
     * oxygen gauge, say) checks {@link #hostPresent()} and stays quiet when a host is there, so a
     * player never sees two.
     */
    public static void declareHost(ResourceLocation hostId) {
        HudElements.get().declareHost(hostId);
    }

    /** Whether a host has declared itself. A plain flag read, safe every frame. */
    public static boolean hostPresent() {
        return HudElements.get().hostPresent();
    }

    /**
     * Every registered element in draw order, or empty before mod loading completes.
     *
     * <p>The returned list is immutable and the same instance every call once frozen, so a host
     * may call this per frame without allocating.
     */
    public static List<HudElement> elements() {
        return HudElements.get().elements();
    }

    /** Ids in the same order as {@link #elements()}, for diagnostics. */
    public static List<ResourceLocation> ids() {
        return HudElements.get().ids();
    }
}
