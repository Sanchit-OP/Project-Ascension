# M1 — `ascension-atmosphere` v0.1

**Goal:** oxygen that is genuinely felt, testable entirely in the vanilla Overworld.

**Exit criteria:** a player can walk into a sealed non-breathable volume, watch oxygen drain,
feel the failure window, escape or die, refill from a tank and a station — on a dedicated
server, surviving world reload, with no measurable heap growth.

## Why this is first

It is the hardest system, not the easiest. Three reasons it still goes first:

1. **No dimensions required.** A test emitter block creates a non-breathable volume in the
   Overworld. We get the flagship system playable long before `ascension-worlds` exists.
2. **Its API shape is the most expensive thing in the project to change later.** `gear`,
   `worlds`, and every compat jar couple to it. Getting it wrong cheap is worth a lot.
3. **It is the module most likely to be adopted by others** (ADR-0003), so its public surface
   deserves the most care.

## Design constraints carried in from the register

- **ADR-0003**: provider registries, not inheritance. Any mod registers an `AtmosphereProvider`
  or `OxygenSource` without extending our classes.
- **ADR-0006**: zones must be describable relative to a **moving** region, not only static world
  coordinates — otherwise the Sable ship bridge is impossible later. This constrains the API
  now even though the bridge is built much later.
- **ADR-0007**: zone recomputation on block change only, bounded to the affected volume; no
  per-tick scans; delta sync; per-`ServerLevel` state.
- **`docs/gameplay/oxygen.md`**: authored logic, not gas simulation. Legible beats granular.
  If it becomes too granular to communicate, it is overdesigned.

---

## Increments

### M1.1 — API design pass (design only, no implementation)

Write `docs/technical/atmosphere-api.md` first and review it before writing code.

Must resolve:
- `AtmosphereProvider` — what answers "is this position breathable?", and the **resolution
  order** when several providers claim the same position. Ambiguity here is the classic source
  of unfixable interop bugs.
- `OxygenSource` — tanks, suits, blocks, vehicles, behind one interface.
- Zone representation — how a zone is anchored so it can later ride a moving sub-level.
- What is `api` (semver contract) versus `internal` (free to change).

**Verify:** design review against ADR-0003 and ADR-0006. No code yet.

### M1.2 — Breathability query + player oxygen state

Server-side only. Per-player oxygen via data attachment, per-`ServerLevel` zone state.
Hardcode "Overworld is breathable" and use a debug command for the rest.

**Verify:** `/ascension atmosphere query` reports breathability and oxygen level. Survives
world unload/reload.

> **Carries the deferred M0.3 requirement.** The dedicated-server path was never stood up
> during M0 — deliberately, since an empty mod could not exercise it. This is the first
> increment with real world state, so the full ADR-0008 server check happens here: server
> starts, client connects, mod present both sides, no client-only class referenced from
> server code. Do not let this slide again; every later increment builds on it.

### M1.3 — Sync + HUD

Delta-synced client mirror, oxygen bar. No per-tick packets.

**Verify:** HUD tracks server state in multiplayer with two clients connected; no packet spam
under a network profiler.

### M1.4 — Refactor pass (ADR-0008)

No features. API surface review before the failure model builds on it.

### M1.5 — Drain and failure model

Depletion, the short failure window, then death — the underwater-suffocation feel specified in
`docs/gameplay/oxygen.md`. Recovery on re-entering breathable air.

**Verify:** in-game death and recovery, both sides, on a dedicated server.

### M1.6 — Zone emitter block + volume caching

A test block that makes a sealed volume non-breathable. Flood-fill bounded and cached, cache
invalidated on block change within bounds only.

**Verify:** seal a room, break one block, watch it re-evaluate. Confirm no per-tick cost with
the profiler attached.

### M1.7 — Tank item + refill station

Portable reserve, refill block. The reserve-versus-efficiency tradeoff from `oxygen.md` is
designed here but not necessarily fully implemented in v0.1.

**Verify:** full expedition loop — leave breathable air, survive on tank, return, refill.

### M1.8 — Refactor + measurement

Per ADR-0007 rule 12: tick time, heap after GC, heap after three reload cycles, against the
M0.5 baseline. Any regression blocks completion.

---

## Definition of done

- [ ] `docs/technical/atmosphere-api.md` written and reviewed
- [ ] Full expedition loop playable
- [ ] Verified on a dedicated server with two clients
- [ ] Survives world unload/reload and dimension change
- [ ] No heap growth vs. M0.5 baseline
- [ ] `api` package reviewed as a third-party contract
- [ ] Two refactor passes done (M1.4, M1.8)
- [ ] Loads and works with **no other Ascension module present** (ADR-0003 rule 6)

## Explicitly out of scope for v0.1

Suit integration (that is `ascension-gear`), vehicle oxygen, Sable sub-level zones, per-planet
atmosphere data, and efficiency upgrades. v0.1 must make the *core loop* feel right; breadth
comes after.

## Open questions to resolve during M1.1

From `docs/gameplay/oxygen.md`, these need answers before the API is fixed:
- Does sprinting, combat, or hazard exposure increase consumption?
- Do sealed bases create local breathable zones, or only refill points?
- How visible is oxygen information in the UI?
- Can players create temporary field refills?
