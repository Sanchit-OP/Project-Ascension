package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.OxygenTracker;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A portable oxygen reserve, with a valve.
 *
 * <p><strong>Only an open tank feeds you.</strong> A closed one is inert: it supplies nothing,
 * drains nothing, and is simply cargo. That single rule does three jobs at once &mdash; it stops
 * a tank quietly emptying itself because you swam across a river, it makes "stow it to save it"
 * a real action, and it gives the Curios slot something to mean, since equipping a tank there is
 * just another way of opening the valve.
 *
 * <p><strong>Opening a valve is not instant.</strong> A freshly opened tank spends
 * {@link AtmosphereTuning#TANK_PRESSURISE_TICKS} doing nothing at all. That is the part of the
 * design that cannot be dodged: capping the inventory does nothing about a shulker box full of
 * spares, but a swap that costs three seconds inside a failure window does not care where the
 * spares came from.
 *
 * <p><strong>Never refills on its own.</strong> Lungs are the free reserve that tops itself up in
 * breathable air; a tank is a resource you plan around and take back to a station. Making both
 * self-refill would collapse them into one supply and delete the expedition loop.
 *
 * <p>Contents and valve state both live in data components, so a tank is readable by anything
 * &mdash; a hopper filter, another mod, a future suit &mdash; without touching this class.
 */
public final class OxygenTankItem extends Item {

    public OxygenTankItem(Properties properties) {
        // One per stack: each tank carries its own charge and valve state, and stacking would
        // have to either merge them or throw them away.
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

    public static boolean isOpen(ItemStack stack) {
        return stack.getOrDefault(AtmosphereContent.TANK_OPEN.get(), Boolean.FALSE);
    }

    /**
     * Set the valve.
     *
     * <p>Does not enforce the one-open rule or start the pressurise timer on its own &mdash; both
     * of those need the player, and this is called from places that only have a stack. See
     * {@link #toggleValve} and {@link TankRules}.
     */
    public static void setOpen(ItemStack stack, boolean open) {
        if (open) {
            stack.set(AtmosphereContent.TANK_OPEN.get(), Boolean.TRUE);
        } else {
            stack.remove(AtmosphereContent.TANK_OPEN.get());
        }
    }

    /** A full, closed tank, for the creative tab and for tests. */
    public static ItemStack filled() {
        ItemStack stack = new ItemStack(AtmosphereContent.OXYGEN_TANK.get());
        setUnits(stack, capacity(stack));
        return stack;
    }

    // --- the valve ----------------------------------------------------------

    /**
     * Right-click, in the air or at a block, opens or closes the valve.
     *
     * <p>Both entry points, because right-clicking while looking at the ground is how people
     * actually use items, and that path never reaches {@link #use}. Blocks with an interaction of
     * their own still win &mdash; including the refill station, which is why filling a tank does
     * not also flip its valve.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            toggleValve(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            toggleValve(serverPlayer, context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /**
     * Flip one tank's valve, keeping every rule that depends on the player.
     *
     * <p>Opening closes every other tank they carry and starts the pressurise timer. Closing
     * clears the timer, so shutting a valve is always instant &mdash; the cost is on the way in,
     * not on the way out.
     */
    public static void toggleValve(ServerPlayer player, ItemStack stack) {
        if (!isTank(stack)) {
            return;
        }
        boolean opening = !isOpen(stack);
        var state = player.getData(AtmosphereAttachments.OXYGEN);

        if (opening) {
            TankRules.closeAllExcept(player, stack);
            setOpen(stack, true);
            state.setPressurisingTicks(AtmosphereTuning.TANK_PRESSURISE_TICKS);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6f, 1.6f);
            player.displayClientMessage(Component.translatable(
                            "item.ascension_atmosphere.oxygen_tank.opened",
                            AtmosphereTuning.TANK_PRESSURISE_TICKS / 20)
                    .withStyle(ChatFormatting.AQUA), true);
        } else {
            setOpen(stack, false);
            state.setPressurisingTicks(0);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6f, 1.2f);
            player.displayClientMessage(
                    Component.translatable("item.ascension_atmosphere.oxygen_tank.closed")
                            .withStyle(ChatFormatting.GRAY), true);
        }
        OxygenTracker.invalidate(player);
    }

    /**
     * Keep the carry rules true.
     *
     * <p>Hung off the item rather than a player tick so it costs nothing at all for the players
     * who are not carrying a tank &mdash; which, for the whole first act of the campaign, is
     * everyone. Rate-limited on top of that, because a player is allowed to pick up a third tank
     * and be told about it a moment later; they are not allowed to keep it.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId,
                              boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (level.getGameTime() % AtmosphereTuning.TANK_SWEEP_INTERVAL_TICKS != 0) {
            return;
        }
        TankRules.enforce(player);
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
        // A closed tank reads grey, so open versus closed is legible from the hotbar without
        // hovering. The state that decides whether you are breathing should not need a tooltip.
        if (!isOpen(stack)) {
            return COLOUR_CLOSED;
        }
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
        tooltip.add(isOpen(stack)
                ? Component.translatable("item.ascension_atmosphere.oxygen_tank.valve_open")
                        .withStyle(ChatFormatting.AQUA)
                : Component.translatable("item.ascension_atmosphere.oxygen_tank.valve_closed")
                        .withStyle(ChatFormatting.DARK_GRAY));
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
    private static final int COLOUR_CLOSED = 0x6A6A6A;
}
