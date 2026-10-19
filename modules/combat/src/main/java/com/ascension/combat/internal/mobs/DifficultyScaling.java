package com.ascension.combat.internal.mobs;

import com.ascension.combat.AscensionCombat;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

/**
 * How much tougher or softer the world's difficulty makes a mob (docs/progression/balance.md
 * section 2), from data: {@code data/ascension_combat/mob_difficulty/<easy|normal|hard>.json}.
 *
 * <pre>
 * { "tiers": { "boss": { "health_percent": 75, "attack_damage_percent": 25 } },
 *   "types": { "minecraft:warden": { "health_total": 1500, "attack_damage_percent": 20 } } }
 * </pre>
 *
 * <p>A mob is scaled by its entry in {@code types}, else by its tier ({@code
 * #ascension_combat:tier/<name>}), else not at all. Percentages are of the mob's own base value, so
 * a 50 HP mob at +30% has 65. Files with the same name from other packs stack, one entry at a time,
 * the later pack winning. {@link MobStats} reads the result; this class only holds it, swapped whole
 * on each reload, with nothing from a world in it (ADR-0007).
 */
public final class DifficultyScaling extends SimpleJsonResourceReloadListener {

    public static final DifficultyScaling INSTANCE = new DifficultyScaling();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Scale NONE = new Scale(0.0, 0.0, Double.NaN);

    /**
     * What a difficulty does to a mob: percent changes to its own base health and attack damage, and
     * optionally a total health that replaces the mob's own ({@code health_total}, for bosses whose
     * health is written down as a number; NaN when absent, and then the percent applies).
     */
    public record Scale(double healthPercent, double attackDamagePercent, double healthTotal) {
        public boolean hasHealthTotal() {
            return !Double.isNaN(healthTotal);
        }
    }

    private record Table(Map<String, Scale> tiers, Map<ResourceLocation, Scale> types) {
    }

    private volatile Map<Difficulty, Table> tables = Map.of();

    private DifficultyScaling() {
        super(new Gson(), "mob_difficulty");
    }

    /** What {@code type} gets at {@code difficulty}: its own entry, else its tier's, else nothing. */
    public Scale scaleFor(EntityType<?> type, Difficulty difficulty) {
        Table table = tables.get(difficulty);
        if (table == null) {
            return NONE;
        }
        Scale own = table.types().get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        if (own != null) {
            return own;
        }
        for (Map.Entry<String, Scale> tier : table.tiers().entrySet()) {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "tier/" + tier.getKey()));
            if (type.is(tag)) {
                return tier.getValue();
            }
        }
        return NONE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<Difficulty, Table> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            Difficulty difficulty = difficultyNamed(id.getPath());
            if (difficulty == null) {
                LOGGER.error("[{}] mob_difficulty/{}: not a difficulty (peaceful, easy, normal, hard)",
                        AscensionCombat.MOD_ID, id.getPath());
                return;
            }
            try {
                Table table = loaded.computeIfAbsent(difficulty, d -> new Table(new HashMap<>(), new HashMap<>()));
                JsonObject root = json.getAsJsonObject();
                if (root.has("tiers")) {
                    GsonHelper.getAsJsonObject(root, "tiers").entrySet()
                            .forEach(e -> table.tiers().put(e.getKey(), scale(e.getValue().getAsJsonObject())));
                }
                if (root.has("types")) {
                    GsonHelper.getAsJsonObject(root, "types").entrySet().forEach(e ->
                            table.types().put(ResourceLocation.parse(e.getKey()), scale(e.getValue().getAsJsonObject())));
                }
            } catch (RuntimeException e) {
                LOGGER.error("[{}] bad mob_difficulty {}: {}", AscensionCombat.MOD_ID, id, e.getMessage());
            }
        });
        tables = Map.copyOf(loaded);
    }

    private static Scale scale(JsonObject object) {
        return new Scale(GsonHelper.getAsDouble(object, "health_percent", 0.0),
                GsonHelper.getAsDouble(object, "attack_damage_percent", 0.0),
                GsonHelper.getAsDouble(object, "health_total", Double.NaN));
    }

    private static Difficulty difficultyNamed(String name) {
        try {
            return Difficulty.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
