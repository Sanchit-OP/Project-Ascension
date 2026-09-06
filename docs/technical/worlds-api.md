# `ascension-worlds` API design

> **Status: reviewed 2026-09-06**, against the implementation that shipped in M2.5/M2.6. Two
> sections below were rewritten during that review because the built system diverged from what
> this document originally proposed — see "Where you land" (§3) and "An answer to dying in
> space" (§3) for what actually shipped and why. Everything else in this document matches the
> code as built. Written before code, for the reason M1.1 established: a module's API is the most
> expensive thing in it to change once other modules couple to it.
>
> Settled going in: [ADR-0004](../decisions/0004-fully-custom-dimensions.md) (custom dimensions,
> data-driven planets), [ADR-0010](../decisions/0010-orbit-is-one-shared-space-dimension.md)
> (one shared space dimension), [ADR-0011](../decisions/0011-tier-1-modules-share-contracts-through-core.md)
> (Tier 1 modules share contracts through `core`).

## 1. What this module owns — and what it deliberately does not

**Minecraft already has a data-driven dimension system.** `data/<ns>/dimension/<id>.json` and
`dimension_type` define the dimension, its height, its light, its worldgen. Datapacks do that
well, third-party tooling understands it, and every worldgen mod already integrates with it.

So a **planet does not define a dimension. It references one.**

| Vanilla datapacks own | `ascension-worlds` owns |
|---|---|
| The dimension, its type, its height and light | Which dimension is a planet's *surface* |
| Biomes, noise settings, surface rules, ores | Where that planet sits in interplanetary space |
| Structures and their placement | What the environment there is like |
| | Travel: approach, descent, arrival |

This boundary is the single most important decision in this document. Reinventing dimension
JSON would put us in the worldgen business, competing badly with a system that already works,
and would break every existing tool that reads it.

### The registry describes where a world is and what it is like — not what happens on it

`docs/progression/planets.md` lists *required fields per planet*: orbit purpose, surface
survival mechanic, movement mechanic, resource gate, core structure, boss condition, post-clear
travel improvement.

**That is an authoring checklist, not a JSON schema.** A boss is an entity in a structure. A
resource gate is an ore in a biome and a recipe. Those are content authored *in* the world, and
putting them in the planet registry would turn it into an object that everything depends on and
nothing can change. The checklist stays in `planets.md`, where it belongs, as the thing a world
designer must answer before a world is finished.

## 2. Environment flows through `core`

Per ADR-0011, `worlds` and `atmosphere` do not know about each other. `core` holds the
vocabulary.

```java
// com.ascension.core.api
public record WorldEnvironment(boolean breathable, float drainMultiplier) {
    public static final WorldEnvironment EARTHLIKE = new WorldEnvironment(true, 0.0f);
}

@FunctionalInterface
public interface WorldEnvironmentSource {
    Optional<WorldEnvironment> environmentOf(ResourceKey<Level> dimension);
}

public final class WorldEnvironmentRegistry {
    public static void register(ResourceLocation id, WorldEnvironmentSource source);
    public static Optional<WorldEnvironment> query(ResourceKey<Level> dimension);
}
```

`worlds` registers a source backed by its planet registry. `atmosphere` registers an
`AtmosphereProvider` at the `DIMENSION` band that consults `WorldEnvironmentRegistry` and
translates. Each module works alone:

| Loaded | Result |
|---|---|
| `atmosphere` only | Registry empty, nothing claims, everything breathable. Unchanged from today. |
| `worlds` only | Environments declared, nothing reads them, nobody suffocates. |
| both | The Moon suffocates you. |

**`WorldEnvironment` is not `Atmosphere`**, and the duplication is deliberate. Atmosphere's
`Atmosphere` describes *a position* — it is what sealed volumes and water and conduit power all
answer with. `WorldEnvironment` describes *a whole dimension*. They happen to share two fields
today; conflating them would mean a sealed room and a planet were the same kind of thing, which
is exactly the confusion the priority bands exist to prevent.

### The consequence worth stating plainly

**`ascension-atmosphere` currently depends on nothing but NeoForge.** Under ADR-0011 it gains a
required dependency on `ascension_core`, so an adopter needs two jars instead of one.

