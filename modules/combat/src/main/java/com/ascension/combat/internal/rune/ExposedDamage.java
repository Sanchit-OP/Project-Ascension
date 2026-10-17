package com.ascension.combat.internal.rune;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The payoff of Exposed ({@link RuneAttachments#EXPOSED}): while it lasts, whatever hits the
 * target hits for the exposure's own strength more, from anyone, a player, a pet, a trap.
 *
 * <p>Runs late in the event ({@link EventPriority#LOW}) so it multiplies the damage after the
 * flat bonuses (Relentless Strikes, Cadence's own third hit) have been added: Exposed is "takes
 * more damage", not "takes more of the base".
 */
public final class ExposedDamage {

    private ExposedDamage() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, ExposedDamage::onIncoming);
    }

    private static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getAmount() <= 0.0f || target.level().isClientSide) {
            return;
        }

        float amplify = target.getData(RuneAttachments.EXPOSED.get()).amplify(target.level().getGameTime());
        if (amplify > 0.0f) {
            event.setAmount(event.getAmount() * (1.0f + amplify));
        }
    }
}
