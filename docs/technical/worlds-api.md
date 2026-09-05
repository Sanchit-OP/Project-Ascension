# `ascension-worlds` API design

> **Status: draft for review.** Nothing here is implemented. Written before code, for the reason
> M1.1 established: a module's API is the most expensive thing in it to change once other
> modules couple to it.
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

### Open: how tall is space?

A dimension's height is configurable and space needs almost none — planets lay out on a plane
and flying is horizontal. A short world (a few hundred blocks) is the cheapest thing to load and
reinforces that space is wide rather than tall. Against that, a ship with no vertical room to
manoeuvre may feel like a corridor. Decide before M2.5.

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
2. **Does `body_radius` do two jobs badly?** It is currently both the rendered size and the
   collision volume. Those may want to be separate once there is a renderer.
3. ~~**Is Earth a planet in this registry?**~~ **Settled 2026-09-05: yes.** Earth is
   `surface: minecraft:overworld` at `[0, 0]`. "Fly home" then falls out of the same mechanism
   as every other descent, with no special case for the one world that matters most.

   The cost is that our registry claims a vanilla dimension, which an adopter might not expect.
   Mitigated by it being *data*: the Earth entry is a JSON file in our datapack, so anyone who
   wants `minecraft:overworld` left alone deletes one file.
4. **How does a player reach the space dimension from Earth's surface?** Out of scope for the
   schema, on M2's critical path, and the answer shapes what `approach_radius` means on the way
   *out* as well as in.

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
