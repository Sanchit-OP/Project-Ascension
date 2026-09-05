# ADR-0010: Orbit is one shared interplanetary space dimension

- **Status:** Accepted
- **Date:** 2026-09-05
- **Supersedes:** the *"Deferred, not settled"* section of
  [ADR-0004](0004-fully-custom-dimensions.md)

## Context

ADR-0004 deferred one question deliberately: **is orbit a separate dimension per planet, or a
high-altitude band of the surface dimension?** It said the answer belonged to the
`ascension-worlds` design pass, before Moon work begins. This is that pass.

Two things changed since ADR-0004 was written.

**A third option appeared.** `docs/technical/dimensions.md` was already asking *"is
interplanetary space a navigable dimension, a set of routed orbit layers, or both?"* — but that
question was filed under exploration, not under dimension representation, so it never reached
ADR-0004's list of alternatives.

**A hard constraint arrived from playing.** High render distance is *necessary* for space travel
to read well, not a nice-to-have. That was established in game with Distant Horizons installed,
and it is recorded in `todo.md`. It changes the cost of the options materially.

| Option | v1 dimensions | At seven worlds | Travel between planets |
|---|---|---|---|
| High-Y band of each surface | 3 | 7 | A transition |
| Orbit dimension per planet | 6 | 14 | A transition |
| **One shared space dimension** | **4** | **8** | **A journey** |

## Decision

**Orbit is a single shared interplanetary space dimension.** Planets are destinations within it.
Each planet has a position in space and a surface dimension; descending to a surface is a
dimension change, and travelling between planets is not.

## Why

**It is the only option where travel is a journey.** ADR-0004 rejected Ad Astra on three
counts, and the first was that *travel is a menu*. Both other options rebuild the menu: with
orbit-per-planet or a high-Y band, going from the Moon to Planet 3 can only be a transition
between two unconnected places, which is a menu with extra steps. A shared space means the
distance between two planets is a distance.

**The high-Y band is the most expensive option at the render distance we need, not the
cheapest.** It looks cheapest — fewest dimensions, no transition on descent — but it keeps the
planet's surface chunks loaded while the player is in orbit. Loaded chunks cost memory and
ticking on both sides, quadratically in view distance. Committing to high render distance and
then choosing the option that maximises loaded chunks would be choosing both halves of a
trade-off.

**It separates two problems that were being conflated.** Seeing far on a *surface* is a chunk
problem — Sodium, Distant Horizons, Chunky. Seeing far in *orbit* should be a rendering problem
with almost no chunks behind it: from orbit you need a planet, which is one rendered body, not
thousands of loaded chunks. A shared space dimension can be near-empty and therefore nearly
free, which is exactly what makes vast view distances affordable there.

**It scales better in the direction we are going.** Eight dimensions at seven worlds against
fourteen. Each dimension carries region files, a Distant Horizons LOD database, and its own
chunk-loading overhead, so halving the count is a real saving and not a tidiness argument.

## Consequences

- **Navigation becomes a design problem, deliberately.** "Where am I and which way is Planet 3"
  is now a real question needing a real answer — instruments, charts, or earned coordinates.
  `docs/gameplay/exploration.md` already asks whether map and scan data are progression items;
  this decision makes answering that mandatory rather than optional.
- **The space dimension must stay cheap.** Void generation, nothing ticking, no structures
  except authored ones. If interplanetary space ever becomes as expensive as a surface, this
  decision has been implemented wrongly.
- **Planets need a position and a scale.** Both become fields in the planet data schema, and
  the distances chosen set the pace of travel. That is a balancing decision, not a technical one.
- **A planet must be visible from space** or the fiction collapses into flying through an empty
  void looking for an invisible waypoint. That is client rendering work, and it is now on M2's
  critical path rather than a nice finish.
- **Gateways get an obvious home.** `dimensions.md` has *"Rift Gates connect orbital spaces
  after discovery"* and asks where ancient gateways physically exist. In a shared space they are
  points in one continuous place, which makes a restored gateway a genuine shortcut across a
  real distance rather than a second menu entry.
- The world border in the space dimension has to accommodate every planet in the eventual
  seven, so positions should be laid out now with all seven in mind even though three are built.

## Alternatives rejected

**Orbit dimension per planet.** Simplest to author and the least design work — one orbit, one
surface, nothing to navigate. Rejected on dimension count at scale and, more importantly,
because it makes interplanetary travel a transition by construction.

**High-Y band of the surface dimension.** Cheapest in dimension count and needs no transition
on descent. Rejected because it is the most expensive option in loaded chunks at the render
distance this project has committed to, and because it complicates worldgen and gravity for a
saving that turns out not to exist.
