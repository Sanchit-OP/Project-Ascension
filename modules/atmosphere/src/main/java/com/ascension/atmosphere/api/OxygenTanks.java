package com.ascension.atmosphere.api;

import com.ascension.atmosphere.internal.supply.OxygenTankItem;
import com.ascension.atmosphere.internal.supply.TankOxygenSource;
import net.minecraft.world.item.ItemStack;

/**
 * Lets another mod treat a stack as a tank without reaching into this module's internals: a worn slot, a suit
 * module or a backpack that wants to breathe from a tank it holds.
 *
 * <p>Pair it with {@link AtmosphereRegistry#register(net.minecraft.resources.ResourceLocation, OxygenSourceCollector)}:
 * the collector finds the stack, {@link #asSource} turns it into a source the accounting pass can draw from and the
 * refill machines can fill. A tank given this way is always treated as open; it is the caller's slot that decides
 * whether it counts, which is what makes a one-tank worn slot one open tank.
 */
public final class OxygenTanks {

    private OxygenTanks() {
    }

    /** True if {@code stack} is one of our tanks (oxygen or helium). */
    public static boolean isTank(ItemStack stack) {
        return OxygenTankItem.isTank(stack);
    }

    /**
     * A view of the tank's charge, reading and writing the stack itself. Short-lived: use it and drop it, do not
     * keep it (ADR-0007). Only call it with a stack for which {@link #isTank} is true.
     */
    public static OxygenSource asSource(ItemStack stack) {
        return new TankOxygenSource(stack);
    }
}
