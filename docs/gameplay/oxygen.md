# Oxygen

> **Updated 2026-09-05.** Settled: oxygen is a **fully custom system**, built as
> `ascension-atmosphere` — the first module we build
> ([M1](../../plans/m1-atmosphere.md)), and the flagship reusable module of the project
> ([ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md)).
>
> Two constraints now bind the design:
> - **Zones must be anchorable to a moving region**, not only static world coordinates, or the
>   Sable ship bridge becomes impossible later
>   ([ADR-0006](../decisions/0006-sable-integration-posture.md)).
> - **Extension is via provider registries** — any mod can register an `AtmosphereProvider` or
>   `OxygenSource` without extending our classes.
>
> The "Authored Simulation Boundary" section below is the single most important part of this
> document and remains fully in force.

## Purpose

Defines oxygen as a progression and exploration system.

## Oxygen Design Goals

- Oxygen should create pressure, not annoyance.
- Oxygen upgrades should broaden mission duration and risk tolerance.
- Oxygen logistics should interact with vehicles, bases, and suits.
- Oxygen should be one of the main reasons planetary preparation matters.

## Current Assumptions

- Earth is breathable and acts as the baseline safe atmosphere.
- Some planets or structures are not breathable and require oxygen support.
- Oxygen support can come from tanks, generators, enchantments, and other upgrades.
- The system must support both short expedition loops and longer established outposts.
- Spacecraft should support onboard oxygen refilling or distribution if properly equipped.
- The current direction is a custom oxygen system if existing mods cannot support the required progression cleanly.

## Oxygen Components

- Personal oxygen tanks
- Suit-linked oxygen storage
- Oxygen generators or refill stations
- Vehicle oxygen support
- Enchantments or upgrades that improve efficiency, reserve capacity, or survivability
- Portable stored oxygen carried as mission supplies

## Design Rules

- Early off-world oxygen should feel limited and tactical.
- Midgame oxygen should improve mission duration, not remove planning.
- Lategame oxygen can reduce friction, but should not erase hostile-world identity.
- Oxygen failures should be harsh enough to matter, especially in space or hostile atmospheres.
- Running out of oxygen should resemble underwater suffocation pressure: a short failure window followed by death if not corrected.
- Players should choose between carrying more reserve oxygen and using gear that consumes oxygen more efficiently.
- Oxygen logic should react to player behavior and environment rather than acting as a flat timer.

## Authored Simulation Boundary

- The project should avoid fully open-ended physical gas simulation.
- "Physics" should mean authored gameplay logic: breathable zones, tank capacity, refill infrastructure, suit efficiency, vehicle support, environmental drain modifiers, and failure states.
- The oxygen system should model pressure and logistics in ways that are legible to players and feasible to maintain.
- If the system becomes too granular to communicate clearly, it is overdesigned.

## System Questions To Resolve

- Is oxygen fully custom or built on top of an existing mod system?
- Is oxygen consumed only in certain dimensions or also in special structures?
- Can players create temporary field refills?
- How do multiplayer rescue and recovery work?
- Does oxygen deplete uniformly, or do specific actions increase usage?

## Open Questions

- What is the base oxygen loop on first orbital entry?
- How visible should oxygen information be in the UI?
- Do sprinting, combat, flight, or environmental hazards increase oxygen usage?
- Do sealed bases and ships create local breathable zones, or only refill points?
