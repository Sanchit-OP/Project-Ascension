package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads every {@code assets/<namespace>/ascension_sky/*.json} on resource reload and answers
 * "what hangs in this dimension's sky". Registered by {@link SpaceClientSetup}.
 *
 * <p>Holds dimension ids and immutable records only, never a level (ADR-0007), and the whole
 * map is replaced on every reload, so a resource pack that removes a body removes it.
 */
final class SkyBodies extends SimpleJsonResourceReloadListener {

    static final String FOLDER = "ascension_sky";
    static final SkyBodies INSTANCE = new SkyBodies();

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionWorlds.MOD_ID + "/sky");

    private volatile Map<ResourceLocation, List<SkyBody>> byDimension = Map.of();
    private volatile Map<ResourceLocation, SkyBody.Haze> hazeByDimension = Map.of();

    private SkyBodies() {
        super(new Gson(), FOLDER);
    }

    static List<SkyBody> forDimension(ResourceLocation dimension) {
        return INSTANCE.byDimension.getOrDefault(dimension, List.of());
    }

    @Nullable
    static SkyBody.Haze hazeFor(ResourceLocation dimension) {
        return INSTANCE.hazeByDimension.get(dimension);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, List<SkyBody>> loaded = new HashMap<>();
        Map<ResourceLocation, SkyBody.Haze> hazes = new HashMap<>();
        files.forEach((file, json) -> SkyBody.SkyFile.CODEC.parse(JsonOps.INSTANCE, json)
                .ifSuccess(sky -> {
                    loaded.computeIfAbsent(sky.dimension(), d -> new ArrayList<>()).addAll(sky.bodies());
                    sky.haze().ifPresent(haze -> {
                        if (hazes.put(sky.dimension(), haze) != null) {
                            LOGGER.warn("Sky file {} replaces another haze for {}", file, sky.dimension());
                        }
                    });
                })
                .ifError(error -> LOGGER.error("Skipping sky file {}: {}", file, error.message())));
        hazeByDimension = Map.copyOf(hazes);
        Map<ResourceLocation, List<SkyBody>> frozen = new HashMap<>();
        loaded.forEach((dimension, bodies) -> frozen.put(dimension, List.copyOf(bodies)));
        byDimension = Map.copyOf(frozen);
        LOGGER.debug("Loaded sky bodies for {} dimension(s)", frozen.size());
    }
}
