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

Settled since drafting: **Earth `[0, 0]`, Moon `[8000, 0]`** — 8000 blocks is the distance you
actually fly, roughly 2–4 minutes each way under power. **Earth is in the registry** as
`surface: minecraft:overworld`, so flying home uses the same descent as every other world
instead of being a special case.

Two consequences that reach M2.5: a planet in space is a **rendered body, not built blocks** —
at 500 chunks away nothing made of blocks will ever draw — and Earth therefore needs a
`body_radius` of its own.

Still open: how tall the space dimension should be, and how a player gets from Earth's surface
into space in the first place.

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

#### 4. Distant Horizons compatibility — ⏭ deferred to M2.5, and it does not get a vote

This plan originally wanted it answered early, with a throwaway dimension, on the grounds that a
negative answer would reshape dimension design.

**That was the wrong framing.** DH is not the game — it is an optional third-party optimisation
mod, and letting one constrain our Tier 1 design inverts ADR-0002 and ADR-0003. A negative
result is DH's problem to route around, not a reason to reshape a planet. So it becomes a
compatibility check against the real thing at M2.5, not a design input before it.

**Verify:** nothing. This increment produces documents.

### M2.2 — The Moon surface, authored as data  *(done, verified in game)*

One dimension, registered from JSON through the schema M2.1 fixes, reachable by command. Airless.

Authored as data from the first world rather than hardcoded and generalised later — ADR-0004's
whole point is that worlds 4–7 are authoring work, and a registry retrofitted around one
hardcoded planet is how that promise quietly breaks.

**Verify:** `/execute in ascension_worlds:moon run tp @s ...` puts you somewhere that generates,
persists across reload, and **suffocates you** — with `DebugAtmosphere` switched off. That last
part is the increment's real purpose: it is the first time the atmosphere system is tested
against something it was not built alongside.

**Result:** it does. The Moon generates, persists, and suffocates you with `DebugAtmosphere`
untouched, on the dev server. `DebugAtmosphere` is now a dev tool rather than the only vacuum in
the project, which is the thing M1 could not test for itself.

Gravel to the horizon, as authored — the flat placeholder's top layer. Terrain is M2.3.

### M2.3 — Moon worldgen

Biomes, surface rules, ore placement, and whatever makes it read as the Moon rather than a grey
Overworld.

**Verify:** fly it with Distant Horizons on and Chunky pre-generating, per M2.1's answer.

### M2.4 — Refactor pass (ADR-0008)  *(done)*

No features. Scheduled, not discretionary.

Pulled the pure arithmetic out of `OxygenTracker` and `TankRules` into small,
Minecraft-free classes (`OxygenAccounting`, `TankCarrySweep`), and merged the tank
item's and the HUD's separately-duplicated low/critical thresholds into one
(`OxygenLevel`). 64 unit tests now exist across `core` and `atmosphere`, up from 17
all in `core`; `atmosphere` had none before this pass. Found and fixed one real bug
along the way: the at-risk accounting path was gathering a player's oxygen sources
twice per pass instead of once.

`DebugAtmosphere` was reviewed rather than removed — it is still useful for
reaching an unbreathable state without a real one on hand, just no longer the
*only* one, so it stays as a documented dev tool.

### M2.5 — Orbit and arrival  *(done, confirmed in a live client)*

Per M2.1's decision. Arrival above the Moon, under the player's control.

**Scoped 2026-09-06.** The data model this needed — the planet registry, `Planet`/`SpacePosition`,
`WorldEnvironment` and its `atmosphere` linkage — already existed and was already proven by M2.2.
Nothing here was a design gap; M2.5 built the player-facing mechanics on top of it.

**Implemented 2026-09-06.** Took several live-client rounds to actually get right — see "Real bugs
found live" below and the checklist at the end of this section. Sanchit's call 2026-09-06: "
satisfactory enough to call this 2.5 done."

1. **The `ascension_worlds:space` dimension.** `dimension_type/space.json` /
   `dimension/space.json`. Void (`minecraft:flat`, empty layers, `the_void` biome), no mob
   spawning. **Height settled at `min_y: -512, height: 1024`** (symmetric ±512 around the
   planetary plane at `y: 0`) rather than this section's earlier `0`–`512` draft — Sanchit's
   call, made explicit as `SpaceDimension.VERTICAL_BOUND = 512`.
