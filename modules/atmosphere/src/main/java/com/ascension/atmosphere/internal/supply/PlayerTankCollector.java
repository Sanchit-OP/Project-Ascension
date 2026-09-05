package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.api.OxygenSource;
import com.ascension.atmosphere.api.OxygenSourceCollector;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Finds tanks in a player's inventory.
 *
 * <p>The first real {@link OxygenSourceCollector}, and deliberately the same mechanism a third
 * party would use: this module reads the player's own inventory and nothing else, so a curio
 * slot, a backpack or a suit module contributes tanks by registering its own collector rather
 * than by us learning about it (ADR-0003 rule 3).
 *
 * <p><strong>Cost.</strong> One pass over the 41 inventory slots per accounting pass, per
 * player, plus one small object per tank found. That only happens off the tracker's fast path
 * &mdash; a player breathing normally with full lungs never gets here &mdash; so the steady
 * state on Earth is unchanged. Measured again at M1.8.
 */
public final class PlayerTankCollector implements OxygenSourceCollector {

    @Override
    public void collect(ServerPlayer player, Consumer<OxygenSource> sink) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (OxygenTankItem.isTank(stack)) {
                sink.accept(new TankOxygenSource(stack));
            }
        }
    }
}
