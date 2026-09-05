# ADR-0007: Performance contract

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Sanchit's requirement: performance must be top-notch, with no excess RAM usage and no leaks,
optimised as we write rather than in a later pass.

This has to be a written contract rather than an intention. Minecraft mods degrade
performance in a small number of well-known, highly repeatable ways, and every one of them is
cheap to avoid at authoring time and expensive to retrofit. A per-tick world scan written in
week two is a rewrite in month six.

## Decision

The following are **binding rules on all Ascension code**, checked at review time.

### Memory and leaks

1. **No static or long-lived collection may hold a `Level`, `LevelChunk`, `Player`, `Entity`,
   or `BlockEntity` reference.** This is the single most common leak class in Minecraft
   modding: it pins an entire dimension in memory after unload. Store `ResourceKey`,
   `UUID`, or `BlockPos` and resolve on demand.
2. **Every registered listener, capability and cache must have a defined teardown**, hooked to
   level unload or server stopping. If you cannot name where it is released, it leaks.
3. **Server data structures are per-`ServerLevel`**, never global singletons keyed by
   dimension, so unload frees them naturally.

### Ticking

4. **No per-tick full-world or full-chunk scans.** All state is event-driven and dirty-flagged.
5. **Zone recomputation happens on block change only**, never on a schedule, and is bounded to
   the affected volume.
6. **Player-facing systems tick at the lowest rate that stays legible.** Oxygen does not need
   20 Hz; it needs to feel responsive. Pick the rate deliberately and document it.
7. **Nothing heavy on the client render thread.** Client state is a cached mirror, not a
   recomputation.

### Networking

8. **Server-authoritative, delta-synced.** No per-tick packets to any player.
9. **Sync on change, plus a low-frequency reconciliation**, not continuous streaming.

### Data and shape

10. **Data-driven over hardcoded.** Planets, zones and hazards are JSON. Adding content must not
    mean adding classes.
11. **No deep coupling to vanilla internals** beyond documented NeoForge extension points. This
    is also what keeps the eventual 1.21.11+ migration affordable (ADR-0001).

### Verification

12. **Every milestone ends with a measurement**, recorded in the milestone plan: baseline vanilla
    versus with-module, capturing average tick time (ms), heap after a forced GC, and heap
    after unloading and reloading a dimension three times. A milestone with a regression is not
    complete.
13. **Any third-party mod we consider depending on gets profiled before commitment**, not after.
    This applies to Sable specifically (ADR-0006).

## Consequences

- Slower to write; substantially cheaper to own.
- Rule 12 means every milestone plan carries a measurement step. That is intentional overhead.
- Some designs will be rejected for being unaffordable to tick. That is the contract working.
- Rules 1-3 in particular are the concrete meaning of "no leaks" — they are checkable, not
  aspirational.
