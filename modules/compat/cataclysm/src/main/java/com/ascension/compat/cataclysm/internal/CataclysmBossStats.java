package com.ascension.compat.cataclysm.internal;

import com.ascension.combat.api.stat.CombatStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Gives the Moon's and the Nether's bosses a place in {@code ascension-combat}'s Armor/Magic
 * Resist model.
 *
 * <p>Found in the first live test (2026-10-05): both showed 0 Armor and 0 Magic Resist.
 * {@code ascension-combat} assigns its own two attributes per vanilla mob type and leaves every
 * other living entity at 0 &mdash; so a boss sourced from another mod had nothing for armor
 * penetration or magic damage to cut through. This is step 3 of the boss-sourcing recipe in
 * {@code plans/m3-vertical-slice.md} ("rebalance the stats"), done in the compat jar so the combat
 * module never learns Cataclysm exists.
 *
 * <p>Values mirror {@code ascension-combat}'s own mob tiers: the Ender Golem as HEAVY ("built to
 * tank a fight" &mdash; it guards the citadel), the Ender Guardian as BOSS, and the Nether's two
 * (ADR-0017: the Netherite Monstrosity, which holds its key, and Ignis, optional) as BOSS too.
 * Placeholders to tune from a fight, like the tiers themselves; if the combat module ever exposes
 * its tiers in its api, read them from there instead of repeating the numbers.
 *
 * <p>Only ever loaded when {@code ascension_combat} is present (see the mod entry point), since it
 * links against that module's api.
 */
public final class CataclysmBossStats {

    private static final ResourceLocation ENDER_GOLEM = ResourceLocation.fromNamespaceAndPath("cataclysm", "ender_golem");
    private static final ResourceLocation ENDER_GUARDIAN = ResourceLocation.fromNamespaceAndPath("cataclysm", "ender_guardian");
    private static final ResourceLocation NETHERITE_MONSTROSITY = ResourceLocation.fromNamespaceAndPath("cataclysm", "netherite_monstrosity");
    private static final ResourceLocation IGNIS = ResourceLocation.fromNamespaceAndPath("cataclysm", "ignis");

    private CataclysmBossStats() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CataclysmBossStats::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        set(event, ENDER_GOLEM, 50.0, 20.0);
        set(event, ENDER_GUARDIAN, 100.0, 40.0);
        set(event, NETHERITE_MONSTROSITY, 100.0, 40.0);
        set(event, IGNIS, 100.0, 40.0);
    }

    @SuppressWarnings("unchecked")
    private static void set(EntityAttributeModificationEvent event, ResourceLocation id, double armor, double magicResist) {
        // getOptional, not get: the entity registry is a defaulted one, and get() on a missing id
        // would quietly hand back a pig.
        BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> {
            EntityType<? extends LivingEntity> living = (EntityType<? extends LivingEntity>) type;
            event.add(living, CombatStats.ARMOR.attribute(), armor);
            event.add(living, CombatStats.MAGIC_RESIST.attribute(), magicResist);
        });
    }
}
