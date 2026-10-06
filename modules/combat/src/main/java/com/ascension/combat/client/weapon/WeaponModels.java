package com.ascension.combat.client.weapon;

import com.ascension.combat.AscensionCombat;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads every {@code assets/<ns>/weapon_models/<item>.json} on resource reload, keyed by the
 * item's id ({@code ascension_combat:veigos_blade}). A resource pack can replace one the usual way.
 * The map holds only parsed geometry and texture ids, and is swapped whole on each reload, so it
 * keeps nothing alive from a world (ADR-0007).
 */
final class WeaponModels extends SimpleJsonResourceReloadListener {
    static final WeaponModels INSTANCE = new WeaponModels();
    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile Map<ResourceLocation, WeaponModel> models = Map.of();

    private WeaponModels() {
        super(new Gson(), "weapon_models");
    }

    WeaponModel get(Item item) {
        return models.get(BuiltInRegistries.ITEM.getKey(item));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, WeaponModel> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, WeaponModel.parse(json.getAsJsonObject()));
            } catch (RuntimeException e) {
                LOGGER.error("[{}] bad weapon model {}: {}", AscensionCombat.MOD_ID, id, e.getMessage());
            }
        });
        models = Map.copyOf(loaded);
    }
}
