# M2 — `ascension-worlds` v0.1

**Goal:** a real off-world dimension you travel to, survive on, and come back from.

**Exit criteria:** a player leaves Earth, arrives above the Moon, descends under their own
control, survives on the surface because of equipment rather than luck, and returns — on a
dedicated server, surviving world reload, with the Moon authored as **data** rather than code.

## Why this is second

M1 built the system that makes an airless world mean something. Until now it has been tested
against `DebugAtmosphere`, a switchable vacuum that exists precisely because no real
non-breathable dimension existed. M2 is what retires it.

It also means atmosphere stops being tested against a stand-in built by the same person who
built the thing being tested — which is the weakest kind of test there is.

## Design constraints carried in from the register

- **[ADR-0004](../docs/decisions/0004-fully-custom-dimensions.md)**: fully custom dimensions, no
  Ad Astra. Planets are **data-driven JSON** — adding a world must be authoring, not
  engineering. And the deferred question lands here: **is orbit a separate dimension or a high-Y
  band?** ADR-0004 says it is decided in this design pass, before Moon work begins.
- **[ADR-0005](../docs/decisions/0005-v1-scope-vertical-slice.md)**: Earth + Moon + Planet 3.
  Planet 3 is in the slice to prove the framework is not Moon-shaped.
- **[ADR-0003](../docs/decisions/0003-modular-architecture-and-compatibility-policy.md)** rule 6:
  `worlds` must load and be testable with only `core` present.
- **[ADR-0007](../docs/decisions/0007-performance-contract.md)**: this is the module where the
  performance contract stops being theoretical. Dimensions are the expensive thing in Minecraft.
- **The render-distance constraint** recorded in `todo.md`: high render distance is *necessary*
  for space travel to read well, not a nice-to-have. That is a design input, not a settings
  question.

---

## Increments

### M2.1 — Design pass (design only, no implementation)  *(in progress)*

Design written and reviewed before any code, per the M1.1 pattern — M1.1 earned it, because a
module's API is the most expensive thing in it to change once other modules couple to it.

Four things to resolve. Three are architecture; one is content.

#### 1. Orbit representation — ✅ settled

**[ADR-0010](../docs/decisions/0010-orbit-is-one-shared-space-dimension.md): one shared
interplanetary space dimension**, superseding ADR-0004's deferral. A third option that was not
on the table when ADR-0004 was written, and the only one where travel between planets is a
journey rather than a transition — which matters, because ADR-0004 rejected Ad Astra partly on
the grounds that *travel is a menu*, and both other options rebuild the menu.

Four dimensions for v1 instead of six; eight instead of fourteen at seven worlds.

#### 2. Planet data schema — 🔶 drafted, awaiting review

[`docs/technical/worlds-api.md`](../docs/technical/worlds-api.md).

Two boundaries do most of the work. **A planet references a dimension, it does not define one** —
vanilla datapacks already own dimension JSON, biomes and worldgen, and competing with that would
put us in the worldgen business and break every tool that reads it. And **the registry describes
where a world is and what it is like, not what happens on it** — `planets.md`'s "required fields
per planet" is an authoring checklist, not a schema; a boss is an entity in a structure.

Open for review: the distance scale between planets, whether Earth belongs in the registry at
all given it has no custom dimension, and how a player reaches space from Earth's surface.

#### 3. Tier 1 → Tier 1 dependencies — ✅ settled

**[ADR-0011](../docs/decisions/0011-tier-1-modules-share-contracts-through-core.md): Tier 1
modules never depend on each other; shared concepts are contracts in `core`.** This amends
ADR-0003, which covered Tier 1 → third-party and Tier 1 → Tier 2 and was silent on this.

Not a `worlds` question — `gear` → `atmosphere` and `progression` → `worlds` hit the same wall,
so it is now the pattern for all of them. It also makes `core` more than a stub for the first
time, and moves the line on what Tier 0 may contain: contracts, never behaviour.

The cost, stated in `worlds-api.md` rather than buried: `atmosphere` currently depends on
nothing but NeoForge and now gains a required dependency on `core`, so an adopter needs two jars
instead of one.

#### 4. Distant Horizons compatibility — ⬜ open

DH is what makes high render distance affordable, and a custom dimension is exactly the thing
that could defeat its LOD generation. Worth answering while dimension design can still move —
and cheaply answerable with a throwaway dimension before M2.2 commits to anything.

**Verify:** nothing. This increment produces documents.

### M2.2 — The Moon surface, authored as data

One dimension, registered from JSON through the schema M2.1 fixes, reachable by command. Airless.

Authored as data from the first world rather than hardcoded and generalised later — ADR-0004's
whole point is that worlds 4–7 are authoring work, and a registry retrofitted around one
hardcoded planet is how that promise quietly breaks.

**Verify:** `/execute in ascension_worlds:moon run tp @s ...` puts you somewhere that generates,
persists across reload, and **suffocates you** — with `DebugAtmosphere` switched off. That last
part is the increment's real purpose: it is the first time the atmosphere system is tested
against something it was not built alongside.

### M2.3 — Moon worldgen

Biomes, surface rules, ore placement, and whatever makes it read as the Moon rather than a grey
Overworld.

**Verify:** fly it with Distant Horizons on and Chunky pre-generating, per M2.1's answer.

### M2.4 — Refactor pass (ADR-0008)

No features. Scheduled, not discretionary.

### M2.5 — Orbit and arrival

Per M2.1's decision. Arrival above the Moon, under the player's control.

### M2.6 — Descent, landing, and failure

Manual descent. `dimensions.md` asks how failed landings are handled; this is where it is
answered in code rather than prose.

### M2.7 — Refactor + measurement

Against the M1.8 full-stack reading, plus the A/B. Dimensions are where ADR-0007 gets tested
properly: two more dimensions means two more of everything the performance contract worries
about.

---

## Definition of done

- [ ] `docs/technical/worlds-api.md` written and **reviewed** (drafted 2026-09-05)
- [x] Orbit representation decided, with an ADR superseding ADR-0004's deferral — ADR-0010
- [x] Tier 1 → Tier 1 dependency policy decided, with an ADR — ADR-0011
- [ ] The Moon authored entirely as data — no per-planet Java
- [ ] Full loop playable: leave Earth, orbit, descend, survive, return
- [ ] Verified on a dedicated server
- [ ] Survives world unload/reload **and** dimension change
- [ ] Atmosphere works on the Moon with `DebugAtmosphere` off
- [ ] No regression against the M1.8 full-stack reading
- [ ] Loads and works with **no other Ascension module present** (ADR-0003 rule 6)
- [ ] Two refactor passes done (M2.4, M2.7)

## Explicitly out of scope for M2 v0.1

Planet 3, the gateway network, bosses, ships as playspaces (that is Sable, ADR-0006), and suit
modules (`ascension-gear`). M2 proves *one* world end to end. Breadth comes after — the same
argument ADR-0005 makes for the campaign as a whole.

## Carried over from M1, unresolved

- **Two-client testing.** Unticked in M1's definition of done and deliberately skipped for lack
  of a second client. Oxygen is per-player state with per-player sync, and that has never been
  observed with two players.
- **Share-air rescue.** Designed at the M1.1 review, the reason `accept` exists on
  `OxygenSource`, still unbuilt. Needs two players to mean anything.
- **M1.9 refactor + measurement.** Scheduled by ADR-0008 and not yet done.
