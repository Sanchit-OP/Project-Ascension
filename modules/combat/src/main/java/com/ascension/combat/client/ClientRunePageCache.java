package com.ascension.combat.client;

import com.ascension.combat.api.rune.RunePath;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * This client's own cached copy of the currently saved rune page, kept up to date by {@link
 * ClientNetworkHandlers#onRunePageSync} and read by {@link RuneScreen} to pre-fill itself &mdash;
 * server-authoritative state the client has no other way to know between opening the screen and
 * the next sync. Mirrors {@code ClientNetworkHandlers}'s own static-field caching for stasis's
 * camera state, just pulled into its own class since the screen needs to read it directly rather
 * than only react to a payload.
 */
public final class ClientRunePageCache {

    private static RunePath primaryPath;
    private static RunePath secondaryPath;
    private static final Set<ResourceLocation> primaryMinors = new HashSet<>();
    private static final Set<ResourceLocation> secondaryMinors = new HashSet<>();

    private ClientRunePageCache() {
    }

    public static void update(
            RunePath newPrimaryPath,
            Set<ResourceLocation> newPrimaryMinors,
            RunePath newSecondaryPath,
            Set<ResourceLocation> newSecondaryMinors) {
        primaryPath = newPrimaryPath;
        primaryMinors.clear();
        primaryMinors.addAll(newPrimaryMinors);
        secondaryPath = newSecondaryPath;
        secondaryMinors.clear();
        secondaryMinors.addAll(newSecondaryMinors);
    }

    public static RunePath primaryPath() {
        return primaryPath;
    }

    public static RunePath secondaryPath() {
        return secondaryPath;
    }

    public static Set<ResourceLocation> primaryMinors() {
        return Set.copyOf(primaryMinors);
    }

    public static Set<ResourceLocation> secondaryMinors() {
        return Set.copyOf(secondaryMinors);
    }
}
