package com.ascension.combat.internal.enchanting;

import com.ascension.combat.AscensionCombat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Smite and Bane of Arthropods as Attack Damage, for the hit that meets what they name.
 *
 * <p>Vanilla gives them a flat {@code minecraft:damage} effect added to the swing, which the legendary items cannot see: their abilities
 * scale off the wielder's Attack Damage (docs/progression/balance.md section 4), and Sharpness already is Attack Damage (an attribute).
 * Smite and Bane cannot be a plain attribute, because they apply only against the undead or arthropods. So on the swing's first event, before
 * vanilla reads the Attack Damage for the hit, the attacker gets one temporary Attack Damage modifier when the target is the kind the
 * main-hand enchantment names. It is then in the swing's own damage (so the flat effect is removed from the enchantments' data, or it
 * would count twice) and in every ability that reads the Attack Damage on that hit.
 *
 * <p>The modifier is removed at the end of the same tick (the swing happens in the packet handling that starts it), so it is never on
 * the sheet longer than one tick. Numbers are vanilla's: +2.5 a level (V: +12.5), kept at Sanchit's word (2026-10-22): against the
 * undead, which is most of what the campaign throws at a player, Smite is plainly the better pick.
 *
 * <p>Teardown (ADR-0007): the only state is a set of player ids, emptied as each is handled, on logout and on server stop.
 */
public final class SmiteAndBane {

    /** Attack Damage per level, as vanilla's own Smite and Bane of Arthropods. */
    public static final double PER_LEVEL = 2.5;

    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "smite_or_bane_hit");
    private static final TagKey<EntityType<?>> SENSITIVE_TO_SMITE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("sensitive_to_smite"));
    private static final TagKey<EntityType<?>> SENSITIVE_TO_BANE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("sensitive_to_bane_of_arthropods"));

    /** Players whose Attack Damage carries the modifier until their tick ends. Ids only, never the player. */
    private static final Set<UUID> pending = new HashSet<>();

    private SmiteAndBane() {
    }

    public static void register(IEventBus gameBus) {
        // Highest, so the modifier is in before any other listener on the swing reads the Attack Damage.
        gameBus.addListener(EventPriority.HIGHEST, SmiteAndBane::onAttack);
        gameBus.addListener(SmiteAndBane::onPlayerTick);
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> pending.remove(event.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent event) -> pending.clear());
    }

    /** The Attack Damage the main-hand weapon's Smite or Bane adds against this target: 0 for anything else. */
    public static double bonus(Player player, ItemStack weapon, Entity target) {
        if (weapon.isEmpty() || !weapon.isEnchanted()) {
            return 0.0;
        }
        var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        double bonus = 0.0;
        if (target.getType().is(SENSITIVE_TO_SMITE)) {
            bonus += PER_LEVEL * EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SMITE), weapon);
        }
        if (target.getType().is(SENSITIVE_TO_BANE)) {
            bonus += PER_LEVEL * EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), weapon);
        }
        return bonus;
    }

    private static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        // One modifier at a time: a sweep or a second target in the same tick starts from nothing.
        attackDamage.removeModifier(MODIFIER);
        double bonus = bonus(player, player.getMainHandItem(), event.getTarget());
        if (bonus <= 0.0) {
            return;
        }
        attackDamage.addTransientModifier(new AttributeModifier(MODIFIER, bonus, AttributeModifier.Operation.ADD_VALUE));
        pending.add(player.getUUID());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (pending.isEmpty() || !pending.remove(event.getEntity().getUUID())) {
            return;
        }
        AttributeInstance attackDamage = event.getEntity().getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(MODIFIER);
        }
    }
}
