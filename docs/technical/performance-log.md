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
./gradlew runDevServer -Pjfr
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

## M2.6 — Dimension transition reading (Earth ↔ Moon round trip)

**Date:** 2026-09-06
**Configuration:** dedicated server (`localhost`) + CurseForge client, generational ZGC
**Mods:** the full 15-mod optimisation stack, plus `ascension_core`/`ascension_atmosphere`/
`ascension_worlds`/`ascension_compat_chunky` 0.1.0
**Test:** Moon → Earth → Moon, two real dimension transitions through `ascension_worlds:space`
(ascent, approach, descent — the full M2.5/M2.6 mechanism), captured with `/spark profiler start`
/ `/spark profiler stop` across both trips
**Report:** https://spark.lucko.me/LKUMbP2xmy

| Metric | Reading |
|---|---|
| TPS | **20.00** flat for the entire ~148s window |
| MSPT | median 3.15 ms, 95th %ile 24.3 ms, max 547 ms |
| CPU (process) | 17.6–18.4% |
| Memory (process) | 1.2 GB / 5.5 GB (21.5%) |

### Our share: below a tenth of a percent

Spark's per-mod time attribution (Server thread):

| Mod | Share |
|---|---|
| `ascension_worlds` | 0.08% |
| `ascension_atmosphere` | 0.02% |
| `ascension_core` | 0.00% |
| `ascension_compat_chunky` | didn't register at all |

For scale: `neoforge` itself is 4.26%, and spark's own profiler overhead is 0.69% — both bigger
than everything we've built combined.

### Where the 547 ms spike and the 24.3 ms tail actually come from

Not from us. The self-time-sorted flat view has no `com.ascension` frame anywhere in it. The real
cost sits in vanilla chunk I/O around the transition: `ChunkMap.processUnloads` (9.60%),
`ChunkSerializer.read`/`write` (4.25% / 1.29%), and the `com.mojang.serialization` codec
decode/encode machinery underneath chunk NBT parsing (roughly 15% combined across several codec
frames). That is the inherent cost of any dimension change touching real chunks, present in
vanilla Minecraft with zero Ascension mods installed — not something M2.5 or M2.6 added.

### Read

**M2.6's own "measured, not asserted" verify step: satisfied for the cost question.** Two real
round trips through `ascension_worlds:space` — the exact mechanism `SpaceMechanics` drives —
held 20 TPS throughout, and the entire Ascension module set never clears a tenth of a percent of
server thread time doing it. The transition-time cost that does exist is vanilla's own chunk
churn, which is precisely what M2.6 lever #1 (pre-loading) and lever #2 (Chunky pre-generation)
exist to get ahead of — this reading doesn't isolate how much they help (see caveat below), only
confirms the cost sitting there isn't something our code is adding on top of vanilla's own.

**Not an A/B, and not yet a "does pre-loading help" measurement.** This is a single reading with
everything on, on a world Chunky had only partly pre-generated at the time — it doesn't isolate
"cost of our code" from "cost of a vanilla dimension change" the rigorous way M1.9's
atmosphere-on/off comparison did, and it doesn't compare against a run with the pre-load ticket
disabled. The per-mod breakdown above is the next best thing to an A/B without a second run, and
answers the question that actually mattered here: is any of this ours.

---

## M1.8 — Full stack, first reading

**Date:** 2026-09-05
**Configuration:** dedicated server (`localhost`) + CurseForge client, `view-distance=8`,
`simulation-distance=6`, G1 (this reading predates the switch to ZGC)
**Mods:** 15 third-party — Sodium, Distant Horizons, EntityCulling, MoreCulling,
ImmediatelyFast, BadOptimizations, Lithium, FerriteCore, ModernFix, Chunky, Alternate Current,
Spark, Curios, JEI, Cloth Config — plus `ascension_atmosphere` 0.1.0 and `ascension_core` 0.1.0
**Chunky pre-generation:** not yet run
**Tools:** Spark `/spark health`, `/spark profiler`, `/spark heapsummary`

| Metric | Reading | Read |
|---|---|---|
| TPS | **20** | At cap. |
| Server tick | 4, 6, 8, **28**, 3, 6, 19, **118** ms | Median around 6 ms — better than the 5 ms M0.5 figure suggests, given fifteen more mods. The spikes are the story, see below. |
| CPU (system) | 70–89% | High, and consistent with DH generating LODs for three dimensions at once. |
| CPU (process) | 42–49% | |
| Memory | 1.1 GB | |
| Client FPS | **200** | Cap lifted, so this number finally means something. |
| Heap (Spark) | **535 MB** across 17,368 classes | |

### Our share of the heap: 1,408 bytes

Parsed out of the Spark heap report rather than eyeballed:

| Class | Instances | Bytes |
|---|---|---|
| `ProviderRegistry$Entry` | 3 | 72 |
| `OxygenEmitterBlock` | 1 | 72 |
| `OxygenRefillStationBlock` | 1 | 72 |
| `OxygenState` | **1** | 40 |
| `SealedVolumeIndex` | **1** | 24 |
| `OxygenSyncPayload` | **1** | 32 |
| ...59 more, all singletons or lambdas | | |
| **total** | **68** | **1,408** |

**0.00026% of the heap.** More to the point, the counts are all **one**: one `OxygenState` for
one player online, one `SealedVolumeIndex` for one level with emitters, one in-flight
`OxygenSyncPayload`. Nothing accumulating anywhere — which is precisely what
[ADR-0007](../decisions/0007-performance-contract.md) rule 1 exists to catch, and the first
direct evidence we have that it holds.

### Where the heap actually goes

`long[]` 104 MB, `int[]` 98 MB, `byte[]` 89 MB, `BlockPos` 541,190 instances / 13 MB,
structure templates 518,241 instances, `PalettedContainer` 170,689 instances.

170,689 chunk sections is roughly 7,000 chunks resident, far more than `view-distance=8` needs
on its own. That is Distant Horizons holding chunk data to build LODs from, and it accounts for
the memory and the CPU together.

### The tick spikes

118 ms is more than twice the tick budget, and it appeared while flying on an elytra — visible
as the world "changing a lot" while moving fast. Two causes, stacked:

1. **Chunk generation.** Chunky has not been run, so flying generates terrain in the hot path.
2. **DH building LODs** for three dimensions concurrently, on first run.

Neither is ours: nothing from `com.ascension` appears in the profile. The fix for the first is
Chunky pre-generation; the second is a one-time cost that settles.

**This is not the M1.9 signal.** It is an absolute reading on a stack that is still warming up,
recorded for context. M1.9 takes the A/B.

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
