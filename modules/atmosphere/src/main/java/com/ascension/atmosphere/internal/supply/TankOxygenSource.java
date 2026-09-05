package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.api.OxygenSource;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import net.minecraft.world.item.ItemStack;

/**
 * One tank stack, seen as an {@link OxygenSource}.
 *
 * <p>A thin view, not a copy: reads and writes go straight to the stack's component, so there is
 * no second copy of the number to fall out of step with the item a player is looking at.
 *
 * <p>Short-lived by contract. {@code OxygenSourceCollector} states that sources handed to the
 * sink are used immediately and not retained, which is what makes holding an {@link ItemStack}
 * here safe: nothing long-lived ever points at it (ADR-0007 rule 1).
 */
public final class TankOxygenSource implements OxygenSource {

    private final ItemStack stack;

    public TankOxygenSource(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int available() {
        return OxygenTankItem.units(stack);
    }

    @Override
    public int capacity() {
        return OxygenTankItem.capacity(stack);
    }

    @Override
    public int consume(int units) {
        if (units <= 0) {
            return 0;
        }
        int taken = Math.min(units, available());
        if (taken > 0) {
            OxygenTankItem.setUnits(stack, available() - taken);
        }
        return taken;
    }

    @Override
    public int accept(int units) {
        if (units <= 0) {
            return 0;
        }
        int added = Math.min(units, capacity() - available());
        if (added > 0) {
            OxygenTankItem.setUnits(stack, available() + added);
        }
        return added;
    }

    @Override
    public int drawOrder() {
        return AtmosphereTuning.TANK_DRAW_ORDER;
    }
}
