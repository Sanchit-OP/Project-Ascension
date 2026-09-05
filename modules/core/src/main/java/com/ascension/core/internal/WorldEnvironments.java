package com.ascension.core.internal;

import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds registered environment sources and resolves queries against them.
 *
 * <p>Two phases, the same shape that has worked in {@code ascension-atmosphere}'s provider
 * registry. During setup, registration mutates a map. At freeze, entries are sorted once into an
 * immutable array. Afterwards a query only walks that array, so resolution sorts nothing and
 * allocates nothing on the server thread (ADR-0007 rules 4 and 6).
 *
 * <p>Holds no {@code Level}, {@code Player} or {@code BlockEntity}; sources are handed a
 * {@link ResourceKey} and resolve their own data (ADR-0007 rule 1).
 */
public final class WorldEnvironments {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldEnvironments.class);

    private static final WorldEnvironments INSTANCE = new WorldEnvironments();

    /** A source paired with the id it was registered under. */
    public record Entry(ResourceLocation id, WorldEnvironmentSource source) {
    }

    /**
     * Concurrent, and deliberately so: {@code FMLCommonSetupEvent} runs mods in parallel, so two
     * mods can register at the same moment. Relying on callers to wrap registration in
     * {@code enqueueWork} would be relying on adopters having read the documentation.
     *
     * <p>Insertion order is therefore not preserved, which does not matter: ordering is decided
     * by id at freeze, so the result is deterministic regardless of who won the race.
     */
    private final Map<ResourceLocation, WorldEnvironmentSource> pending = new ConcurrentHashMap<>();

    /** Sorted by id. Null until frozen, which is also how "still open" is tested. */
    private volatile Entry[] sources;

    /** Conflicts already reported, so a genuine clash is logged once rather than per query. */
    private final Set<String> reportedConflicts = ConcurrentHashMap.newKeySet();

    /** Logged once if anything queries before freeze, which is a load-order mistake. */
    private volatile boolean warnedAboutEarlyQuery;

    /**
     * Package-private rather than private so tests can build their own instance.
     *
     * <p>The alternative was a {@code reset()} on the singleton, which is test-only API living in
     * production code and one careless call away from clearing the registry at runtime. Freezing
     * is one-way by design; a test that needs a frozen registry should get a fresh one.
     */
    WorldEnvironments() {
    }

    public static WorldEnvironments get() {
        return INSTANCE;
    }

    // --- registration -------------------------------------------------------

    public void add(ResourceLocation id, WorldEnvironmentSource source) {
        if (sources != null) {
            throw new IllegalStateException(
                    "World environment registration is closed. Register during mod setup, not "
                            + "later. Attempted id: " + id);
        }
        if (pending.putIfAbsent(id, source) != null) {
            throw new IllegalArgumentException("Duplicate WorldEnvironmentSource id: " + id);
        }
    }

    /**
     * Sort once and close registration.
     *
     * <p>Called on {@code FMLLoadCompleteEvent}, not on this module's own common setup. Common
     * setup runs per mod and in parallel, so freezing there would close the registry before other
     * mods had a chance to register &mdash; and which mods lost would depend on load order,
     * making it the kind of bug that appears only on someone else's machine.
     */
    public void freeze() {
        if (sources != null) {
            return;
        }
        List<Entry> sorted = new ArrayList<>(pending.size());
        pending.forEach((id, source) -> sorted.add(new Entry(id, source)));
        sorted.sort(Comparator.comparing(e -> e.id().toString()));
        sources = sorted.toArray(new Entry[0]);

        LOGGER.info("World environment registry frozen: {} source(s)", sources.length);
    }

    // --- resolution ---------------------------------------------------------

    public Optional<WorldEnvironment> resolve(ResourceKey<Level> dimension) {
        Entry[] snapshot = sources;
        if (snapshot == null) {
            if (!warnedAboutEarlyQuery) {
                warnedAboutEarlyQuery = true;
                LOGGER.warn("World environment queried before mod loading completed; answering "
                        + "'unknown'. Something is resolving worlds during setup.");
            }
            return Optional.empty();
        }
        for (int i = 0; i < snapshot.length; i++) {
            Optional<WorldEnvironment> claim = snapshot[i].source().environmentOf(dimension);
            if (claim.isPresent()) {
                warnIfAmbiguous(snapshot, i, dimension);
                return claim;
            }
        }
        return Optional.empty();
    }

    /**
     * Report two sources claiming the same dimension.
     *
     * <p>Cannot be caught at freeze: whether two sources conflict depends on the dimension being
     * asked about, so the check belongs here, where the conflict is real. The winner is still
     * deterministic &mdash; ties break on id, fixed at freeze &mdash; so this is a diagnostic
     * rather than a correctness fix, and it is the difference between an adopter debugging their
     * mod and an adopter filing a bug against ours.
     */
    private void warnIfAmbiguous(Entry[] snapshot, int winner, ResourceKey<Level> dimension) {
        for (int i = winner + 1; i < snapshot.length; i++) {
            if (snapshot[i].source().environmentOf(dimension).isEmpty()) {
                continue;
            }
            String pair = snapshot[winner].id() + " vs " + snapshot[i].id() + " @ "
                    + dimension.location();
            if (reportedConflicts.add(pair)) {
                LOGGER.warn("Two sources describe {}: {} and {}. '{}' wins by id ordering. A "
                                + "source should only answer for dimensions it owns.",
                        dimension.location(), snapshot[winner].id(), snapshot[i].id(),
                        snapshot[winner].id());
            }
        }
    }

    // --- accessors ----------------------------------------------------------

    public List<ResourceLocation> ids() {
        Entry[] snapshot = sources;
        if (snapshot == null) {
            return List.of();
        }
        List<ResourceLocation> out = new ArrayList<>(snapshot.length);
        for (Entry entry : snapshot) {
            out.add(entry.id());
        }
        return List.copyOf(out);
    }
}
