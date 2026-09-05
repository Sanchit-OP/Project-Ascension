# `ascension-atmosphere` API Design

**Status:** Frozen for v0.1 (M1.1 complete, 2026-09-05). Implementation begins at M1.2.

This document is written *before* implementation deliberately. Every other Ascension module and
every compat jar couples to this API, so it is the most expensive surface in the project to get
wrong. See [`plans/m1-atmosphere.md`](../../plans/m1-atmosphere.md).

## Constraints this design must satisfy

| Source | Constraint |
|---|---|
| [ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md) | Extension via provider registries. Never require anyone to extend our classes. Must work with only `core` present. |
| [ADR-0006](../decisions/0006-sable-integration-posture.md) | Zones must be anchorable to a **moving** region, or the Sable ship bridge is impossible later. No Tier-1 reference to Sable. |
| [ADR-0007](../decisions/0007-performance-contract.md) | No per-tick world scans. Bounded, cached recompute on block change only. Delta sync. Per-`ServerLevel` state. No long-lived `Level`/`Player` references. |
| [`oxygen.md`](../gameplay/oxygen.md) | Authored logic, not gas simulation. Legible beats granular. If it is too granular to communicate, it is overdesigned. |

---

## 1. The central query

Everything reduces to one question: **is this position breathable, and how fast does it drain
you?**

```java
public record Atmosphere(boolean breathable, float drainMultiplier) {
    public static final Atmosphere BREATHABLE = new Atmosphere(true, 0.0f);
    public static final Atmosphere VACUUM     = new Atmosphere(false, 1.0f);
}
```

Two fields, deliberately. `drainMultiplier` expresses "environmental drain modifiers" from
`oxygen.md` without simulating gases — a thin atmosphere drains at `0.5`, a corrosive one at
`2.0`. Anything richer than this crosses the line the Authored Simulation Boundary draws.

## 2. Providers and resolution order

Ambiguity about *who wins* is the classic source of unfixable interop bugs, so it is settled
explicitly rather than by registration order.

```java
public interface AtmosphereProvider {
    /** Empty means "I make no claim about this position". */
    Optional<Atmosphere> query(AtmosphereContext context);

    /** Higher wins. Use a band constant from AtmospherePriority. */
    int priority();
}
```

### Priority bands

| Band | Value | Who |
|---|---|---|
| `DIMENSION` | 0 | The planet's baseline. `ascension-worlds` registers these. |
| `STRUCTURE` | 1000 | Authored structures — a sealed bunker on an airless world. |
| `SEALED_VOLUME` | 2000 | Player-built rooms with an oxygen emitter. |
| `VEHICLE` | 3000 | Ship and sub-level interiors. |
| `OVERRIDE` | 10000 | Admin, debug, creative. |

**Highest claiming provider wins.** Ties break on registration id, fixed once when the
registry freezes, so the outcome never depends on mod load order.

> **Corrected during M1.2 implementation.** This section originally said equal-priority
> conflicts would "fail loudly at startup". That is not knowable at startup: two providers
> sharing a priority is legal and common — every dimension baseline sits at `DIMENSION` —
> and whether they genuinely conflict depends on the position being queried. The check
> therefore happens at query time, where the conflict is real: if a second provider at the
> winner's priority also claims the position, it is logged once per pair, not every tick.
> Resolution stays deterministic either way, so this is a diagnostic rather than a
> correctness fix.

### Debuggability is a feature

`/ascension atmosphere why` lists every provider that claimed the player's position, its
priority, and which one won.

Without this, a third-party integration that mysteriously loses to ours is undiagnosable, and
we will be the ones fielding the bug report. This is cheap to build now and is the difference
between an API people can adopt and one they give up on.

## 3. Anchoring — how ships become possible later

A zone cannot be stored as world coordinates, because a Sable sub-level *moves*. This is the
one place where ADR-0006 reaches directly into the M1 API.