2. **`SpaceEnvironment`**, a `WorldEnvironmentSource` sibling to `PlanetEnvironments` (space is
   not a planet *surface*, so the existing one never touches it) — registers
   `ascension_worlds:space` as `WorldEnvironment.AIRLESS`.
3. **Ascent.** `SpaceMechanics` reads the *current dimension's own* `getMaxBuildHeight()` rather
   than a hardcoded altitude — so this works unchanged for any planet's surface, not just Earth's
   320, with no per-planet Java (ADR-0004). Crossing it while airborne calls
   `ServerPlayer#changeDimension` with a `DimensionTransition` carrying the player's own
   `getDeltaMovement()`/yaw/pitch, landing at that planet's `SpacePosition`. Vanilla elytra +
   rockets; no new gear (worlds-api.md §6 item 4).
4. **Approach detection, plus a placeholder descent.** `SpaceMechanics` checks
   `SpacePosition.distanceSquaredTo` against `approachRadius` every tick while in space, using a
   new per-player attachment (`SpaceTravelState`, unserialised — a UI-notification marker, not
   game state) to fire the arrival message once on entry rather than every tick. **Originally
   scoped to stop here** — landing site, actual descent, and return-location persistence were
   meant to stay M2.6's job. Revised after live-client testing: reaching an approach shell with
   only a chat message and no payoff read as broken twice in a row rather than as scoped, so
   `checkApproach` now also calls `descendTo`, a minimal, explicitly-labelled-placeholder
   dimension change to a fixed point near the surface's own origin. **Not the real system** — no
   authored landing site, nothing remembers where a player last departed from. Both are still
   M2.6's actual job; this only exists so the mechanism has something to show for itself while
   testing the rest of M2.5.
5. **The vertical bound: message only, no correction, and no vanilla void death either.**
   Beyond `y: ±512` `SpaceMechanics` repeats an action-bar notice every 20 ticks for as long as it
   stays true — deliberately *not* a wall or a redirect, matching Sanchit's call ("I want people
   free to do what they choose"). Separately, vanilla's own "fell out of the world" damage
   (`DamageTypes.FELL_OUT_OF_WORLD`, keyed off `min_y - 64`) is cancelled outright for any entity
   in `ascension_worlds:space`, in both directions — normal worlds kill you in the void below
   them, but space has no floor to fall out of, and this dimension's declared height was never
   meant to be a build limit players could die against. Air remains the only real limiter, same
   as the horizontal case worlds-api.md already argued for.
6. **Sky rendering: planet bodies at true apparent size, plus a full starfield.**
   `SpaceSkyRenderer` bills a quad at each planet's actual `(x, 0, z)` world position, sized to its
   real `body_radius` — ordinary perspective projection then produces the correct apparent size on
   its own, matching worlds-api.md's `2 * atan(radius / distance)` table without this class doing
   any angle math itself. `SpaceStarField` fills the rest of the void: the same random-quads-on-a-
   sphere technique vanilla's own star buffer uses, but drawn in every direction and every frame,
   unconditionally, since space has no day/night cycle and no "up" to restrict it to. This was the
   one genuinely new piece of engineering in this milestone, and it took several live-client rounds
   to actually get right — see below.

### Real bugs found live, all fixed

None of these were catchable headlessly — all needed a human looking at a running client, exactly
the case ADR-0008 exists for.

- **Invisible, then visible-but-spinning wildly as the camera turned.** Root cause was choosing
  `RenderLevelStageEvent.Stage.AFTER_SKY`: `LevelRenderer` dispatches that one specific stage with
  a `null` pose stack (the event replaces it with a blank identity `PoseStack`), while every other
  stage (`AFTER_ENTITIES`, `AFTER_PARTICLES`, `AFTER_WEATHER`, ...) gets a real one *and* fires
  after `LevelRenderer` has already pushed the camera's rotation onto `RenderSystem`'s own global
  modelview matrix stack (right before entity rendering, popped after clouds). Fixed by moving to
  `AFTER_PARTICLES`.
- **Backface culling.** Never explicitly disabled, so the billboard's visibility depended on
  whichever way its winding order happened to face the camera at a given angle — the "sometimes
  visible, wanky" symptom. Fixed with `RenderSystem.disableCull()`/`enableCull()` bracketing the
  draw calls.
