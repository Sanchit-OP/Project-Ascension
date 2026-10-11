package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Doses the players on a hazardous surface: the Nether's rot (ADR-0017).
 *
 * <p>Once a second, and only on a surface whose planet has a hazard ({@link PlanetSurfaces});
 * anywhere else it is one modulo a tick and one map lookup a second. The check walks that level's players and their four
 * armour slots, never blocks or chunks.
 */
public final class PlanetHazardMechanics {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlanetHazardMechanics::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % HazardDoses.CHECK_INTERVAL != 0) {
            return;
        }
        Planet.Hazard hazard = PlanetSurfaces.hazardOn(level.dimension());
        if (hazard == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            dose(player, hazard);
        }
    }

    private static void dose(ServerPlayer player, Planet.Hazard hazard) {
        MobEffectInstance current = player.getEffect(hazard.effect());
        HazardDoses.Dose dose = HazardDoses.next(
                player.isCreative() || player.isSpectator() || !player.isAlive(),
                warded(player, hazard.ward()),
                current != null,
                current == null ? 0 : current.getDuration());
        if (dose == HazardDoses.Dose.NONE) {
            return;
        }
        player.addEffect(new MobEffectInstance(hazard.effect(), hazard.duration(), hazard.amplifier()));
        if (dose == HazardDoses.Dose.START) {
            hazard.message().ifPresent(message -> player.displayClientMessage(message, true));
        }
    }

    /** Any one worn armour piece the ward accepts is enough. */
    static boolean warded(ServerPlayer player, Planet.Ward ward) {
        for (ItemStack worn : player.getArmorSlots()) {
            if (worn.isEmpty()) {
                continue;
            }
            if (ward.items().isPresent() && worn.is(ward.items().get())) {
                return true;
            }
            if (ward.calmsPiglins() && worn.makesPiglinsNeutral(player)) {
                return true;
            }
        }
        return false;
    }

    private PlanetHazardMechanics() {
    }
}
