# The journey: every step a player takes, and where it breaks

> **First draft, 2026-10-18**, written while completing the quest book (`quests.md`) after Sanchit asked: *"complete the
> book, add all the things we want to add; by this we will also know what will be the use of the resources we will be
> giving in the planets, and have a deep understanding of what we need to add or tune to make the journey and the full
> flow of the game seamless."* Every claim below was read from the repo's data, from the installed mod jars, or from the
> records of earlier tests, and is labelled by how well it is known. **Nothing in this page has been built because of
> it**; section 5 is the list for Sanchit to choose from.

**How sure each claim is** (the same words are used throughout):

| Label | Meaning |
|---|---|
| **Seen** | Sanchit saw it work in his client (the live-test notes in `plans/`) |
| **Checked** | A scripted run or a dedicated server proved it, nobody watched |
| **Built** | The code or data exists and compiles; never run in a game |
| **Read** | Read from a mod's jar or docs; never run |
| **Missing** | Does not exist |

## 1. The spine, step by step

One row per thing a player does between spawning and lighting the Nether's gate. The right-hand column is where the
journey is weakest.

| # | The player... | Needs | Comes from | Book page | State | Weak point |
|---|---|---|---|---|---|---|
| 0 | Spawns, learns the controls (J book, H sheet, K web, R runes, Z ability, M map) | Nothing | `pack/config/defaultoptions/keybindings.txt` | First Days | **Seen** (keys, sheet, web, runes); the book **Checked** by a scripted client run | Nothing tells a brand-new player to press J (see 5.9) |
| 1 | Finds that Nether and End portals are dead (ADR-0014) | Nothing | `PortalBlockers` | First Days, "Dead doors" | **Seen** (2026-10-06) | None |
| 2 | Smelts iron, finds diamonds and gold | Vanilla | Vanilla | First Days | Vanilla | None |
| 3 | Mines titanium, smelts ingots | A **diamond pickaxe** (ore is y -64 to 0) | Worldgen, `titanium_ingot_*` recipes | First Days, "Titanium" | **Checked** (`/loot`, recipes load) | Never mined by hand in a client |
| 4 | Crafts a tank, learns the valve | Iron and glass | `ascension_atmosphere` recipes | First Days | **Seen** (M1.8) | None |
| 5 | Finds the Ancient City and beats the Warden | Armour, food, wool, patience | Vanilla Deep Dark; Warden drop modifier | First Days | The city is vanilla's; the tendril drop **Built, not seen** | The Warden fight at 20 HP is untested (growth.md section 3 says so) |
| 6 | Crafts gatestones and the keystone | 28 gatestones (8 polished deepslate and 1 sculk make 8), 4 diamonds, 2 echo shards, 2 sculk catalysts, a compass | Deep Dark loot, recipes | First Days | recipes **Checked** (load); **never crafted in a client** | Sculk needs Silk Touch; the quantities are heavy |
| 7 | **Builds the ring** (15 wide, 7 deep, 20 high, 418 blocks) | About 250 cobbled deepslate, bone block pillars, quartz or calcite for the pale parts, the gatestones, the keystone | Earth materials; the **plan is now a picture in the book**, drawn from `gateway.json` | First Days (plan beside the gate quests) | The frame, activation and travel were **Seen** (2026-10-06) from the debug `build` command; **a player has never built it block by block** | Until now nothing taught the layout. Whether a hand-built frame passes every rule is unproven (front/rotation, "mirrored builds are not accepted") |
| 8 | Lights it with a tendril | One tendril per gate | The Warden | First Days | **Seen** (gate lights; the mark steps) | A gate alone leads nowhere; the book says so |
| 9 | Learns Create (alloy, casing, power, press) | Andesite, iron nuggets, logs, an iron block, a water wheel | Create 6.0.10 | Spin | **Read** (recipes read from the jar; advancement ids exist) | The cliff: Create is a mod of its own. The book leans on Ponder (hold W) and JEI |
| 10 | Builds the ship: bearing, propeller, envelope and burner, assembler, wheel | 3 titanium, 9+ iron sheets, wool, coal blocks, alloy, a lever | Create, Simulated, Aeronautics, **our titanium bearing recipe** | Wings | Parts **Read**; the bearing recipe **Checked**; **a ship built from survival parts has never been flown** | **The biggest unknown.** See 5.1 |
| 11 | Climbs past y 320 | Lift and thrust against thin air (about a third of sea level at the line) | Sable's `dimension_physics` data in `ascension-compat-sable` | Skyline | The crossing **Seen** (2026-10-11) with a **command-built** ship | Same as above |
| 12 | Is in space; steers to the Moon | Two tanks (space is a vacuum); the Planets page (point and arrow) | `SpaceEnvironment`, `ChartOverlay` | Skyline, The Moon, Planets | Space **Seen**; the marker in space **Checked** (pixels read back) | Range and air: 5.2. The in-space chart without opening the book: 5.3 |
| 13 | Comes down on the Moon | Nothing (the ship is carried down) | `ascension-compat-sable` | The Moon | **Seen** (2026-10-11) | None |
| 14 | Lives without air; finds the Archive (-160, -256), the ring (-160, 0), the citadel (800, 0) | Tanks, a sealed room with an emitter for refills | Atmosphere, structure sets | The Moon | Archive, ring, Earthrise, dust storm **Seen**; the citadel's bosses **Seen** spawning and fighting | The Armor/Magic Resist fix on the bosses has not been seen |
| 15 | Beats the Ender Golem and the Ender Guardian, takes the key | Armour, food, a bow | Cataclysm, `ascension-compat-cataclysm` | The Moon | **Seen** (2026-10-05) | Balance is untested |
| 16 | Lights the Moon's ring; **both rings lit: travel between them** | The key | `GatewayNetworkState.canTravel` (both ends must be lit) | The Moon ("Two rings") | **Seen** | None |
| 17 | Flies 18,000 blocks south-east to the Nether | Air for the trip, a ship fast enough | Range is air x speed (`worlds-api.md`) | The Rot | Headless only (a command-built ship) | 5.2 |
| 18 | Lives under the rot: one piece of gold armour in any slot | Gold | `PlanetHazardMechanics` | The Rot | **Checked** (dose timing unit-tested, headless); **not seen in a client** | Whole-world client test is still owed |
| 19 | Finds the ring in the warped forest, the colony halls, the Monstrosity | Armour, a key hunt | Seek-placement, Cataclysm | The Rot | Placement **Checked** headless | Never seen |
| 20 | Lights the Nether's gate: both lit, a path home | The Monstrosity's key | Loot modifier | The Rot | **Checked** headless | Never seen |
| 21 | Blazes, brass, smart propeller; ancient debris, netherite | Blaze rods, a burner, copper and zinc, quartz, diamonds | Nether, Create | The Rot, Brass | Materials **Checked** present; tin can be made on Earth and the Brass page gives three burners since ADR-0032 (`oil-and-fuels.md` section 5) | Brass is a Create ladder the player must learn |
| 21a | Flies 32,000 blocks south to the End, falls (if they fall) back to space, kills the Dragon, steps into her exit portal | A ship, wings help | `EndGate`, `VoidFall`, `planet/end.json` (ADR-0028) | The Belt | Compiled and unit-tested only; **not run on a server or in a client** | No ship has flown there; the Dragon is untested at 4,550 HP |
| 21b | Finds an End city, the Elytra, shulker shells | Wings or a gateway pearl | Vanilla worldgen | The Belt | Not checked | Pre-generation cost unmeasured |
| 22 | Grows: points, web, runes, enchants | Bosses, gates, places | `ascension-combat` | Gear and Growth | **Seen** (screens) and **Checked** (64 scripted checks) | See 5.6: nothing to wear at the top |
| 23 | Plays with friends | A party | FTB Teams | Together | **Checked** with stand-in players on a server | Two real clients never tried |

