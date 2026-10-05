package com.ascension.worlds.internal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Keeps vanilla's Nether and End portals dormant ({@link WorldsConfig#disableVanillaPortals}).
 * Sanchit's call, 2026-10-05: those worlds are meant to become planets in this campaign, reached
 * the way every other planet is, not through vanilla's portals.
 *
 * <p>Two hooks, because the two portals are built differently:
 * <ul>
 *   <li>A Nether portal frame never lights ({@link BlockEvent.PortalSpawnEvent}) &mdash; the
 *       clearest possible "this does not work here", rather than a purple portal that silently
 *       does nothing.</li>
 *   <li>The End portal has no comparable creation event (eyes of ender fill the frame directly),
 *       so travel itself is refused: any <em>portal-driven</em> trip into the Nether or the End is
 *       cancelled ({@link EntityTravelToDimensionEvent}). That also catches a Nether portal that
 *       already existed, or one another mod creates.</li>
 * </ul>
 *
 * <p>"Portal-driven" is {@link Entity#portalProcess} being set, which vanilla only does while an
 * entity stands in a portal block. Commands and this module's own space travel never set it, so
 * operators can still reach those dimensions, and the day the Nether becomes a planet, planet
 * travel will not be caught by this.
 */
public final class VanillaPortals {

    private VanillaPortals() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VanillaPortals::onPortalSpawn);
        NeoForge.EVENT_BUS.addListener(VanillaPortals::onTravel);
    }

    private static boolean enabled() {
        return WorldsConfig.SPEC.isLoaded() && WorldsConfig.INSTANCE.disableVanillaPortals.getAsBoolean();
    }

    private static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (enabled()) {
            event.setCanceled(true);
        }
    }

    private static void onTravel(EntityTravelToDimensionEvent event) {
        if (!enabled() || !isVanillaPortalTarget(event.getDimension())) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.portalProcess == null) {
            return;
        }
        event.setCanceled(true);
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.translatable("ascension_worlds.vanilla_portal.dormant").withStyle(ChatFormatting.DARK_PURPLE),
                    true);
        }
    }

    private static boolean isVanillaPortalTarget(ResourceKey<Level> dimension) {
        return dimension.equals(Level.NETHER) || dimension.equals(Level.END);
    }
}
