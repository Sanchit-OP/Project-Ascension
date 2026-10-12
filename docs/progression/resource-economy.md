# The resource economy

> **Draft for discussion, 2026-10-12. Nothing here is decided.** Sanchit stepped back from
> Planet 3 to look at materials as a whole: what each one is *for*, how loot is managed, how a
> player finds out where things come from, and how the combat items are ever obtained. This page
> audits what exists, names what is broken, and proposes a shape. Once decided, the answers go to
> an ADR. It also stands in for the "resource ledger" `todo.md` lists as missing.

## 1. What exists today

### Materials we added

| Material | Where it comes from | What uses it |
|---|---|---|
| Raw titanium | Titanium ore on **Earth**, y −64 to 0; Lunar Archive stores (2 to 5) | **Nothing.** No ingot, no smelting recipe. |
| Raw helium crystal | Lunar helium ore on **the Moon**, y −64 to 24; Archive stores (1 to 4) | **Nothing.** |
| Warden tendril | The Warden (`add_table` modifier) | The gateway keystone (Earth's gate) |
| Gateway key, Moon | The Ender Guardian in the Moon's citadel, **only with compat-cataclysm** | Attunes the Moon's gate |
| Gateway key, Nether | The Netherite Monstrosity, **only with compat-cataclysm** | Attunes the Nether's gate |
| Forge Record | The Netherite Monstrosity (same jar) | Lore |
| Inscribed gatestone | Crafted (polished deepslate or Moon basalt, with sculk or an echo shard); Archive stores | Gate frames |
| Archive tablets | Archive and colony stores, plinths | Lore |
| Moon stone (anorthosite and basalt families) | The Moon's terrain | Building |

Vanilla materials already doing a job for us: echo shards, sculk catalysts and diamonds (the
keystone); iron and glass (the oxygen tank, emitter and refill station); gold (the Nether's ward);
netherite (vanilla's own top tier).

### Items with no source at all

**All 26 combat items** (11 weapons, 15 armour and off-hand pieces, `combat-api.md` §6) exist only
in the creative tab: no recipe, no loot, no drop.

### What a player can see

JEI is installed and shows every crafting recipe, because recipes are sent to the client. It shows
nothing about where a raw material comes from, because loot tables, mob drops and ore placement
stay on the server. `ascension-compat-jei` has been on the architecture map since ADR-0003 and was
never started.

## 2. What is broken

Most blocking first.

### 2.1 The road to space is closed in survival

Checked in the installed jars on 2026-10-12 (Create 6.0.10, Create Aeronautics 1.3.2):

- In space, balloons do nothing and **only propellers push a ship** (ADR-0015).
- Every Aeronautics propeller needs **brass casing**: `propeller_bearing`, `smart_propeller` and
  `gyroscopic_propeller_bearing`. So do the `navigation_table` and most of Aeronautics' controls.
- Create makes brass in **one** way: copper and zinc in a *heated* mixer. The only heat source is a
  Blaze Burner. Its casing is made with netherrack, and its fire is a captured blaze. **Both are
  Nether-only, and the Nether is Planet 3** (ADR-0014, ADR-0017).
- So a survival player can build a hot-air balloon from Earth materials: wool envelopes, and an
  adjustable burner made of iron plates, andesite alloy, redstone and a coal block. It may climb to
  space, but once there it has nothing to push it to the Moon. **The Moon is unreachable, and so is
  everything after it.** The live test on 2026-10-11 used a ship built by command, which is why
  nobody hit this.
- Untested: whether anything else can push a ship in space, such as Aeronautics' steam vents or
  Simulated's sails.
- Brass also gates most of Create's middle tier: deployers, mechanical arms, mechanical crafters,
  sequenced assembly. **Earth's Create is andesite and copper only.**

This also decides the starter ship (decision 5 in `plans/m3-vertical-slice.md`): there is nothing to
design until a propeller can be made on Earth.

### 2.2 The Moon can be skipped

Planet 3 is 18,000 blocks from Earth. At 60 b/s that is 300 s, against 320 s of air in one tank, and
a player may carry two (ADR-0009). Nothing from the Moon is needed to reach Planet 3 or survive
there. That breaks the vision's "planets cannot be skipped in the main progression route".

**Range is a soft gate**: carry another tank, build a faster ship. It may be no gate at all. If an
Oxygen Emitter keeps a sealed cabin on a ship breathable in space, air stops limiting range
entirely. **Untested.**

### 2.3 Titanium and helium have no job

They are mined and looted, and they do nothing. The lore has already promised them jobs
(`docs/lore/the-moon.md`):
- **Titanium:** "the ancients' rocket metal… You will build your next ship from it."
- **Helium:** the ancients' fuel. By helium light, things touched by the Observers show what they
  really are.

### 2.4 The combat items cannot be obtained

None of the 26 can be made, found or dropped.

### 2.5 Nothing says where anything comes from

There is no ledger for us, and nothing in JEI for players.

### 2.6 The gateway keys exist only with Cataclysm installed

`ascension-worlds` on its own has no way to light the Moon's or the Nether's gate. That is fine for
our pack. It is a gap for anyone using the module on its own, which is ADR-0003's first priority.

## 3. Proposed principles

1. **A material has a job before the next material is added.** At least one use on the critical
   path, and ideally one optional use. A material with sources and no uses is a bug.
2. **Each world's material pays for the next world, plus one new verb.** It buys the part that gets
   you onward. It also buys one new thing to *do*, never just bigger numbers. That second part is
   the tech tree's "new capabilities, not stronger numbers".
3. **The onward gate is hard, not soft.** It is a thing you cannot do without the part: land, cross,
   survive. More air and more speed are soft gates, because a player can always carry more, and
   §2.2 shows they leak.
4. **Materials cross module lines as `c:` tags, never as item ids.** Suppose a recipe in
   `ascension-atmosphere` asks for `#c:ingots/titanium`:
   - It loads whether `ascension-worlds` is installed or not, and is just uncraftable when it isn't.
   - Another mod's titanium works in it.
   That is ADR-0003's connector idea applied to data, and it keeps Tier 1 modules apart (ADR-0011).
5. **Loot is added to, never replaced.**
   - Our own structures get our own tables.
   - Everyone else's tables get `neoforge:add_table` modifiers, as the Warden and the Monstrosity
     already do.
   - The one existing exception, compat-cataclysm's replacement of the Ender Guardian's table,
     gets a written reason.
6. **One ledger, generated, and checked by the build.**
   - A tool reads our recipes, loot tables, loot modifiers and ore placements, and writes the
     ledger.
   - A test fails when one of our items has no source, or one of our materials has no use.
   - Titanium would have failed that test on the day it was added.

## 4. A proposed shape for v1

Three worlds, each paying for the next. Every line is a proposal.

### Earth: titanium, the rocket metal

- Raw titanium smelts to **titanium ingots** in a blast furnace. Create's crusher gives its usual
  bonus.
- **Its job: the first ship that can cross space.** The recommendation is a second recipe for
  Aeronautics' own propeller bearing, with titanium where the brass casing was.
  - It is a data file, with no code and no new block, in the Aeronautics compat jar.
  - Only the plain propeller bearing gets the titanium recipe. Smart and gyroscopic propellers, the
    navigation table, and Create's whole brass tier stay a later world's reward.
  - The other way is **titanium brass**: an unheated mixing recipe of copper, zinc and titanium.
    That opens all of Create's middle tier on Earth, after a deep dig.
- Optional: the oxygen tank's shell moves from iron to `#c:ingots/titanium`, so the first trip to
  space asks for a deep dig on Earth.

### The Moon: helium, the fuel and the light

- **Its job: the part a ship needs to come down on Planet 3.** Call it a heat shield, for the
  burning world: titanium and helium.
  - **How the check works.** A planet's JSON names a block tag, and the descent from space only
    lands a ship that carries a block from it. Without one, the ship stays in space with a message.
  - **Data-driven.** One generic mechanism, so every later planet can ask for its own part with no
    new class. That is the familiar rocket-tier shape, done with our ships.
  - **It makes the Moon impossible to skip**, whatever the range.
- **Its verb: the helium lamp** (`the-moon.md`). By its light, hidden Sealer carvings show, and so
  does what the Observers touched. Planet 3's Sealer carving could carry a line only helium light
  reveals. That gives the Moon a second, softer reason to matter on Planet 3.
- Optional: a **helium burner**, a Create heat source, so brass opens at the Moon instead of at
  Planet 3.

### Planet 3: gold, blazes and netherite

- Already doing jobs: gold is the ward, and netherite is vanilla's top tier.
- **Blazes open Create's brass tier** (through the Blaze Burner), and the superheated tier with it,
  unless the helium burner opened brass at the Moon. That is the tech tree's "Planet 3 unlocks
  stronger production", for free.
- **The onward part for Planet 4** (not in v1) can wait for Planet 4's design. Netherite is the
  obvious candidate.

## 5. The combat items

The combat session builds this. This page only proposes where the items sit in the economy.

### How they are made

Three options:

- **(a) Crafting-table recipes.** The simplest, and JEI shows them for free.
- **(b) League-style build paths.** A few components are crafted first (a titanium edge, a helium
  core, a netherite core), then combined. Familiar to League players, but more items to make and
  draw.
- **(c) Smithing templates as Sealer records (recommended).**
  - At a smithing table: a vanilla piece of the same slot, plus that world's material, plus a
    template found on that world. For example, diamond boots, titanium and an Earth template make
    Ninja Tabi.
  - It is exactly how vanilla's netherite upgrade works, so players already know it.
  - `the-nether.md` already calls the netherite template "a Sealer record in pictures".
  - Templates in the Archive's and the colony halls' stores give exploration a gear reward.
  - Like vanilla, a template copies itself with diamonds and that world's stone.

### Which world each item belongs to

A first cut to argue with: eight per world, plus two that only bosses drop.

| World | Items |
|---|---|
| **Earth** | All six boots: Ninja Tabi, Mercury Treads, Swifty, Berserker's Greaves, Wizard's Agony, Tasted Blood. Darius's Axe. Mortal Reminder. |
| **Moon** | Veigo's Blade, Wraithblade, Spear of Shojin, Galeforce, Heartwood of Pallas, Edge of Night, Gargoyle Stoneplate, Rocketbelt. |
| **Planet 3** | Darken Blade, Ragefang, Endless Hunger, Divine Sunderer, The Protean Mask, Collector's Pants, Unending Despair, Sterak's Harness. |
| **Boss drops only** | Guardian's Wing (revive) and Zonia's Hourglass (stasis): the two that cheat death. The vision says "bosses unlock capability"; these are capability. |

The anti-tank family lands one or two to a world: Darius's Axe and Mortal Reminder on Earth, Veigo's
Blade on the Moon, Divine Sunderer on Planet 3. The family stays a thread through the whole game.

## 6. Managing loot

| Kind of source | Where it lives |
|---|---|
| Ores | Planet worldgen for our planets. A biome modifier for Earth, which is vanilla's dimension. |
| Our structures' chests | Our own tables (`archive/stores`, `nether_colony/stores`) |
| Mob and boss drops | `add_table` modifiers. A boss's drop lives in that boss's compat jar. |
| Vanilla chests | `add_table` modifiers on vanilla's tables. For example: Earth templates in ancient cities and trial vaults, titanium nuggets in ruined portals. |

**The ledger tool:** `tools/resource-ledger.py`. It reads every module's data and writes two things:
- `docs/progression/resource-ledger.md`, for us: every item, every source, every use.
- A sources file for JEI, for players (§7).

The test from principle 6 runs over the same data.

## 7. JEI: where things come from

- **Recipes already show**, for free.
- **Our own `ascension-compat-jei` (recommended).** A "Where it's found" page for each of our items:
  - **Mined:** which planet, what depth, what pickaxe.
  - **Dropped:** by what, at what chance, on which planet.
  - **Found:** in which ruin's chest, on which planet.

  It also adds info pages for things that are not recipes: a key attunes a gate, gold wards off
  the rot. It reads the ledger's sources file, shipped inside the jar, so it works on a dedicated
  server, where loot tables never reach the client.
- **Just Enough Resources** (a third-party JEI add-on, with a NeoForge 1.21.1 build listed) does
  the same for vanilla and other mods: mob drops, chest loot, ore heights.
  - It knows nothing about planets: it would place titanium at y −64 to 0 without saying which
    world.
  - It is an optional pack addition next to ours. Its behaviour on a dedicated server needs
    checking first.

## 8. Decisions for Sanchit

1. **How the first ship crosses space on Earth materials:** a titanium recipe for the propeller
   bearing (recommended), or titanium brass?
2. **What makes the Moon necessary:** a part a ship must carry to land on Planet 3, from helium
   (recommended, and data-driven for every later planet)? Or retuning distances, or accepting that
   the Moon can be skipped in v1?
3. **Where Create's brass tier opens:** at Planet 3 with blazes (as now), or at the Moon with a
   helium burner?
4. **How the combat items are made:** smithing templates (recommended), crafting recipes, or build
   paths?
5. **The item-to-world table in §5**, and the two items only bosses drop: agree, or move things?
6. **Keys without Cataclysm:** fine as pack-only, or should Tier 1 have a fallback?
7. **Just Enough Resources** in the pack, as well as our own JEI pages?

## 9. Order of work, once decided

1. **The road to space** (decision 1). It blocks every survival playthrough, and the starter ship
   waits on it.
2. **The ledger tool and its test.** Every gap on this page becomes a failing test.
3. **Titanium's and helium's jobs**, including the landing check if decision 2 goes that way.
4. **`ascension-compat-jei`'s sources pages.**
5. **The combat items' recipes and templates**, with the combat session.
6. **A refactor pass** (ADR-0008).

Planet 3's one client test is separate and can come before or after this, as Sanchit prefers. It
carries a ship to the Nether by command, so §2.1 does not block it.