```java
public sealed interface AtmosphereAnchor {
    record World(ResourceKey<Level> dimension) implements AtmosphereAnchor {}
    record Region(ResourceKey<Level> dimension, UUID regionId) implements AtmosphereAnchor {}
}
```

Zones store positions in **anchor-local space**. Resolving a world position into that space is
delegated:

```java
public interface AnchorSpace {
    /** World position to local block space, or empty if outside this anchor. */
    Optional<BlockPos> toLocal(Vec3 worldPos);
}
```

`World` anchors resolve as identity. `Region` anchors are resolved by whoever owns the moving
structure — `ascension-compat-sable` supplies that transform. **Tier 1 never learns what Sable
is; it only knows that some anchors are resolved by someone else.**

### v0.1 scope

Define the full seam, implement only `World`. Defining the interface now costs almost nothing.
Retrofitting it after `gear`, `worlds` and the HUD have all assumed world coordinates would be
a rewrite.

## 4. Oxygen sources

```java
public interface OxygenSource {
    int available();
    int capacity();
    int consume(int units);   // returns units actually consumed
    int accept(int units);    // returns units actually accepted
    int drawOrder();          // lower is consumed first
}
```

`accept` is the counterpart to `consume`, and it pays for itself three times over: it is how a
refill station fills a tank, how a tank is topped up from a base supply, **and** how one player
shares air with another. One method, three features.

Sources are discovered per player by registered collectors, so other mods can contribute
inventory slots, curios, suit modules or vehicle tanks without us knowing they exist:

```java
public interface OxygenSourceCollector {
    void collect(ServerPlayer player, Consumer<OxygenSource> sink);
}
```

**Draw order: portable tanks are consumed before suit-integrated reserve.** The suit is your
safety margin, so running dry on tanks is a warning rather than a death sentence. This is what
makes the reserve-versus-efficiency tradeoff in `oxygen.md` a real decision.

## 5. Consumption model

Base rate, adjusted by registered modifiers. Deliberately enumerable — a player should be able
to hold the whole model in their head.

```
drain = baseRate
      * atmosphere.drainMultiplier      // where you are
      * product(activeDrainModifiers)   // what you are doing
      / gearEfficiency                  // what you are wearing
```

### Drain modifiers are a registry, not a hardcoded list

**Decided 2026-09-05.** Rather than atmosphere knowing about sprinting, thrusters or combat,
anything can register a modifier:

```java
public interface DrainModifier {
    ResourceLocation id();              // shown by /ascension atmosphere why
    float multiplier(ServerPlayer p);   // 1.0 = no effect
}
```

`ascension-gear` can make thrusters cost air without `atmosphere` ever learning what a thruster
is. This is the same provider-registry philosophy as `AtmosphereProvider`, applied to
consumption, and it is what lets us add drain triggers later without touching this module or
breaking its API.

**Sprinting is explicitly not a modifier.** Every planet involves a great deal of walking, and
taxing ordinary traversal reads as friction rather than tension. Drain triggers should be
discrete and meaningful — sustained combat, thruster burns, hostile-atmosphere exposure — not
a constant background penalty on moving around.

**v0.1 ships the registry with no built-in modifiers.** The seam exists; the content arrives
with `gear` and `worlds`.

### Units

**Decided 2026-09-05.** Stored as **integer units**, displayed as **seconds of air remaining**.

Integers keep arithmetic exact and avoid float drift across save/load. Seconds are what a
player can actually act on — "40 seconds" is a decision, "1200 units" is not. The conversion
lives in one place so the display can be retuned without touching consumption logic.

### Tick rate

Per ADR-0007 rule 6, chosen deliberately: **accounting runs every 10 ticks (0.5s)**. Oxygen does
not need 20 Hz, and 0.5s granularity is imperceptible against a multi-second failure window.

> **Corrected 2026-09-05.** This paragraph previously claimed the client bar interpolates
> between updates. It does not — the client renders the last value it received, so the readout
> steps in 0.5s increments. Client-side prediction is worth building (it would smooth the bar
> *and* let the server sync roughly 4x less often, since the client already receives
> `drainPerSecond` and could extrapolate from it), but it is not built, and the document should
> not describe work that does not exist. Tracked for M1.8.

