# The resource economy

> **Decided 2026-10-12, except the build paths' details (§5).** Sanchit stepped back from Planet 3 to look at materials as
> a whole: what each one is *for*, how loot is managed, how a player finds out where things come
> from, and how the combat items are ever obtained. The first draft's seven questions were answered
> the same day (§8). The biggest answer went to its own ADR: **no world is ever required**
> ([ADR-0018](../decisions/0018-no-world-is-required.md)). Still open: the details of the combat
> items' build paths (§5). This page also stands in for the "resource ledger" `todo.md` lists as
> missing.

## 1. What exists today

### Materials we added

| Material | Where it comes from | What uses it |
|---|---|---|
| Raw titanium | Titanium ore on **Earth**, y −64 to 0; Lunar Archive stores (2 to 5) | **Nothing.** No ingot, no smelting recipe. |
| Raw helium crystal | Lunar helium ore on **the Moon**, y −64 to 24; Archive stores (1 to 4) | **Nothing.** |
| Warden tendril | The Warden (`add_table` modifier) | The gateway keystone (Earth's gate) |
| Gateway key, Moon | The Ender Guardian in the Moon's citadel (compat-cataclysm) | Attunes the Moon's gate |
| Gateway key, Nether | The Netherite Monstrosity (compat-cataclysm) | Attunes the Nether's gate |
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
stay on the server.

## 2. What is broken

### 2.1 The road to space is closed in survival

Checked in the installed jars on 2026-10-12 (Create 6.0.10, Create Aeronautics 1.3.2):

- In space, balloons do nothing and **only propellers push a ship** (ADR-0015).
- Every Aeronautics propeller needs **brass**.
- Create makes brass in **one** way: copper and zinc in a *heated* mixer. The only heat comes from
  a Blaze Burner: a captured blaze in a casing made with netherrack. **Both are Nether-only.**
- So a survival player can build a hot-air balloon from Earth materials, and may climb to space
  with it. Once there, nothing pushes it on to the Moon. The live test on 2026-10-11 used a ship
  built by command, which is why nobody hit this.
- What else brass and the Nether hold back in Create and Aeronautics is listed in Appendix A.

**Built 2026-10-12:** a titanium propeller bearing, from Earth (§4).

### 2.2 The Moon could be skipped

Planet 3 is within one iron tank of Earth, so nothing from the Moon is needed to reach it. The first
draft proposed a hard gate to fix that. **Decided the other way** (ADR-0018): no world is
required, so this is not a problem.

### 2.3 Titanium and helium have no job

They are mined and looted, and they do nothing. The lore has already promised them jobs
(`docs/lore/the-moon.md`): titanium is "the ancients' rocket metal… You will build your next ship
from it"; helium is the ancients' fuel, and by its light, things touched by the Observers show what
they really are.

### 2.4 The combat items cannot be obtained

None of the 26 can be made, found or dropped. **Decided:** League-style build paths (§5).

### 2.5 Nothing says where anything comes from

There is no ledger for us, and nothing in JEI for players. **Decided:** add Just Enough Resources,
and build our own JEI pages for what it cannot know (§7).

### 2.6 The gateway keys exist only with Cataclysm installed

**Decided:** keep it that way. Cataclysm is part of the pack, and the keys stay in its compat jar.

## 3. Principles

1. **A material has a job before the next material is added.** A material with sources and no uses
   is a bug.
2. **Materials reward; they never lock** (ADR-0018). A world's materials make a player stronger or
   their machines better. The best gear asks for materials from several worlds, so going to many is
   worth it, but no world is required for another.
3. **Mod tiers become upgrades, not walls.** Brass is the propeller upgrade, not the price of
   leaving Earth.
4. **Materials cross module lines as `c:` tags, never as item ids.** Suppose a recipe in
   `ascension-combat` asks for `#c:ingots/titanium`:
   - It loads whether `ascension-worlds` is installed or not, and is just uncraftable when it isn't.
   - Another mod's titanium works in it.
   That is ADR-0003's connector idea applied to data, and it keeps Tier 1 modules apart (ADR-0011).
5. **Loot is added to, never replaced.**
   - Our own structures get our own tables.
   - Everyone else's tables get `neoforge:add_table` modifiers, as the Warden and the Monstrosity
     already do.
   - The one existing exception, compat-cataclysm's replacement of the Ender Guardian's table,
     gets a written reason.
6. **One ledger, generated, and checked by the build. Built 2026-10-12** (§6).
   - A tool reads our recipes, loot tables, loot modifiers and ore placements, and writes the
     ledger.
   - The build fails when one of our items has no source, or one of our materials has no use.
   - Titanium would have failed that check on the day it was added.

## 4. Each world's materials

### Earth: titanium, the rocket metal

- **Built 2026-10-12, checked on a dedicated server, not yet seen in a client:**
  - **Titanium ingots.** Raw titanium or the ore smelts to an ingot in a furnace, or twice as fast
    in a blast furnace. The ingot carries `#c:ingots/titanium`, the raw chunk
    `#c:raw_materials/titanium`, and the ore `#c:ores/titanium`.
  - **The titanium propeller bearing.** A second recipe for Aeronautics' own plain propeller
    bearing: three titanium ingots round an iron plate, a wooden slab on top. The original uses
    brass casing.
    - It lives in `ascension-compat-sable`, loads only when Aeronautics is installed, and asks for
      `#c:ingots/titanium`.
    - It is data only, with no code. A first ship can now cross space from Earth.
  - Create has no crushed titanium, so no ore-doubling for it yet.
- **Brass as the propeller upgrade (decided).**
  - **How it works now (proposed):** the plain bearing stays one block, and the upgrades are
    Aeronautics' own brass propellers, the smart propeller and the gyroscopic propeller bearing.
    So are the brass controls: the throttle lever and the navigation table. No code against
    Aeronautics' classes.
  - **The other way:** a weaker titanium bearing as its own block, so even the plain brass bearing
    is an upgrade. That means a block of our own built on Aeronautics' classes, which is more
    fragile across their updates.
- **In the combat build paths:** titanium is Earth's metal for the physical-damage components
  (§5).

### The Moon: helium, the fuel and the light

- No ship part (ADR-0018). Its jobs:
  - **In the combat build paths:** helium is the Moon's material for the magic-damage and energy
    components (§5).
  - **The helium lamp** (`the-moon.md`): by its light, hidden Sealer carvings show, and so does
    what the Observers touched. It is a new thing to do, not a number. Planet 3's Sealer carving
    could carry a line only helium light reveals.
  - **Optional, later:** a helium burner, a Create heat source, would open brass without the
    Nether. Not needed now that the titanium bearing gets ships into space.

### Planet 3: gold, blazes and netherite

- **Already doing jobs:** gold is the ward, and netherite is vanilla's top tier.
- **Blazes open Create's brass tier** through the Blaze Burner, and the superheated tier with it.
  That covers the propeller upgrades, deployers, mechanical crafters, crushing wheels and trains
  (Appendix A).
- **Quartz** opens electron tubes, observers and comparators.
- **In the combat build paths:** blaze powder, magma cream, ghast tears and netherite are Planet 3's
  materials (§5).

## 5. The combat items: League-style build paths

**Decided (Sanchit, 2026-10-12):**
- League-style build paths.
- Materials from different places.
- One weapon is very hard to make.
- A boss drop is needed to finish an item, so every extra item means more boss kills.

The combat module is the parallel combat session's. This section is the contract for it, the way
the art contracts work. Everything below the decided lines is a proposal.

### Three tiers, as in League

| Tier | What it is | Made from |
|---|---|---|
| **Basic** | League's starter components: Long Sword, Dagger, Cloth Armor, Null-Magic Mantle, Ruby Crystal, Amplifying Tome, Boots | One world's refined material, in quantity. For example, a Long Sword from titanium ingots (Earth) or an Amplifying Tome from helium (the Moon). |
| **Epic** | League's middle components: Pickaxe, B.F. Sword, Recurve Bow, Chain Vest, Negatron Cloak, Phage, Caulfield's Warhammer, and so on | Two or three basics, plus a rarer material from **a different world** than its basics. So every epic touches at least two worlds. |
| **Legendary** | The 26 items in `combat-api.md` §6 | Two or three epics. The strong ones, above all those that heal, also need **a boss's heart** (below). |

An illustrative path, not a final recipe. **Darius's Axe** needs three things:
- **Phage:** a Long Sword (Earth titanium) and a Ruby Crystal, with blaze powder from Planet 3.
- **Caulfield's Warhammer:** two Long Swords, with helium from the Moon.
- **The Warden's heart.**

So it takes Earth, the Moon, Planet 3 and a boss.

### The boss drop

**Decided (Sanchit, 2026-10-12):**
- **Boots never need a heart.** They are the cheap first item, as in League.
- **Hearts go into the strong items**, mostly the ones that heal:
  - The healers: Divine Sunderer, Endless Hunger, Unending Despair and Sterak's Harness.
  - Tasted Blood heals too, but it is boots, so it needs no heart.
  - Proposed: also the two that cheat death, Guardian's Wing (revive) and Zonia's Hourglass
    (stasis).

**Decided (Sanchit, 2026-10-12): one heart per boss.** Every boss drops its own heart. The lore already gives the bosses netherite hearts: they are the
Sealers' keepers. Each legendary names the heart it needs.
- **Bosses:** the Warden, the Ender Golem, the Ender Guardian, the Netherite Monstrosity, Ignis and
  the Wither, and later worlds' bosses as they come.
- **Why one heart per boss:** a weapon then means hunting a particular boss, and Sanchit's "more
  grind" means fighting that boss again for every copy.
- **Turned down:** one shared boss material that every boss drops. Simpler, and any boss farm
  works, but it makes no difference which boss you fight.

### Rough cost of one legendary

About five to seven basics, and two or three rare materials from other worlds. Strong items also
take one heart, and the strongest two. That is two or three worlds per item, and a boss kill for
the strong ones.

### Balance and enchantments

**Sanchit, 2026-10-12:** the items need balancing, and they should feel strong. That fits the
cost: an item that takes three worlds and a boss has to be worth it.

Enchantments still need thinking through: how vanilla's enchantments stack with each item's stats
and procs. For example, Sharpness on Darius's Axe, Looting next to Collector's Pants, Mending on
Guardian's Wing. `docs/progression/enchantments.md` holds the open questions. That work belongs to
the combat session.

### Still open

- **Which strong items take a heart, and whose.** One per boss is decided.
- **Where items are crafted:** a crafting table to start, so JEI shows every step. A dedicated
  forge block could come later; Planet 5 was imagined as the forge world.
- **Who builds it:** the combat session, from this contract (proposed), or this session?

## 6. Managing loot

| Kind of source | Where it lives |
|---|---|
| Ores | Planet worldgen for our planets. A biome modifier for Earth, which is vanilla's dimension. |
| Our structures' chests | Our own tables (`archive/stores`, `nether_colony/stores`) |
| Mob and boss drops, including the hearts | `add_table` modifiers. A boss's drop lives in that boss's compat jar, or in ours for vanilla bosses. |
| Vanilla chests | `add_table` modifiers on vanilla's tables, where a component or a material should turn up |

**The ledger (built 2026-10-12).** `tools/resource-ledger.py` writes
[`resource-ledger.md`](resource-ledger.md): every item we register, where a player gets it, and what
uses it.

- **What it reads:** every module's own data. That is recipes, loot tables and loot modifiers, ore
  features and the biomes they are placed in, planet terrain, structure templates (followed from
  each structure's start pool through its jigsaw links), and gate ruin sites.
- **What the data cannot say** lives in `tools/resource-ledger-rules.json`: which items are
  equipment, which code consumes, the names of structures, and the known gaps with their reasons.
- **The build runs it with `--check`.** Every module's `check` task fails when:
  - an item has no source, or a material has no use, and the rules don't excuse it;
  - the rules excuse something that no longer needs it, so the gap list only shrinks;
  - a loot modifier isn't registered, so it would never run;
  - the ledger page is stale.
- **The JEI sources file** (§7) comes with `ascension-compat-jei`, from the same reading.

**What it found on its first run:**
- **Both ores dropped only their raw material,** with no Silk Touch drop. So the ore blocks could
  not be had at all, and the new ore-smelting recipes could never be used. Fixed: Silk Touch now
  drops the ore, as vanilla's ores do. There is still no Fortune bonus; that is a rarity call.
- **The Nether planet had no display name,** so the gate destination screen would have shown
  `planet.ascension_worlds.nether` once its gate was lit. It is "The Nether" now.
- **27 known gaps:**
  - the 26 combat items, waiting on the build paths;
  - raw helium, waiting on its jobs.
- **Five creative-only blocks, by design:**
  - the Archive's and the colony halls' unbreakable plinths;
  - the orrery;
  - the carved glyph;
  - the combat session's test blade.

## 7. JEI: where things come from

- **Recipes already show**, build paths included, as long as they are crafting recipes.
- **Just Enough Resources (decided).** A JEI add-on that shows mob drops, chest loot and ore
  heights for vanilla and other mods.
  - The file: `JustEnoughResources-NeoForge-1.21.1-1.6.0.12.jar` on CurseForge, by way2muchnoise.
    It is marked beta, and is the only 1.21.1 NeoForge build.
  - It knows nothing about planets: it would place titanium at y −64 to 0 without saying which
    world.
  - Its behaviour on a dedicated server needs checking once it is installed.
- **Our own `ascension-compat-jei`** fills in what JER cannot know.
  - **A "Where it's found" page** for each of our items: which planet, what depth, which boss, which
    ruin's chest.
  - **Info pages** for things that are not recipes: a key attunes a gate, gold wards off the rot.
  - **Its data:** the ledger's sources file, shipped inside the jar, so it works on a dedicated
    server.

## 8. Decisions

**Answered by Sanchit, 2026-10-12:**

1. **The first ship:** a titanium propeller bearing. Brass becomes the propeller upgrade.
2. **Making the Moon necessary:** no. No world is ever required, and players go where they choose
   (ADR-0018). Helium is not a ship part.
3. **Where brass opens:** with blazes on Planet 3, as an upgrade, not a wall.
4. **The combat items:** League-style build paths, materials from different places, very hard to
   make, with a boss drop needed. Boots never need a heart, and hearts go into the strong items,
   mostly the ones that heal. The items must feel strong, and their balance and how enchantments
   interact with them come next.
5. **The first draft's item-to-world table:** replaced by the build paths, where each item draws
   on several worlds.
6. **Keys without Cataclysm:** keep Cataclysm. The keys stay pack-only.
7. **Just Enough Resources:** add it. **Done:** Sanchit added 1.6.0.17. It loads on the dedicated
   server (2026-10-15); not yet seen in a client.

**Still open:**
- How brass upgrades propellers: Aeronautics' own brass propellers (proposed), or a weaker
  titanium bearing of our own (§4).
- The three build-path questions still open in §5.

## 9. Order of work

1. **Titanium ingots and the titanium propeller bearing. Built 2026-10-12** (§4). It unblocks every
   survival playthrough and the starter ship. It is seen in a client at the next client test.
2. **Install Just Enough Resources**, and check it on a dedicated server. **Done 2026-10-15.**
3. **The ledger tool and its check. Built 2026-10-12** (§6). It runs with every build.
4. **The build-path contract**, finished from §5 and handed to the combat session.
5. **`ascension-compat-jei`'s pages.**
6. **The helium lamp.**
7. **A refactor pass** (ADR-0008).

Planet 3's client test is separate, and can come before or after any of this.

## Appendix A: what brass and the Nether hold back

Worked out on 2026-10-12 from every recipe and item tag in vanilla 1.21.1, NeoForge, Create 6.0.10
and the three mods inside Create Aeronautics 1.3.2 (Aeronautics, Simulated, Offroad).
- **Blocked** means no recipe for it, or for anything it needs, avoids a Nether or End input.
- **Machines count.** A deployer step needs a deployer, and a deployer needs brass.
- **Not counted:** scrap outputs and chance-only outputs.

**Why so much is blocked.** Three things sit underneath almost everything below:
- **Brass:** only a heated mixer makes it, and only a blaze heats one.
- **Electron tubes:** made from rose quartz, which is made from Nether quartz.
- **The two big machines:** deployers and mechanical crafters need both of the above. So does
  everything made on them. That includes **crushing wheels**: without them, Earth has no
  ore-doubling.

Create can crush diorite into quartz, but only with crushing wheels, so that door is shut on Earth
too.

### Aeronautics, Simulated, Offroad

| Held back by | Items |
|---|---|
| Brass | `propeller_bearing` (getting a titanium recipe), `directional_linked_receiver`, `modulating_linked_receiver`, `redstone_inductor`, `throttle_lever` |
| Brass and quartz | `smart_propeller`, `gyroscopic_propeller_bearing`, `gyroscopic_mechanism`, `navigation_table`, `analog_transmission`, `docking_connector`, `gimbal_sensor`, `optical_sensor`, `linked_typewriter`, `plunger_launcher`, `redstone_accumulator`, `mounted_potato_cannon`, `rockcutting_wheel` (Offroad) |
| The End | `levitite_blend` and `end_stone_powder`: levitite, the floating stone, needs End stone and a heated mixer |

**What Earth can build for a ship:**
- **Lift:** hot-air balloons (the envelopes, and the adjustable burner of iron, andesite alloy,
  redstone and a coal block).
- **Steering and structure:** `steering_wheel`, `physics_assembler`, `swivel_bearing`.
- **Power:** `red_portable_engine`, `steam_vent`.
- **Sensors:** altitude, velocity and laser sensors.
- **Rigging:** rope winches and couplings, honey glue, the redstone magnet.
- **Ground vehicles:** wheels and tyres (Offroad).

### Create

| Held back by | Items |
|---|---|
| Brass | brass ingot, nugget, sheet, block and casing; brass hand; `display_link`, `elevator_pulley`, `flywheel`, `pulse_repeater`, `pulse_extender`, `pulse_timer`, `attribute_filter`, `placard`; brass bars, doors, ladders, scaffolding, table cloth |
| Brass and quartz | **`deployer`, `mechanical_arm`, `mechanical_crafter`, `crushing_wheel`** (and with it every crushed ore, `powdered_obsidian`, `sturdy_sheet`), `precision_mechanism`, brass funnel and tunnel, `smart_chute`, `smart_fluid_pipe`, `sequenced_gearshift`, `rotation_speed_controller`, `content_observer`, `stockpile_switch`, `clockwork_bearing`, `mechanical_roller`, `controls`, `extendo_grip`, `potato_cannon`, `wand_of_symmetry`, `factory_gauge`, **every train part** (`track`, `track_station`, `track_signal`, `track_observer`, `railway_casing`, `schedule`, train doors), `blaze_cake` |
| The Nether, without brass | `electron_tube` and the rose quartz family, `nixie_tube`, `display_board`, `contraption_controls`, `controller_rail`, `adjustable_chain_gearshift` (all quartz); `empty_blaze_burner` (netherrack); `chocolate` (heated); scoria; the netherite backtank and diving gear |

**What Earth's Create still has:**
- **Power and transmission:** shafts, cogs, gearboxes, clutches and gearshifts; water wheels,
  windmills, steam engines.
- **Processing:** presses, unheated mixers, millstones, fans (washing, smelting, smoking; no
  haunting), saws, drills, ploughs.
- **Moving things:** bearings, rope pulleys, gantries, chassis and stickers.
- **Logistics:** belts, chutes, andesite funnels and tunnels, depots.
- **Fluids:** pumps, spouts, item drains, fluid tanks.
- **Control and building:** redstone links, the linked controller, the schematicannon.

### Vanilla items the dormant Nether also holds back

Comparators, observers and daylight detectors (quartz); ender eyes, ender chests, fire charges and
brewing stands (blaze); redstone lamps and respawn anchors (glowstone); beacons; netherite gear;
soul torches and lanterns.

Several of these are everyday redstone parts. Since ADR-0018 lets a player go to the Nether
whenever they can get there, this is the price of a first trip rather than a wall.
