package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads every {@code assets/<namespace>/ascension_space/*.json} on resource reload and answers
 * "how does this planet look from space". Registered by {@link SpaceClientSetup}.
 *
 * <p>Holds planet ids and immutable records only, never a level (ADR-0007), and the whole map is
 * replaced on every reload. Two files for one planet: the later one loaded wins, with a warning.
 */
final class SpaceBodies extends SimpleJsonResourceReloadListener {

    static final String FOLDER = "ascension_space";
    static final SpaceBodies INSTANCE = new SpaceBodies();

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionWorlds.MOD_ID + "/space");

    private volatile Map<ResourceLocation, SpaceBody> byPlanet = Map.of();

    private SpaceBodies() {
        super(new Gson(), FOLDER);
    }

    @Nullable
    static SpaceBody forPlanet(ResourceLocation planet) {
        return INSTANCE.byPlanet.get(planet);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, SpaceBody> loaded = new HashMap<>();
        files.forEach((file, json) -> SpaceBody.CODEC.parse(JsonOps.INSTANCE, json)
                .ifSuccess(body -> {
                    if (loaded.put(body.planet(), body) != null) {
                        LOGGER.warn("Space file {} replaces another look for planet {}", file, body.planet());
                    }
                })
                .ifError(error -> LOGGER.error("Skipping space file {}: {}", file, error.message())));
        byPlanet = Map.copyOf(loaded);
        SpaceSkyRenderer.clearBelts();
        LOGGER.debug("Loaded space looks for {} planet(s)", loaded.size());
    }
}
