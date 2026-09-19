package com.ascension.compat.jade.internal;

import com.ascension.combat.api.CombatAttributes;
import com.ascension.compat.jade.AscensionCompatJade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Appends Armor and Magic Resist to Jade's entity tooltip (docs/technical/combat-api.md section
 * 3), the same way HP already shows there &mdash; reads {@link CombatAttributes}, this module's
 * stable public handle for both, never {@code internal.CombatAttributes} directly.
 *
 * <p>Both attributes are {@code setSyncable(true)} (see {@code internal.CombatAttributes}'s own
 * registration), so the client already has a live copy of whatever the wearer/wielder is currently
 * modifying &mdash; Darius's Axe's shred, The Protean Mask's stacks, and so on show up here as they
 * change, with no extra sync of this module's own.
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

        AttributeInstance armor = target.getAttribute(CombatAttributes.armor());
        if (armor != null) {
            tooltip.add(Component.literal("Armor: " + (int) armor.getValue()));
        }

        AttributeInstance magicResist = target.getAttribute(CombatAttributes.magicResist());
        if (magicResist != null) {
            tooltip.add(Component.literal("Magic Resist: " + (int) magicResist.getValue()));
        }
    }
}
