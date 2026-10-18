package com.ascension.combat.internal.growth;

import com.ascension.combat.internal.rune.RuneUnlocks;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Every moment a player's growth can change, and nothing else: growth never ticks (ADR-0007).
 *
 * <ul>
 *   <li>An advancement earned that is a milestone: a shared one is recorded for the team and
 *       awarded to every other player online; the player's growth is refreshed and they are told
 *       what they gained.</li>
 *   <li>An advancement revoked by command: refreshed, so its points go.</li>
 *   <li>Login: the team's shared milestones the player is missing are awarded (a late joiner
 *       arrives with the team's points), then refreshed.</li>
 *   <li>Respawn: a new body carries only base values, so growth is applied again.</li>
 *   <li>{@code /reload}: every player refreshed against the reloaded advancements.</li>
 * </ul>
 *
 * <p>Wherever a team milestone is gained or lost, the rune page is told too: its slots open with
 * the milestone count ({@link RuneUnlocks}).
 */
final class GrowthEvents {

    private GrowthEvents() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(GrowthEvents::onAdvancementEarned);
        gameBus.addListener(GrowthEvents::onAdvancementProgress);
        gameBus.addListener(GrowthEvents::onLoggedIn);
        gameBus.addListener(GrowthEvents::onRespawn);
        gameBus.addListener(GrowthEvents::onDatapackSync);
        gameBus.addListener(GrowthEvents::onServerStarted);
    }

    private static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.server;
        AdvancementHolder advancement = event.getAdvancement();
        List<GrowthMilestone> worth = PlayerGrowth.milestonesFor(server, advancement.id());
        if (worth.isEmpty()) {
            return;
        }

        // The first player to earn a shared milestone passes it on. Each award fires that player's
        // own event, which lands here, finds the team already has it, and stops.
        boolean shared = worth.stream().anyMatch(GrowthMilestone::shared);
        if (shared && PlayerGrowth.team(server).add(advancement.id())) {
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other != player) {
                    PlayerGrowth.awardIfMissing(other, advancement);
                }
            }
        }

        PlayerGrowth.refresh(player, true);
        if (shared) {
            RuneUnlocks.milestonesChanged(player);
        }
        int points = worth.stream().mapToInt(GrowthMilestone::points).sum();
        if (points > 0) {
            player.sendSystemMessage(Component.translatable(
                    "growth.ascension_combat.points_gained", points, PlayerGrowth.title(advancement)));
        }
    }

    private static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        if (event.getProgressType() == AdvancementEvent.AdvancementProgressEvent.ProgressType.REVOKE
                && event.getEntity() instanceof ServerPlayer player
                && !PlayerGrowth.milestonesFor(player.server, event.getAdvancement().id()).isEmpty()) {
            PlayerGrowth.refresh(player, false);
            RuneUnlocks.milestonesChanged(player);
        }
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (ResourceLocation id : PlayerGrowth.team(player.server).earned()) {
            AdvancementHolder advancement = player.server.getAdvancements().get(id);
            if (advancement != null) {
                PlayerGrowth.awardIfMissing(player, advancement);
            }
        }
        PlayerGrowth.refresh(player, false);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Healing to full is not done here: gear and runes are still being put back at this
            // point, so the stat system does it last (HealthAcrossLogin).
            PlayerGrowth.refresh(player, false);
        }
    }

    /** Fires for one player at login (handled there) and for everyone, player null, after /reload. */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        GrowthDataCheck.run(event.getPlayerList().getServer());
        event.getPlayerList().getPlayers().forEach(player -> {
            PlayerGrowth.refresh(player, false);
            RuneUnlocks.milestonesChanged(player);
        });
    }

    private static void onServerStarted(ServerStartedEvent event) {
        GrowthDataCheck.run(event.getServer());
    }
}
