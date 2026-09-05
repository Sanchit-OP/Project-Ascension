package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * How many tanks a player may carry, and how many may be open.
 *
 * <p>Separate from {@link OxygenTankItem} because these are rules about a <em>player</em>, not
 * behaviour of an item, and because the Curios compat jar will need to change them without
 * touching the item: once a dedicated tank slot exists, the inventory allowance drops and the
 * open tank is the equipped one.
 *
 * <p><strong>What this deliberately does not do.</strong> It counts the player's own 41 slots.
 * A shulker box in the inventory, a backpack from another mod, a chest at their feet &mdash;
 * none of those are visible from here, and chasing each one would be a separate integration per
 * mod that a single vanilla shulker box defeats anyway. The pressurise delay on a freshly opened
 * valve is what actually makes stockpiling useless, precisely because it does not care where the
 * spares were kept.
 */
public final class TankRules {

    private TankRules() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(TankRules::onPickup);
    }

    /**
     * Refuse to pick up a tank the player has no room for.
     *
     * <p>Without this the sweep and the player fight each other: {@link #enforce} drops the
     * third tank, the player walks over it and picks it straight back up, and a second later it
     * is on the floor again. Declining the pickup makes the cap something you bump into once,
     * rather than an item that will not stay in your bag and will not stay on the ground either.
     */
    private static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemStack stack = event.getItemEntity().getItem();
        if (!OxygenTankItem.isTank(stack)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (count(player) < AtmosphereTuning.MAX_TANKS_CARRIED) {
            return;
        }
        event.setCanPickup(TriState.FALSE);
        // Throttled: the event fires every tick the player stands on the item, and an action-bar
        // message reprinted twenty times a second is not a message, it is a strobe.
        if (player.tickCount % 20 == 0) {
            player.displayClientMessage(Component.translatable(
                            "item.ascension_atmosphere.oxygen_tank.too_many",
                            AtmosphereTuning.MAX_TANKS_CARRIED)
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /** Tanks in the player's own inventory. */
    public static int count(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        int found = 0;
        for (int slot = 0; slot < size; slot++) {
            if (OxygenTankItem.isTank(inventory.getItem(slot))) {
                found++;
            }
        }
        return found;
    }

    /**
     * Bring a player's tanks back within the rules.
     *
     * <p>Two invariants, enforced in one pass: at most {@link AtmosphereTuning#MAX_TANKS_CARRIED}
     * tanks in the inventory, and at most one of them open. Extras are dropped at the player's
     * feet rather than deleted &mdash; a mod that silently eats items is a mod nobody trusts with
     * their inventory.
     *
     * <p>Idempotent, so it is safe to run from anywhere and safe to run often.
     */
    public static void enforce(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        TankCarrySweep sweep = new TankCarrySweep();
        boolean dropped = false;

        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!OxygenTankItem.isTank(stack)) {
                continue;
            }

            switch (sweep.next(OxygenTankItem.isOpen(stack))) {
                case DROP -> {
                    inventory.setItem(slot, ItemStack.EMPTY);
                    player.drop(stack, false);
                    dropped = true;
                }
                case KEEP_CLOSED -> OxygenTankItem.setOpen(stack, false);
                case KEEP -> { }
            }
        }

        if (dropped) {
            player.displayClientMessage(Component.translatable(
                            "item.ascension_atmosphere.oxygen_tank.too_many",
                            AtmosphereTuning.MAX_TANKS_CARRIED)
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /**
     * The tank currently feeding this player, or empty if none is open.
     *
     * <p>First open tank wins, and {@link #enforce} guarantees there is at most one.
     */
    public static ItemStack openTank(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (OxygenTankItem.isTank(stack) && OxygenTankItem.isOpen(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Close every tank a player is carrying. Used when one is opened, so only one ever is. */
    public static void closeAllExcept(ServerPlayer player, ItemStack keep) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != keep && OxygenTankItem.isTank(stack)) {
                OxygenTankItem.setOpen(stack, false);
            }
        }
    }
}
