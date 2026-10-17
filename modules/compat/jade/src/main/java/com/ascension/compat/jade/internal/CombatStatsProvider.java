package com.ascension.compat.jade.internal;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.compat.jade.AscensionCompatJade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Appends Armor and Magic Resist to Jade's entity tooltip (docs/technical/combat-api.md section
 * 3), the same way HP already shows there. Reads them through {@link StatReader}, combat's one way
 * to read a stat (docs/technical/item-stats.md section 6), never its attributes directly.
 *
 * <p>Both stats are synced to clients, so the client already has a live copy of whatever the
 * wearer/wielder is currently modifying &mdash; Darius's Axe's shred, The Protean Mask's stacks,
 * worn armour, and so on show up here as they change, with no extra sync of this module's own.
 */
public enum CombatStatsProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AscensionCompatJade.MOD_ID, "combat_stats");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof LivingEntity target)) {
            return;
        }

        tooltip.add(Component.literal("Armor: " + (int) StatReader.value(target, CombatStats.ARMOR)));
        tooltip.add(Component.literal("Magic Resist: " + (int) StatReader.value(target, CombatStats.MAGIC_RESIST)));
    }
}