ADR-0003 rule 6 always presumed this — *"testable with only `core` present"* — so it is the
intended shape rather than a surprise. But it does slightly raise the cost of adopting the
module, which is the project's stated first priority, and it is worth being honest that the
compatibility argument in ADR-0011 buys third-party *interoperability* at the price of a second
jar.

## 3. What a position actually means

Three places exist. Only one of them is shared.

```
  minecraft:overworld           ascension_worlds:space            ascension_worlds:moon
  (Earth's surface)             ONE shared dimension              (Moon's surface)

    ┌──────────┐              [0,0]             [8000,0]            ┌──────────┐
    │ fly up   │ ─ ascend ─▶   ⬤ Earth ···· 8000 blocks ····▶ ⬤ Moon ─ descend ─▶ │ you land │
    └──────────┘              body_r 384        body_r 192          └──────────┘
                                                approach_r 320
```

**There is no "Earth orbit" and no "Moon orbit".** That is precisely what
[ADR-0010](../decisions/0010-orbit-is-one-shared-space-dimension.md) changed. There is one
space; Earth is at `[0, 0]` in it and the Moon is at `[8000, 0]`, and **8000 is the distance you
fly.**

Ascending from Earth's surface puts you in space just outside Earth's body. From there the Moon
is 8000 blocks away. Reach its `approach_radius` and descent to `ascension_worlds:moon` becomes
possible.

### Why 8000

| Travel method | One way |
|---|---|
| Elytra and rockets, ~35 b/s | ~3.8 min |
| A ship at 60 b/s | ~2.2 min |
| Creative fast flight, ~22 b/s | ~6 min |

A trip you feel, without being a chore. **Settled 2026-09-05.** Distances for planets 4–7 should
be laid out before any of them is built, because travel time is pacing and a distance a player
has already learned cannot be changed quietly.

### Two consequences worth stating before M2.5

**A planet body in space is rendered, not built.** At 8000 blocks — roughly 500 chunks —
Minecraft will never draw a sphere made of blocks. So a planet in space is a rendered body like
the sun or the moon, and `body_radius` is *its apparent size plus a volume you cannot fly
into*. You never land on it in space; approaching triggers descent to its surface dimension.

This is also what keeps the shared space affordable: there is nothing there to load, which is
what makes seeing across it cheap (ADR-0010).

For scale: a 192-block radius body seen from 8000 blocks subtends about 2.7°, roughly five times
the apparent size of the real moon from Earth. Clearly a place, clearly far away.

**Earth needs a `body_radius` too**, now that it is a body in space you can look at and cannot
fly through.

### Space is 3D. Only the *layout* is flat.

Worth correcting a sloppy phrase: "space is a plane" describes **where the planets sit**, not how
you move. `ascension_worlds:space` is an ordinary Minecraft dimension. You can pitch, climb,
dive, and fly perpendicular to the planetary plane — a rocket with angled thrust is meaningless
otherwise.

Y in space is **manoeuvring room, not a navigation axis.** Planets share one altitude, so
climbing away from it takes you somewhere with nothing in it. The dimension should be tall enough
that its ceiling is never the thing that stops you: what limits how far you stray should be air,
not a build limit.

#### Apparent size is the navigation

A planet is drawn at a size derived from its distance, `2 * atan(radius / distance)`.

| Distance | Moon, `body_radius` 192 | Earth, `body_radius` 384 |
|---|---|---|
| 320 — the Moon's approach shell | **62 deg**, fills the view | |
| 512 — just launched from Earth | | **74 deg**, Earth behind you |
| 8,000 — the Moon seen from Earth | **2.75 deg**, a clear disc | 5.50 deg |
| 18,000 — Planet 3 | 1.22 deg | |
| 100,000 — Planet 7 | **0.22 deg**, a bright dot | |

For scale: a full moon from Earth is about 0.5 deg, and a hand at arm's length about 10 deg.

This is not decoration. **It is the primary navigation instrument**, and the most legible one
available: distant worlds are points of light, they grow as you close, and they shrink when you
are going the wrong way. A player reads their entire navigational situation out of the window
without a single number.

#### Ascent triggers on altitude, and carries your heading