## 2. What every material is for

The question this page was asked: *what will each resource be used for?* This is the honest table. A material with a
source and no job is a bug (`resource-economy.md` principle 1).

| Material | Where it comes from | What uses it today | What is planned | Verdict |
|---|---|---|---|---|
| **Raw titanium / ingot** | Titanium ore on Earth (diamond pick); Archive chests | The titanium propeller bearing (our recipe, needs Aeronautics) | Earth's metal in the combat build paths | **One job.** Fine, and the build paths add the rest |
| **Raw helium crystal** | Moon helium ore (iron pick); Archive chests | **Nothing** | The Moon's material in the build paths; a **helium lamp** (`resource-economy.md` section 4) | **No job. A bug.** 5.4 |
| **Warden tendril** | The Warden, if a player struck the last blow | Wakes Earth's gate (one per gate) | Nothing more | Done |
| **Elytra, shulker shells, chorus, End stone** | The End (outer islands; ships reach them without killing the Dragon) | Wings; shulker boxes; Aeronautics' levitite blend | Nothing | New (2026-10-20), not yet seen |
| **Gateway key (Moon / Nether)** | Ender Guardian / Netherite Monstrosity | Lights that world's gate | Nothing more | Done |
| **Inscribed gatestone, keystone** | Crafted from Deep Dark materials | The gate's frame | Nothing more | Done |
| **Moon stone** (anorthosite, lunar basalt and their bricks) | The Moon | Building; every frame tag accepts it (an Earth gate in Moon stone matches the Moon's ruin) | Nothing | Cosmetic. The Moon page nudges it with one quest |
| **Archive tablets, the Forge Record** | Read in place on their plinths; no loot gives a tablet (2026-10-21); the Forge Record's plinth is in the colony's forge hall | Read in place (the story) | Nothing | Done |
| **Oxygen tank, helium tank, Air Compressor, Air Dispenser** | Iron, glass, Create parts; helium for the level-2 tank (Oxygen v2, ADR-0026) | A Create-powered compressor makes air, fills tanks by right-click, and fills pipes and tanks; an Air Dispenser holds a sealed room from that air. The old emitter and refill station are legacy (no recipe) | Ship cabins (5.2) | Built 2026-10-19, **not yet run in a game** |
| **Rune chalk** | Amethyst and calcite (geodes) | Removing or replacing a rune (with 3 levels) | Nothing | Done |
| **Tomes of Scrapping and Extraction** | A book, lapis, flint or amethyst | Wiping or moving enchantments | Nothing | Done |
| **Our five enchantments + Soulbound** | **Chests only** (Archive, colony). Soulbound is also tradeable and random loot; the five (`own` tag) are **not** in the table, trades or random loot | Gear stats | The book could point at the chests | **Works, but only by chest luck.** Worth a decision (5.8) |
| **Gold** | Earth, the Nether | The Nether's ward; piglin trade; gates (gold block is in two frame tags) | Nothing | Done; "Keep some gold" now pays off in one page (boots) |
| **Netherite** | Ancient debris | Vanilla's top tier; keeps the rot off | Build-path finisher | Done |
| **Blaze rods, brass, quartz** | Nether fortresses; Create's heated mixer | Create's upper tier: propeller upgrades, controls, deployers (`resource-economy.md` Appendix A) | Nothing | Done |
| **The 26 combat items** | **Nowhere** (creative tab only) | Nothing | League-style build paths, one boss heart per boss | **The largest gap.** 5.6 |
| **Boss hearts** | Not designed | Nothing | Needed to finish strong items | Design pending (resource-economy section 5) |

## 3. What the book now teaches, page by page

> **Recut 2026-10-25 (ADR-0033, `quests.md` section 17):** nine projects, each a blueprint page with a finale, in the order a player climbs. Pages 1 and 5 to 12 below are the projects.

| Page | Quests | The thread |
|---|---|---|
| First Light | 16 | Controls, iron, diamonds, gold, titanium, air; then the dark city, the Warden, the gate (with its plan). Project 1; its finale is Earth's gate |
| Runes and Skill Points | 14 | What runes and the skill web are, where points come from, what is locked and why |
| Build the Gate | 7 | Read the plan, level ground, the five stone families and their counts, gatestones, the keystone, raise the ring, wake it |
| Planets | 4 | Earth, the Moon, the Nether and the End on a chart, with a live "you are here" |
| Spin | 27 | Create from nothing: shafts and cogs, a crank, water, wind, stress, grinding, fans, mixing, belts, steam, pipes and a spout; finale **a working line** |
| Brass | 7 | Three burners (a gift, ADR-0032), brass, a deployer, crafters, an electron tube (quartz from diorite, gold or soul sand); finale **a precision mechanism** |
| Current | 5 | Power aboard: an alternator, an accumulator, wires, an electric motor; finale **an accumulator** (pays no point) |
| Crude | 8 | Oil for the rocket: steel, a pumpjack, an electrolyzer, the tower, bronze, kerosene, a platinum tank; finale **a bucket of kerosene** |
| Wings | 26 | A ship is a contraption: glue, weight, a balloon and burner, propellers, a helm, a gyroscope, a control stand, thrusters; finale **a ship that obeys** |
| Breath | 5 | A compressor, how far you can go, a room of air, a bigger breath, what holds air; finale **a room that holds air** |
| Skyline | 8 | Air pressure, the altitude and velocity sensors, goggles, a navigation table, thin air, a first hop; finale **where the sky ends** |
| The Moon | 18 | How a ship gets there (the chart, coming down, a rocket), packing, landing, no air, dust, the Archive, the fortress and its two guardians, the ring; finale **the Moon's gate lit** |
| The Rot | 15 | Gold, the long flight, the ring in the teal forest, the smith, wealth (piglins, bastion, netherite), fire (blaze rods, the Wither, Ignis) |
| The Belt | 9 | The longest flight, landing, no floor, the Dragon, her portal as a gate, the small gateway, an End city, wings, shulker shells |
| Gear and Growth | 10 | Armour, enchanting, tomes, abilities; points and the web; runes, chalk, slots |
| Together | 6 | Parties, catching up, no friendly fire, one shared book, the map, a breath for a friend |

**Rules the book keeps** (checked by the build): no guarded story word (now including Sealers, Openers, the Heir, the
voice, keepers, the destroyer); every item, advancement and structure exists; titles and lines fit the panel; no task
consumes what the player holds; every page has a way in.

**What the book gives away deliberately:** coordinates of the Moon's three places (read from the structure sets),
because a player with no map would otherwise wander an airless world.

## 4. Facts found while writing it

Each of these changed a quest or is a fact a later session needs.

- **Titanium ore needs a diamond pickaxe** (the ledger). First Days had no diamond step, so "Diamonds" was added.
- **Three Aeronautics advancements mark the ship's milestones:** `head_in_the_clouds` (hot air filled), `for_every_action`
  (a propeller placed and powered), `in_thrust_we_trust` (a bearing assembled). Create has `andesite_alloy`,
  `andesite_casing`, `water_wheel`, `mechanical_press`, `brass`. Spin and Wings wait on those.
- **A propeller is four iron sheets round an alloy; a burner is four sheets, two alloy, redstone and a coal block; a
  bearing is one sheet.** So a first ship needs at least nine sheets, which needs a mechanical press, which needs spinning
  power. That is a real tutorial's worth of Create.
- **A gate works only when both ends are lit** (`canTravel`). Earth's gate alone is a doorway to nowhere; the first
  usable pair is Earth and the Moon, after the Moon's two bosses.
- **Space is always a vacuum.** A tank is 5 minutes (1,200 units at 4 a second), lungs 20 seconds, and a player may carry
  **two** tanks. At the design's 60 blocks a second that is about 37,000 blocks of flight, and the Nether is 18,000 out.
- **Air now comes from Create (Oxygen v2, ADR-0026):** an Air Compressor fills tanks, and an Air Dispenser fed by pipes
  holds a sealed room. `ascension-compat-sable` now registers a ship space (position to the ship's plot) and recomputes
  rooms when a ship appears; **never seen on a real ship**, so the book's flight lines still describe tanks only.
- **Quest rewards are claimed per player** (`QuestKey` is a player and a reward). So a player who joins a party late should
  be able to claim what the party already earned. **Read from the jar, not seen.**
- **FTB item tasks do not consume by default**, and the server check now proves none of ours does.
- **The Nether's rot, and gold, are named by the game itself** ("The rot is in the air here. Wear gold."), so the Rot page
  may use those words.

## 5. What to add or tune, in the order it hurts

### Decided by Sanchit, 2026-10-18 (read this before the gaps below)

| Gap | Decision |
|---|---|
| 5.1 First survival ship | **Leave it.** Players try it in the alpha; the alpha test decides whether a starter design or a schematic ruin is needed. |
| 5.2 Range and air | **A ship is a closed space, so the Oxygen Emitter aboard should pressurise it.** No tank change. This becomes part of **Oxygen v2** (below), which he starts soon. |
| 5.3 HUD chart in space | Leave for now; something later. |
| 5.4 Helium | **Fuel: decided and built** (ADR-0025): raw helium is a superheated blaze-burner fuel, so it is the strongest thruster fuel. A better tank stays an idea for Oxygen v2. |
| 5.4b Oil | **Liquid fuel: decided, staged** (ADR-0031 and ADR-0032, 2026-10-25): crude oil on Earth and a refinery from Create: Petrochem; its five fuels feed the liquid Thruster (`oil-and-fuels.md`). The refinery's heat and its tin used to come from the Nether; two Earth recipes now make them (the electron tube's quartz is Create's own, from diorite, gold or soul sand), so oil comes **before the first ship** and the Crude page precedes Wings. Unseen in a client. |
| 5.6 Build paths | A **new session** starts from `plans/session-prompts/4-build-paths.md`. |
| 5.8 Enchantments | Put them in the table (**they already are**, see 5.8), and add the **Lootr** mod so every player gets their own loot from a chest (**installed**, not yet seen in a client). |
| 5.9 First minutes | Tell the player about the book at first login (**built**, not yet seen). |
| 5.7 Client verification | **One run at the end**, once every page and every path is done. No client run before. |

**What is true about air on a ship today (so Oxygen v2 starts from fact, not hope).** The Oxygen Emitter pressurises a sealed
room (up to 4,096 blocks, 24 out), and a ship's cabin is exactly such a room, so the idea is right. But **it has never been
seen to work aboard a moving ship, and the code to do it does not exist.** What exists is the seam: `AtmosphereAnchor.Region`
(a pressurised zone stored in the ship's own space and translated when asked) and the `AnchorSpace` a mod registers to say
how. Nothing registers one: `ascension-compat-sable` has no atmosphere code. What Oxygen v2 needs for this: (1) the Sable side
of that seam; (2) an emitter and a refill station that work aboard; (3) a cabin that stays valid when the ship's blocks
change; (4) doors and hatches that seal (todo.md: "Airlocks need solid blocks"). With a cabin that holds air, tanks are only for
walking outside, and the "29 blocks a second" limit in 5.2 goes away. Until v2 lands, the book's "A long flight" quest
(tanks count the trip) is the true story and must be rewritten when it does.

### 5.1 No one has ever built a ship from survival parts and flown it (blocks the whole spine)

The road to space is by airship (ADR-0015), titanium makes the bearing (ADR-0018), and the book now teaches the parts.
But every crossing seen so far was a **command-built** ship. Unknown: whether a ship built only from what the book
teaches can climb past y 320 in a third of the air, how fast it flies, and how heavy it can be. Also open since
2026-10-11: there is no starter design, and the ADR-0015 idea of a sky-dock ruin with a schematic is not built.

**Recommended (one client session, no new code):** build a reference ship from survival parts in a fresh world with the
Ascension Dev client, record its climb, top speed and fuel use, fly it to the Moon, and write the numbers into
`worlds-api.md`. Decide from the result whether a schematic ruin or just a better book page is enough.

### 5.2 Range and air: tune the tank, or build the cabin

Two tanks and the lungs are 620 seconds of vacuum. At 60 blocks a second the Nether is 300 seconds out, so a round trip
is just inside the limit (600 of 620). At 30 a one-way trip just fits and a return does not; **a ship slower than about 29
blocks a second cannot reach the Nether at all.** ADR-0018 says each world must be answerable "with what a player could
have before arriving", so **the speed of a real survival ship decides whether the Nether is reachable.** Options once
5.1 gives a speed:

1. Do nothing: distance is a gate that needs no code, gates make the return free. Fine if a normal ship does 50 or more.
2. Raise `TANK_CAPACITY`, or allow a third tank, or make a big tank (a one-line tuning change, `AtmosphereTuning`).
3. Build the **ship cabin bridge** in `ascension-compat-sable`: a ship's sealed interior counts as a sealed room, so an
   emitter and a refill station aboard refill tanks in space. The design allows it (ADR-0006, zones anchored to moving
   regions); nothing implements it. Medium work, one new Tier-2 class.

### 5.3 A pilot cannot see where they are without opening the book

The Planets page shows the point and the arrow, but a pilot should not have to stop to open it. The HUD chart in space
(Q5 in `quests.md`) needs a `core` contract and one packet, in the HUD session's area.

### 5.4 Helium has no job (a bug under the economy's own rule)

**Update 2026-10-18: built as ship fuel, ADR-0025.** Sanchit chose fuel, and chose Create Simulated Thrusters as the thruster mod
("that is how we make the rockets"), which amends ADR-0015's "no rocket engines". The Blaze Thruster burns Create's blaze-burner fuels;
raw helium is now in the *superheated* list (4,800 ticks, a blaze cake is 3,200), the strongest push. It is one data file in
`ascension-compat-create`. It loads without error on a dedicated server; **no thruster has been fuelled with helium in a client**.
The analysis below is what led there.

Sources exist (ore, Archive chests), uses do not. Sanchit (2026-10-18) is weighing a **better tank** made with it, or a **fuel**.
What was found, so the choice is made on facts:

- **Supply is not the limit.** Moon only. The ore feature tries 32 veins of up to 6 per chunk between y -64 and 24 (iron-like
  abundance), one raw helium per ore block (no Fortune bonus is set), and an Archive store chest rolls 1 to 4 on a third of its
  rolls. It needs an iron pickaxe, so it opens after the Moon, not before.
- **Demand today is only the build-paths draft:** eight epic components (Pickaxe, Caulfield's Warhammer, Giant's Belt, Recurve
  Bow, Chain Vest, Seeker's Armguard, Last Whisper, Noonquiver) and the helium lamp. A fuel or a tank would be a second and third use.
- **There is no rocket engine to feed**, and ADR-0015 says so on purpose ("no vacuum mechanics, no rocket engines"). A true rocket
  engine would need a new ADR that supersedes it. What a ship has is **propellers turned by rotation**.
- **The nearest thing in the installed mods is Simulated's Portable Engine** (Create: Aeronautics ships Simulated): it burns fuel
  items to turn a shaft, shows "Burn time", and a blaze cake superheats it. That is the ship-friendly power source, so a
  **helium fuel would be ship fuel**, not rocket fuel. It looks up an item's burn time through its own helper
  (`getBurnTime(ItemStack)`), which on NeoForge is very likely the standard furnace-fuel data map; **not yet confirmed**, and it is
  a one-file change plus a server check (a data map giving raw helium, or a crafted helium cell, a long burn time). A cost: a
  furnace would burn the same item, so a crafted cell is cleaner than the raw crystal.
- **The Hot Air Burner needs no fuel** (a coal block is only its crafting ingredient, the tag `aeronautics:burner_fire`), so
  the balloon cannot be fed helium without code. **Levitite** (end stone powder, zinc, water, heat) is the lift material.
- **A tank is code in `ascension-atmosphere`** (a second tank item with its own capacity), and it belongs inside **Oxygen v2**,
  where tank tiers will be decided anyway. A grounded name: a **heliox** tank (the breathing mix divers use).

**Recommendation:** do the fuel first (cheap, data only, and it makes ship range a real choice: coal is bulky, a helium cell is
dense), after confirming on a server that the Portable Engine takes it; leave the tank to Oxygen v2. They do not exclude each
other. Either way the book gets a quest the day it has a job.

### 5.5 Titanium has one job

The bearing is a real job, but the metal is "the ancients' rocket metal" in the lore. The build paths (5.6) are its second
job. No action until then.

### 5.6 There is nothing to wear at the top of the ladder (the biggest content gap)

All 26 combat items have no recipe, loot or drop. Growth, runes and enchantments give numbers and behaviours, vanilla
armour converts, netherite tops the vanilla ladder; but the League-style items, the reason for the stats system, cannot
be obtained. Without them the Gear page can only teach what exists. **Next session in the plan:** build paths
(`plans/session-prompts/4-build-paths.md`), which needs the heart-per-boss design finished. When it exists, the book gets a
forge chain on Gear and Growth with real choice rewards (reward tables, below).

### 5.7 Verifications that have never happened

- The book in a client with the new pages (a scripted run is ready to extend; needs Sanchit's go).
- **Hand-building the gate** in survival from the new plan, to prove a block-by-block build passes every rule.
- The Warden tendril drop; crafting the keystone.
- The Nether, whole, in a client (the world test the plan has carried since 2026-10-11).
- Two real clients: a party, share-air, friendly fire, a late joiner claiming past rewards.

### 5.8 Smaller things

- **Correction (2026-10-18): our five enchantments are already in the enchanting table, sold by villagers and in loot.** An
  earlier version of this list said they were chest-only; that was wrong. Vanilla's `in_enchanting_table`, `tradeable` and
  `on_random_loot` tags are all built on `#minecraft:non_treasure`, ours are in it, and the combat client check
  (`EnchantmentChecks.ourEnchantmentsAreOffered`) has passed. Soulbound is the exception: loot and trades only, by design.
  Nothing to add.
- **Lootr** (proposed by Sanchit): a mod that gives every player their own copy of a chest's loot. Modrinth, MIT, needed on
  client and server, no dependencies. It converts chests that use a loot table, so it covers the Archive, the colony halls and
  every vanilla structure; chests filled by code are left alone. Removing it later deletes the converted chests.
  **Installed 2026-10-18** in the Ascension Dev instance (his yes), booted once on a dedicated server, not yet seen in a client; the
  details are in `docs/technical/pack-extras.md`.
- Reward choices ("one of three") need FTB reward tables. The generator does not write them yet; the format was read from
  the jar (`reward_tables/<name>.snbt`, a table has `loot_size` and weighted `rewards`, a choice reward names it by
  `table_id`).
- The Wither now has a quest (optional, after the fortress). Its advancement is a growth milestone, so the first team kill gives points.

### 5.9 First minutes (built 2026-10-18, not yet seen)

Nothing told a new player to press J; now `QuestBookWelcome` (in `ascension-compat-ftbquests`) sends one chat line, once per
player, naming the player's own key for the book. Original problem: nothing told a new player to press J. FTB Quests puts a button in the inventory sidebar (a red mark when there is
something to claim), but a first-login chat line like the one `ascension-compat-ftbteams` already shows for parties would
cost very little.

## 6. Suggested order (after Sanchit's decisions, 2026-10-18)

1. **Oxygen v2**, including air aboard a ship (his, soon). The quest book's flight pages change with it.
2. **Build paths** (a new session). Its draft already spends helium on eight epic components, so helium has a use there; a
   helium tank or fuel would be a second use, and the supply must cover both.
3. ~~Helium's second job~~ built as thruster fuel (ADR-0025); a helium tank can come with Oxygen v2.
4. ~~Lootr~~ installed (2026-10-18).
5. **One client run at the very end**, covering every page, every path, the hand-built gate, a real ship, the Nether whole and
   two clients.
6. Left for later: the HUD chart (5.3), a starter ship (after the alpha).

## 7. What was changed in the book because of this

Added "Dead doors", "Diamonds", "The Warden", "Gatestone", "The keystone" and the gate plan to First Days; wrote the five
remaining pages; fixed the first page's wording that the new guarded words caught ("keeper").

Later the same night, at Sanchit's request: **Build the Gate** (the plan was a picture nobody could find) and four teaching pages for
Create and Aeronautics (`quests.md` section 14), a corrected line on the hot air burner (redstone-powered, not coal-fed), and the
rocket quests of ADR-0025.
