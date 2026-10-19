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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
        gameBus.addListener((AddReloadListenerEvent event) -> event.addListener(DifficultyScaling.INSTANCE));
        StatSources.register(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, ID_PATH_PREFIX),
                Component.translatable("stat_source.ascension_combat.mob_type"));
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            apply(living);
            // A mob new to the world starts at its scaled health, not its unscaled health.
            if (!event.loadedFromDisk() && living instanceof Mob) {
                living.setHealth(living.getMaxHealth());
            }
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
        applyDifficulty(living);
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

    /**
     * The world difficulty's share (docs/progression/balance.md section 2): percent of the mob's own
     * base health and attack damage, as one modifier each, never saved. A mob keeps the difficulty it
     * entered or was last reloaded under; changing difficulty reaches it on the next reload or rejoin.
     */
    private static void applyDifficulty(LivingEntity living) {
        if (!(living instanceof Mob)) {
            return;
        }
        DifficultyScaling.Scale scale = DifficultyScaling.INSTANCE.scaleFor(living.getType(), living.level().getDifficulty());
        if (scale.hasHealthTotal()) {
            setTotalHealth(living, scale.healthTotal());
        } else {
            setPercent(living, Attributes.MAX_HEALTH, "difficulty_health", scale.healthPercent());
        }
        setPercent(living, Attributes.ATTACK_DAMAGE, "difficulty_attack_damage", scale.attackDamagePercent());
    }

    /**
     * Makes the mob's maximum health exactly {@code total}, whatever else it has.
     *
     * <p>Measured with this mod's own modifier taken off, so it is the mob's base and everything other mods have added
     * (Cataclysm's Ender Golem and Ender Guardian each carry an {@code animation_health} modifier of +150% of their
     * base, which an earlier version of this method, working from the base alone, added on top of the total).
     * The modifier is a flat addition, so it is sized to land the final figure on {@code total} even when something
     * else multiplies health. Applying it again gives the same answer. Health another mod changes after this runs is
     * not followed until the next reload or rejoin.
     */
    private static void setTotalHealth(LivingEntity living, double total) {
        AttributeInstance instance = living.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, ID_PATH_PREFIX + "difficulty_health");
        instance.removeModifier(id);
        double without = instance.getValue();
        double flat = instance.getBaseValue();
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                flat += modifier.amount();
            }
        }
        // value = flat * multiplier, so the flat part has to become total / multiplier, and the multiplier is without / flat.
        double amount = flat > 0.0 && without > 0.0 ? flat * (total / without - 1.0) : total - without;
        instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void setPercent(LivingEntity living, Holder<Attribute> attribute, String name, double percent) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, ID_PATH_PREFIX + name);
        if (percent == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, percent / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