Crossing the threshold altitude on a surface transitions you to space **whatever your pitch** —
angle does not gate the transition. But **velocity is preserved through it**, so an angled launch
emerges already moving in that direction.

That makes the launch angle the first navigation act of a journey, and it is what connects a
surface to interplanetary space: where on Earth you launched from is irrelevant, but which way
you were pointing is not.

#### Aiming: the disc you can see is the reticle

A heading error does **not** slow your approach in proportion. It converts almost entirely into
*miss distance*, and it does so immediately. For a straight shot at the Moon with no steering,
closest approach is `8000 * sin(error)`:

| Heading error | Closest approach | Arrive? |
|---|---|---|
| 0.5 deg | 70 | yes |
| 1 deg | 140 | yes |
| 2 deg | 279 | yes |
| **2.3 deg** | **321** | **miss** |
| 5 deg | 697 | miss |
| 45 deg | 5,657 | miss |
| 89 deg | 7,999 | miss |

At 89 degrees you come **1.2 blocks closer**, after 140 blocks of travel, and then leave forever.
"Almost perpendicular but still slowly approaching" is not what happens: the lateral motion
dominates within a few hundred blocks. Ballistic tolerance to the Moon is about **±2.3 degrees**.

**And that is exactly as wide as it should be**, because of a coincidence in the numbers worth
keeping:

| | Angular radius, seen from Earth |
|---|---|
| The Moon's visible disc | **1.37 deg** |
| The Moon's approach shell | **2.29 deg** |

**Aim anywhere inside the disc you can see and you land inside the shell.** The visible planet is
not merely feedback, it is the aiming reticle — and a conservative one, since the shell is
two-thirds wider than the disc. It also gets easier as you close: the disc grows while the shell
stays 320 blocks, so a course that was marginal at launch becomes comfortable on approach.

If `body_radius` or `approach_radius` are ever retuned, this relationship is the thing to
preserve. A shell narrower than the disc would mean aiming at a planet and missing it, which
would read as broken.

#### What you pass on the way

The spiral (below) means a direct flight is rarely empty. Perpendicular miss distances along
each route, with apparent size at closest approach:

| Flying Earth to | What passes | Distance | When | Apparent size |
|---|---|---|---|---|
| Planet 3 (18,000) | **the Moon** | 6,553 | 25% in | **3.36 deg** |
| Planet 4 (32,000) | Planet 3 | 14,745 | 32% in | 1.49 deg |
| Planet 5 (50,000) | Planet 4 | 26,213 | 37% in | 0.84 deg |
| Planet 6 (72,000) | Planet 5 | 40,958 | 40% in | 0.54 deg |
| Planet 7 (100,000) | **the Moon** | 7,970 | 1% in | 2.76 deg |

Flying to Planet 3, the Moon swells to **larger than it ever appears from Earth**, passes to one
side, and shrinks away behind. None of that was arranged; it falls out of the spiral, and it is a
good reason not to flatten the layout onto one axis.

#### What if a player flies perpendicular, or simply the wrong way?

**Nothing special happens, and that is the correct answer.**

Everything shrinks, which is the feedback. And then they run out of air — the same failure as
overreaching in any direction, and the one the entire game is already built around. Bad
navigation and overambition are punished identically, by asphyxiation, and the lesson is the
same: check your air against your distance before committing.

**With steering, though, almost any heading works.** The table above is a *ballistic* shot, held
straight. A player who keeps turning toward the growing disc converges from any initial heading
under 90 degrees — they simply fly further to get there, and further is air. So the punishment
for a bad launch is not failure, it is fuel: an expensive arrival, or an arrival you cannot
return from.

That is the right shape. Precision is rewarded, imprecision is survivable, and carelessness is
fatal — all through one resource that is already on the HUD.

No invisible walls, no corrective nudge, no "you cannot go that way". The plane is a fact about
where things are, not a fence.

Two things this makes non-optional rather than nice to have:

- **A distance readout to the nearest body**, so a player can judge whether they can still get
  home. Air remaining and distance remaining are the two numbers this game is actually about, and
  one of them is already on the HUD.
