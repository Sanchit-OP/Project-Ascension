package com.ascension.combat.internal.rune.hud;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import com.ascension.combat.internal.rune.aetherwright.ArcState;
import com.ascension.combat.internal.rune.aetherwright.StarfallState;
import com.ascension.combat.internal.rune.bulwark.BraceState;
import com.ascension.combat.internal.rune.bulwark.PlatingState;
import com.ascension.combat.internal.rune.hud.RuneHudEntry.Kind;
import com.ascension.combat.internal.rune.vanguard.CadenceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reads the running state of each equipped rune that has something worth showing, in the one shape
 * the HUD draws ({@link RuneHudEntry}). It only reads: no rune's behaviour changes, and a rune with
 * no running state (a pure stat bonus) has no probe and no chip.
 *
 * <p>Adding a rune to the HUD is one line in {@link #PROBES}.
 */
public final class RuneHudProbes {

    /** Reads one rune's state; {@code rune} is its definition from the player's page. */
    @FunctionalInterface
    private interface Probe {
        RuneHudEntry read(ServerPlayer player, RuneDefinition rune, ResourceLocation id, int path, long now);
    }

    private record Registered(ResourceLocation id, Probe probe) {
    }

    private static final List<Registered> PROBES = List.of(
            // Counts that build.
            new Registered(Runes.id("conquerors_resolve"), (p, r, id, path, now) -> {
                CombatStackState s = p.getData(RuneAttachments.CONQUERORS_RESOLVE);
                return new RuneHudEntry(id, path, Kind.STACKS, s.stacks(), r.count("max_stacks", 10), 0L);
            }),
            new Registered(Runes.id("relentless_strikes"), (p, r, id, path, now) ->
                    stacking(p.getData(RuneAttachments.RELENTLESS_STRIKES), r.count("max_stacks", 4), id, path, now)),
            new Registered(Runes.id("momentum"), (p, r, id, path, now) ->
                    stacking(p.getData(RuneAttachments.MOMENTUM), r.count("max_stacks", 5), id, path, now)),

            // Runs toward a payoff.
            new Registered(Runes.id("cadence"), (p, r, id, path, now) -> {
                int needed = r.count("hits", 3);
                CadenceState s = p.getData(RuneAttachments.CADENCE);
                return new RuneHudEntry(id, path, Kind.CHARGE, Math.min(s.hits(), needed), needed, 0L);
            }),
            new Registered(Runes.id("arc"), (p, r, id, path, now) -> {
                int needed = r.count("hits", 4);
                ArcState s = p.getData(RuneAttachments.ARC);
                return new RuneHudEntry(id, path, Kind.CHARGE, s.charged() ? needed : Math.min(s.hits(), needed),
                        needed, 0L);
            }),

            // Time until it can fire again.
            new Registered(Runes.id("second_wind"), (p, r, id, path, now) ->
                    cooldown(p.getData(RuneAttachments.SECOND_WIND_COOLDOWN), id, path)),
            new Registered(Runes.id("pack_leader"), (p, r, id, path, now) ->
                    cooldown(p.getData(RuneAttachments.PACK_SAVE), id, path)),
            new Registered(Runes.id("starfall"), (p, r, id, path, now) -> {
                StarfallState s = p.getData(RuneAttachments.STARFALL);
                return new RuneHudEntry(id, path, Kind.COOLDOWN, 0, 0, s.cooldownUntil());
            }),
            new Registered(Runes.id("brace"), (p, r, id, path, now) -> {
                BraceState s = p.getData(RuneAttachments.BRACE);
                return new RuneHudEntry(id, path, Kind.COOLDOWN, 0, 0, s.cooldownUntil());
            }),

            // Windows that open for a while.
            new Registered(Runes.id("drop_in"), (p, r, id, path, now) ->
                    window(p.getData(RuneAttachments.DROP_IN), id, path)),
            new Registered(Runes.id("riposte"), (p, r, id, path, now) ->
                    window(p.getData(RuneAttachments.RIPOSTE), id, path)),
            new Registered(Runes.id("killing_surge"), (p, r, id, path, now) ->
                    window(p.getData(RuneAttachments.KILLING_SURGE_BURST), id, path)),

            // A window, then a cooldown.
            new Registered(Runes.id("vanish"), (p, r, id, path, now) -> {
                DurationState open = p.getData(RuneAttachments.VANISH_WINDOW);
                if (open.isActive(now)) {
                    return new RuneHudEntry(id, path, Kind.WINDOW, 0, 0, open.expiresAtGameTime());
                }
                return cooldown(p.getData(RuneAttachments.VANISH_COOLDOWN), id, path);
            }),
            new Registered(Runes.id("plating"), (p, r, id, path, now) -> {
                PlatingState s = p.getData(RuneAttachments.PLATING);
                if (s.active(now)) {
                    return new RuneHudEntry(id, path, Kind.WINDOW, s.hitsLeft(), 0, s.windowEnds());
                }
                return new RuneHudEntry(id, path, Kind.COOLDOWN, 0, 0, s.cooldownUntil());
            }));

    private RuneHudProbes() {
    }

    /** Every equipped rune that has state to show, in a fixed order, for this player right now. */
    public static List<RuneHudEntry> read(ServerPlayer player, long now) {
        List<RuneHudEntry> out = null;
        for (Registered probe : PROBES) {
            Optional<RuneDefinition> rune = Runes.active(player, probe.id());
            if (rune.isEmpty()) {
                continue;
            }
            int path = rune.get().path().map(RunePath::ordinal).orElse(0);
            if (out == null) {
                out = new ArrayList<>(4);
            }
            out.add(probe.probe().read(player, rune.get(), probe.id(), path, now));
        }
        return out == null ? List.of() : out;
    }

    private static RuneHudEntry stacking(StackingChargeState s, int max, ResourceLocation id, int path, long now) {
        // Stacks the rune has not cleared yet but whose time is up count as gone.
        int held = now < s.expiresAtGameTime() ? s.stacks() : 0;
        return new RuneHudEntry(id, path, Kind.STACKS, held, max, s.expiresAtGameTime());
    }

    private static RuneHudEntry cooldown(DurationState s, ResourceLocation id, int path) {
        return new RuneHudEntry(id, path, Kind.COOLDOWN, 0, 0, s.expiresAtGameTime());
    }

    private static RuneHudEntry window(DurationState s, ResourceLocation id, int path) {
        return new RuneHudEntry(id, path, Kind.WINDOW, 0, 0, s.expiresAtGameTime());
    }
}
