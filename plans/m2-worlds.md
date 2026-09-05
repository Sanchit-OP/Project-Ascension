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

### M2.1 — Design pass (design only, no implementation)

Write `docs/technical/worlds-api.md` and review it before writing code. This is the M1.1
pattern, and M1.1 earned it: the API shape is the most expensive thing in a module to change
once other modules coupled to it.

**Four things must be resolved. Three are architecture; one is content.**

#### 1. Orbit representation — the ADR-0004 deferral

Three candidates, not two. The third was not on the table when ADR-0004 was written:

| Option | Dimensions for v1 | Dimensions at 7 worlds |
|---|---|---|
| Orbit as a high-Y band of each surface | 3 | 7 |
| An orbit dimension per planet | 6 | 14 |
| **One shared interplanetary space dimension** | **4** | **8** |

The third also answers a question `dimensions.md` already asks — *"is interplanetary space a
navigable dimension?"* — and it is the only one of the three where travel between planets is a
journey rather than a transition. ADR-0004 rejected Ad Astra partly because *travel is a menu*;
two of these three options rebuild the menu.

#### 2. Planet data schema

Data-driven is locked by ADR-0004, so the schema **is** the API. `planets.md` already lists the
required fields per planet: orbit purpose, surface survival mechanic, movement mechanic, resource
gate, structure, boss condition, post-clear travel improvement. Not all of those are M2's
business, but the schema has to leave room for them or every later world reopens it.

#### 3. Tier 1 → Tier 1 dependencies — a gap in ADR-0003

A planet has an atmosphere. So `worlds` wants to tell `atmosphere` about it. But rule 6 says
`worlds` must work with only `core` present, and ADR-0003 says nothing about one Tier 1 module
depending on another — it only covers Tier 1 → third-party and Tier 1 → Tier 2.

This needs settling **now**, because it is not a `worlds` question. `gear` → `atmosphere` and
`progression` → `worlds` hit the same wall, and whatever is decided here is the pattern for all
of them. Options, in increasing cost:

- **Soft dependency.** `worlds` compiles against `atmosphere`'s `api` as `compileOnly` and
  registers an `AtmosphereProvider` only when `ascension_atmosphere` is loaded. Both modules
  stay standalone. This is the same shape as a Tier 2 compat jar, pointed inward.
- **Contract in `core`.** A neutral description of a world's environment that both read. Keeps
  the modules ignorant of each other, but grows Tier 0 into something `architecture.md`
  currently forbids: *"NeoForge only. Infra, no gameplay."*
- **A Tier 2 bridge between our own modules.** Consistent with the existing rules and almost
  certainly overkill.

Whatever is chosen wants an ADR, because it closes a hole rather than restating a rule.

#### 4. Distant Horizons compatibility

Recorded in `todo.md` as an open question and it belongs here. DH is what makes high render
distance affordable, and a custom dimension is exactly the thing that could defeat its LOD
generation. Worth answering while dimension design can still move.

**Verify:** nothing. This increment produces a document.

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

- [ ] `docs/technical/worlds-api.md` written and reviewed
- [ ] Orbit representation decided, with an ADR superseding ADR-0004's deferral
- [ ] Tier 1 → Tier 1 dependency policy decided, with an ADR
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
