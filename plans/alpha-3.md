# Alpha 3

Started 2026-10-23, the day after alpha.2 went out (`main` at `f29f397` was alpha.2; everything after it is alpha.3).

**Scope, decided by Sanchit on 2026-10-25 after he tested alpha.2 with friends: alpha.3 is the fixes and the ship/flight mods below. New
structures, Iron's Spells 'n Spellbooks and guns/mechs are NOT in alpha.3**; their plans are kept at the bottom for alpha.4.

## In alpha.3 already (committed locally, not pushed)

| Commit | What |
|---|---|
| `137d257`, `d58c802` | The item audit (`docs/progression/item-audit.md`): Cataclysm's Ignitium, Cursium and Monstrous Helm at least the netherite they are smithed from; its 15 boss weapons raised and held under every legendary; the bow and crossbow add Attack Damage and every bow is swung slowly; three more Cataclysm abilities are magic; **four ability keys instead of one**. Server-checked (`damage-checks` 405/0, `check-balance` 0 problems), **not yet pressed in a client**. |

The unreleased changelog is the top section of `pack/launch/CHANGELOG.md`. The network version went from 6 to 7, so alpha.2 and alpha.3 cannot
share a server.

## Findings from the alpha.2 test with friends (Sanchit, 2026-10-25)

Written down as he said them. "Status" is kept current as each lands; nothing is done until it is seen in a client or a dedicated server.

### Server and launcher

| # | Finding | Plan | Status |
|---|---|---|---|
| F1 | The whitelist is in the way: friends cannot join. | The server pack ships `white-list=false` and `enforce-whitelist=false`; the running server gets `whitelist off`. `online-mode=true` stays. | Pack default and docs done 2026-10-25. The running server still needs `whitelist off` (Sanchit's console). |
| F2 | After every new version the main menu has Distant Horizons begging for an update. | Switch its update check and prompt off in the pack config. | Done: `enableAutoUpdater` and `enableSilentUpdates` false in `pack/config/DistantHorizons.toml`. Not seen on a fresh client. |

### Recipes and drops

| # | Finding | Plan | Status |
|---|---|---|---|
| F3 | Brass is needed for a few things and Create only makes it with a Nether blaze. | A hand-crafting recipe for the brass ingot (no Nether), deliberately dearer than Create's mixing. Numbers are placeholders. | Done: 3 copper + 3 zinc make 2 brass (3 ingots per brass against the mixer's 1). `create-checks` 31/0. Not seen in a client. |
| F4 | The Trial Chambers' mace item is too rare ("molten core"; assumed to be the Heavy Core, **confirm**). | Raise its drop rate a little, not a lot. | Done: the Heavy Core is only in the ominous vault (7.5% a vault); a separate 5% bonus makes it 12.2% (measured 12.26% by `loot-checks`, 17/0). Not seen in a client. Still **confirm** "molten core" meant this. |

### Bugs

| # | Finding | Plan | Status |
|---|---|---|---|
| F5 | Create's Goggles and Create Aeronautics' aviator goggles will not go in the Curios head slot. | Tag them for the Curios head slot (compat jar). | Done for the aviator's goggles: tag plus a goggles predicate (`AviatorGogglesCurios`), because Aeronautics' own predicate looks at the helmet slot only. Server data check: both goggles are allowed in `head`. **Create's own goggles were already allowed** (Create tags and assigns the slot itself), so if they still would not go in, it is something a client shows and a server cannot. Not drawn on the head while in the slot (Create draws only its own); the predicate is untested without a player. |
| F6 | Neither our Air Dispenser nor our Air Compressor can be broken properly. The compressor can at least be picked up with a wrench; the dispenser can only be destroyed and drops nothing. | Both mineable with a tool, both drop themselves, the dispenser wrenchable too. | Done: the cause was `requiresCorrectToolForDrops` with no pickaxe tag. Pickaxe tags for all four machines, `create:wrench_pickup` for the dispenser. `atmosphere-checks` and `create-checks` pass on a dedicated server. Not seen in a client. |
| F7 | The Air Dispenser on an Aeronautics ship kept saying "the block is open" while the ship was fully sealed. | Find why the sealed-room check fails in a ship's own space (`ascension-compat-sable` registers a ship space); fix and check with a real ship. | Partly: `ship-checks` builds a sealed room with a door, makes a real Sable ship of it and finds it sealed and pressurised in the plot, with the rider lookup right (11/0, dedicated server). The likely cause was the door (F8). Sanchit's ship had glass blocks (they seal) and a hot air balloon: the audit found the balloon's **adjustable burner leaked**, so it is now in the new airtight tag. Slabs and glass panes seal too (his call); stairs and iron bars still leak. Not seen with a real Aeronautics ship in a client. |
| F8 | Closed doors do not count as a sealed wall: neither wooden doors nor Create's glass doors. | A closed door and trapdoor seal; an open one does not (gates left leaking). | Done: `SealedVolume.isClosedDoor`, and opening or closing is noticed through the game event. 26 checks, 0 failures on a dedicated server (wooden and iron door, trapdoor, fence still leaks). Create's sliding doors are `DoorBlock`s and post the same event (read from the jar). Not seen in a client. |

