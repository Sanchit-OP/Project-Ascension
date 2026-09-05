package com.ascension.worlds.api;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Registry keys owned by this module. */
public final class WorldsRegistries {

    /**
     * The planet registry.
     *
     * <p>A <strong>datapack registry</strong>, so planets are authored as JSON and can be added,
     * overridden or removed by any datapack &mdash; which is what makes ADR-0004's promise
     * ("adding a world is authoring, not engineering") true for other people and not only for us.
     *
     * <p>Synced to clients, because the client needs a planet's position and size to draw it in
     * the sky. Under ADR-0010 that is not decoration: a planet you cannot see from space turns
     * interplanetary travel into flying through an empty void toward an invisible waypoint.
     *
     * <p><strong>Files live at {@code data/<namespace>/ascension_worlds/planet/<name>.json}.</strong>
     * The doubled namespace looks like a mistake and is not: Minecraft puts non-vanilla datapack
     * registries under {@code <registry namespace>/<registry path>}, so ours is
     * {@code ascension_worlds/planet} inside whichever namespace is authoring. Our own planets
     * therefore sit at {@code data/ascension_worlds/ascension_worlds/planet/}.
     */
    public static final ResourceKey<Registry<Planet>> PLANET = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "planet"));

    private WorldsRegistries() {
    }
}
