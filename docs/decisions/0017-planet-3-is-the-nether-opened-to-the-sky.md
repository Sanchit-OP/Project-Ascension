# ADR-0017: Planet 3 is the real Nether, opened to the sky

- **Status:** Accepted
- **Date:** 2026-10-11

## Context

v1's third world (ADR-0005) is the Nether world, decided 2026-10-06: mostly Nether in feel, no
roof so it joins the shared space dimension (ADR-0010), netherite its resource, the Netherite
Monstrosity its boss. The M3 plan then said it would be **our own dimension** using vanilla's
Nether biomes, with vanilla's Nether left dormant.

ADR-0014 had already put it the other way round: the Nether and the End "are meant to become
planets in this campaign", and "the day the Nether becomes a planet, planet travel into it is not
caught by" the dormant-portal rule.

Sanchit, 2026-10-11, asked the question directly: "let it be nether, it's already good with it;
we just remove the roof so we have a way into the nether." Asked which Nether, he chose the real
one.

## Decision

**`minecraft:the_nether` itself becomes Planet 3.** `ascension-worlds` overrides it as data, the
way Earth is already `minecraft:overworld` in the planet registry:

- **Its terrain opens to the sky.** The Nether is solid rock up to a bedrock ceiling, so removing
  the roof means our own `noise_settings`: the same biomes, ores, lava sea and cave character,
  with the rock thinning out above into open sky instead of closing into a ceiling. Biomes stay
  vanilla's `minecraft:nether` biome source, so fortresses, bastions, ancient debris and every
  Nether mob arrive through their usual biome tags, and so does any other mod's Nether content.
- **Its dimension type follows:** no ceiling, and a height a ship can climb out of. The details
  (skylight, the permanent-night clock, the 8× coordinate scale, the look of its sky) are settled
  while building and recorded in `worlds-api.md`.
- **A planet entry** puts it in space, so it is reached like the Moon: by airship (ADR-0015) and
  later by its gateway. Sable gets its air data like Earth's.
- **Vanilla portals stay dormant** (ADR-0014). Only portal-driven travel is blocked, so ships and
  gateways go in.
- **Survival rule: wear gold.** The air carries the rot (`docs/lore/the-nether.md`): without gold
  on you it slowly withers you. "Gold" is exactly what keeps piglins calm (NeoForge's
  `makesPiglinsNeutral`), so the lore, the rule and the vanilla mechanic are one thing; netherite
  armour counts too, so the planet's reward upgrades its own rule.
- **Traversal: the Nether as it is.** Its lava seas and cliffs are the challenge; no extra
  mechanic.
- **Life: vanilla's Nether mobs only** for v1.
- **Art** (its sky, the gate ruin, colony ruins, tablets) goes to a separate design session through
  a contract doc, as the gateway and the lore art did.

## Consequences

- **Fresh worlds only.** A world's dimension settings are frozen when it is created
  (`dev-environment.md`), so an existing world keeps its old Nether. Nobody has been there (the
  portals are dormant), so nothing is lost; the dev world needs a fresh start to see Planet 3.
- **ADR-0014's recipe debt is paid** once Planet 3 is reachable: ancient debris, quartz, blaze
  rods and glowstone are back, and Earth's keystone becomes craftable in survival.
- **Cataclysm's two Nether bosses come back with it.** The Netherite Monstrosity (Soul Black Smith)
  is Planet 3's boss as planned. Ignis (Burning Arena) also generates in the Nether, and stays
  there (Sanchit, 2026-10-11): a second, optional Nether boss.
- **Other mods that reshape the Nether conflict with this** (two overrides of one dimension: the
  last datapack loaded wins). An adopter who wants vanilla's Nether back removes our override files.
  If that proves common, a load condition can gate them later.
- Vanilla's Nether advancements and mods that target the Nether dimension itself keep working,
  which an own-dimension Planet 3 would have lost.

## Alternatives rejected

- **Our own Nether-like dimension** (the M3 plan's version): cleaner separation, but it drops
  Nether advancements and any mod content keyed to the dimension rather than its biomes, and
  leaves the real Nether a dead world forever. Sanchit chose the real one.
- **Delete only the bedrock roof.** Leaves solid netherrack overhead: the Nether's rock fills the
  space right up to its ceiling.
