package com.ascension.compat.chunky.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.iterator.PatternType;
import org.popcraft.chunky.shape.ShapeType;

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
 * DH's LOD pass rather than giving it room to work. Chaining through
 * {@link ChunkyAPI#onGenerationComplete}, one world's completion is what starts the next world's
 * task, so at most one pre-generation ever runs at once, on top of {@link #tryStartNext}'s own
 * {@code getGenerationTasks().isEmpty()} check.
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
 * <p><strong>Compiling against Chunky's real classes is safe here in a way it would not be in
 * Tier 1.</strong> This jar's own {@code neoforge.mods.toml} declares {@code chunky} as a
 * required dependency, so NeoForge refuses to load this mod at all when Chunky is absent &mdash;
 * the classes referenced below are never missing at the point they would actually be loaded. That
 * refusal is the actual "optional" mechanism (same posture ADR-0006 sets for Sable): a Tier 1
 * module stays entirely ignorant of this jar's existence, and this jar simply does not exist as
 * far as the game is concerned when Chunky is not installed.
 */
public final class ChunkySequentialPregen {

    /**
     * Radius in blocks around a planet's own {@code (0, 0)} to pre-generate. In chunks (100) per
     * Sanchit's request 2026-09-06 -- much larger than the old per-arrival radius made sense to
     * be, because this runs in server idle time before anyone is standing there waiting on it,
     * rather than reactively at the moment someone arrives.
     */
    private static final int PREGEN_RADIUS_BLOCKS = 100 * 16;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChunkySequentialPregen::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        Chunky chunky = ChunkyProvider.get();
        if (chunky == null) {
            // Chunky's own instance is tied to a running server, which unconditionally exists by
            // the time this event fires. Null here means Chunky itself failed to initialise, not
            // a timing issue with this listener -- nothing to pre-generate against.
            return;
        }

        MinecraftServer server = event.getServer();
        // Registered once, globally, rather than per world: every completion anywhere (this
        // module's own task, a resumed one, even someone's manual /chunky start) is a reason to
        // check whether the next planet in line is now free to start.
        chunky.getApi().onGenerationComplete(complete -> tryStartNext(server, chunky));
        tryStartNext(server, chunky);
    }

    private static void tryStartNext(MinecraftServer server, Chunky chunky) {
        if (!chunky.getGenerationTasks().isEmpty()) {
            // Something is already generating -- ours, a resumed interrupted task, or a manual
            // one. Starting a second one now would be the exact simultaneous-load problem this
            // class exists to avoid; onGenerationComplete calls this again once it finishes.
            return;
        }

        Registry<Planet> planets = server.registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        List<Planet> ordered = planets.stream()
                .sorted(Comparator.comparingInt(Planet::order))
                .toList();

        for (Planet planet : ordered) {
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
            // onGenerationComplete callback if this one started, or the next server restart if it
            // didn't (api.startTask returning false with nothing already running is not a case
            // expected in practice, so there is nothing more useful to do here than leave the
            // marker unset and let a later check retry it).
            return;
        }
        // Nothing left unstarted -- every registered planet has had its pre-generation kicked off
        // at some point. Nothing more for this chain to do until a new planet is registered and
        // the server restarts.
    }

    private ChunkySequentialPregen() {
    }
}
