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

    /**
     * True while the valve has been opened but the tank has not come up to pressure yet.
     *
     * <p>A pressurising tank reports zero available and refuses to be drawn from, but keeps its
     * full {@link #capacity()} and still accepts a refill. Keeping the capacity matters for feel:
     * the HUD bar stays the same length and simply empties, so the player sees "you have almost
     * no air" rather than the whole bar resizing under them.
     */
    private final boolean pressurising;

    public TankOxygenSource(ItemStack stack) {
        this(stack, false);
    }

    public TankOxygenSource(ItemStack stack, boolean pressurising) {
        this.stack = stack;
        this.pressurising = pressurising;
    }

    @Override
    public int available() {
        return pressurising ? 0 : OxygenTankItem.units(stack);
    }

    @Override
    public int capacity() {
        return OxygenTankItem.capacity(stack);
    }

    @Override
    public int consume(int units) {
        if (units <= 0 || pressurising) {
            return 0;
        }
        int taken = Math.min(units, available());
        if (taken > 0) {
            OxygenTankItem.setUnits(stack, available() - taken);
        }
        return taken;
    }

    /**
     * Filling works even while pressurising &mdash; a station should never refuse a tank because
     * of what its valve happens to be doing.
     */
    @Override
    public int accept(int units) {
        if (units <= 0) {
            return 0;
        }
        int stored = OxygenTankItem.units(stack);
        int added = Math.min(units, capacity() - stored);
        if (added > 0) {
            OxygenTankItem.setUnits(stack, stored + added);
        }
        return added;
    }

    @Override
    public int drawOrder() {
        return AtmosphereTuning.TANK_DRAW_ORDER;
    }
}
