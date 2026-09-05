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

### M1.1 — API design pass (design only, no implementation)  *(done — 2026-09-05)*

Write `docs/technical/atmosphere-api.md` first and review it before writing code.

Must resolve:
- `AtmosphereProvider` — what answers "is this position breathable?", and the **resolution
  order** when several providers claim the same position. Ambiguity here is the classic source
  of unfixable interop bugs.
- `OxygenSource` — tanks, suits, blocks, vehicles, behind one interface.
- Zone representation — how a zone is anchored so it can later ride a moving sub-level.
- What is `api` (semver contract) versus `internal` (free to change).

**Done.** `docs/technical/atmosphere-api.md` written and reviewed; API frozen for v0.1.
Five decisions settled at review: bar + minimal hazard indicator, `drainMultiplier` ships
in v0.1, integer storage with seconds on display, sprinting does not cost oxygen (drain
modifiers became a registry instead), and share-air rescue (which added `accept` to
`OxygenSource`).

### M1.2 — Breathability query + player oxygen state  *(done — 2026-09-05)*

Server-side only. Per-player oxygen via data attachment, per-`ServerLevel` zone state.
Hardcode "Overworld is breathable" and use a debug command for the rest.

**Verified in game** (CurseForge `Ascension Dev`, 1.21.1 / neoforge-21.1.249):

- `query` reports breathable, and reports NOT breathable after `debug vacuum`
- `why` correctly lists providers in resolution order and attributes the win to
  `ascension_atmosphere:debug_vacuum` — confirming both priority resolution and the
  diagnostic path work end to end
- `debug clear` restores breathable

> **Dedicated-server check: still outstanding, now due at M1.3.**
>
> This is the second time it has moved, which is exactly how a requirement quietly dies, so it
> is written down rather than left implied. The honest reason: M1.2 is server-side only and has
> no sync, so a dedicated server would only prove that no client-only class is referenced —
> real, but thin. M1.3 introduces the client mirror and the HUD, which is where side-only bugs
> actually live and where single-player's integrated server stops being representative.
>
> **M1.3 does not close without it.** No third deferral.

### M1.3 — Sync + HUD  *(done — 2026-09-05)*

Delta-synced client mirror, oxygen bar. No per-tick packets.

**Verified on a dedicated server** (`localhost`, online-mode=false, NeoForge 21.1.249):

- server started clean with `Ascension Atmosphere loaded (0.1.0)` and no client-class errors,
  which is what the dist-separation work was protecting
- HUD rendered from genuine network state: the client's default is `0 units, breathable,
  hidden`, so a visible bar reading `2m 30s` required `units`, `breathable` and
  `drainPerSecond` to all arrive from the server
- changing oxygen with `debug oxygen <n>` updated the bar each time, confirming repeated
  change-driven sync rather than a single packet at join

Two findings worth keeping:

1. **HUD position.** `registerAbove(VanillaGuiLayers.AIR_LEVEL, ...)` sets draw *order*, not
   layout. The first version drew centred above the hotbar; it now anchors to the right-hand
   status column where vanilla shows air, lifting 10px only while genuinely underwater.
2. **Op permissions.** `debug` requires permission level 2, and Brigadier hides subcommands the
   caller cannot use, so the whole branch was invisible on a fresh server. Single-player never
   showed this because the host is op by default — a test-setup gap that only a real server
   exposes.

**M1.4 refactor findings:** fully-qualified names left behind by patch scripts (cleaned), and a
per-tick allocation in `capacityOf` — a captured `int[]` box allocated per collector, per
player, per accounting pass, inside the one loop ADR-0007 asks to keep quiet. Replaced with a
reusable accumulator that allocates nothing when no collectors are registered.



### M1.4 — Refactor pass (ADR-0008)  *(done — 2026-09-05)*

No features. API surface review before the failure model builds on it.

### M1.5 — Drain, failure model, and water unification

Depletion, the short failure window, then death — the underwater-suffocation feel specified in
`docs/gameplay/oxygen.md`. Recovery on re-entering breathable air.

**Also in this increment: water counts as unbreathable atmosphere.** Drowning and vacuum are
the same problem, so one bar and one failure timer covers both. Respiration and turtle helmets
map to `gearEfficiency`; conduit power claims breathable at the `STRUCTURE` band; an oxygen tank
works underwater for free. Built in but individually switchable, defaulting on, because
silently seizing a vanilla mechanic would make this module untrustworthy to adopt. See
`docs/technical/atmosphere-api.md` section 5b.

**Verify:** in-game death and recovery, both sides, on a dedicated server. Plus: drown with the
feature off (vanilla behaviour intact) and with it on (our bar, our timer).

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
