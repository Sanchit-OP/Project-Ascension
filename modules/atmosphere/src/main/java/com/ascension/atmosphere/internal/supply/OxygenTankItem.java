package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A portable oxygen reserve.
 *
 * <p>Works from anywhere in the inventory &mdash; it does not have to be held. Holding a tank
 * in your hand to breathe would mean choosing between air and a pickaxe, which is friction
 * without a decision in it.
 *
 * <p><strong>Never refills on its own.</strong> That is the entire point of a tank as opposed to
 * lungs: lungs are the free reserve that tops itself up in breathable air, a tank is a resource
 * you plan around and take back to a station. Making both self-refill would collapse them into
 * one supply and delete the expedition loop.
 *
 * <p>Contents live in a data component, so a tank is readable by anything &mdash; a hopper
 * filter, another mod, a future suit &mdash; without touching this class.
 */
public final class OxygenTankItem extends Item {

    public OxygenTankItem(Properties properties) {
        // One per stack: each tank carries its own charge, and stacking would have to either
        // merge or discard it.
        super(properties.stacksTo(1));
    }

    public static boolean isTank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof OxygenTankItem;
    }

    /**
     * Capacity of this particular tank.
     *
     * <p>Takes the stack rather than reading a constant, so larger tanks later are a new item
     * and not a change to every call site.
     */
    public static int capacity(ItemStack stack) {
        return AtmosphereTuning.TANK_CAPACITY;
    }

    /**
     * Units currently stored.
     *
     * <p>Clamped on read as well as on write: a stack whose component was set by a command, a
     * datapack or another mod should not be able to hand out more air than the tank holds.
     */
    public static int units(ItemStack stack) {
        int stored = stack.getOrDefault(AtmosphereContent.OXYGEN_UNITS.get(), 0);
        return Mth.clamp(stored, 0, capacity(stack));
    }

    public static void setUnits(ItemStack stack, int units) {
        int clamped = Mth.clamp(units, 0, capacity(stack));
        if (clamped == 0) {
            // An empty tank carries no component at all, so it is indistinguishable from a
            // freshly crafted one.
            stack.remove(AtmosphereContent.OXYGEN_UNITS.get());
        } else {
            stack.set(AtmosphereContent.OXYGEN_UNITS.get(), clamped);
        }
    }

    /** A full tank, for the creative tab and for tests. */
    public static ItemStack filled() {
        ItemStack stack = new ItemStack(AtmosphereContent.OXYGEN_TANK.get());
        setUnits(stack, capacity(stack));
        return stack;
    }

    // --- display ------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(MAX_BAR_WIDTH * (float) units(stack) / capacity(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int seconds = units(stack) / AtmosphereTuning.BASE_UNITS_PER_SECOND;
        if (seconds <= SECONDS_CRITICAL) {
            return COLOUR_CRITICAL;
        }
        return seconds <= SECONDS_LOW ? COLOUR_LOW : COLOUR_OK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // Seconds, not units, for the same reason the HUD shows seconds: "5m 0s" is something a
        // player can plan a trip around.
        tooltip.add(Component.translatable(
                        "item.ascension_atmosphere.oxygen_tank.contents",
                        AtmosphereTuning.formatDuration(
                                units(stack) / AtmosphereTuning.BASE_UNITS_PER_SECOND),
                        AtmosphereTuning.formatDuration(
                                capacity(stack) / AtmosphereTuning.BASE_UNITS_PER_SECOND))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ascension_atmosphere.oxygen_tank.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Vanilla's durability bar is thirteen pixels wide. */
    private static final float MAX_BAR_WIDTH = 13.0f;

    /** Same thresholds and colours as the HUD, so the two never disagree about "low". */
    private static final int SECONDS_LOW = 30;
    private static final int SECONDS_CRITICAL = 10;
    private static final int COLOUR_OK = 0x43C5F0;
    private static final int COLOUR_LOW = 0xE0B33A;
    private static final int COLOUR_CRITICAL = 0xD84B3A;
}