- **An answer to dying in space.** **Not decided — left to vanilla, deliberately, not by
  oversight.** `SpaceMechanics` cancels only the "fell out of the world" damage type; an ordinary
  death in space (asphyxiation, a rocket accident) still runs vanilla's own respawn, which sends
  the player to their bed or world spawn — on Earth, for anyone who hasn't set one elsewhere. No
  code decides this; it falls out of not writing a special case. Revisit once dying in space with
  cargo or a ship at stake is actually possible (Sable) and "you just respawn, nothing is lost"
  stops being an accurate description of the stakes.

### How space coordinates relate to world coordinates

**They are the same thing.** `position` is not a separate coordinate system — it is literally
where in `ascension_worlds:space` that planet's body sits. Fly to the Moon and F3 reads
`X: 8000, Z: 0`.

What needs care is that **each dimension has its own independent coordinate space**, and a
transition discards the previous one. Walked through, as F3 would show it:

| Where you are | Dimension | F3 |
|---|---|---|
| On Earth, wherever you happen to be | `minecraft:overworld` | `420 / 71 / -1130` |
| Just ascended | `ascension_worlds:space` | `0 / 64 / 512` |
| Arrived at the Moon | `ascension_worlds:space` | `8000 / 64 / 0` |
| Landed | `ascension_worlds:moon` | `0 / 78 / 0` |

Note rows one and two: the Earth coordinates `420 / -1130` have **nothing** to do with the space
position `0 / 512`. Leaving Earth's surface puts you at *Earth's position in space*, which is
the origin, regardless of where on Earth you took off from. Two players launching from opposite
sides of the world arrive in the same place.

`512` is Earth's `approach_radius` — you emerge on the shell where descent is possible, not
inside the body.

**Y is nearly meaningless in space.** Planets sit at one altitude and flying is horizontal, so Y
is a thin slab you move within rather than a dimension of navigation. That is what makes the
layout a plane (§3).

#### The body is a symbol, not a scale model

A planet is 192 blocks across in space. Its surface is an effectively unbounded Minecraft world.
**Those cannot be reconciled and should not be.**

There is no spatial mapping from "which part of the body I touched" to "where on the surface I
land", because the surface is millions of times larger than the thing representing it. Any
scale factor would be a fiction pretending to be arithmetic. The body in space *stands for* the
planet; descent is a transition between two coordinate spaces, and the surface coordinate you
arrive at is **authored, not computed**.

Worth writing down so nobody later tries to "fix" the mismatch. It is not a bug.

#### Where you land — settled, and revised once against this document's own original proposal

**Every arrival returns you near where you last ascended into space from — not where you were
last standing, and not an authored landing site.**

This document originally proposed an authored landing structure for first arrival and "return to
where you departed" after that. Both halves changed once the mechanism was actually built and
tested live:

