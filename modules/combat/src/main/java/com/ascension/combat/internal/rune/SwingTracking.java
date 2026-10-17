package com.ascension.combat.internal.rune;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * Notes each melee swing for {@link SwingMarks}: when it begins, whether it is fully charged; when
 * the critical-hit check runs, whether it crit.
 *
 * <p>The crit is read at the lowest priority, after every rune that changes it (Pounce raises a
 * crit's strength, Riposte makes the next hit one), so what is marked is what lands.
 */
public final class SwingTracking {

    /** Vanilla's own line between a full swing and a partial one. */
    public static final float FULL_CHARGE = 0.9f;

    private SwingTracking() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SwingTracking::onAttack);
        bus.addListener(EventPriority.LOWEST, SwingTracking::onCriticalHit);
    }

    private static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(RuneAttachments.SWING.get()).swing(event.getTarget().getUUID(), player.level().getGameTime(),
                    player.getAttackStrengthScale(0.5f) >= FULL_CHARGE);
        }
    }

    private static void onCriticalHit(CriticalHitEvent event) {
        if (event.isCriticalHit() && event.getEntity() instanceof ServerPlayer player) {
            player.getData(RuneAttachments.SWING.get()).markCrit(event.getTarget().getUUID(), player.level().getGameTime());
        }
    }
}
