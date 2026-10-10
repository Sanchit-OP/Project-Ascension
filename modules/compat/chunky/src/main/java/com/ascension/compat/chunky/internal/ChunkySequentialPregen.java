package com.ascension.compat.chunky.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.event.task.GenerationTaskFinishEvent;
import org.popcraft.chunky.iterator.PatternType;
import org.popcraft.chunky.shape.ShapeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-generates every registered planet's surface in the background, one at a time, starting the
 * moment the server finishes booting &mdash; instead of waiting for a player to physically reach
 * each world first. Sanchit's call 2026-09-06, replacing the original per-arrival trigger this
 * module shipped with: reacting to arrival meant the pre-generation started right as someone was
 * standing there needing it, and it only ever covered the one planet they happened to be visiting.
 * Doing the whole planet list proactively, in idle time before anyone arrives, is strictly more
 * useful and gives Distant Horizons' background LOD generation a head start on real terrain
 * instead of racing freshly-generated chunks (the exact conflict
 * {@code docs/technical/optimisation-stack.md} warns Chunky and DH's LOD pass can get into).
 *
 * <p><strong>One planet at a time, on purpose.</strong> Running every planet's pre-generation
 * simultaneously is the more obvious version of this and the wrong one -- it is exactly the kind
 * of sustained multi-task CPU/IO load ADR-0007 exists to catch, and it is what actually starves
 * DH's LOD pass rather than giving it room to work. One world's task <em>finishing</em> is what
 * starts the next world's, so at most one pre-generation ever runs at once, on top of
 * {@link #tryStartNext}'s own {@code getGenerationTasks().isEmpty()} check.
 *
 * <p><strong>Off unless a server turns it on</strong> ({@link ChunkyCompatConfig}, since
 * 2026-10-10). With it off, nothing here runs: no task is started, the governor never looks, and
 * {@code /chunky} by hand is untouched. It is off because a fresh server crashed while this ran:
 * generated chunks were not unloaded while Chunky worked, and the heap filled. The cause is not in
 * this class (a pause made no difference to it), and is not yet found; see the Chunky bug in
 * {@code plans/m3-vertical-slice.md}.
 *
 * <p><strong>Finishing, not stopping.</strong> Chunky's public {@code onGenerationComplete} fires
 * at the end of every task, paused and cancelled ones included, and carries only the world's name.
 * Until 2026-10-10 this listened to it, so pausing Earth's task started the Moon's at once. Now it
 * listens to Chunky's {@link GenerationTaskFinishEvent}, which carries the task, and counts a task
 * as finished only when its chunk iterator has run dry: a pause or a cancel always leaves chunks
 * in it. (Not {@code Progress.isComplete()}: Chunky stops issuing chunks before the last ones are
 * written, so at this event that flag can still be false for a finished task.)
 *
 * <p><strong>It backs off when the server cannot keep up.</strong> Every 5 seconds the
 * {@link PregenGovernor} looks at the server's average tick time: past 40 ms it pauses our running
 * task, and under 25 ms it resumes one it paused, so a pre-generation never lags players who are
 * online. It governs only planet surfaces whose pre-generation this class started, and resumes
 * only what it paused itself, so an admin's {@code /chunky pause} stays paused. It does not stop
 * the 2026-10-10 crash: ticks stayed at 3 to 10 ms right up until the heap was full.
 *
 * <p><strong>Ordered by {@link Planet#order()}</strong>, the field that already exists purely for
 * "sort order for listings" &mdash; reusing it here means Earth (order 10) always pre-generates
 * before the Moon (order 20) with no separate sequencing concept to define or keep in sync.
 *
 * <p><strong>Persists across restarts for free, in two different ways.</strong> Which planets have
 * already had a task <em>started</em> is the same serialised {@link PregenMarker} attachment the
 * original arrival-based version used, so a restart resumes the sequence from the first
 * not-yet-started planet rather than re-running finished ones. Which chunks within an
 * <em>interrupted</em> task are already generated is Chunky's own concern, not this module's --
 * Chunky persists live task state to {@code config/chunky/tasks/} and resumes it automatically on
 * its own startup (the "[Chunky] No tasks to continue" log line on a clean server is that exact
 * check finding nothing to do). This module only ever needs to decide "has a task for this world
 * been started, yes or no" -- resuming what a started-but-interrupted task didn't finish is a
 * problem Chunky already solves.
 *
 * <p><strong>Threads and lifetimes.</strong> Chunky calls its event on its own task thread, so the
 * listener only reads the finished task there and hands the rest to the server thread, which is
 * the only place a level's attachments are touched. Nothing here holds the server or a level: the
 * listener looks the server up when it runs, and the governor's memory is a world's name, cleared
 * when the server stops (ADR-0007).
 *
 * <p><strong>Compiling against Chunky's real classes is safe here in a way it would not be in
 * Tier 1.</strong> This jar's own {@code neoforge.mods.toml} declares {@code chunky} as a
 * required dependency, so NeoForge refuses to load this mod at all when Chunky is absent &mdash;
 * the classes referenced below are never missing at the point they would actually be loaded. That
 * refusal is the actual "optional" mechanism (same posture ADR-0006 sets for Sable): a Tier 1
 * module stays entirely ignorant of this jar's existence, and this jar simply does not exist as
 * far as the game is concerned when Chunky is not installed.
 */
public final class ChunkySequentialPregen {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkySequentialPregen.class);

    /**
     * Radius in blocks around a planet's own {@code (0, 0)} to pre-generate. In chunks (100) per
     * Sanchit's request 2026-09-06 -- much larger than the old per-arrival radius made sense to
     * be, because this runs in server idle time before anyone is standing there waiting on it,
     * rather than reactively at the moment someone arrives.
     */
    private static final int PREGEN_RADIUS_BLOCKS = 100 * 16;

    /** How often the governor reads the tick time: every 5 seconds. */
    private static final int CHECK_EVERY_TICKS = 100;

    /**
     * One instance, so a second server start in the same game (single-player) can unsubscribe the
     * first start's subscription rather than stacking another beside it.
     */
    private static final Consumer<GenerationTaskFinishEvent> ON_TASK_FINISHED = ChunkySequentialPregen::onTaskFinished;

    /** The world (Chunky's name for it) whose task the governor paused, or null. */
    @Nullable
    private static String pausedForLoad;

    private static int ticksUntilCheck = CHECK_EVERY_TICKS;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChunkySequentialPregen::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ChunkySequentialPregen::onServerTick);
        NeoForge.EVENT_BUS.addListener(ChunkySequentialPregen::onServerStopped);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (!ChunkyCompatConfig.INSTANCE.backgroundEnabled()) {
            LOGGER.info("Background pre-generation is off (config/ascension_compat_chunky-server.toml)");
            return;
        }
        Chunky chunky = ChunkyProvider.get();
        if (chunky == null) {
            // Chunky's own instance is tied to a running server, which unconditionally exists by
            // the time this event fires. Null here means Chunky itself failed to initialise, not
            // a timing issue with this listener -- nothing to pre-generate against.
            return;
        }
        chunky.getEventBus().unsubscribe(GenerationTaskFinishEvent.class, ON_TASK_FINISHED);
        chunky.getEventBus().subscribe(GenerationTaskFinishEvent.class, ON_TASK_FINISHED);
        tryStartNext(event.getServer(), chunky);
    }

    /** On Chunky's task thread. Every task ends here, whether it finished, paused or was cancelled. */
    private static void onTaskFinished(GenerationTaskFinishEvent event) {
        if (event.generationTask().getChunkIterator().hasNext()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            Chunky chunky = ChunkyProvider.get();
            if (chunky != null) {
                tryStartNext(server, chunky);
            }
        });
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (--ticksUntilCheck > 0) {
            return;
        }
        ticksUntilCheck = CHECK_EVERY_TICKS;
        if (!ChunkyCompatConfig.INSTANCE.backgroundEnabled()) {
            return;
        }
        Chunky chunky = ChunkyProvider.get();
        if (chunky == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        ChunkyAPI api = chunky.getApi();
        String running = runningPregen(server, api);
        if (running != null) {
            // Running again, whoever resumed it: nothing of ours is paused any more.
            pausedForLoad = null;
        }
        double msPerTick = server.getAverageTickTimeNanos() / 1.0e6;
        switch (PregenGovernor.decide(running != null, pausedForLoad != null, msPerTick)) {
            case PAUSE -> {
                if (api.pauseTask(running)) {
                    pausedForLoad = running;
                    LOGGER.info("Pausing pre-generation of {}: the server is at {} ms a tick", running,
                            Math.round(msPerTick));
                }
            }
            case RESUME -> {
                if (api.continueTask(pausedForLoad)) {
                    LOGGER.info("Resuming pre-generation of {}: the server is back to {} ms a tick",
                            pausedForLoad, Math.round(msPerTick));
                }
                pausedForLoad = null;
            }
            case NONE -> {
            }
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        pausedForLoad = null;
        ticksUntilCheck = CHECK_EVERY_TICKS;
    }

    /** Chunky's name for the planet surface whose pre-generation (ours) is running, or null. */
    @Nullable
    private static String runningPregen(MinecraftServer server, ChunkyAPI api) {
        for (Planet planet : planetsInOrder(server)) {
            ServerLevel surface = server.getLevel(planet.surface());
            if (surface == null || !surface.getData(ChunkyAttachments.PREGEN_MARKER).started()) {
                continue;
            }
            String worldId = planet.surface().location().toString();
            if (api.isRunning(worldId)) {
                return worldId;
            }
        }
        return null;
    }

    private static List<Planet> planetsInOrder(MinecraftServer server) {
        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        return planets.stream()
                .sorted(Comparator.comparingInt(Planet::order))
                .toList();
    }

    private static void tryStartNext(MinecraftServer server, Chunky chunky) {
        if (!chunky.getGenerationTasks().isEmpty()) {
            // Something is already generating -- ours, a resumed interrupted task, or a manual
            // one. Starting a second one now would be the exact simultaneous-load problem this
            // class exists to avoid; the next task to finish calls this again.
            return;
        }

        for (Planet planet : planetsInOrder(server)) {
            ResourceKey<Level> surfaceKey = planet.surface();
            ServerLevel surface = server.getLevel(surfaceKey);
            if (surface == null) {
                continue;
            }
            PregenMarker marker = surface.getData(ChunkyAttachments.PREGEN_MARKER);
            if (marker.started()) {
                continue;
            }

            ChunkyAPI api = chunky.getApi();
            String worldId = surfaceKey.location().toString();
            boolean started = api.startTask(
                    worldId,
                    ShapeType.CIRCLE,
                    0.0,
                    0.0,
                    PREGEN_RADIUS_BLOCKS,
                    PREGEN_RADIUS_BLOCKS,
                    // Concentric: a planet's own (0, 0) finishes first and coverage widens
                    // outward, so an interrupted task has still covered the part nearest the
                    // authored spawn/landing area before anything further out.
                    PatternType.CONCENTRIC);
            if (started) {
                marker.markStarted();
            }
            // Only ever one planet started per call. The rest wait their turn: either the next
            // finished task if this one started, or the next server restart if it didn't
            // (api.startTask returning false with nothing already running is not a case expected
            // in practice, so there is nothing more useful to do here than leave the marker unset
            // and let a later check retry it).
            return;
        }
        // Nothing left unstarted -- every registered planet has had its pre-generation kicked off
        // at some point. Nothing more for this chain to do until a new planet is registered and
        // the server restarts.
    }

    private ChunkySequentialPregen() {
    }
}