- **Still visibly spinning after the above two fixes.** The quad's local frame was matched to
  `camera.rotation()` in full — vanilla's own technique for a particle that must always face the
  viewer edge-on, copied on the assumption it would carry over. It does not: a full-rotation
  billboard also inherits the camera's pitch and roll, which visibly rotates a quad's recognisable
  texture as the camera looks up or down, even though the quad is technically always "facing" the
  camera. Fixed by billboarding around world-Y only — right axis from world-up crossed with the
  object-to-camera direction, up axis fixed at world-up, no pitch or roll term anywhere in the
  math — which cannot show that spin regardless of where the camera looks.
- **Ascent landing exactly on the departure planet's own coordinate.** Put that planet's rendered
  body around/through the player instead of ahead of them, and — since `approach_radius` is always
  larger than `body_radius` by construction — trivially placed the player back inside their own
  departure shell, which `checkApproach` read as a fresh arrival on the very next tick and
  immediately descended them right back where they left. Fixed by placing the ascent destination
  `approach_radius + 100` blocks out along the player's heading, so they land genuinely outside
  their own shell with the body visibly at a real distance rather than wrapped around the camera.
- **Placeholder descent landing below bedrock (`y = -65`), on both the Moon and Earth.** Root
  cause was querying the target column's heightmap and landing exactly on it — which returns
  `minBuildHeight - 1` for an unloaded, ungenerated, or already-dug-out column, all of which are
  indistinguishable from "no ground here" to a heightmap query. Sanchit's call rather than a patch
  to the heightmap query: land near the surface dimension's own ceiling instead, so arriving reads
  as a fall/glide back down through atmosphere ("we should be in the air not under... feel we
  descended from space") rather than a teleport onto whatever the ground happens to be. Removes the
  whole bug class rather than papering over one column-query edge case.
- **No way to tell Earth and the Moon apart at a glance.** Added a `color` field to `Planet`
  (packed `0xRRGGBB`, tinted onto the placeholder quad via `RenderSystem.setShaderColor`) and swapped
  the placeholder texture from `sun.png`'s cross/star glyph to `moon_phases.png`'s full-moon frame,
  a rounder shape. Real per-planet art is still later polish; this is the cheap version that needed
  no new art to ship.

**Revert done:** the Moon's `space.position` in `planet/moon.json` was temporarily `[1500, 0]`
during the live-testing rounds above; reverted to the settled `[8000, 0]` on 2026-09-06 once
Sanchit called M2.5 done.

**Live-client checklist — all confirmed 2026-09-06:**
- [x] Fly up on Earth with elytra, confirm the transition to space fires at the right altitude
      with velocity/heading carried through. Confirmed both from Earth and the Moon.
- [x] Confirm the Moon and Earth render as discs that visibly grow while approaching and shrink
      while receding, staying visible regardless of viewing angle, without spinning as the camera
      turns.
- [x] Confirm the approach-shell message fires once on arrival, not repeatedly.
- [x] Confirm the placeholder descent actually lands you on the target surface, in the air rather
      than underground.
- [x] Fly past `y: ±512`, confirm the warning appears, repeats, and stops when back in range, with
      no damage taken at any point.
- [x] Revert the Moon's test position to `[8000, 0]`.

**Rock/debris field: root-caused 2026-09-06, not a bug.** Never encountered on test flights.
Verified headlessly with the same RCON+region-scan method that found the crater/mountain bugs:
`/place feature ascension_worlds:rock_debris` placed real blocks (stone, deepslate, basalt,
blackstone, cobbled_deepslate, all five, clustered correctly), confirmed by directly reading the
on-disk region files afterward — the Java is correct. The actual cause was `rarity_filter`'s
`chance: 1200`, tuned for a full 8000-block flight, being far too rare to ever show up on the
short `[1500, 0]` test route. Lowered to `chance: 200`. **Caveat that matters for testing:**
already-generated chunks never regenerate, so a corridor already flown (and Distant Horizons has
pre-generated a wide swath around both Earth and the Moon in the background over this session's
testing) keeps whatever rarity was in effect when it first generated — seeing the new rate
requires either fresh, never-before-loaded space chunks or a deleted world.

**Verify:** fly from Earth to the Moon's approach shell entirely under manual control, on a
dedicated server, surviving on tank the whole way — the M1.7/M1.8 expedition-loop mechanics
already apply unchanged, space is just another place they get tested against. Then check
`ascension_worlds:space` against Distant Horizons and Chunky (M2.1's deferred DH question; it
does not get a vote, but it does get tested now that there is a real dimension to test against).

### M2.6 — Descent, landing, and the seam  *(done, confirmed in a live client 2026-09-06)*

Manual descent. `dimensions.md` asks how failed landings are handled; this is where it is
answered in code rather than prose.

**And where the transition stops reading as a loading screen.** The dimension change cannot be
removed — the client discards its level and rebuilds it, and no flag turns that off — so the
work is making it fast and putting the fiction in front of it. Four levers, in
[`worlds-api.md`](../docs/technical/worlds-api.md):

1. **`approach_radius` doubles as a pre-load radius.** *(Done 2026-09-06.)* `SpaceMechanics` now
   splits arrival into two radii instead of one: crossing `approach_radius` starts a self-expiring
   chunk-load ticket (`TicketType`, modelled on vanilla's own `TicketType.PORTAL`) at the eventual
   landing spot, refreshed once a second while still approaching; the actual dimension change only
   fires on crossing `body_radius` — the boundary `Planet.SpacePosition` already documents as "the
   volume you cannot fly into". The distance between the two radii is what turns the last stretch
   of a flight into loading time instead of a stutter at the moment of arrival.
2. **Chunky pre-generation** keeps worldgen out of the transition entirely. *(Done 2026-09-06,
   redesigned same day — see below.)* Built as a real Tier 2 module, `ascension-compat-chunky` —
   the module `docs/technical/architecture.md` had already named and scheduled here, just not
   built yet. Compiles against Chunky's real API (`org.popcraft.chunky.api.ChunkyAPI`, resolved
   via Modrinth's maven — `maven.modrinth:chunky:1.4.23` — since Chunky isn't on a conventional
   repository), and this jar's own `neoforge.mods.toml` declares Chunky as a required dependency,
   so NeoForge simply never loads it at all without the real thing present (same posture ADR-0006
   sets for Sable).

   First version reacted to arrival: `ascension-worlds` fired a `PlanetArrivalEvent`
   (surface + landing position) on every `descendTo`, and the compat jar pre-generated a
   512-block radius around whoever had just landed. Sanchit's redesign the same day it shipped:
   react at the moment someone needs it, and it only ever covers the one planet they happened to
   land on. `ChunkySequentialPregen` instead starts the moment the server finishes booting,
   walks every registered planet in `Planet.order()` (Earth before the Moon, automatically, with
   no separate sequencing concept to define), and pre-generates a 100-chunk-radius circle around
   each planet's own `(0, 0)` — big, because this now runs in idle server time before anyone is
   waiting on it, not reactively. One planet at a time, chained through
   `ChunkyAPI.onGenerationComplete`, so DH's background LOD pass is never fighting more than one
   Chunky task at once. `PlanetArrivalEvent` was removed along with the design it existed for —
   nothing else had ever picked it up, and keeping a public event whose one stated reason to
   exist was gone would have been exactly the "designed for a hypothetical future" surface
   `CLAUDE.md`'s style section warns against. If a real second consumer shows up later, adding an
   event back for it is cheap; keeping an unused, mis-justified one wasn't worth it in the
   meantime.

   Which planets have already had a task *started* persists the same way it always did — the
   serialised `PregenMarker` attachment, unchanged, so a restart resumes the sequence at the
   first not-yet-started planet. Which chunks within an *interrupted* task are already done is
   Chunky's own concern (it persists live task state to `config/chunky/tasks/` and resumes it on
   its own, independent of anything this module does).

   **Confirmed live 2026-09-06** — the Spark reading below shows a real Chunky task running for
   `minecraft:overworld` during the Moon → Earth → Moon test, and it registered essentially zero
   cost of its own (see the performance section).
