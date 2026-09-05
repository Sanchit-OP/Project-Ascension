# Performance Log

Required by [ADR-0007](../decisions/0007-performance-contract.md) rule 12: every milestone ends
with a measurement, and a milestone with an unexplained regression is not complete.

## How to take a reading

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

These do not block M1.2. They must be closed before M1.8 signs off, since M1.8 compares against
this row.
