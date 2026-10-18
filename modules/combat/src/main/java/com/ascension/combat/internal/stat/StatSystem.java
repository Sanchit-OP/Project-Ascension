package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.StatSources;
import com.ascension.combat.internal.mobs.MobStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;

/**
 * Wires up item stats (docs/technical/item-stats.md, ADR-0019): the data maps, the listener that
 * gives every item its stats, the two fixes for vanilla timing (gear after {@code /reload}, health
 * after login), {@code /ascension stats}, and what reading a stat on a client needs. The one call
 * {@code AscensionCombat} makes for all of it.
 */
public final class StatSystem {

    private StatSystem() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        StatDataMaps.register(modBus);
        HealthAcrossLogin.register(modBus, gameBus);
        ItemStatModifiers.register(gameBus);
        WornGear.register(gameBus);
        StatCommands.register(gameBus);
        MobStats.register(gameBus);

        // Vanilla syncs every combat stat's attribute to clients except Attack Damage, so a
        // client read it as a bare hand's 1 whatever was held: the character sheet showed exactly
        // that (item-stats.md section 1, trap 4). StatReader promises every stat on both sides.
        // This runs on both sides before any entity exists, and costs a few bytes when an entity's
        // Attack Damage changes.
        Attributes.ATTACK_DAMAGE.value().setSyncable(true);

        // Vanilla names its effects' modifiers effect.<name>: Strength, Speed, Haste and the rest.
        StatSources.register(ResourceLocation.withDefaultNamespace("effect."),
                Component.translatable("stat_source.ascension_combat.effects"));

        // Enchantments give stats through minecraft:attributes effects whose ids start with
        // ascension_combat:enchantment/ (docs/progression/enchantments.md, ADR-0022). Vanilla adds a
        // /<slot> suffix to the id, so the prefix still matches.
        StatSources.register(ResourceLocation.fromNamespaceAndPath("ascension_combat", "enchantment/"),
                Component.translatable("stat_source.ascension_combat.enchantments"));
    }
}
