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

## 3. The planet schema

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

## 4. Registry mechanism

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

## 5. Open for review

1. **Distances.** What is the scale between planets? This sets the pace of the entire campaign
   and is a balancing decision, not a technical one. A candidate to argue about: Earth at
   `[0, 0]`, Moon at `[8000, 0]` — far enough that the trip is a trip, close enough that it is
   not a chore.
2. **Does `body_radius` do two jobs badly?** It is currently both the rendered size and the
   collision volume. Those may want to be separate once there is a renderer.
3. **Is Earth a planet in this registry at all?** It has no custom dimension — it is
   `minecraft:overworld`. Treating it as a planet with `surface: minecraft:overworld` is elegant
   and makes "fly home" fall out for free. It also means our registry claims a vanilla dimension,
   which an adopter might not expect.
4. **How does a player reach the space dimension from Earth's surface?** Out of scope for the
   schema, on M2's critical path, and the answer shapes what `approach_radius` means on the way
   *out* as well as in.
