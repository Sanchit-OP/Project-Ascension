# ADR-0026: Air is a fluid, and a dispenser holds the room

- **Status:** Proposed (waiting on Sanchit's answers, 2026-10-19; see `docs/technical/oxygen-v2.md`)
- **Date:** 2026-10-19

## Context

Version 1 of `ascension-atmosphere`'s machines is scaffolding: one free emitter holds a room, one
free station fills a tank, and neither uses any other mod. Sanchit wants air made by a Create
machine, carried by Create's tanks and pipes, and spent by a block that holds a closed room
(a base, or a ship's cabin).

## Decision (proposed)

1. **Air is a NeoForge fluid** (`ascension_atmosphere:air`, no bucket, no world block), 1 unit of
   oxygen = 1 mB. Tier 1 owns it.
2. **The Air Dispenser is Tier 1** and exposes `Capabilities.FluidHandler.BLOCK`, accepting only
   air. Create's pipes and tanks, and any other mod's, connect with no Create code in Tier 1
   (ADR-0003 rule 1 holds).
3. **The powered Air Compressor is Tier 2** (`ascension-compat-create`, its first Java and first
   compile dependency): rotation in, air out. Right-click fills a held portable tank.
4. **The old emitter and station stay** as the free "basic" versions so Tier 1 works alone;
   `ascension-compat-create` removes their recipes in the pack, as it does for Create's backtank.
   Item ids the quest book uses do not change.
5. **Ships:** the Sable side of `AnchorSpace` goes in `ascension-compat-sable`. Tier 1 learns
   nothing about Sable (ADR-0006, ADR-0016).
6. **Level-2 tank** is its own item, made by a smithing upgrade that keeps charge and valve state,
   with helium named through a common item tag so `atmosphere` never names `worlds` (ADR-0011).

## Why not

- **Create code in Tier 1:** breaks ADR-0003.
- **A bespoke pipe system:** Create already has one; players expect it.
- **Gas physics:** `docs/gameplay/oxygen.md` forbids it; one plain rule per machine instead.

## Consequences

- The compat-create jar needs a compile-time Create (same approach as `compat/sable`, `compat/jade`).
- Existing worlds keep their emitters and stations untouched.
- Open until Sanchit answers: see the questions in `oxygen-v2.md`.
