# Oil and the fuels it makes (2026-10-25)

> **Status: staged and checked on a dedicated server, never run in a client.** The mod is Create: Petrochem 1.3.3 ([ADR-0031](../decisions/0031-crude-oil-and-the-refinery-are-create-petrochem.md)).
> The numbers below are read from the mod's own recipes in its jar and from our five Thruster fuel files; the yields per bucket are arithmetic on those recipes
> (`scratchpad/dump_petrochem.py` printed them), not something watched in a game. Sanchit asked how the mod's fuels differ and that the mod keep its own
> character ("if the mod has it for something we should keep that mod's essence"). Both are answered here.

## 1. Where the oil is

Oil is a **pumpjack** job, and the pumpjack's recipes are keyed to **biomes**, so oil exists only where a vanilla biome says so. Nothing of ours decides it.

| Biome the pumpjack stands in | Gives, per cycle | Needs desalting? |
|---|---|---|
| Desert | 80 mB petroleum | yes |
| Deep ocean (any) | 95 mB petroleum | yes |
| Jungle (any) | 100 mB desalted oil | no |
| Swamp | 110 mB desalted oil | no |

The Moon, the Nether and the End have none of these biomes, so they yield nothing. **Jungle and swamp oil skips the electrolyzer**, which is the only machine in the
chain that takes electricity; a base in a swamp can run the whole refinery on rotation and heat.

## 2. The chain, and what a bucket becomes

Petrochem's base recipes (the `petrochem_expert` add-on, which is not in the pack, would replace them with a longer chain). Amounts are millibuckets (mB).

| Step | Machine | In | Out |
|---|---|---|---|
| Desalt | Electrolyzer (power: the recipe defaults to 100 energy) | 500 petroleum | 500 desalted oil, and salt dust half the time |
| Flash | Distillation tower, flash mode | 1,200 desalted oil | 1,000 oil, 200 light naphtha, 200 LPG |
| Atmospheric | tower | 2,000 oil | 800 oil residue, 400 diesel, 300 kerosene, 500 heavy naphtha |
| Vacuum | tower | 800 oil residue | 200 heavy oil residue, 300 heavy gas oil, 300 fuel oil |
| Gasoline | heated mixer | 300 heavy gas oil and 500 heavy naphtha | 800 gasoline |
| Coking | compacting | 600 heavy oil residue | 2 petroleum coke and 400 fuel oil |

So **one bucket (1,000 mB) of desalted oil becomes**, if every cut is taken as far as it goes:

| Product | mB (or items) from a bucket | Share |
|---|---|---|
| Gasoline | 333 | the largest |
| Fuel oil | 181 (125 from the vacuum cut, 56 from coking) | |
| Diesel | 167 | |
| LPG | 167 | |
| Light naphtha | 167 (becomes plastic, 1 for 1) | |
| Kerosene | 125 | **the smallest** |
| Petroleum coke | 0.28 item | |

The heavy gas oil and heavy naphtha that the gasoline step wants come out of the tower in exactly the ratio it needs (0.417 of a batch each), so nothing is left over.
Kerosene is only the thin middle cut of the atmospheric step: **a bucket of kerosene costs 8 buckets of desalted oil**, a bucket of gasoline 3, diesel 6, LPG 6.

## 3. What each fuel is for in Petrochem itself (the mod's essence, kept)

We add a Thruster use to each fuel and **remove nothing**: every fuel keeps the job the mod gave it, because the Thruster data is an addition and Petrochem's recipes are untouched.

| Fuel | Petrochem's own use (burn time in its engine recipe; higher lasts longer per mB) | Other jobs in the mod |
|---|---|---|
| **Diesel** | Diesel engine, burn 5 | none |
| **Fuel oil** | Diesel engine, burn 3 | Mixed with LPG into asphalt and lubricant; with petroleum coke into Blaze Candy; coking turns the heavy residue into more of it |
| **Raw petroleum** | Diesel engine, burn 1 (crude burns, badly) | Desalting |
| **Gasoline** | Gasoline engine, burn 12 | none |
| **Kerosene** | Gasoline engine, **burn 15** (it lasts longest there) | none |
| **LPG** | Turbine, burn 10 | Mixed into asphalt and lubricant |
| **Light naphtha** | none | Plastic |
| **Petroleum coke** | none | A steel ingredient (iron and coke), and Blaze Candy |

