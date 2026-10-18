package com.ascension.combat.internal.mechanics;

import com.ascension.core.api.TeamRegistry;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Teammates cannot hurt each other (docs/progression/teams.md section 5, ADR-0023), unless the
 * server turns {@link TeamConfig#friendlyFire} on.
 *
 * <p>Runs first, ahead of the mitigation formula and every on-hit effect, so a cancelled hit starts
 * no combat timer, heals no one through Omnivamp and triggers no rune. The attacker is the player
 * behind the damage: the one who swung, the shooter of an arrow, the lighter of the fuse, or the
 * owner of a tamed animal that bit. Hurting yourself is not covered, and neither is hurting a
 * stranger: only a player and the players of their own team.
 *
 * <p>With no team mod, everyone is on {@link TeamRegistry#EVERYONE}, a stand-in and not a group of
 * friends, so it is never protected and a server's pvp stays its own business.
 */
public final class TeamFire {

    private TeamFire() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGHEST, TeamFire::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || TeamConfig.INSTANCE.friendlyFire.get()) {
            return;
        }

        UUID attacker = playerBehind(event.getSource());
        if (attacker == null || attacker.equals(victim.getUUID())) {
            return;
        }

        if (protects(TeamRegistry.teamOf(attacker), TeamRegistry.teamOf(victim.getUUID()))) {
            event.setCanceled(true);
        }
    }

    /** Whether two players' teams are one real team, which is the only thing friendly fire protects. */
    static boolean protects(UUID attackerTeam, UUID victimTeam) {
        return attackerTeam.equals(victimTeam) && !attackerTeam.equals(TeamRegistry.EVERYONE);
    }

    /** The player whose doing this damage was, if any: the attacker, or the owner of the animal that attacked. */
    private static UUID playerBehind(DamageSource source) {
        Entity cause = source.getEntity();
        if (cause instanceof ServerPlayer player) {
            return player.getUUID();
        }
        if (cause instanceof TamableAnimal pet) {
            return pet.getOwnerUUID();
        }
        return null;
    }
}