### What the accounting pass actually costs

The honest shape, after the M1.5 optimisation pass:

| Player state | Work per pass |
|---|---|
| Breathing normally | one atmosphere query, a short-circuit on drain, two comparisons, then return |
| Draining | the above, plus one walk of the player's oxygen sources, plus one packet |

The fast path matters because breathing normally is the common case for the entire first act of
the campaign. Before it existed, an idle player on Earth paid for a full payload construction
and two extra walks of every registered collector, every half second, to discover that nothing
had changed.

Drain is inherently time-based — something has to tick for a depleting resource — so the goal is
not to eliminate the pass but to make the common case nearly free.

**Deliberately not cached: the atmosphere query itself.** Caching it per player would need
invalidation on block change, which does not exist until sealed volumes arrive in M1.6. Adding
a position-keyed cache before then would risk a stale answer when water flows in or a wall is
broken — a correctness bug traded for a saving too small to measure at any realistic player
count.

## 5b. Water is the same problem as vacuum

**Decided 2026-09-05.** Drowning and suffocating in vacuum are the same situation: no air to
breathe. Running them as two parallel meters with two separate failure timers is a UI accident
of vanilla, not a design choice, and this project should not inherit it.

So water registers as an ordinary unbreathable atmosphere, and one bar covers both.

### Why this is more than tidiness

It makes existing gear meaningful off-world instead of dead weight, and it makes new gear
meaningful underwater without any special-casing:

| Vanilla thing | Becomes |
|---|---|
| Respiration enchantment | a `gearEfficiency` contribution |
| Turtle helmet | a `gearEfficiency` contribution |
| Conduit power | a `STRUCTURE`-band provider claiming breathable |
| An oxygen tank | works underwater, because water is just another atmosphere |

A player who kits out for the Moon is, without being told, also kitted out for deep water. That
is the kind of unification worth having.

### The catch, and how it is handled

Taking this over means suppressing vanilla's air supply and drowning damage. For a module
whose first priority is being adoptable by other people (ADR-0003), silently seizing a core
vanilla mechanic is exactly the kind of surprise that makes a library untrustworthy.

Therefore:

- The water provider is **built in but individually switchable**, defaulting **on** for
  Project-Ascension and documented prominently for anyone else.
- It claims at the `DIMENSION` band, so any structure, vehicle or sealed volume still overrides
  it. A pressurised submarine works with no extra code.
- Vanilla air supply is held full only while our system is actually managing the player, so
  disabling the feature restores stock behaviour exactly rather than leaving the player in a
  half-converted state.

### Scheduling

Implemented in **M1.5**, alongside drain and the failure window. Doing it before consumption
exists would produce a bar that correctly says "you cannot breathe" and then never moves,
which tests nothing.

## 6. Failure model

From `oxygen.md`: *"a short failure window followed by death if not corrected."*

On reaching zero supply, a **grace window** opens — escalating damage with loud audio and
visual feedback. Re-entering breathable air or refilling within the window recovers fully.

This mirrors vanilla drowning on purpose. Players already understand that pressure curve, so
it needs no tutorial.

## 7. Sealed volumes

Emitter block pressurises the enclosed space via a **bounded** flood fill:

- Hard cap on volume and radius. Exceeding it means the space is not sealed, and the emitter
  reports failure visibly rather than silently doing nothing.
- Result cached per emitter.
- Invalidated **only** by block changes inside the emitter's bounds, tracked by `SectionPos`.
- Never recomputed on a schedule (ADR-0007 rule 5).

## 7b. The emitter is a placeholder

**Recorded 2026-09-05.** The Oxygen Emitter as built is a single block that pressurises a room
for free. That is scaffolding for testing sealed volumes, not the intended machine.

The real shape is three parts, built around Create:

