package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.internal.net.PlanetWeatherSyncPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ticks each surface's {@link PlanetWeatherState} and tells whoever needs to know when it flips.
 *
 * <p>Only levels {@link PlanetWeathers} actually has an entry for pay any cost here &mdash; a
 * planet with no {@code weather} block is one map lookup per level tick, not a scan of anything.
 */
public final class PlanetWeatherMechanics {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlanetWeatherMechanics::onLevelTick);
        NeoForge.EVENT_BUS.addListener(PlanetWeatherMechanics::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PlanetWeatherMechanics::onPlayerLoggedIn);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Planet.Weather weather = PlanetWeathers.get(level.dimension());
        if (weather == null) {
            return;
        }
        PlanetWeatherState state = level.getData(SpaceAttachments.WEATHER_STATE);
        if (state.tick(weather, level.getRandom())) {
            PacketDistributor.sendToPlayersInDimension(level, new PlanetWeatherSyncPayload(state.active()));
        }
    }

    /**
     * The two moments a client's weather state can otherwise go stale: arriving somewhere new
     * (dimension change) and connecting in the first place (login, which fires no dimension-change
     * event of its own). Both resolve and send the truth for wherever the player actually now is,
     * including an explicit {@code false} for a surface with no weather at all &mdash; see
     * {@link PlanetWeatherSyncPayload}'s javadoc for why that matters.
     */
    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendCurrentState(player, event.getTo());
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendCurrentState(player, player.level().dimension());
        }
    }

    private static void sendCurrentState(ServerPlayer player, ResourceKey<Level> dimension) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Planet.Weather weather = PlanetWeathers.get(dimension);
        boolean active = false;
        if (weather != null) {
            ServerLevel level = server.getLevel(dimension);
            active = level != null && level.getData(SpaceAttachments.WEATHER_STATE).active();
        }
        PacketDistributor.sendToPlayer(player, new PlanetWeatherSyncPayload(active));
    }

    private PlanetWeatherMechanics() {
    }
}