### Mods to replace (Sanchit's verdict; each needs research, then his pick, then a compat check)

| # | Finding | Wanted |
|---|---|---|
| F9 | The paraglider mod is bad: costs too much, breaks easily, does not feel smooth. | A better one. |
| F10 | The backpack mod is not good: it cannot hold a sleeping bag. A modpack he played had a backpack that did, "sleeping bags are so goated". | A backpack with a sleeping bag in it. |
| F11 | Does not like Create Simulated's thrusters; has another mod in mind (not named yet; probably Create Propulsion below, **confirm**). | Better thrusters. |
| F12 | No sorting for chests or inventory. | A sorting mod. |
| F13 | No quiver. | A quiver slot in Curios. His question: do our special bows work with special arrows? (Already true for damage: an arrow adds the shooter's Attack Damage, any bow, any arrow. Quiver itself and tipped/special-arrow behaviour with our bows: to verify.) |

### Mods to add (Sanchit asked for these by name)

First flight to the Moon failed for the whole group, "so many issues". These are what he thinks will fix it:

| Mod | Why he wants it |
|---|---|
| [Create Aeroworks](https://www.curseforge.com/minecraft/mc-mods/create-aeroworks) | Makes ships easy to control. |
| [Create Propulsion (Simulated)](https://www.curseforge.com/minecraft/mc-mods/create-propulsion-simulated) | Better thrusters and controls; they run on fuels. |
| [Create Crafts & Additions](https://www.curseforge.com/minecraft/mc-mods/createaddition) | Electricity: store power and run a ship from it. |
| [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) | Food. |

Per mod before it is installed: 1.21.1 NeoForge build exists; works with the Create, Simulated and Aeronautics versions we run; what its
recipes and blocks do to `docs/progression/resource-ledger.md` (every material needs a use, every item a source); whether the rocket quests
(ADR-0025: Simulated thrusters, helium fuel) must change, which means a new ADR; the jar goes into the CurseForge pack and the server pack
(`pack/curseforge-extras.json`). The instance is shared with other sessions: compare its jars before writing to it.

### What the research found (2026-10-25, from Modrinth's API; nothing downloaded or installed)

We run Create 6.0.10, Create Aeronautics 1.3.2 (bundles Simulated, Aeronautics and Offroad) and Sable 2.0.5.

| Mod | Version for 1.21.1 NeoForge | Needs | Notes |
|---|---|---|---|
| Create Aeroworks | 1.5.0 (2026-08-27), 790 KB | NeoForge 21.1.225+, Create 6.0.0+, Aeronautics 1.0.3+; both sides | **Gyroscope** (keeps a ship level, scales with RPM), **Joystick** (two axes out as redstone signals, keyboard mode), **modular control console** (lever, joystick, button, wheel, yoke, throttle). Custom licence ("LicenseRef-Create-Aeroworks-License"): read it before the server zip carries its jar. |
| Create Propulsion: Simulated | 1.1.5 (2026-07-23), 1.7 MB, MIT | Sable and Aeronautics; both sides | **Thruster** (fuel), **Ion Thruster** (Forge Energy), multiblock thrusters 2x2x2 and 3x3x3, copycat wings, a solid-fuel thruster. Since 1.1.4 **fuels are data**, so helium can be added as a fuel by datapack (format to be read from the jar). Names Create Crafts & Additions among its fuel mods. |
| Create Crafts & Additions | 1.7.2 (2026-10-01, beta), 1.8 MB | Create; both sides | Electricity: generators, batteries (Modular Accumulator), motors, wires, Tesla coil. **Wires do not work on a ship by themselves**: there is a small addon, *Create Additions - Sable Compat*, for that, and *Create Crafts & Additions and Aeronautics Compat* lets a docking connector pass energy. Both would be needed for "power on a ship". |
| Farmer's Delight | 1.3.4 (2026-08-29), 3.2 MB | none; both sides | Food. Adds crops, a cooking pot and a cutting board, so the resource ledger gets new items; its villagers' trades and world generation are the things to watch. |

**What changes with Propulsion**: ADR-0025 says rockets are ships with Create Simulated Thrusters and helium is their fuel, and the quest book, the journey map and
`ascension-compat-create`'s helium fuel entry are written against that. Replacing the thrusters means a new ADR that supersedes it, a datapack making helium
Propulsion's fuel, and the quest pages rewritten; Simulated Thrusters would be removed from the pack.

**How they get in**: the CurseForge pack is built from the launcher's record of the Ascension Dev instance (`tools/build-curseforge-pack.py`), so each mod is added
through the CurseForge app, then checked on a dedicated server, then the pack zips are rebuilt. Another session installs jars into that instance: compare before
writing to it.

### Candidates for the replacements (Modrinth, 1.21.1 NeoForge; Sanchit picks)

| For | Candidate | What it is | Concerns |
|---|---|---|---|
| F9 paraglider | **Paragliders** (Tictim, GPL-3, 2.9M downloads) | A Breath-of-the-Wild glider with stamina, updrafts over fires, durability optional in config, speed configurable. | It adds Spirit Orbs and Goddess Statues that raise Max Health and stamina: those must be switched off (config says they can be) or they fight the growth tree. Not seen in play. |
| F9 | the Gliders we have (Jeryn99's, `vc_gliders` 1.1.8) | Tiers wood to netherite plus reinforced paper. | Its recipes are data, so a cheaper tier or a cheaper recipe is a datapack change, no new mod. Whether durability is configurable: not read yet. |
| F10 backpack with sleeping bag | **Traveler's Backpack** (21.9M downloads, updated 2026-10-23) | Upgradeable backpacks, Curios integration. | It is the one I know carries a sleeping bag inside the backpack; the Modrinth description does not say so, **confirm in its wiki** before swapping. Replaces Sophisticated Backpacks (and the `back` slot). |
| F10 | Comforts (24M) or Sleeping Bags (55K) beside the backpack | Sleeping bags and hammocks that do not set the spawn. | Separate items, not inside a backpack. |
| F12 sorting | **Inventory Profiles Next** (42M, client only) | Sorting, quick moving, slot locking, gear sets. | Client-side, so no server jar. Alternatives: Client Sort, Sort It Out!. Sophisticated Backpacks and Tom's Storage have their own sorting, not the player inventory's. |
| F13 quiver | Nyf's Quivers (262K, five tiers) | Quivers as items. | Whether it has a Curios slot is not on its page; **ask for or check a Curios quiver slot** before choosing. |
| F14 map | **Xaero's Minimap + World Map** (121M) | Shows players, mobs, waypoints, cave mode. | With **Open Parties and Claims** teammates show on the map; but we use FTB Teams and FTB Chunks (which has a map). JourneyMap (15.6M) is the other: it has underground slices by Y level. Team health on the HUD is ours to build (the HUD module's deferred T4), not a map mod's job. |

### Decided by Sanchit, 2026-10-25, and where each stands

| What | Decision | Status |
|---|---|---|
| Walls that hold air | Slabs and glass panes seal; stairs and iron bars do not. | **Done**, tag `ascension_atmosphere:airtight`; the balloon's adjustable burner is in it too (the audit found it leaking). |
| F9 paraglider | **Paragliders** (Tictim), with the Spirit Orb, Heart Container, Stamina Vessel and statue parts off. He likes that the glider is held, not worn. | Config in `pack/config/paraglider-*.toml`, `ascension-compat-paraglider` empties the three statue tags. **In the instance since 2026-10-25**, the statues not yet looked for in a world. Stamina use while gliding is left on (one wheel, recovers on the ground); durability is off by default. |
| F10 backpack | **Traveler's Backpack** (confirmed). | In the instance since 2026-10-25. Its sleeping bag inside the backpack is still to be seen. |
| F11 thrusters | **Create Propulsion: Simulated** (confirmed). | ADR-0030; helium is a solid-fuel thruster fuel; quest page rewritten. In the instance since 2026-10-25; never flown. |
| F12 sorting | **Inventory Profiles Next** (+ libIPN, Kotlin for Forge). | In the instance since 2026-10-25. |
| F13 quiver | **Dropped.** "It's fine, leave it." | Closed. |
| F14 map | **Xaero's Minimap** (+ World Map). The team HUD with teammates' health is a separate session. | In the instance since 2026-10-25; the HUD prompt is `plans/session-prompts/12-team-hud-and-map.md`. |
| Shaders | **Both** Complementary Reimagined and Unbound, **off by default** (Sanchit, 2026-10-25), clouds off everywhere. | **Built, unseen in a client.** The manifest names both by CurseForge id (Reimagined 627557 file 8884654, Unbound 385587 file 8884656, both r5.9.3, tracked by the launcher); `leave_out_of_the_manifest` is gone from `pack/pack.json`; `pack/shaderpacks/<pack>.zip.txt` holds `CLOUD_STYLE_DEFINE=0` (OFF in the pack's own language file) and goes in `overrides/shaderpacks`; no `iris.properties` is shipped, so no pack is selected and a new player has shaders off. Still to do: a client look at space, the Moon, a ship and a planet's edge under each pack, and the credit lines in the CurseForge description at publish (`pack-extras.md`). Notes from before: the pack's own cloud switch is `CLOUD_STYLE_DEFINE=0` (OFF), which goes in the shader options file the pack ships beside the zip (`shaderpacks/<pack>.zip.txt`), not in the pack itself, which its licence forbids modifying; Iris's `config/iris.properties` says which pack is on by default. Both packs are already in the dev instance and credited rules are in `pack-extras.md`. alpha.2 left shaders out for clouds in space, toothed planet edges and an Iris warning from Aeronautics, which is what the client look is for. **Euphoria Patches added 2026-10-25 (Sanchit):** the Euphoria Patcher mod (project 915902, file 8884680, client only) builds two more folder packs from the two Complementary zips; their option files are `pack/shaderpacks/<folder>.txt`; seen once in a dev client (Earth, Moon, belt; no errors, no clouds); details in `pack-extras.md`. |
| Flight | The failure was the controls. | Aeroworks (gyroscope, control stand, joystick) and the flight quest page; **never flown in a client**. |

**Applied to the Ascension Dev instance on 2026-10-25** (Sanchit said yes): the whole new mod set had been built into a copy of the instance's mods, booted on a
dedicated server and checked (`create-checks` 34/0). The 21 jars it replaced or dropped (Gliders, Sophisticated Backpacks, Create Simulated Thrusters and the
old `ascension_*` jars, whose only unique content was the Cat's Eye and Silent Step runes and the single armour-ability key that main had already replaced)
are in the instance's `mods-backup-before-alpha3`; the 19 `ascension_*` jars are the build output; `python tools/sync-pack-config.py` ran. The CurseForge app then
fingerprinted the folder, so its record now tracks every mod (69), and `tools/build-curseforge-pack.py` counts a mod listed both there and in
`curseforge-extras.json` once. **No client has started with this set yet.**

**At publish**: the CurseForge description's mod lists (Storage, Gliders, Keys) and `KNOWN_ISSUES.md` still describe alpha.2.

### Good to have in alpha.3 (not required)

| # | Finding |
|---|---|
| F14 | The map and the team HUD: other mods or settings felt better. Seeing teammates on the map was easy elsewhere, with their health in the HUD. A minimap with Y-level layers (the map at different heights). |

## Left over from the audit

- Curios items (Cataclysm's belts and rings are not worn any more; Icarus wings and the gliders) were not read; they are not item attributes.
- Shields add nothing. A design question, not a bug.
- A client look at the four keys, their tooltips and a bow's slow blow.

## Parked for alpha.4 (Sanchit, 2026-10-25: not in alpha.3)

The three sections below are unchanged from 2026-10-23.

### Proposed: more places to explore (structures from other mods)

Low risk if it is one or two mods and each is brought in as the Cataclysm Ruined Citadel was: a biome tag in a compat jar says where it
may generate, and the planet loot system already pays out every chest on the Moon, the Nether and the End. Candidates found on Modrinth for
1.21.1 NeoForge on 2026-10-23: Dungeons and Taverns (a datapack, easiest to tune), YUNG's Better Dungeons, When Dungeons Arise (large, heavy),
Moog's Voyager Structures. Not Incendium (it replaces the Nether's biomes; Planet 3 is ours) and not an End structure mod (Planet 4 is ours).
Costs to check for each: world generation time and pre-generation, pack size, and a tier tag for every mob it adds (a mob with no tier is paper
next to our bosses).

### Proposed: Iron's Spells 'n Spellbooks (the careful one)

Why it is attractive: magic would stop being a few procs and a defence stat. Wizards that cast at the player make Magic Resist matter; the
rarity ladder of its spells can follow our bosses, early weapons, mid spells, late guns and mechs. Facts checked: 1.21.1 NeoForge build 3.16.3
(2026-08-18), over 100 spells in 9 schools, 9 casters, 2 bosses, 12 armour sets, 11 structures; all rights reserved, so it goes in by manifest like
our other such mods. **Iron's Spellbooks Tweaks** (MIT, server side) caps the rarity a player may cast and unlocks more by advancement or kill,
from datapack files, which is the gate Sanchit asked for and fits ADR-0018 (bosses unlock capability; no world is locked).

#### Why care, specifically

1. **It is a one-way door for worlds.** Adding it to an existing world is harmless. A world played with it will not load cleanly without it. So
   it ships only when we mean to keep it.
2. **It is a third stat system** beside the legendary items and the runes and growth: its own mana, spell power, cooldown and spell resistance. Left
   alone it fights our numbers; wired badly it doubles a defence (Iron's resistance and our Magic Resist on one hit).
3. **Its numbers are tuned for 20-health mobs.** Our bosses carry 1,000 to 4,500 health and 100+ Armor and Magic Resist.
4. **Overlap with systems we built:** the HUD module (its mana bar), Curios (we cut the slots to seven on 2026-10-22; it wants its own), Lootr and the
   planet loot modifiers (it adds scrolls to chests), teams (spells that hurt or heal allies; ours cannot hurt a teammate), Better Combat, and the
   mixins `ascension-compat-cataclysm` and `-sable` already carry (ADR-0013, ADR-0016).
5. **Per-player unlocks against per-team progress.** Tweaks unlocks trigger on a player's own advancement or kill; our milestones belong to the team.
   Does a teammate who did not land the killing blow get the advancement? Unchecked.
6. **It updates often**, and its config has had multiplayer sync problems that Tweaks works around with its own file.

#### How, in order (each step ends at a gate; no step is skipped)

0. **Decide, no code.** What do spells scale off: our Attack Damage, Magic Penetration and runes, or Iron's own spell power and mana? Is magic a
   *lane* (its own resource, its own armour) or a layer over the legendary kit? Sanchit's call; everything after depends on it.
1. **Sandbox.** A branch and its own worktree, and a copy of the test mods folder: **not** the Ascension Dev instance (another session installs
   jars there; compare before overwriting). Downloading the jars is Sanchit's go.
2. **Read before wiring.** The method that worked for Cataclysm: take the jar apart and list its damage types, projectile and summon entities,
   attributes, Curios slots, loot modifiers, structure biome tags, mixins and event hooks. Write what each does in `docs/technical/` before any
   compat code exists.
3. **The compat jar `ascension-compat-ironsspells`** (Tier 2, ADR-0003: Tier 1 never names it, and no mixin without an ADR): magic damage tags,
   tier tags and stats for its mobs, item data for its armour (**the item audit finds the Cataclysm fault in it; use the floor rule**), Curios slots,
   the rarity ladder as Tweaks unlock files tied to our growth milestones, and a plain rule for casting in vacuum.
4. **Balance by measurement**, as the weapons were: a server check casts with a stand-in player and the result is held by `check-balance` against the
   legendary band. A spell build must not kill the standard boss faster than the slowest legendary alone (54 s) at the same stage.
5. **Verify on the order ADR-0008 sets:** dedicated server, then two clients (teams), then a world unload and reload, then a performance-log entry
   (Iron's spawns many entities).
6. **Only then** the quest page, the recipes that gate it, the page text.

#### Go or no-go

If step 0 or step 2 turns up something we cannot answer (a mixin that fights ours, damage that skips the damage events, no per-player gate we can
tie to a team), **Iron's does not ship**; it waits and the sandbox branch stays. A cheaper alternative if magic is the
point: grow our own magic lane (Arcane Edge, Aether Tip and Spellpiercing exist) and use Iron's only for its structures and mobs.

### Proposed: guns and mechs

Guns exist for 1.21.1 NeoForge: TaCZ (guns are data, so their damage can be set), Superb Warfare (guns, tanks, helicopters), Create: Gunsmithing.
No ready-made mech mod turned up in a Modrinth search; mechs may be ours, perhaps built on Sable. Open.

## A liquid fuel from crude oil on Earth (asked 2026-10-25, research only)

Sanchit: the liquid Thruster needs a fuel that is not helium; he is thinking of a mod that puts crude oil on Earth with a full refining chain, whose
products fuel the rocket engine and Propulsion. Read from the mods' own repositories and Modrinth pages (no jar downloaded, nothing installed).

**What the Thruster accepts.** Create Propulsion's liquid fuels are data (`data/createpropulsion/thruster_fuels/<ns>/<name>.json`: fluid, thrust
and consumption multipliers, `required_mod`). Its jar already carries entries for Create: Diesel Generators (diesel 1.1 thrust / 0.85 use,
gasoline 1.2 / 0.9, biodiesel, ethanol, plant oil), TFMG (diesel, gasoline, kerosene 1.05 / 0.9, naphtha), Immersive Engineering and Petroleum, Mekanism,
Northstar and Stellaris. A mod it does not name needs one small file per fluid from our `ascension-compat-create` jar.

| | Create: Diesel Generators 1.3.15 | Create: The Factory Must Grow 1.2.0 | Create: Petrochem 1.3.3 |
|---|---|---|---|
| Size and health | 6.4 M downloads, MIT, NeoForge 1.21.1, updated 2026-07-30 | 2 M downloads, MIT-NON-AI licence, 1.21.1 builds, repo active 2026-10-19 | 11 k downloads, MIT, 1.21.1 only, updated 2026-09-20; a small following, so expect bugs |
| Needs | Create | Create | Create 6.0.10 (ours); its electrolyzer takes Forge Energy, which Crafts & Additions makes |
| Oil on Earth | Per-chunk oil amounts from noise, richer in plains, desert, savanna, badlands and oceans (`oil_biomes`); a pumpjack and an oil scanner; a `deny_oil_biomes` tag keeps it off the Moon | Underground oil deposits, a pumpjack (not read in detail) | A pumpjack on desert, jungle, swamp or deep ocean ground (80 to 95 mB per cycle, endless); recipes are keyed by biome, so our Moon, Nether and End get none. An add-on, Petro Deposits, makes finite deposits with scanners |
| Refining | One step: heated distillation tank, 100 mB crude to 50 diesel and 50 gasoline (superheated 75 and 75); plus plant oil, ethanol, biodiesel | The deepest industry of the three: coke ovens, blast furnaces, large distilleries, steel, concrete, aluminium and sulfur (not read in detail) | A real chain: electrolyzer desalts, a distillation tower in three modes (flash: oil, light naphtha, LPG; atmospheric: diesel, kerosene, heavy naphtha, oil residue; vacuum: heavy gas oil, fuel oil, heavy residue), gasoline mixed from the cuts; steel pipes and tank. An "Expert" mode that roughly doubles the fluids is announced |
| Rocket fuel | Diesel and gasoline; no kerosene | Kerosene, diesel, gasoline, naphtha | **Kerosene**, diesel, gasoline, LPG, fuel oil |
| Propulsion fuel data | Shipped | Shipped | **Ours to write**, four small files |
| Weight it adds | Light (tens of items) | Heavy: its own steel, aluminium, bauxite and sulfur world generation, a second progression | Medium: steel and its own machines |

**Reading.** Diesel Generators is the safe choice and the thinnest refinery. TFMG is the biggest game and would become a second pack inside ours (the
Petrochem author calls its mechanics glitchy; unverified). Petrochem is the closest to "crude to a full refinery to kerosene for a rocket" and the one that
keeps oil off every world but Earth without any tag, at the cost of being young. Proposed: Petrochem, with Diesel Generators as the fallback if it misbehaves
on a server. Before either is installed: download the jar (one file, about 2 MB, from Modrinth), add it to the staging copy, boot a dedicated server, check
that its fluids, recipes and pumpjack load beside the rest, then write the Thruster fuel files and a quest section. 
**Decided 2026-10-25: Petrochem** (ADR-0031), Diesel Generators the fallback. Petrochem is on CurseForge too (project 1494004, 331 k downloads; the 11 k above is
Modrinth alone). Staged with the whole set and booted: 2.3 s, no mixin conflict, `create-checks` 52/0 including the five Thruster fuels read back out of Create
Propulsion's own table; the quest lane (oil, steel, pumpjack, electrolyzer, distillation, kerosene) loads (`quest-checks` 10/0). **Finding:** steel, bronze and the tower are
all *heated* recipes, so the refinery needs a Blaze Burner, which only a Planet 3 blaze gives (Create's `blaze_burner_capturable` tag is the blaze alone). Oil is
therefore the upgrade after the Nether; the Moon still runs on coal, charcoal or helium. **Superseded the same day (ADR-0032):** Sanchit said oil is needed for the first ship, so a Blaze Burner (lava bucket,
magma block, iron sheets) and tin (two zinc nuggets, an iron nugget) were made craftable on Earth (an Earth electron tube was added and removed: Create makes quartz on Earth).
**Sanchit then said (2026-10-25) no Blaze Burner on Earth; `tools/audit-nether-gating.py` found everything ship-critical traces to that one item, and he chose option B of `docs/progression/oil-and-fuels.md` section 5: the burner recipe is removed and the Brass page
gives three Blaze Burners** (a mined burner drops itself, so they can be moved); tin is still an Earth recipe; the oil chain no longer touches the Nether. `create-checks` 66/0. How the five
fuels differ is `docs/progression/oil-and-fuels.md`. **In the instance since 2026-10-25** (with the rebuilt `ascension_compat_create` jar and the quest lane synced); a pumpjack, electrolyzer and tower are unseen in a client.

## Not decided (needs Sanchit)

For alpha.3:
1. ~~Is "molten core" the Heavy Core (F4)?~~ Yes (2026-10-25).
2. ~~May the staged mod set be applied?~~ Yes, done.
3. ~~Shaders?~~ Both, off by default.
4. ~~A liquid fuel~~ Petrochem (ADR-0031), installed.
5b. ~~Progression idea (his, 2026-10-25): make the quest book work like Dr. Stone's roadmaps.~~ **Built (option B, ADR-0033):** nine projects as blueprint pages, 16 pages and 185 quests with every id kept, six finales pay 1 point each and six discoveries stopped paying (total still 41), oil before the first ship, the test flight a lesson in Skyline. Commits `7a6fba2`, `756f64f`, `d9eefc5`. `quest-checks` 10/0, 49 tests, `Growth: 23 milestones worth 41 points`. **Not seen in a client**: how the sheets sit under the quests and lines, the title blocks, the icons. Open: whether kerosene should clearly be the rocket fuel (`oil-and-fuels.md` section 4).
5. The "few more things before we publish" and the "final rollout" list he mentioned earlier, which still have not been told to me.

For alpha.4: spell scaling and lane (step 0 above), and which structure mods.
