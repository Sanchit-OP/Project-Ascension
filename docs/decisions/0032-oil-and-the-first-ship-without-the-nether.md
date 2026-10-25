# ADR-0032: Oil and the first ship do not need the Nether: three Earth recipes

- **Status:** Accepted, 2026-10-25 (Sanchit: "oil needs to come earlier as we need it in the first ship part"; "if a mod has it for something we should keep that mod's essence")
- **Date:** 2026-10-25
- **Amends:** [ADR-0031](0031-crude-oil-and-the-refinery-are-create-petrochem.md) (its first consequence said the refinery was a Nether-tier road)
- **Builds on:** [ADR-0018](0018-no-world-is-required.md) (nothing is required, so an Earth road beside the Nether one costs nothing),
  [ADR-0003](0003-modular-architecture-and-compatibility-policy.md) (the recipes live in the Tier-2 Create jar), [ADR-0030](0030-rocket-parts-are-create-propulsion-and-aeroworks.md)

## Context

ADR-0031 recorded a finding, not a decision: Petrochem's steel, bronze and distillation tower are all *heated* Create recipes, so the refinery needs a **Blaze Burner**, and a Blaze Burner needs a
captured blaze from the Nether. Sanchit then said oil is needed for the first ship's liquid Thruster, so it has to come before the Nether, not after it.

Working the chain back from kerosene found **three** hidden Nether inputs, not one:

1. **A Blaze Burner.** Create's recipe is a captured blaze (`create:blaze_burner_capturable` is the blaze alone). Without it nothing heated runs: steel, bronze, steam, the tower.
2. **An electron tube.** Created from polished rose quartz, which is Nether quartz and eight redstone. The pumpjack's controller needs a precision mechanism, a precision mechanism is
   built by a deployer (sequenced assembly), and a deployer needs electron tubes.
3. **Petrochem's tin.** It has no ore. A zinc nugget is filled with a thick potion by a spout, and a thick potion needs glowstone dust and a brewing stand (a blaze rod, again).

## Decision

**Three recipes, each an addition, none replacing the original** (`modules/compat/create/src/main/resources/data/ascension_compat_create/recipe/`):

| Item | Recipe | Why it is dearer than the original |
|---|---|---|
| `create:blaze_burner` | Shaped: a lava bucket over a magma block, with iron sheets round it and below (`" L "`, `"IMI"`, `" I "`) | A lava bucket, a magma block (found on ocean floors) and four sheets; Create's own recipe is one captured blaze |
| `create:electron_tube` | Shapeless: an amethyst shard, four redstone and an iron sheet | An amethyst geode, redstone and an iron sheet in place of one polished rose quartz |
| `petrochem:tin_nugget` | Shapeless, only when Petrochem is loaded: two zinc nuggets and an iron nugget | Two zinc for one tin; the spout road costs a thick potion and zinc, 25 mB at a time |

Each has a recipe-book advancement beside it. Nothing of Create's or Petrochem's is removed or edited: a player who reaches the Nether still takes the faster road (a captured blaze, polished
rose quartz, a thick potion), and the Nether still pays what only it can: blaze cake and superheated heat, netherite, and quartz.

**The essence of the mod is kept** (Sanchit's rule for this work): Petrochem's recipes, machines and fuels are untouched, and every fuel keeps the job the mod gave it. See
`docs/progression/oil-and-fuels.md` for what each one is for.

## What was proved, and what was not

On a dedicated server (`create-checks`, 61 checks, 0 failures; `checkBurnerByHand` and `checkEarthRecipes`): the three recipes load, each makes the item named (Create's own `blaze_burner`, which is the
item a captured blaze gives, not the empty one), and the ingredient lists are what the table says, with no Nether item among them (no netherrack or blaze rod in the burner, no quartz in the tube). Create's empty
burner recipe is untouched. **Not proved:** a hand-made burner heating a basin or the tower in a client, a precision mechanism built from a hand-made tube, a bronze plate from hand-made tin. These are listed with
the other unseen items in `plans/alpha-3.md`.

## Consequences

- ADR-0031's first consequence is amended: oil is an Earth road. The refinery needs iron, coal, copper, lava, a magma block, an amethyst shard, redstone, zinc and iron nuggets, and no blaze. The Moon can be
  reached on coal, charcoal or helium, or on kerosene made on Earth.
- The quest book places **Crude** (steel, bronze, pumpjack, electrolyzer, tower, kerosene) before **Wings** (the first ship's controls and thrusters). The burner and the electron tube are quests of
  **Brass** (`pack/quests/brass.py`), and Crude's Bronze quest tells the tin recipe (`crude.py`). Nothing in the book gates a path, so a player who prefers the Nether road loses nothing.
- The Create jar now has three recipes that depend on `minecraft:` and `create:` items only; the tin one carries a Petrochem condition. No Tier-1 module is touched.
- If Create or Petrochem ever rename these items, the server check fails first (it loads each recipe and resolves each result).
