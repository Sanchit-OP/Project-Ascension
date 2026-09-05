# Balancing

> **Recorded 2026-09-05.** One coupling that is easy to miss and expensive to discover late:
> **distance to a planet and carried air are the same dial seen from two ends.**
>
> Space is vacuum, so a journey is spent entirely on tank. At current tuning a tank plus lungs
> is 320 seconds; the Moon at 8,000 blocks is a 267-second round trip at 60 b/s. Changing
> `AtmosphereTuning.TANK_CAPACITY` therefore changes how far away every planet effectively is,
> and changing a planet's position changes what tier of life support it demands.
>
> Neither number can be tuned alone. See
> [`worlds-api.md`](worlds-api.md) §3 and
> [ADR-0010](../decisions/0010-orbit-is-one-shared-space-dimension.md).

## Purpose

Defines balancing methodology so the pack feels authored rather than chaotic.

## Balancing Priorities

- Time-to-unlock pacing
- Material scarcity
- Combat lethality
- Automation payoff
- Travel friction
- Multiplayer fairness

## Metrics To Track

- Average prep time per mission
- Boss attempt time
- Planet clear time
- Resource throughput after automation
- Death recovery cost

## Open Questions

- What pacing target defines a satisfying era?
- How hard should recovery after failure be?
- Which systems are intentionally harsh and which are convenience-biased?
