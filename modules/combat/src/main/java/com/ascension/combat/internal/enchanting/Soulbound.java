package com.ascension.combat.internal.enchanting;

import com.ascension.combat.AscensionCombat;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Soulbound enchantment (docs/progression/enchantments.md): an item with it is not lost when its owner
 * dies, but comes back in the same inventory slot after the respawn.
 *
 * <p>The enchantment itself is data ({@code data/ascension_combat/enchantment/soulbound.json}); vanilla has
 * no data effect for "keep on death", so this is the small piece of code behind it. No mixin: on death,
 * before vanilla empties the inventory, each Soulbound stack moves into a saved attachment
 * ({@link KeptItems}), which is copied onto the new player and unloaded into their inventory on respawn.
 *
 * <p>It does nothing when the {@code keepInventory} rule is on (everything is kept anyway) and runs last
 * among death listeners, so a death another mod cancels (a totem) takes nothing out of the inventory.
 */
public final class Soulbound {

    public static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "soulbound"));

    private Soulbound() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, Soulbound::onDeath);
        gameBus.addListener(Soulbound::onRespawn);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        Holder<Enchantment> soulbound = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ENCHANTMENT).orElse(null);
        if (soulbound == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        KeptItems kept = player.getData(EnchantingAttachments.KEPT_ITEMS);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(soulbound, stack) > 0) {
                kept = kept.with(new KeptItems.Kept(slot, stack.copy()));
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        player.setData(EnchantingAttachments.KEPT_ITEMS, kept);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        List<KeptItems.Kept> kept = player.getData(EnchantingAttachments.KEPT_ITEMS).items();
        if (kept.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (KeptItems.Kept item : kept) {
            if (item.slot() < inventory.getContainerSize() && inventory.getItem(item.slot()).isEmpty()) {
                inventory.setItem(item.slot(), item.stack());
            } else {
                inventory.placeItemBackInInventory(item.stack());
            }
        }
        player.removeData(EnchantingAttachments.KEPT_ITEMS);
    }
}
