# ADR-0032: Oil and the first ship do not need the Nether: tin by hand, three burners as a gift

- **Status:** Accepted, 2026-10-25 (Sanchit: "oil needs to come earlier as we need it in the first ship part"; "if a mod has it for something we should keep that mod's essence"), **then amended the same day twice:
  the electron tube needs no recipe of ours (Sanchit: "we didn't need a recipe for quartz"), and the Blaze Burner is a gift of three, not a recipe (Sanchit: "we can't have that overground, as there is no Nether", then "option B is fine, gift 3 blaze burners")**
- **Date:** 2026-10-25
- **Amends:** [ADR-0031](0031-crude-oil-and-the-refinery-are-create-petrochem.md) (its first consequence said the refinery was a Nether-tier road)
- **Builds on:** [ADR-0018](0018-no-world-is-required.md) (nothing is required, so an Earth road beside the Nether one costs nothing),
  [ADR-0003](0003-modular-architecture-and-compatibility-policy.md) (the recipes live in the Tier-2 Create jar), [ADR-0030](0030-rocket-parts-are-create-propulsion-and-aeroworks.md)

## Context

ADR-0031 recorded a finding, not a decision: Petrochem's steel, bronze and distillation tower are all *heated* Create recipes, so the refinery needs a **Blaze Burner**, and a Blaze Burner needs a
captured blaze from the Nether. Sanchit then said oil is needed for the first ship's liquid Thruster, so it has to come before the Nether, not after it.

Working the chain back from kerosene found **two** hidden Nether inputs, not one (a third, the electron tube, was first counted and is not one: see the correction below):

1. **A Blaze Burner.** Create's recipe is a captured blaze (`create:blaze_burner_capturable` is the blaze alone). Without it nothing heated runs: steel, bronze, steam, the tower.
2. **Petrochem's tin.** It has no ore. A zinc nugget is filled with a thick potion by a spout, and a thick potion needs glowstone dust and a brewing stand (a blaze rod, again).

## Decision

**One recipe and one gift, each an addition, none replacing the original:**

| Item | Earth way | Why it is dearer than the original |
|---|---|---|
| `create:blaze_burner` | **Three are given** by the first quest of the Brass page (`brass.burner`, *Three burners*, a checkmark quest: one for the basin, two for the tower's base). No recipe | Create's own way is one captured blaze; here there are only the three, and a placed burner mined with a pickaxe drops itself whole, in every lit state, so they can be moved |
| `petrochem:tin_nugget` | Shapeless recipe (`ascension_compat_create`, `recipe/tin_nugget_by_hand.json`), only when Petrochem is loaded: two zinc nuggets and an iron nugget | Two zinc for one tin; the spout road costs a thick potion and zinc, 25 mB at a time |

The tin recipe has a recipe-book advancement beside it. Nothing of Create's or Petrochem's is removed or edited: a player who reaches the Nether still takes the faster road (a captured blaze, polished
rose quartz, a thick potion), and the Nether still pays what only it can: more burners, blaze cake and superheated heat, netherite, and quartz.

**The essence of the mod is kept** (Sanchit's rule for this work): Petrochem's recipes, machines and fuels are untouched, and every fuel keeps the job the mod gave it. See
`docs/progression/oil-and-fuels.md` for what each one is for.

### Correction: the Blaze Burner is a gift, not a recipe (Sanchit, 2026-10-25)

The first version also gave a burner by hand (a lava bucket over a magma block, iron sheets round it). Sanchit: a burner cannot be crafted overground, as there is no Nether. The audit below showed that
everything ship-critical traces to that one item, so the choice was between four ways (`docs/progression/oil-and-fuels.md` section 5): a heater block of our own (A), gifted burners (B), the recipe as it
was (C), or oil after the Nether (D). He picked **B**. The recipe and its advancement are removed. Three because the basin needs one for steel, bronze and brass, and Petrochem's tower sums the heat under
its base and needs 2 for the atmospheric and vacuum steps (a lit burner counts 1; flash mode needs none), so a moved burner would stop the mixer while the tower ran. Option A stays the answer for a player who
loses a burner (lava, a creeper) and is the likely alpha.4 job if the gift proves too thin.

### Correction: the electron tube (Sanchit, 2026-10-25)

The first version of this ADR also gave an electron tube by hand (an amethyst shard, four redstone and an iron sheet), because Create's tube wants polished rose quartz and quartz was taken to be a Nether
item. It is not: **Create itself makes quartz on Earth**, by a crushing wheel on diorite (a quarter of the time), a fan's water over crushed raw gold (half the time) or over soul sand, and the book's fan page
gives the first block of soul sand, from which a fan's soul fire makes more out of sand (Create's haunting). That route is Create's own design and the soul sand reward was added for it, so the hand recipe was
removed and the Brass page's tube quest now teaches the quartz routes. `tools/audit-nether-gating.py` reads every recipe in the jars and agrees: the tube is makeable from Earth with none of our recipes.

## What the audit found (the same day)

`python tools/audit-nether-gating.py --without-patches --without-gifts` reads the recipes of Create, Aeronautics, Aeroworks, Propulsion, Crafts & Additions and Petrochem and finds **105 items that cannot be made from Earth.
All but seven trace back to one root, the Blaze Burner** (brass ingots and casings and everything built on them, Petrochem's steel and bronze and everything built on those, and Crafts & Additions'
biomass); the seven are the Blaze Cake with its base and cinder flour, the empty burner (all netherrack) and the netherite diving gear (removed from the pack). Nothing else the ship needs is Nether-gated: Aeroworks' gyroscope and control stand,
Propulsion's thrusters and tanks, Crafts & Additions' motor and accumulator, our air machines and Aeronautics' own parts are all makeable on Earth once there is brass. Hand-crafted brass (alpha.3, F3) lifts the
brass half; steel, bronze, biomass and the distillation tower still want heat. The tower asks Create's boiler-heat API for a total of 2 or more under its base for the atmospheric and vacuum steps (a kindled Blaze
Burner gives 1, a seething one 2; lava, fire and magma give 0), and its code asks for the heat only outside flash mode, the first step.

