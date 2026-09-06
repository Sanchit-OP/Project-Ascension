package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Everything about being in {@link SpaceDimension} rather than descended onto a world: ascent
 * from a planet's surface, recognising arrival at another (with a pre-load window and a remembered
 * landing spot, see {@link #descendTo}), the vertical bound's warning, and suppressing the "fell
 * out of the world" death vanilla would otherwise apply out here.
 *
 * <p>Design record: {@code plans/m2-worlds.md}'s M2.5 and M2.6 sections.
 */
public final class SpaceMechanics {

    private static final int WARNING_INTERVAL_TICKS = 20;

    /**
     * Extra clearance added past a planet's own {@code approach_radius} when placing a player who
     * just ascended from it. Arriving exactly on top of the planet's own coordinate put its
     * rendered body around/through the player instead of ahead of them, and left them trivially
     * inside their own departure planet's approach shell to boot. Arbitrary margin past the shell
     * itself, not a distance that means anything physically -- just enough that the body reads as
     * a real object at a real distance, in front of you, the moment you arrive.
     */
    private static final int ASCENT_CLEARANCE = 100;

    /**
     * How often (in ticks) the destination pre-load ticket below is refreshed while a player is
     * still crossing a planet's approach shell. Cheap per-player bookkeeping, not per-tick-
     * critical, so once a second is plenty.
     */
    private static final int PRELOAD_REFRESH_INTERVAL_TICKS = 20;

    /**
     * The ticket type used to pre-load a landing spot while a player is still crossing a planet's
     * approach shell. A 400-tick (20s) self-expiry is the real teardown -- refreshed every
     * {@link #PRELOAD_REFRESH_INTERVAL_TICKS} while still approaching (see
     * {@link #preloadLanding}), so it stays alive for a normal approach but disappears on its own
     * within 20s of a player leaving the shell, logging off, or dying mid-flight, with no explicit
     * cleanup required to avoid a permanent forced-chunk leak (ADR-0007 rule 2). Modelled directly
     * on vanilla's own {@code TicketType.PORTAL}, which does the same job for Nether portal
     * linking.
     */
    private static final TicketType<ChunkPos> LANDING_PRELOAD =
            TicketType.create("ascension_worlds_landing_preload", Comparator.comparingLong(ChunkPos::toLong), 400);

    /** Chunk-ticket propagation level for the pre-load above -- a small area, not a render radius. */
    private static final int PRELOAD_LEVEL = 2;

    /**
     * How far below a dimension's own ceiling to land when nothing is remembered yet -- the
     * placeholder used only for a planet nobody has ever ascended from before. See
     * {@link #resolveLandingSpot}.
     */
    private static final int DESCENT_MARGIN = 20;

    /**
     * How far {@link #findSafeLandingSpot} will search, in blocks, for open ground near a
     * remembered ascent point before giving up and falling back to the placeholder. Small and
     * bounded on purpose -- this runs once per dimension transition, not per tick, but it is still
     * a search, not a scan; a few rings out is plenty to dodge "someone built a wall here since you
     * left" without turning into an unbounded walk.
     */
    private static final int SAFE_SPOT_SEARCH_RADIUS = 4;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpaceMechanics::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SpaceMechanics::onIncomingDamage);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (level.dimension().equals(SpaceDimension.KEY)) {
            warnIfBeyondVerticalBound(player);
            checkApproach(player);
        } else {
            checkAscent(player, level);
        }
    }

    /**
     * Crossing a planet's own build height while airborne on its surface sends you to space, at
     * that planet's position, velocity and heading preserved.
     *
     * <p>Deliberately reads the <em>current dimension's own</em> max build height rather than a
     * hardcoded altitude, so this works unchanged for any planet's surface &mdash; Earth's 320,
     * the Moon's own height, or a planet 4&ndash;7 nobody has authored yet. No per-planet Java,
     * per ADR-0004.
     */
    private static void checkAscent(ServerPlayer player, Level level) {
        if (player.getY() < level.getMaxBuildHeight()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : planets.entrySet()) {
            Planet planet = entry.getValue();
            if (!planet.surface().equals(level.dimension())) {
                continue;
            }
            ServerLevel space = server.getLevel(SpaceDimension.KEY);
            if (space == null) {
                return;
            }
            // Arriving exactly AT this planet's own SpacePosition put its rendered body
            // around/through the player rather than ahead of them, and was trivially inside its
            // own approach shell (distance 0) besides, since approach_radius is always larger
            // than body_radius by construction -- checkApproach saw that as a fresh arrival on the
            // very next tick and immediately descended back to where the player just left. Placing
            // the player past approach_radius instead, along the direction they were already
            // facing, fixes both: they're genuinely outside their own departure shell (no special
            // "already known" bookkeeping needed to avoid a self-triggered descent), and the body
            // sits ahead of them, roughly back the way they came, like a real object receding
            // behind an outbound ship rather than a skybox wrapped around the camera.

            // Recorded here, at the moment of crossing, rather than polled periodically while on
            // the ground: several players riding the same ship up together each stand somewhere
            // slightly different on it, and none of those ground positions relate to each other
            // once the ship's gone -- but this exact moment, crossing into space, is a shared
            // anchor for all of them. See PlanetArrivalMemory's javadoc.
            player.getData(SpaceAttachments.ARRIVAL_MEMORY)
                    .recordAscent(level.dimension().location(), player.blockPosition(), player.getYRot());

            double offset = planet.space().approachRadius() + ASCENT_CLEARANCE;
            Vec3 destination = AscentDestination.compute(
                    planet.space().x(), planet.space().z(), player.getYRot(), offset);
            player.changeDimension(new DimensionTransition(
                    space, destination, player.getDeltaMovement(), player.getYRot(), player.getXRot(),
                    DimensionTransition.DO_NOTHING));
            return;
        }
    }

    /**
     * Entering a planet's approach shell is announced once, on the transition into range, using
     * the per-player {@link SpaceTravelState} to tell "just arrived" from "still here" &mdash;
     * that recognition was the whole of M2.5's original scope here.
     *
     * <p>M2.6 splits what happens next into two radii instead of one: crossing
     * {@code approach_radius} starts (and keeps refreshing) a chunk pre-load at the eventual
     * landing spot, while the actual descent only fires once the player reaches
     * {@code body_radius} &mdash; the boundary {@code Planet.SpacePosition} already documents as
     * "the volume you cannot fly into". The distance between the two is what turns "the last
     * stretch of the flight" into loading time instead of a stutter at the moment of arrival, per
     * {@code plans/m2-worlds.md}'s M2.6 lever #1.
     */
    private static void checkApproach(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        ResourceLocation shellId = null;
        Planet shellPlanet = null;
        boolean arrived = false;
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : planets.entrySet()) {
            Planet.SpacePosition space = entry.getValue().space();
            long distanceSquared = space.distanceSquaredTo((int) player.getX(), (int) player.getZ());
            long approachSquared = (long) space.approachRadius() * space.approachRadius();
            if (distanceSquared <= approachSquared) {
                shellId = entry.getKey().location();
                shellPlanet = entry.getValue();
                long bodySquared = (long) space.bodyRadius() * space.bodyRadius();
                arrived = distanceSquared <= bodySquared;
                break;
            }
        }

        SpaceTravelState state = player.getData(SpaceAttachments.TRAVEL_STATE);
        if (shellId != null && !shellId.equals(state.nearPlanet())) {
            player.displayClientMessage(
                    Component.literal("Approach shell reached: " + shellId.getPath()), false);
        }
        state.setNearPlanet(shellId);

        if (shellId == null) {
            return;
        }
        if (arrived) {
            descendTo(player, server, shellPlanet);
        } else if (player.tickCount % PRELOAD_REFRESH_INTERVAL_TICKS == 0) {
            preloadLanding(player, server, shellPlanet);
        }
    }

    /** Where a player should end up on a planet's surface, and which way they should be facing. */
    private record LandingSpot(Vec3 pos, float yaw) {
    }

    /**
     * The landing spot a player would arrive at on {@code planet} right now: near where they last
     * ascended from (see {@link PlanetArrivalMemory}) if they have ever left it before, or a
     * placeholder near the surface dimension's own ceiling if not.
     *
     * <p><strong>Near, not exactly on.</strong> Landing on the literal remembered coordinate risks
     * two different things going wrong: the ground there may no longer be safe (built over, dug
     * out, flooded since they left -- Create Aeronautics or whatever else eventually authors a
     * real landing surface owns fixing that properly; this only owns not teleporting a player into
     * a wall), and several players who ascended together a block or two apart would otherwise
     * return to positions a block or two apart too, which starts overlapping fast once solid
     * ground is involved rather than open space. {@link #findSafeLandingSpot} resolves to the
     * nearest actually-standable spot instead of the exact point.
     *
     * <p>Shared by {@link #preloadLanding} and {@link #descendTo} so the two can never disagree
     * about where "there" is &mdash; pre-loading the wrong chunk would be worse than not
     * pre-loading at all.
     */
    private static LandingSpot resolveLandingSpot(ServerLevel surface, ServerPlayer player) {
        PlanetArrivalMemory memory = player.getData(SpaceAttachments.ARRIVAL_MEMORY);
        PlanetArrivalMemory.DeparturePoint remembered = memory.lastAscentFrom(surface.dimension().location());
        if (remembered != null) {
            BlockPos safeSpot = findSafeLandingSpot(surface, remembered.pos());
            if (safeSpot != null) {
                return new LandingSpot(Vec3.atBottomCenterOf(safeSpot), remembered.yaw());
            }
        }
        // Either a planet nobody has ever ascended from before, or nothing safe was found nearby
        // -- land near the ceiling instead of querying a heightmap. A heightmap read returns the
        // same "nothing solid here" sentinel for an unloaded chunk, a dug-out hole, and a genuine
        // hole in the terrain alike, which is exactly how an earlier version of this put players
        // below bedrock. Landing high and letting them glide down under their own control
        // sidesteps the whole class of failure, and reads as "descending from space" rather than
        // "teleported onto the ground".
        double landingY = surface.getMaxBuildHeight() - DESCENT_MARGIN;
        return new LandingSpot(new Vec3(0.5, landingY, 0.5), player.getYRot());
    }

    /**
     * Searches outward from {@code preferred} in widening square rings, up to
     * {@link #SAFE_SPOT_SEARCH_RADIUS} blocks, for the nearest column with solid ground and two
     * clear blocks to stand in. Returns {@code null} if nothing qualifies within that radius,
     * leaving {@link #resolveLandingSpot} to fall back to the placeholder rather than land a
     * player somewhere arbitrary far from where they meant to be.
     */
    private static BlockPos findSafeLandingSpot(ServerLevel level, BlockPos preferred) {
        for (RingSearch.Offset offset : RingSearch.offsetsUpTo(SAFE_SPOT_SEARCH_RADIUS)) {
            BlockPos candidate = preferred.offset(offset.dx(), 0, offset.dz());
            if (isSafeToStandOn(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSafeToStandOn(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * Keeps the destination's landing chunk loaded while a player is still crossing the approach
     * shell, so it is already resident by the time {@link #descendTo} actually needs it. See
     * {@link #LANDING_PRELOAD}'s javadoc for the self-expiring teardown.
     */
    private static void preloadLanding(ServerPlayer player, MinecraftServer server, Planet planet) {
        ServerLevel surface = server.getLevel(planet.surface());
        if (surface == null) {
            return;
        }
        LandingSpot spot = resolveLandingSpot(surface, player);
        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(spot.pos()));
        surface.getChunkSource().addRegionTicket(LANDING_PRELOAD, chunkPos, PRELOAD_LEVEL, chunkPos);
    }

    /**
     * Sends a player down to a planet's surface, at the spot {@link #resolveLandingSpot} resolves
     * for them right now.
     *
     * <p>Authoring an actual landing structure is explicitly not this module's job (Sanchit's call
     * 2026-09-06: Create Aeronautics owns that once it lands), and what happens on a genuinely bad
     * landing -- fall damage, a remembered spot with nothing safe left nearby -- is the same open
     * "failed landings" question {@code plans/m2-worlds.md}'s M2.6 lists as deferred to v2 and to
     * whichever module ends up owning ship/landing damage, not something resolved here. What this
     * does resolve: returning to a planet you have already left from lands you back near that
     * point, not at a single fixed point every time.
     */
    private static void descendTo(ServerPlayer player, MinecraftServer server, Planet planet) {
        ServerLevel surface = server.getLevel(planet.surface());
        if (surface == null) {
            return;
        }
        LandingSpot spot = resolveLandingSpot(surface, player);
        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(spot.pos()));
        surface.getChunkSource().removeRegionTicket(LANDING_PRELOAD, chunkPos, PRELOAD_LEVEL, chunkPos);
        player.changeDimension(new DimensionTransition(
                surface, spot.pos(), Vec3.ZERO, spot.yaw(), player.getXRot(), DimensionTransition.DO_NOTHING));
    }

    /**
     * The vertical bound has no wall behind it &mdash; see {@link SpaceDimension#VERTICAL_BOUND}.
     * This is the entire enforcement: a message, repeated while it stays true, never a correction.
     * Repeated every {@value #WARNING_INTERVAL_TICKS} ticks rather than every tick so it reads as
     * persistent (the action bar fades in a few seconds if not refreshed) without flooding the
     * connection.
     */
    private static void warnIfBeyondVerticalBound(ServerPlayer player) {
        if (Math.abs(player.getY()) <= SpaceDimension.VERTICAL_BOUND) {
            return;
        }
        if (player.tickCount % WARNING_INTERVAL_TICKS != 0) {
            return;
        }
        player.displayClientMessage(
                Component.literal("Notice: beyond this point, the map has nothing left to show you."),
                true);
    }

    /**
     * No void death out here. Vanilla's "fell out of the world" damage exists to punish leaving
     * the playable volume of a normal world &mdash; space has no such volume to fall out of, and
     * the vertical bound above is a warning, not a wall, so nothing should be able to kill a
     * player for reaching it.
     */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                && event.getEntity().level().dimension().equals(SpaceDimension.KEY)) {
            event.setCanceled(true);
        }
    }

    private SpaceMechanics() {
    }
}