| Part | Job |
|---|---|
| **Generator** | Produces oxygen. Powered by Create rotation, or electricity from a Create addon. |
| **Tank** | Buffers oxygen. Decouples production rate from consumption, so a room survives a stall. |
| **Pressuriser** | Consumes from the tank to hold a sealed volume breathable. |

The generator can feed a tank, or feed a pressuriser directly &mdash; the direct path being the
cheap early-game setup, and the buffered path what you build once you care about surviving a
power cut.

### Why this does not change the current code

The sealed-volume machinery does not care what pressurises a room. `SealedVolumeProvider`
answers "is this position inside a volume"; what *maintains* that volume is the block's problem.
Splitting one emitter into generator, tank and pressuriser is a block-layer change, and the
index, the fill, the invalidation and the provider are all untouched by it.

The pressuriser will also want to *stop* holding a volume when it runs out of stored oxygen,
which the index already supports: that is the same path as a wall being broken.

### Dependency note

This lands in a Tier 2 `ascension-compat-create` jar, not here. Tier 1 may not depend on Create
(ADR-0003 rule 1). `ascension-atmosphere` keeps a working standalone emitter so the module
stays useful with no Create installed; the Create machines are the Project-Ascension
progression version of the same idea.

## 8. State and sync

| What | Where | Why |
|---|---|---|
| Player oxygen | NeoForge data attachment | Serialises with the player, no side table to leak |
| Zone index | Per-`ServerLevel` | Unload frees it naturally (ADR-0007 rule 3) |
| Emitter cache | Per-`ServerLevel` | Same |

Server is authoritative. The client is sent **only the viewing player's own oxygen level and
current breathability** — a handful of bytes, on change, plus a low-frequency reconciliation.
Zones are never synced wholesale; the client does not need them to draw a bar.

No static collection anywhere holds a `Level`, `Player`, `Entity` or `BlockEntity`
(ADR-0007 rule 1).

## 9. `api` versus `internal`

**`com.ascension.atmosphere.api`** — semver contract:
`Atmosphere`, `AtmosphereProvider`, `AtmospherePriority`, `AtmosphereContext`,
`AtmosphereAnchor`, `AnchorSpace`, `OxygenSource`, `OxygenSourceCollector`,
read-only `OxygenView`, registration entry points, events.

**`com.ascension.atmosphere.internal`** — no guarantees:
flood fill, caches, packets, HUD, commands, attachment implementations, the provider registry
implementation.

A consumer needing anything from `internal` means the `api` is wrong and should be extended.

---

## Decisions taken at review (2026-09-05)

1. **UI** — oxygen bar plus a **minimal hazard indicator**. Enough to show *why* you are
   draining once `drainMultiplier` varies by world, without building a readout nobody reads.
2. **`drainMultiplier` ships in v0.1.** It is one float, and reshaping a record after third
   parties depend on it is a breaking change.
3. **Units** — integer storage, seconds on display. See above.
4. **Sprinting does not cost oxygen.** Drain modifiers are a registry; triggers are added later
   by the modules that own them.

## Multiplayer rescue

**Decided 2026-09-05: share air.**

A player can transfer oxygen from their own supply to a teammate's. This fits the co-op-first
pillar in `vision.md`, turns a teammate's reserve into part of team planning, and costs one
method on `OxygenSource` — `accept` — which refill stations need anyway.

Deliberately *not* chosen: a downed-and-revive state. It is the most dramatic option and a real
co-op moment, but it is a whole system and belongs well after v0.1. Nothing in this API
prevents adding it later, because reviving does not change how oxygen moves.

The decision had to be made now rather than later: adding a transfer operation to
`OxygenSource` after third parties implement the interface is a breaking change.

---

## API status

**Frozen for v0.1 as of 2026-09-05.** All review questions are settled. Implementation begins
at M1.2.

Changes to anything in the `api` package from this point need a note in the commit explaining
what forced it — that friction is the point, and it is why this document was written before any
code existed.
