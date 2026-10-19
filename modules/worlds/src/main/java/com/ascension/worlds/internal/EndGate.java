package com.ascension.worlds.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The End's gate is the Ender Dragon's exit portal (ADR-0028), not a frame ruin with a key.
 *
 * <p>Three small jobs, all event-driven:
 * <ul>
 *   <li><strong>Lighting.</strong> Once a second, and only while the End is loaded and its gate is
 *       dark, the fight is asked one boolean ({@code hasPreviouslyKilledDragon}). When it is true the
 *       exit portal is found once, and the End joins the gate network
 *       ({@link GatewayNetworkState#activate}), which steps the mark and gives the gate advancement.
 *       Lighting is idempotent, so respawning the Dragon with crystals changes nothing.</li>
 *   <li><strong>Using it.</strong> Stepping into the exit portal opens the destination screen
 *       instead of sending the player to their bed (and, the first time, instead of the credits,
 *       which are kept for the real ending).</li>
 *   <li><strong>Credits.</strong> A player in the End is marked as having seen them, so the portal
 *       never plays them.</li>
 * </ul>
 * Follows {@link WorldsConfig#disableVanillaPortals}: with vanilla's portals allowed, vanilla's
 * exit portal is left alone.
 */
public final class EndGate {

    /** Ticks between looks at the fight. A boolean read; the cost is the same each time. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** The podium's centre column and how far up and down to look for the portal block. */
    private static final int PORTAL_SEARCH_MIN_Y = 40;
    private static final int PORTAL_SEARCH_MAX_Y = 110;

    /** Where arrivals stand, east of the portal, clear of the podium's torches. */
    private static final int ARRIVAL_OFFSET = 6;

    private EndGate() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EndGate::onLevelTick);
        NeoForge.EVENT_BUS.addListener(EndGate::onTravel);
        NeoForge.EVENT_BUS.addListener(EndGate::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(EndGate::onLoggedIn);
    }

    private static boolean enabled() {
        return WorldsConfig.SPEC.isLoaded() && WorldsConfig.INSTANCE.disableVanillaPortals.getAsBoolean();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.END)
                || level.getGameTime() % CHECK_INTERVAL_TICKS != 0 || !enabled()) {
            return;
        }
        ResourceLocation here = level.dimension().location();
        GatewayNetworkState network = level.getServer().overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
        if (network.isActive(here)) {
            return;
        }
        EndDragonFight fight = level.getDragonFight();
        if (fight == null || !fight.hasPreviouslyKilledDragon()) {
            return;
        }
        BlockPos portal = findPortal(level);
        if (portal == null) {
            return; // the portal blocks are not placed yet; try again next look
        }
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                new BlockPos(portal.getX() + ARRIVAL_OFFSET, 0, portal.getZ()));
        if (network.activate(here, ground)) {
            GateProgress.networkChanged(level.getServer());
        }
    }

    /** The exit portal's block, in the podium's centre column, or null if it is not there. */
    private static BlockPos findPortal(ServerLevel level) {
        if (!level.hasChunkAt(BlockPos.ZERO)) {
            return null;
        }
        for (int y = PORTAL_SEARCH_MAX_Y; y >= PORTAL_SEARCH_MIN_Y; y--) {
            BlockPos pos = new BlockPos(0, y, 0);
            if (level.getBlockState(pos).is(Blocks.END_PORTAL)) {
                return pos;
            }
        }
        return null;
    }

    private static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!enabled() || entity.portalProcess == null || !entity.level().dimension().equals(Level.END)
                || event.getDimension().equals(Level.END)) {
            return;
        }
        event.setCanceled(true);
        if (entity instanceof ServerPlayer player && entity.level() instanceof ServerLevel level) {
            GatewayDestinations.onStepIn(player, level);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(Level.END) && event.getEntity() instanceof ServerPlayer player) {
            skipCredits(player);
        }
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(Level.END)) {
            skipCredits(player);
        }
    }

    private static void skipCredits(ServerPlayer player) {
        if (enabled()) {
            player.seenCredits = true;
        }
    }
}
