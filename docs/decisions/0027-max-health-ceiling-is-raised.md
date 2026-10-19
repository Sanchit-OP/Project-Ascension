# ADR-0027: Max Health's ceiling is raised, so bosses can have the health the balance gives them

- **Status:** Accepted, 2026-10-19 (Sanchit found every boss stuck at 1,024 health instead of the 3,000 or 6,000 the
  balance intends)
- **Date:** 2026-10-19
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md) (no mixins in Tier 1; foreign
  integration in Tier 2), [ADR-0019](0019-item-stats-are-data.md) (stats are attributes)

## Context

The balance (`docs/progression/balance.md` section 5) gives the six bosses their own total health, per difficulty:
Warden 1,275 to 1,875, Wither 5,525 to 8,125, and Cataclysm's Ender Golem, Ender Guardian, Ignis and Netherite
Monstrosity 2,125 to 6,500. These are applied as modifiers on the vanilla Max Health attribute (`MobStats`).

**Vanilla clamps that attribute at 1,024.** The limit is inside the attribute object (a `RangedAttribute` built with a
maximum of 1,024), so no modifier, command or datapack can pass it. Every boss total above it came out at exactly 1,024:
the Wither was no tougher than the Warden, the late bosses were no tougher than the early ones, and every
percent-of-health weapon (which reads the target's maximum health) hit a 1,024-health boss for a sixth of what the
balance tuned it for.

Armor met the same wall and was solved by giving the game a second attribute of ours (`CombatAttributes.ARMOR`), because
Armor is only a stat our code reads. Max Health is the attribute the game itself reads for life, the health bar, boss
bars and every healing effect, so it cannot be replaced by another.

## Decision

**Raise the maximum of vanilla's Max Health attribute to 1,000,000, once, at startup, on both sides.**
`internal/mobs/HealthCeiling` writes the attribute's `maxValue` field through NeoForge's `ObfuscationReflectionHelper`
in the common setup event. Both sides, because the client clamps what the server sends it with its own copy of the
attribute; ascension-combat is required on both anyway.

- **It is not a mixin**, so it is allowed in Tier 1 (ADR-0003 rule 2). It is a single field write on a vanilla constant, and
  it is recorded here because it is that kind of change.
- **It fails loudly.** After the write the ceiling is read back; if it did not take (the field moved in a Minecraft
  update) the game stops loading with a message naming this ADR, instead of quietly capping every boss at 1,024 again.
- **Only Max Health.** Attack Damage (2,048) and the other vanilla ceilings are untouched until something needs them.
- **A fixed number, not a setting.** A server setting would reach the client after the attribute is already in use, and
  a client and server that disagreed would clamp differently.

A second fault was found by the same check and fixed with it: `MobStats.setTotalHealth` worked a boss's total out from
its base health alone, so any other modifier added on top. Cataclysm's Ender Golem and Ender Guardian each carry an
`animation_health` modifier (+150% of base), which made them 2,725 and 3,699.5 where the data says 2,500 and 3,200.
The total is now measured with our own modifier taken off, so it is exact whatever else the mob has.

## Consequences

- The six bosses reach the totals in the data, on all three difficulties by the same code path. Seen on a dedicated
  server on Normal (`damage-checks.txt`: Warden 1,500, Wither 6,500, Ender Golem 2,500, Ender Guardian 3,200, Ignis
  5,200, Netherite Monstrosity 4,200); not yet seen in a client, where the boss bar and any health readout are the
  things to look at.
- **Existing worlds:** a boss already alive keeps the health it had; it reaches its total when it is next loaded
  with the data applied (the rules in `MobStats` apply on join and on `/reload`).
- **Players** can also have more than 1,024 Max Health now, which nothing in the game gives them.
- **Other mods that raise or read the 1,024** (a health-cap mod, an attribute-range mod) will find the ceiling already
  raised; nothing here conflicts with them.
- Health another mod changes on a boss after our rule has run is not followed until the next reload or rejoin.

## Alternatives rejected

- **A mixin** into `RangedAttribute` or `LivingEntity`. Banned in Tier 1, and a Tier-2 jar would make the balance depend
  on an optional mod being present.
- **An access transformer.** Equivalent to the field write, but a build-time change to every module's setup for one
  field, and it cannot fail loudly in the game.
- **Effective health:** keep 1,024 real health and divide incoming damage by (total / 1,024). The boss bar, a Jade
  readout and every percent-of-health weapon would then show or use the wrong number, and healing would be wrong.
- **Lower the totals to fit 1,024.** The progression from the Warden to the late bosses is the point of the balance.