The engines and the turbine give the *power* side of the oil economy; the Thruster is a second way to burn the same fluids, which is why none of them is taken away.

## 4. The Thruster numbers (ours, `data/createpropulsion/thruster_fuels/petrochem/`)

Create Propulsion's liquid Thruster reads a **thrust** multiplier and a **burn** multiplier for each fluid. Its own baseline is turpentine at 1.0 and 1.0, and for diesel and
gasoline it uses 1.1 / 0.85 and 1.2 / 0.9 for every mod that makes them (Diesel Generators, Immersive Petroleum, TFMG, Stellaris). Ours match those two. **Kerosene is our own pick**
(Propulsion's table has a kerosene only for TFMG, at 1.05 / 0.9), and so are LPG and fuel oil.

| Fuel | Thrust | Burn | Thrust per burn (higher is better) | A tank lasts, against turpentine | Buckets from one bucket of crude | Thrust-seconds from one bucket of crude |
|---|---|---|---|---|---|---|
| Gasoline | 1.20 | 0.90 | 1.33 | 1.11 times | 0.333 | **444** |
| Diesel | 1.10 | 0.85 | 1.29 | 1.18 times | 0.167 | 216 |
| Kerosene | 1.15 | 0.80 | **1.44** | **1.25 times** | 0.125 | 180 |
| LPG | 1.00 | 0.95 | 1.05 | 1.05 times | 0.167 | 175 |
| Fuel oil | 0.90 | 1.00 | 0.90 | 1.00 times | 0.181 | 163 |

(Thrust-seconds is thrust times the fuel's yield divided by its burn, in the Thruster's own units: a bucket of turpentine would be 1,000.)

### How to read it

- **Per bucket of fuel in the tank, kerosene is best**: the strongest burn saving of the five (0.8), and a tank of it lasts a quarter longer than turpentine. That is the sense in which it is the climb fuel.
- **Per bucket of oil pumped, gasoline wins by a wide margin**: it is the fuel with the most of the barrel in it, and its thrust is the highest (1.20). Two and a half times as much gasoline thrust comes out of a barrel as kerosene thrust.
- **Diesel is the middle fuel**: second in thrust from a barrel (216) and second in how long a tank lasts (1.18 times), at 6 buckets of oil to the bucket.
- **LPG and fuel oil are the leftovers.** They work, but LPG is only for a ship that has nothing better, and fuel oil is weaker than the baseline.
- **So today the choice is mostly gasoline**, with kerosene better only when the tank, not the oil, is the limit (a small ship that must climb a long way on one filling). Kerosene beats gasoline by only
  8% on thrust per burn, while costing 2.7 times as much oil. That is a flatter choice than the name "rocket fuel" suggests.

### Open: should kerosene be clearly the rocket fuel?

If it should (the name and its place in Petrochem's gasoline engine both point that way), the data is one file: for example kerosene 1.3 / 0.7 (thrust per burn 1.86) would make it
worth its eight buckets, while gasoline stays the cheap, plentiful fuel for short hops. This is Sanchit's call; the numbers above are the first pass. A different tune changes five small JSON files and nothing else.

## 5. The way in without the Nether (2026-10-25, [ADR-0032](../decisions/0032-oil-and-the-first-ship-without-the-nether.md))

Sanchit: the first ship part needs oil, so oil must come early. The chain hides two Nether inputs. (A third, the electron tube, is not one: Create makes quartz on Earth by crushing
diorite, by washing crushed raw gold, and by washing soul sand, and the book's fan page gives the first soul sand. An Earth tube recipe was added and removed the same day, at Sanchit's word.)

| The chain needs | It is made from | What Earth has |
|---|---|---|
| A **Blaze Burner** (steel, bronze and the tower are all heated) | a captured blaze | **Three, given by the Brass page** (`brass.burner`); no recipe makes one |
| **Tin** (bronze needs a tin nugget) | a thick potion (glowstone, a brewing stand) | An Earth recipe (`ascension_compat_create`): two zinc nuggets and an iron nugget |

Tin is dearer than the original road, on purpose. The burners are finite, but a placed burner mined with a pickaxe comes back whole (Create's loot table drops the filled burner in every lit state; `create-checks` proves it),
so three can be moved from the basin to the tower. The Nether still makes more (an empty burner used on a blaze), and gives a blaze cake and superheated heat, and netherite.

### Decided: option B, three burners as a gift (Sanchit, 2026-10-25)

Sanchit said a Blaze Burner cannot be crafted overground, as there is no Nether, and then picked B: "gift three Blaze Burners". The hand recipe from the first alpha.3 build is removed. The Brass page's first
quest, *Three burners*, gives them (a checkmark quest, so a player who already has a captured-blaze burner can still take them), and the Spin, Brass and Crude pages say where they are. Why three: one under the
basin for steel, bronze and brass, and two under the tower's base, which needs a heat total of 2 for its atmospheric and vacuum steps (flash, the first step, needs none). A burner moved from the basin to the
tower works too, but then the mixer stops, so three keeps both lines running. The options as they were put to him:

`tools/audit-nether-gating.py` shows that without any recipe of ours **all but seven of the 105 blocked items of the ship and oil chain trace to the Blaze Burner**: brass, steel, bronze, biomass and what is
built from them (the seven are the Blaze Cake, its base, cinder flour, the empty burner and the netherite diving gear). Hand-crafted brass (F3) already lifts the brass half. What still wants heat is Petrochem's steel and bronze (heated mixing), Crafts & Additions' biomass, and the distillation tower, whose
atmospheric and vacuum steps need a boiler-heat total of 2 under its base (a fuelled Blaze Burner gives 1, a seething one 2; lava, fire and magma give 0, so a lava floor cannot do it). The choices:

| | What it is | Cost | Leaves |
|---|---|---|---|
| **A. An Earth heater of our own** | A coal-fired "firebox" block that gives Create's basin and the tower the same heat a kindled burner does | A new block: Java, a model and textures drawn by our scripts, a recipe; checked on a server, looked at in a client | No burner on Earth, no Nether needed, the Blaze Burner stays the Nether's upgrade |
| **B. Gift the burners (chosen)** | The Brass page hands out three Blaze Burners, the way the fan quest hands out the first soul sand (the Brass page, not the oil page, because brass is heated too and comes first) | Rewards only; no recipe | Burners cannot be made on Earth, only the three given; one lost to lava or a creeper cannot be replaced before the Nether |
| **C. As first built** | The hand-made burner recipe (lava bucket, magma block, iron sheets) | None | A burner crafted on Earth. Removed |
| **D. Oil after the Nether** | Remove the hand recipe; steel and bronze keep their heated recipes | None | The first ship burns coal or Propulsion's turpentine; kerosene waits for a captured blaze |

Option A (a coal-fired heater block of our own) stays the better answer for a player who loses a burner, and is the natural alpha.4 job if the gift turns out too thin in play.
`tools/audit-nether-gating.py` now counts the gift (the burner is the only Nether item it counts, and it checks that a quest still gives it): with it, seven items of the six mods are still Nether-only, and
they are the Blaze Cake with its base and cinder flour, the empty burner and the netherite diving gear.

## 6. What is not proved

A pumpjack, an electrolyzer and a distillation tower working in a client, kerosene running a Thruster, the gifted Blaze Burners heating a basin and the tower in play (two lit under the tower's base), and Petrochem's
machines beside Sable. The server run (`create-checks`, 66 checks, 0 failures) proves the fluids, the recipes, the fuel table and the tin recipe load, that no burner recipe of ours exists and that a mined burner gives itself back;
it does not prove they work.