3. **Space is void**, so leaving a surface is nearly free by construction — an accidental
   dividend of ADR-0010.
4. **Atmospheric entry covers the rest.** *(Done 2026-09-06.)* Plasma and shaking is what the
   player expects to see anyway; a vanilla loading screen is not. Replaced vanilla's
   `ReceivingLevelScreen` (the blurred-menu "Downloading Terrain" screen every dimension change
   shows) with `AtmosphericEntryScreen`, a flickering warm-glow band, for both directions of a
   space transition. No mixin, despite touching vanilla's own screen &mdash; NeoForge's
   `RegisterDimensionTransitionScreenEvent`/`DimensionTransitionScreenManager` is a first-class,
   already-official extension point for exactly this (it exists so mods can theme portal-style
   transitions), keyed on `SpaceDimension`'s id alone via `registerIncomingEffect`/
   `registerOutgoingEffect` so it covers ascent and descent for every planet, present or future,
   without knowing any of their ids. Still a *real* loading screen underneath — subclassing
   `ReceivingLevelScreen` rather than replacing it outright keeps the actual
   "close once chunks are really in" gate intact; only what renders during the wait changed.
   **Not yet confirmed in a live client** &mdash; entirely client-rendering code, so a headless
   server boot (which doesn't even load client-only classes) can't touch it at all this time.

**Return-location memory.** *(Done 2026-09-06, revised same day — see below.)* `PlanetArrivalMemory`
(a serialised, per-player attachment) records where a player last **ascended into space from**,
not wherever they last happened to be standing. First version recorded ground position, polled
once a second while on solid ground; Sanchit's correction: that breaks in co-op the moment a ship
is involved. Several players riding the same ship up together each stand somewhere slightly
different on it, a block or two apart, and none of those ground positions relate to each other
once the ship's gone — but the moment they all crossed into space together *is* a shared, load-
bearing anchor, so `checkAscent` records each player's own position at exactly that moment
instead. `resolveLandingSpot` reads it back at both pre-load and descent time, and resolves to the
**nearest actually-standable spot** (`findSafeLandingSpot`, a small bounded ring-search, not the
literal remembered coordinate) rather than teleporting straight onto it — which is what keeps
several players who ascended a block or two apart from being placed a block or two apart on solid
ground on the way back, on top of covering ordinary terrain drift since they left. A planet nobody
has ever ascended from yet still falls back to the near-ceiling placeholder from the M2.5 round.

**First live crash, found and fixed 2026-09-06.** `recordAscent` threw
`UnsupportedOperationException` out of `Map.put` on the very first ascent after a save/reload
cycle, not the first ascent ever -- a brand-new player's attachment is built through the no-arg
constructor's own fresh `HashMap` and worked fine, but `Codec.unboundedMap`'s decode side hands
back Guava's immutable `ImmutableMap`, and reconstructing this class straight from that decoded
map (the private constructor just assigned it, no copy) meant the very next write crashed the
player's connection. Fixed by copying into a fresh `HashMap` in the private constructor
regardless of which caller built it, so no downstream code needs to know or care what the
codec's own map implementation happens to be. Confirmed via the actual crash log (ascending from
the Moon after a save cycle), not caught headlessly -- this is exactly the class of bug ADR-0008
exists to catch with a live client rather than a compile check.