## What was proved, and what was not

On a dedicated server (`create-checks`, 66 checks, 0 failures; `checkNoBurnerByHand` and `checkEarthRecipes`): the tin recipe loads and makes Petrochem's tin nugget; no burner recipe or advancement of ours exists
and Create's empty-burner recipe is untouched; a smouldering, fading, kindled or seething burner mined with a pickaxe drops `create:blaze_burner`, the filled one (so the three given are never lost to a pickaxe);
and Create's four quartz routes (diorite, crushed raw gold, soul sand, haunting sand) are loaded. The quest tooling (50 tests, and the build's `--check`) proves the Brass quest exists and gives the item. The audit
(`tools/audit-nether-gating.py`) counts the gift and, with it, leaves seven items Nether-only (the Blaze Cake and its base and cinder flour, the empty burner, the netherite diving gear).
**Not proved:** a gifted burner heating a basin or the tower in a client (two lit burners under the tower's base are read from Petrochem's code, not watched), a bronze plate from hand-made tin, the checkmark quest
paying out. These are listed with the other unseen items in `plans/alpha-3.md`.

## Consequences

- ADR-0031's first consequence is amended: oil is an Earth road. The refinery needs iron, coal, copper, zinc and iron nuggets, the three burners the book gives, and no blaze. The Moon can be
  reached on coal, charcoal or helium, or on kerosene made on Earth.
- The quest book places **Crude** (steel, bronze, pumpjack, electrolyzer, tower, kerosene) before **Wings** (the first ship's controls and thrusters). The burner and the electron tube are quests of
  **Brass** (`pack/quests/brass.py`; *Three burners* is the page's first quest and gives them, the tube quest teaches Create's own quartz), and Crude's Bronze quest tells the tin recipe (`crude.py`). Nothing in the book gates a path, so a player who prefers the Nether road loses nothing.
- The Create jar has one recipe for this, the tin one, which carries a Petrochem condition. No Tier-1 module is touched.
- A burner lost for good (lava, a creeper) cannot be replaced before the Nether. That is the cost of B, and the reason option A is kept in reserve.
- If Create or Petrochem ever rename these items, the server check fails first (it loads the recipe and resolves its result).
- The audit tool counts only the burner as a quest gift and warns if no quest gives it any more, so the audit cannot quietly go on trusting a gift that was removed.
