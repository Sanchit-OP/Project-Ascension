package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.net.MagicHitPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells a player when a magic hit has landed on them, so their screen edge can show it
 * (docs/technical/damage-kinds.md). Reads {@link LivingDamageEvent.Post}, the damage that really landed:
 * a hit a shield swallowed or an invulnerable player shrugged off shows nothing.
 *
 * <p>Players only, and only the hit player is told. Holds no state, so there is nothing to tear down.
 */
public final class MagicHitNotifier {

    private MagicHitNotifier() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MagicHitNotifier::onDamagePost);
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        // A machine's stand-in player (a deployer, say) has nobody on the other end to show it to.
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        float landed = event.getNewDamage();
        if (landed <= 0.0f || !DamageKinds.isMagic(event.getSource())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new MagicHitPayload(MagicHitPulse.strengthFor(landed, player.getMaxHealth())));
    }
}