- **No authored landing site.** Sanchit's call, M2.6: Create Aeronautics is expected to own
  physical landing once it integrates, and `worlds` has no reason to build or place a landing
  structure of its own ahead of that. A planet nobody has ever ascended from yet lands near the
  surface dimension's own ceiling instead — a deliberate placeholder (`SpaceMechanics
  .resolveLandingSpot`), not a structure, so descent reads as falling back through atmosphere
  rather than teleporting onto whatever the ground happens to be.
- **"Where you departed" turned out to be the wrong anchor.** The natural reading — wherever a
  player was last standing — breaks the moment a ship is involved: several players riding the
  same ship into space each stand a block or two apart, and none of those ground positions relate
  to each other once the ship is gone. The moment they all crossed into space together is the
  shared anchor instead, so `PlanetArrivalMemory` records each player's own position at the
  instant they cross into space (`SpaceMechanics.checkAscent`), not while standing around
  beforehand or afterward.
- **Landing is "near", not "on".** `resolveLandingSpot` resolves the remembered point to the
  *nearest actually-standable spot* (`findSafeLandingSpot`, a small bounded ring search), never
  the literal coordinate — which is what keeps several players who ascended a block or two apart
  from being placed a block or two apart on solid ground on the way back, and also covers
  ordinary terrain drift (built over, dug out) since they left.

Persisted state: `PlanetArrivalMemory`, a serialised per-player attachment holding a `BlockPos`
and yaw per surface dimension a player has ascended from — a `ResourceLocation` key, never a
`Level` (ADR-0007 rule 1). Full design and the live crash found in it: `plans/m2-worlds.md`'s
M2.6 section.

### Seamless travel: what is actually possible

**The dimension change cannot be removed.** `ServerPlayer.changeDimension` sends a respawn
packet, the client discards its `ClientLevel` entirely and builds a new one, and
`ReceivingLevelScreen` covers the gap until chunks arrive. There is no flag that turns that off,
and a mod that faked it would be fighting the client's own lifecycle.

So the goal is not "no transition". It is **a transition nobody reads as a loading screen** —
which is a different problem, and a solvable one.

#### What actually costs the time

Not worldgen, if we have pre-generated. Not the server-side move, which is microseconds. It is
**getting chunks to the client**: the screen stays up until the chunk the player is standing in,
plus a small ring, has arrived. Everything else streams in behind.

Which means the levers are: have the chunks ready, make there be fewer of them, and put
something in front of the gap.

#### 1. `approach_radius` earns a second job

The approach shell already exists as the radius where descent becomes possible. It should
**also be the pre-load trigger**: crossing into it force-loads the destination's arrival chunks
server-side, using a chunk ticket around the landing position.

A player then spends the last 320 blocks of the journey closing on the planet — and that flight
time *is* the loading time. By the time they commit to descent, the server is holding hot chunks
and can push them immediately.

This is the single biggest win available, and it costs one radius we already had.

#### 2. Chunky pre-generation removes worldgen from the transition

A landing on ungenerated terrain pays for generation inside the transition, which is by far the
most expensive thing that can happen there. Pre-generating the landing area means arrival is
disk reads, not worldgen. Recorded in
[`optimisation-stack.md`](optimisation-stack.md); this is the other reason it matters.

#### 3. Leaving a surface is nearly free by construction

Space is a void dimension. There is almost nothing to send, so surface-to-space should be fast
without any special handling. The expensive direction is space-to-surface, and that is the one
the two levers above target.

An accidental benefit of [ADR-0010](../decisions/0010-orbit-is-one-shared-space-dimension.md):
choosing a near-empty shared space to make distance cheap also made half of every journey's
transition cheap.

#### 4. Put the fiction in front of the gap

Whatever remains gets covered by something the player expects to see anyway.

Descent through an atmosphere is plasma glow, heat shimmer and shaking. A launch is a burn and a
receding surface. Neither is a loading screen; both are the moment the game is about, and both
are perfectly good masks for a few hundred milliseconds. The trick commercial games use is the
elevator ride and the airlock cycling — the load is real, and nobody minds because they are
watching the thing they came for.

Mechanically: `ScreenEvent.Opening` on the client to substitute our own render for
`ReceivingLevelScreen`, **guarded to transitions between our own dimensions** so that vanilla
nether and end travel is untouched. Client-only code, dist-separated like the HUD.

And **velocity and orientation carry through** (see above), so motion is continuous across the
seam rather than restarting.

#### The genuinely seamless option, and why not

One dimension containing both space and every planet's surface, with a custom chunk generator
producing void almost everywhere and terrain near planet positions. Descent would then be
literally flying downward, with no transition at all.

Rejected. A dimension has **one** `dimension_type` — one height, one light, one sky — so every
planet would share Earth's, which deletes per-planet identity, the thing planets are for. The
chunk generator becomes a single monster that has to know about every world, which is the
opposite of ADR-0004's data-driven promise. And Chunky and Distant Horizons would see one
enormous world rather than several, making pre-generation and LODs harder for the two mods
doing most to help us.

It trades a few hundred milliseconds for the identity of every planet. Not worth it.

#### Measure it, do not assume it

All of the above is a plan, not a result. The transition cost is measurable —
`/spark profiler` across a dimension change, or JFR — and it should be measured at M2.6 before
anyone claims it feels seamless. Compiling is not evidence, and neither is reasoning about
packet sizes.

### Seven planets in one space: range is the gate

Every planet is a coordinate on the same plane. Seven planets is seven `planet.json` files and
no new machinery — the interesting question is not *how* but *where*, because in a shared space
**distance is a gate that needs no code.**

A player can point their ship at Planet 7 on day one. They will die on the way. That is not a
hole in ADR-0010, it is the mechanism: you can go anywhere you can survive the trip to, and what
you can survive is a function of life support, which is progression.

#### The numbers are already coupled

Space is vacuum, so a journey is spent entirely on tank.

| | |
|---|---|
| Tank + lungs at current tuning | `1200 + 80` units ÷ `4`/s = **320 s** |
| Moon at 8000 blocks, ship at 60 b/s | 133 s each way, **267 s** round trip |
| Margin | **53 s** |
| The same trip on an elytra, ~35 b/s | 229 s each way, **457 s** — you do not get back |

So the Moon sits exactly at the edge of a single tank with a fast ship, and **cannot be done
round-trip on anything slower without a refill station on arrival**. The M1.7 expedition loop
lands on the campaign's very first journey, without that having been arranged.

This is worth knowing before the ladder is set: **the distance to a planet and the air a player
can carry are the same balancing dial seen from two ends.** Changing `TANK_CAPACITY` changes how
far away a planet effectively is.

#### The ladder, and the shape it makes

This is a **plane, not a line.** A planet is any `[x, z]`, negatives included, exactly as if the
system were sketched on paper with Earth at the origin. Distance from home is
`sqrt(x² + z²)`, so a planet at `[-12000, 5300]` sits about 13,100 blocks out.

Radii are chosen so each world demands a real improvement in carried air, ship speed or onboard
life support. **Bearings are chosen so the route graph works**, which turns out to matter as much
as the distances: laid out as an outward spiral, roughly 55° per step.

| Planet | Radius | Bearing | `[x, z]` | Hop from previous | Direct from Earth |
|---|---|---|---|---|---|
| Earth | 0 | — | `[0, 0]` | — | — |
| Moon | 8,000 | 0° | `[8000, 0]` | 8,000 | 8,000 |
| Planet 3 | 18,000 | 55° | `[10324, 14745]` | **14,927** | 18,000 |
| Planet 4 | 32,000 | 110° | `[-10945, 30070]` | **26,215** | 32,000 |
| Planet 5 | 50,000 | 165° | `[-48296, 12941]` | **41,091** | 50,000 |
| Planet 6 | 72,000 | 220° | `[-55155, -46281]` | **59,618** | 72,000 |
| Planet 7 | 100,000 | 275° | `[8716, -99619]` | **83,213** | 100,000 |

**Every outward hop is shorter than flying to that planet from Earth.** That is the property the
spiral buys, and it is worth having on purpose: pushing on from your furthest foothold is always
cheaper than going home and setting out again, so a forward base is rewarded rather than merely
allowed. Home, meanwhile, is always a straight line inward from anywhere.

The failure this avoids: pick bearings carelessly and two consecutive planets end up on opposite
sides of the origin, so the critical path makes a player cross the entire system to advance one
step — while flying home first would have been shorter. That reads as a bug even when it is
geometry.

**Provisional beyond the Moon.** Only the Moon's 8,000 is settled; 4–7 are unbuilt per ADR-0005
and their radii are a shape rather than values. They are written down now because travel time is
pacing, and a distance a player has already learned cannot be changed quietly.

Gateways then have real work to do. A restored gateway to Planet 5 saves a 50,000-block flight,
which is the *"permanent post-clear travel improvement"* from `planets.md` being a genuine reward
rather than a second menu entry.

#### Set the space dimension's world border deliberately

Vanilla's default border is ±29,999,984, and there is no reason for interplanetary space to be
that large. Nothing is out there, and a player who flies to 20 million is well into the range
where floating-point precision and chunk maths misbehave.

Set it to something that comfortably contains all seven — ±150,000 leaves room for later
additions without inviting a trip nothing survives.

#### Space is Earth-centric, on purpose

Earth is at `[0, 0]` and there is no star as a place. That is not astronomy — a real system would
put the sun at the origin and Earth in orbit around it.

It is a choice about whose map this is. The player starts on Earth, so Earth is the origin, and
every coordinate is a distance from home. That is what an early spacefaring civilisation's chart
would actually look like, and it means a position reads as *how far out you are*. The sun stays a
skybox feature.

#### Headings, not just distance

Planets should sit at **different bearings**, not all along +X. All-on-one-axis makes travel
one-dimensional — "keep flying east" — and wastes the navigation problem ADR-0010 deliberately
created.

The cost is that "which way is Planet 4" needs an in-game answer: an instrument, a chart, or
coordinates earned as progression. `docs/gameplay/exploration.md` already asks whether map and
scan data are progression items; this is what makes answering it mandatory.

### Space height — settled 2026-09-06: `min_y: -512`, `height: 1024`

A short world, per this section's own reasoning: cheap, and reinforces that space is wide rather
than tall. The planetary plane sits at `y: 0` — the middle of the symmetric range — giving 512
blocks of manoeuvring room both above and below it (`SpaceDimension.VERTICAL_BOUND` in code),
which is enough headroom for elytra flight without needing to feel tall. Revisit if
Sable/Aeronautics ships later make "feels like a corridor" a real complaint rather than a
hypothetical one; nothing here is load-bearing enough to be expensive to change.

Not to be confused with `approach_radius` (§4) — that is a *horizontal* distance from a planet's
`[x, z]` position within the plane, already settled per-planet in the schema. This section is
about the dimension's own vertical build limit, a separate axis entirely.

**This limit is a warning, not a wall.** Minecraft does not clamp entity movement to a
dimension's declared height — only block placement and worldgen respect it — so a player who
holds "up" can fly straight past `y: 512` with nothing stopping them, and vanilla's own void
damage (keyed off `min_y - 64`, i.e. `y: -576` here) would eventually kill them below it. Both
are handled explicitly by `SpaceMechanics`, on Sanchit's call: a repeating message beyond
`±512`, no forced correction, and void damage suppressed entirely in this dimension — the
player stays in full control in both directions, and only air is a real limiter, same as the
horizontal case this document already argues for. See `plans/m2-worlds.md`'s M2.5 section.

## 4. The planet schema

```
data/ascension_worlds/planet/moon.json
```

```json
{
  "surface": "ascension_worlds:moon",
  "space": {
    "position": [ 8000, 0 ],
    "body_radius": 192,
    "approach_radius": 320
  },
  "environment": {
    "breathable": false,
    "drain_multiplier": 1.0
  },
  "order": 20
}
```

And Earth, which is a planet like any other — that is the point of claiming it:

```
data/ascension_worlds/planet/earth.json
```

```json
{
  "surface": "minecraft:overworld",
  "space": {
    "position": [ 0, 0 ],
    "body_radius": 384,
    "approach_radius": 512
  },
  "order": 10
}
```

No `environment` block: it defaults to `EARTHLIKE`, which is what Earth is. Nothing about the
Overworld changes because of this file — it declares where Earth *is*, so that flying home uses
the same descent every other world uses.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `surface` | dimension id | yes | The dimension a player lands in. Defined by a datapack, not here. |
| `space.position` | `[x, z]` | yes | Where the planet is in the shared space dimension. |
| `space.body_radius` | int | yes | How large the body is — what gets rendered, and what you cannot fly through. |
| `space.approach_radius` | int | yes | Distance within which descent to `surface` is possible. |
| `environment` | object | no | Defaults to `EARTHLIKE`. Published through `core`. |
| `order` | int | no | Sort order for any listing. Defaults to `0`. |

**No `name` field.** The display name is a lang key derived from the id —
`planet.ascension_worlds.moon` — which is the vanilla convention and means translations work
without a field per language.

### Position is two-dimensional, and that is a hard constraint

Y is unusable for interplanetary distance. A dimension's total height caps at a few thousand
blocks; X and Z run to ±29,999,984. Planets are therefore laid out on a **plane**, and space is
wide rather than tall.

This is a limit that happens to be a gift: a plane can be drawn on a chart, which makes
navigation — now a real design problem per ADR-0010 — something a player can reason about
instead of guess at.

**Lay out all seven positions now**, even though three are built. Positions set travel times,
travel times set pacing, and retrofitting a coordinate after players have flown between two
planets changes a distance they have already learned.

### Two design rules for this schema

**1. No field lands here until something reads it.** No `unlocked_by`, no
`post_clear_shortcut`, no `gravity` — those belong to `progression` and `gear`, which do not
exist. A schema field nothing consumes is a promise that has not been tested, and the same
friction argument as ADR-0011 applies: contracts get added when two things genuinely need them.

**2. Be frugal in JSON, generous in Java.** These look contradictory against the M1.1 decision
to ship `drainMultiplier` early, and the difference is real: **adding an optional field to a
codec is not a breaking change** — existing JSON still parses. Adding a component to a Java
record in an `api` package *is* — it changes the canonical constructor for everyone who
implements it. So a data schema can grow safely and a public record cannot, and they get
opposite treatment.

## 5. Registry mechanism

A **datapack registry**, via NeoForge's `DataPackRegistryEvent.NewRegistry`, with a codec and
network sync.

- Codec validation means a malformed planet fails at datapack load with a real message, rather
  than at some later moment when something reads a null.
- Datapack-overridable, which is what makes ADR-0004's "adding a world is authoring" true for
  other people and not just for us.
- **Synced to clients**, because the client needs positions and names to render a planet in the
  sky and to draw any chart. That is not optional under ADR-0010 — a planet you cannot see from
  space makes the shared space dimension an empty void with invisible waypoints.

Held as a registry, resolved on demand, never cached in a static map keyed by dimension
(ADR-0007 rule 1 and rule 3).

## 6. Open for review

1. ~~**Distances.**~~ **Settled 2026-09-05: Earth `[0, 0]`, Moon `[8000, 0]`.** See §3.
   Planets 4–7 still need laying out, and should be laid out before any of them is built.
2. **Does `body_radius` do two jobs badly?** It is both the rendered size and the collision
   volume, and stays that way through M2 — the renderer exists now (`SpaceSkyRenderer`) and
   `body_radius` still drives both without visible trouble at two planets. Left open rather than
   resolved: worth revisiting once a planet's real rendered size and its "cannot fly into" volume
   actually need to differ (an atmosphere entry effect keyed to a radius wider than the body, say),
   not before.
3. ~~**Is Earth a planet in this registry?**~~ **Settled 2026-09-05: yes.** Earth is
   `surface: minecraft:overworld` at `[0, 0]`. "Fly home" then falls out of the same mechanism
   as every other descent, with no special case for the one world that matters most.

   The cost is that our registry claims a vanilla dimension, which an adopter might not expect.
   Mitigated by it being *data*: the Earth entry is a JSON file in our datapack, so anyone who
   wants `minecraft:overworld` left alone deletes one file.
4. ~~**How does a player reach the space dimension from Earth's surface?**~~ **Settled
   2026-09-06: vanilla elytra + firework rockets, no new gear.** No `gear` module exists yet and
   ships (Sable, ADR-0006) are explicitly out of M2 v0.1's scope, so the ascent mechanism has to
   work with what already exists in the base game. Crossing `y: 320` — the Overworld's own build
   height, chosen so Earth's `dimension_type` needs no override — while airborne on Earth
   transitions the player to `ascension_worlds:space` at Earth's `SpacePosition` (`[0, 0]`,
   `y: 256`, the plane), velocity and heading preserved, per the "ascent triggers on altitude"
   behaviour already described above.

   **Create Aeronautics + Sable ship-based ascent is planned, later, as the Tier 2 upgrade** —
   this does not block it and does not need to be designed around now. When it lands, it is
   another way to cross the same threshold, not a replacement for it: `ascension-compat-sable`
   bridges ships into the same dimension-change hook, per ADR-0006's own posture of ships as an
   optional integration.

### Distant Horizons: tested when orbit exists, and it does not get a vote

Deliberately **not** answered with an early throwaway dimension, which is what this document
originally proposed.

The reason is better than the proposal was: *DH is not the game.* It is an optional third-party
optimisation mod, and letting one constrain our dimension design would invert
[ADR-0002](../decisions/0002-custom-mods-not-curated-modpack.md) and
[ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md) — the whole
posture of this project is that third-party mods are things we work *with*, never things our
Tier 1 design answers to. A negative result would be DH's problem to route around, not a reason
to reshape a planet.

So it is a compatibility check at M2.5, against the real thing, not a design input beforehand.
