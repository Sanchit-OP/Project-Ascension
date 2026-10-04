package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.OxygenState;
import com.ascension.atmosphere.internal.OxygenTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Share-air rescue: right-click a teammate with an oxygen tank to breathe for them.
 *
 * <p>Decided at the M1.1 review (atmosphere-api.md, "Multiplayer rescue") over a downed-and-revive
 * state, and the reason {@code accept} exists on the API. Built here without it, though: the air
 * goes into the receiver's <em>lungs</em>, which are player state rather than an
 * {@code OxygenSource}.
 *
 * <p><strong>Lungs only, deliberately.</strong> A rescue is a breath, not a refill: it buys a
 * suffocating teammate their lung reserve (20s base, more with gear) and the time to get to air
 * or open their own tank. Topping up their <em>tanks</em> would make a teammate a refill station
 * on legs and blur the "tanks go back to a station" expedition loop the tank item is built around.
 *
 * <p>From the held tank, valve open or closed: sharing is not breathing, so neither the one-open
 * rule nor the pressurise delay applies to it. That does let a teammate skip your own valve's
 * pressurise delay for you &mdash; which is the co-op point, and is bounded by your lung capacity
 * and paid for 1:1 out of their tank.
 *
 * <p>Refuses, with a reason, rather than wasting air: when the receiver can already breathe where
 * they stand (lungs refill free there), when their lungs are full, or when the tank is empty.
 */
public final class AirSharing {

    private AirSharing() {
    }

    /**
     * How many units one share moves: as much as the tank holds, up to the receiver's empty lung
     * space. Never negative, even if a lung reserve sits above a capacity that just shrank.
     */
    static int unitsToShare(int tankUnits, int receiverLungUnits, int receiverLungCapacity) {
        int room = Math.max(0, receiverLungCapacity - receiverLungUnits);
        return Math.max(0, Math.min(tankUnits, room));
    }

    static void share(ServerPlayer giver, ItemStack tank, ServerPlayer receiver) {
        Component receiverName = receiver.getDisplayName();

        if (AtmosphereRegistry.query(receiver.serverLevel(), receiver.getEyePosition()).breathable()) {
            tell(giver, Component.translatable("item.ascension_atmosphere.oxygen_tank.share.can_breathe", receiverName),
                    ChatFormatting.YELLOW);
            return;
        }

        TankOxygenSource source = new TankOxygenSource(tank);
        OxygenState lungs = receiver.getData(AtmosphereAttachments.OXYGEN);
        int units = unitsToShare(source.available(), lungs.lungUnits(), OxygenTracker.lungCapacity(receiver));
        if (units == 0) {
            String key = source.available() == 0
                    ? "item.ascension_atmosphere.oxygen_tank.share.empty"
                    : "item.ascension_atmosphere.oxygen_tank.share.lungs_full";
            tell(giver, Component.translatable(key, receiverName), ChatFormatting.YELLOW);
            return;
        }

        int moved = source.consume(units);
        lungs.setLungUnits(lungs.lungUnits() + moved);

        // Round up, so a small top-up never reads as "0s".
        String duration = AtmosphereTuning.formatDuration(
                (moved + AtmosphereTuning.BASE_UNITS_PER_SECOND - 1) / AtmosphereTuning.BASE_UNITS_PER_SECOND);
        receiver.level().playSound(null, receiver.blockPosition(),
                SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 1.3f);
        tell(giver, Component.translatable("item.ascension_atmosphere.oxygen_tank.share.given", duration, receiverName),
                ChatFormatting.AQUA);
        tell(receiver, Component.translatable("item.ascension_atmosphere.oxygen_tank.share.received",
                giver.getDisplayName(), duration), ChatFormatting.AQUA);

        // Both totals just changed; resync both bars on the next pass rather than the next reconcile.
        OxygenTracker.invalidate(giver);
        OxygenTracker.invalidate(receiver);
    }

    private static void tell(ServerPlayer player, Component message, ChatFormatting colour) {
        player.displayClientMessage(message.copy().withStyle(colour), true);
    }
}