**Explicitly cut from this module's version of M2.6 — Sanchit's call 2026-09-06:**
- **An authored landing site.** Create Aeronautics is expected to own physical landing once it's
  integrated; `worlds` has no reason to build or place a landing structure of its own. (The
  broken-teleporter framing floated earlier is shelved with it — if it comes back, it's as content
  for whichever module ends up owning arrival, not this one. `todo.md`'s "where do ancient
  gateways physically exist?" stays open.)
- **Failed-landing damage/consequences** (`dimensions.md`'s question). Explicitly not a `worlds`
  concern — the same boundary that keeps oxygen mechanics in `atmosphere` rather than here — and
  explicitly not v1 scope regardless of which module eventually owns it. `worlds` v1 stays "the
  descent is whatever vanilla physics already does if you fly it badly," full stop.

Both bullets are considered closed for this module's M2.6, not merely deferred — nothing here is
waiting on either before M2.6 can be called done.

**Verify: done 2026-09-06.** `/spark profiler` across a real Moon → Earth → Moon round trip —
[`docs/technical/performance-log.md`](../docs/technical/performance-log.md)'s "M2.6 — Dimension
transition reading". 20 TPS held throughout both transitions; the entire Ascension module set
(worlds, atmosphere, core, compat-chunky combined) never clears a tenth of a percent of server
thread time. The 547 ms spike and 24.3 ms 95th-percentile tick that do show up trace entirely to
vanilla chunk I/O (`ChunkMap.processUnloads`, `ChunkSerializer`, chunk NBT codec decode) — not a
single `com.ascension` frame appears anywhere in the self-time breakdown. Answers the question
that actually mattered ("is any of this cost ours") with real evidence rather than a claim; does
**not** yet isolate how much the pre-load ticket specifically helps (that needs an A/B with it
disabled, not done here) — see the reading's own caveat.

**M2.6 called done 2026-09-06** — Sanchit: "we can mark the testing done." All four transition
levers implemented, return-location memory implemented and its co-op bug fixed, the one live
crash found and fixed, and the verify step closed out with a real measurement. The billboard
circle fix and the Chunky sequencing redesign (both requested during this same testing round)
also landed and were confirmed live.

### M2.7 — Refactor + measurement

Against the M1.8 full-stack reading, plus the A/B. Dimensions are where ADR-0007 gets tested
properly: two more dimensions means two more of everything the performance contract worries
about.

**Refactor half done 2026-09-06** (the performance half is the M2.6 Spark reading above — real,
but a spot check rather than the M1.8-style A/B this section still calls for). No new features,
per ADR-0008; scope was everything built since M2.4 (the crater/mountain terrain fixes, and all
of M2.5/M2.6).

`ascension-worlds` had **zero tests of its own** going into this pass — every one of the
project's 64 unit tests as of M2.4 lived in `core` or `atmosphere`. Pulled the same way M2.4
pulled arithmetic out of `atmosphere`:

- **`AscentDestination`** — the trig placing an ascending player past their departure planet's
  approach shell, out of `SpaceMechanics.checkAscent`. Four numbers in, a point out, no
  `ServerPlayer` involved.
- **`RingSearch`** — the widening-square-ring search order behind
  `SpaceMechanics.findSafeLandingSpot`, out on its own. What actually makes that search "nearest
  safe spot" rather than "some safe spot" is the enumeration order, and that's pure integer math
  independent of whether any given block happens to be standable.

13 new tests across both, plus a third file that is pure regression coverage:
**`PlanetArrivalMemoryTest`**, which reproduces the live `ImmutableMap` crash from this same
testing round without needing a server, a save file, or a relog to trigger it — encode, decode,
write. `worlds` module test count: 0 → 13.

**One more thing found and removed, not just fixed:** `PlanetArrivalEvent`, `ascension-worlds`'
one public event, existed for exactly one reason stated in its own javadoc — letting
`ascension-compat-chunky` react to an arrival without either module knowing about the other. That
reason stopped being true the moment Chunky's pre-generation was redesigned to run proactively at
server start instead of reactively on arrival (see M2.6 lever #2 above) — nothing else had ever
subscribed to it. Kept, its javadoc would have gone from accurate to false; genericised, it would
have been exactly the "designed for a hypothetical future" surface this project's own style notes
warn against keeping. Removed instead, along with the stale "PlanetArrivalEvent" references it
left behind in `compat:chunky`'s `neoforge.mods.toml` comment and `docs/technical/
architecture.md`'s module table — both were still describing the retired per-arrival design.
Re-adding an event later, if a real second consumer ever needs one, costs nothing; carrying an
unused, mis-justified one in the meantime was not worth it.

**Not done in this pass:** the M1.8-style A/B (mods on vs off, same session, same stack) that
would isolate exactly how much M2.5/M2.6 cost rather than confirming it costs "basically
nothing" — the Spark mods-view breakdown answered the question that mattered most (is any of
this cost ours) without one. Worth doing properly before the next milestone that touches
performance claims more centrally than this one did.

---

## Definition of done

- [ ] `docs/technical/worlds-api.md` written and **reviewed** (drafted 2026-09-05)
- [x] Orbit representation decided, with an ADR superseding ADR-0004's deferral — ADR-0010
- [x] Tier 1 → Tier 1 dependency policy decided, with an ADR — ADR-0011
- [x] The Moon authored entirely as data — no per-planet Java
- [x] Full loop playable: leave Earth, orbit, descend, survive, return — Moon → Earth → Moon
      confirmed live 2026-09-06 (Earth is authored as the "return to" side of that loop; Planet 3
      would be the next new destination, not yet built)
- [~] Dimension transitions measured, and masked rather than shown as a loading screen — measured
      half done (see performance-log.md's M2.6 reading); the masking screen
      (`AtmosphericEntryScreen`) shipped and renders during the same test session but was not
      itself specifically confirmed by Sanchit, so not checking this off outright
- [x] Verified on a dedicated server — every M2.5/M2.6 test this whole increment ran on one
- [ ] Survives world unload/reload **and** dimension change
- [x] Atmosphere works on the Moon with `DebugAtmosphere` off
- [ ] No regression against the M1.8 full-stack reading
- [ ] Loads and works with **no other Ascension module present** (ADR-0003 rule 6)
- [~] Two refactor passes done (M2.4, M2.7) — M2.7's code-review half is done (0 → 13 tests in
      `worlds`, one mis-justified public event removed); the M1.8-style performance A/B it also
      calls for is still outstanding

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
