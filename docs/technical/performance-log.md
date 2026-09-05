# Performance Log

Required by [ADR-0007](../decisions/0007-performance-contract.md) rule 12: every milestone ends
with a measurement, and a milestone with an unexplained regression is not complete.

## What a baseline can be compared to

**Settled 2026-09-05.** A baseline is only a baseline *for the configuration it was taken in*.
Mod set, single-player versus dedicated server, and heap ceiling are all part of it. Comparing
across configurations produces a number that looks like evidence and is not.

This was decided when optimisation mods were planned for the M2 boundary, and it immediately
exposed a problem in the reading already recorded here: **M0.5 was taken in single-player, on
the integrated server, with no third-party mods.** Since M1.3 the test loop has been a dedicated
server with a separate client, and JEI and Curios are now installed. The M0.5 tick figure is
therefore not comparable to anything we measure today, and was quietly on its way to being cited
as though it were.

### So the primary signal is an A/B, not a historical baseline

**Measure our mod on versus our mod off, in the same session, on the same stack.**

That answers the question ADR-0007 rule 12 actually cares about — *did our code cost anything* —
and it is immune to everything that makes historical baselines rot: mod updates, a new
optimisation mod, a different machine, a changed heap ceiling. Both halves of the comparison
move together.

Absolute baselines are still recorded, for context and for spotting slow drift across
milestones. They are just no longer the thing a milestone is judged on.

### Two configurations, on purpose

| Configuration | What it is for |
|---|---|
| **Clean** — our modules only | One reading per module release. This is the number an adopter installing `ascension-atmosphere` into their own pack cares about, and ADR-0003 makes that priority. |
| **Full stack** — optimisation mods and all | The pack's real performance, which is what a player experiences. Becomes the standing reference from M2 onward. |

Every recorded reading names its configuration and the exact mod versions. Changing the mod set
invalidates the absolute numbers and means retaking them; it does **not** invalidate an A/B.

## Profiling with JFR

Java Flight Recorder is in the JDK the build already pins, so this needs no mod and no
dependency. It is off unless asked for.

```bash
./gradlew :modules:atmosphere:runServer -Pjfr
```

Play, then **quit the server cleanly** — `dumponexit` is what writes the file, so killing the
process gets you nothing. The recording lands at `run/server/ascension-server.jfr`
(`-Pjfr` works on `runClient` too, writing `run/client/ascension-client.jfr`).

Reading it, with the `jfr` tool that ships alongside `java`:

```bash
jfr summary run/server/ascension-server.jfr
```

```bash
jfr print --events jdk.ObjectAllocationSample run/server/ascension-server.jfr
```

Or open the file in JDK Mission Control for flame graphs and the heap-over-time view.

**This is the tool that closes the gaps listed under M0.5 below.** The F3 method that follows
asks for a sawtooth low point to be eyeballed and three reload cycles to be remembered, which is
exactly why neither was ever captured. JFR gives the live set after GC and allocation attributed
to a call site, which is what ADR-0007 rule 1 and rule 12 are really asking for.

Stack depth is raised to 256 frames deliberately. At the default 64, an allocation inside our
code truncates before the stack reaches a `com.ascension` frame, and "who allocated this" becomes
"something, somewhere in Minecraft".

## How to take a reading

The F3 method below still works for a quick look, and is what the M0.5 row was taken with.
Prefer JFR for anything being recorded as a milestone measurement.

In game, F3. Record:

| Metric | Where on F3 | Notes |
|---|---|---|
| Tick time | `Integrated server @ X/Y ms` | X is tick time, Y is the 50 ms budget |
| FPS | top left | Note any frame cap, or the number is meaningless |
| Memory use | right column | Sawtooths constantly — see below |
| Allocation rate | right column | MB/s of garbage produced |
| Allocated | right column | Committed heap, not live data |
| Packets | `tx / rx` | Watch for per-tick chatter |

### The memory reading that actually matters

`Memory use` climbs continuously and drops when GC runs, so any single reading is close to
meaningless. **The number we track is the low point of the sawtooth** — the value immediately
after it falls. That approximates the live set: memory genuinely still referenced.

A leak shows up as that *low point* climbing over time, not as a high peak.

### Leak check

1. Note the sawtooth low point in a loaded world.
2. Quit to title, rejoin the world. Three times.
3. Read the low point again.

Flat across cycles is healthy. Climbing means something is holding a `Level`, `Player` or
`BlockEntity` past unload — [ADR-0007](../decisions/0007-performance-contract.md) rule 1.

---

## M0.5 — Baseline: vanilla + NeoForge + empty `ascension_core`

**Date:** 2026-09-05
**Instance:** `Ascension Dev` (CurseForge), MC 1.21.1 / neoforge-21.1.249
**Mods:** `ascension_core` 0.1.0 only — no gameplay code
**Heap ceiling:** 4 GB (CurseForge allocation)

| Metric | Reading | Read |
|---|---|---|
| Integrated server tick | **5 ms / 50 ms** | 10% of the tick budget. Healthy headroom. |
| FPS | **118** (capped ~120) | Effectively at cap; FPS is not a useful signal until the cap is lifted. |
| Memory use | 1150 MB / 4 GB | Instantaneous, mid-sawtooth. **Not yet the tracked figure.** |
| Allocated (committed) | 1408 MB (34%) | Heap the JVM has claimed, not live data. |
| Allocation rate | 60 MB/s | Normal vanilla-ish churn. |
| Packets | 1 tx / 380 rx | Single-player integrated server. |

### What this baseline is good for

**Tick time is the number that matters most**, and 5 ms is the figure every later milestone gets
compared against. `ascension-atmosphere` must not move it meaningfully — it is event-driven and
recomputes only on block change, so any measurable rise means a rule in ADR-0007 has been
broken somewhere.

Allocation rate is the second signal. Oxygen accounting runs every 10 ticks; if allocation rate
climbs noticeably after M1, something is allocating per-tick that should not be.

### Gaps — still outstanding

- **Sawtooth low point not captured.** `1150 MB` is a mid-cycle reading, so it cannot serve as
  the leak reference. Needs the low-point method above.
- **Reload cycles not run.** Without the three unload/reload readings there is no leak baseline,
  and leak detection is the entire point of tracking memory here.
- **Sable + Aeronautics not profiled.** Required by ADR-0006 before anything depends on them.
  Deferred — nothing depends on them yet.

**Superseded 2026-09-05.** These gaps are no longer closed by retaking this row. M0.5 was
single-player with no third-party mods, and neither is true of how we test now — so the row is
kept as history and is not a comparison target. See *What a baseline can be compared to* above.

M1.9 takes a fresh reading on the real configuration (dedicated server, current mod set) and
runs the atmosphere-on / atmosphere-off A/B, which is the measurement that actually signs the
milestone off.
