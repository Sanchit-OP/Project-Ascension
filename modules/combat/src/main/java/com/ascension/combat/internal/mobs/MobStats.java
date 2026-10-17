package com.ascension.combat.internal.mobs;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatSources;
import com.ascension.combat.internal.stat.StatDataMaps;
import com.ascension.combat.internal.stat.StatMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Gives mobs their place in the stat model (docs/technical/combat-api.md section 3a) from data:
 * {@code data_maps/entity_type/mob_stats.json}, by mob type or by tier tag
 * (docs/technical/item-stats.md section 9, Sanchit 2026-10-15).
 *
 * <p>The tiers are entity tags, {@code #ascension_combat:tier/swarm} to {@code tier/boss}, and the
 * numbers are data: a new mod's boss joins a tier with one tag line, and any mod's boss in
 * NeoForge's {@code #c:bosses} is a boss here with no line at all. Until 2026-10-16 the tiers were
 * Java ({@code MobStatTier}), set once at startup through {@code EntityAttributeModificationEvent},
 * which runs before any datapack loads and so could never read data.
 *
 * <p>A mob's stats are applied as it enters a world, spawned or loaded, as one modifier per stat
 * ({@code ascension_combat:mob_stats/<stat>}) on top of its own attributes. Modifiers rather than
 * base values: they are never saved with the mob, so the data is always the truth, and they never
 * fight a mod that scales a mob by setting its base values. A {@code /reload} re-applies them to
 * every loaded mob, once.
 *
 * <p>HP and Attack Damage are left to vanilla: those already vary per mob (a Zombie's 20 HP versus a
 * Ravager's 100), unlike Armor and Magic Resist. The data could set them too.
 */
public final class MobStats {

    private static final String ID_PATH_PREFIX = "mob_stats/";

    private MobStats() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MobStats::onJoinLevel);
        gameBus.addListener(MobStats::onDatapackSync);
        StatSources.register(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, ID_PATH_PREFIX),
                Component.translatable("stat_source.ascension_combat.mob_type"));
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            apply(living);
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // A player joining fires this too; only a reload changes the data under live mobs.
        if (event.getPlayer() != null) {
            return;
        }
        for (ServerLevel level : event.getPlayerList().getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    apply(living);
                }
            }
        }
    }

    /** Brings one mob's type stats in line with the data: added or updated, or removed if gone. */
    private static void apply(LivingEntity living) {
        StatMap data = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(living.getType()).getData(StatDataMaps.MOB_STATS);
        for (CombatStat stat : CombatStats.all()) {
            AttributeInstance instance = living.getAttribute(stat.attribute());
            if (instance == null) {
                continue;
            }

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, ID_PATH_PREFIX + stat.name());
            Double written = data == null ? null : data.stats().get(stat);
            if (written == null || written == 0.0) {
                instance.removeModifier(id);
            } else {
                instance.addOrUpdateTransientModifier(stat.modifier(id, written));
            }
        }
    }
}
