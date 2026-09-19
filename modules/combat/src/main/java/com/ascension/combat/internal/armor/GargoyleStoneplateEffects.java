package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gargoyle Stoneplate's identity (docs/technical/combat-api.md section 6): flat armor and magic
 * resist while worn; the active grants a shield scaling with the number of nearby living
 * entities, on a cooldown.
 *
 * <p>Triggered by {@link com.ascension.combat.internal.net.ActivateArmorAbilityPayload}, not a
 * right-click &mdash; see that class and {@link com.ascension.combat.client.CombatKeybinds} for
 * why worn armor needed a new input path. {@link #tryActivate} is the entry point the network
 * handler calls; it re-checks the wearer still has this item equipped rather than trusting the
 * client, since the packet carries no information about which item to activate.
 *
 * <p>The shield reuses vanilla absorption, same as every other Shield/Absorb consumer in this
 * catalog (combat-api.md section 5). Cooldown is tracked against the item itself via {@link
 * net.minecraft.world.item.ItemCooldowns}, the same as a held weapon's active would be, even
 * though this item is never held.
 */
public final class GargoyleStoneplateEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final double ARMOR_BONUS = 2.0;

    private static final double MAGIC_RESIST_BONUS = 2.0;
    private static final float SHIELD_PER_NEARBY_ENEMY = 4.0f;
    private static final float MAX_SHIELD = 40.0f;
    private static final double SCAN_RADIUS = 12.0;
    private static final int COOLDOWN_TICKS = 400;

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "gargoyle_stoneplate_armor");
    private static final ResourceLocation MAGIC_RESIST_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "gargoyle_stoneplate_magic_resist");

    private GargoyleStoneplateEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(GargoyleStoneplateEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean wearing = player.getItemBySlot(EquipmentSlot.CHEST).is(CombatContent.GARGOYLE_STONEPLATE.get());
        applyPassive(player, wearing);
    }

    private static void applyPassive(ServerPlayer player, boolean wearing) {
        AttributeInstance armor = player.getAttribute(CombatAttributes.ARMOR);
        AttributeInstance magicResist = player.getAttribute(CombatAttributes.MAGIC_RESIST);

        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
        }
        if (magicResist != null) {
            magicResist.removeModifier(MAGIC_RESIST_MODIFIER_ID);
        }

        if (!wearing) {
            return;
        }

        if (armor != null) {
            armor.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
        if (magicResist != null) {
            magicResist.addTransientModifier(new AttributeModifier(
                    MAGIC_RESIST_MODIFIER_ID, MAGIC_RESIST_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void tryActivate(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(CombatContent.GARGOYLE_STONEPLATE.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.GARGOYLE_STONEPLATE.get())) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long nearbyEnemies = level
                .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SCAN_RADIUS))
                .stream()
                .filter(entity -> entity != player && entity.isAlive())
                .count();

        float shield = Math.min(MAX_SHIELD, nearbyEnemies * SHIELD_PER_NEARBY_ENEMY);
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), shield));
        player.getCooldowns().addCooldown(CombatContent.GARGOYLE_STONEPLATE.get(), COOLDOWN_TICKS);
    }
}
