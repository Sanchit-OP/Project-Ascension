package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Re-applies worn and held items' modifiers when item stats change under them
 * (docs/technical/item-stats.md section 1, trap 5).
 *
 * <p>Vanilla only asks an item for its modifiers when its slot's item changes. After a
 * {@code /reload} that retunes a rate, worn armour would keep its old numbers until taken off. So
 * on a reload every online player's gear is re-applied. Mobs catch up the next time their gear
 * changes. {@link OnDatapackSyncEvent} fires after the data maps are applied (NeoForge applies them
 * on the tags update that precedes it), so the new numbers are the ones read.
 *
 * <p>{@link #reapply} updates modifiers in place rather than removing and re-adding them: a removal
 * would lower max health for a moment, and vanilla would cut the player's health down to it.
 */
public final class WornGear {

    private WornGear() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(WornGear::onDatapackSync);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // A player joining fires this too; HealthAcrossLogin re-applies their gear itself.
        if (event.getPlayer() != null) {
            return;
        }
        event.getPlayerList().getPlayers().forEach(WornGear::reapply);
    }

    /**
     * Brings an entity's item-stat modifiers in line with what its worn and held items give now:
     * every item's modifiers are added or updated, and any of ours no item gives any more are
     * removed. Vanilla's own bookkeeping is untouched, so its next equipment check still sees the
     * same items and does nothing.
     */
    public static void reapply(LivingEntity entity) {
        Set<ResourceLocation> given = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            stack.forEachModifier(slot, (attribute, modifier) -> {
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(modifier);
                    given.add(modifier.id());
                }
            });
        }

        for (CombatStat stat : CombatStats.all()) {
            AttributeInstance instance = entity.getAttribute(stat.attribute());
            if (instance == null) {
                continue;
            }

            for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
                if (ItemStatModifiers.isOurs(modifier.id()) && !given.contains(modifier.id())) {
                    instance.removeModifier(modifier.id());
                }
            }
        }
    }
}
