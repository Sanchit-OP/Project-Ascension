# Pack extras: quality-of-life, teams and quests

Third-party mods the pack adds **beside** the game we are building. Like the
[optimisation stack](optimisation-stack.md), none of these is a dependency of any Ascension module
(ADR-0002, ADR-0003): they change how the game is played and seen, not what it is. Where a mod needs
an Ascension connector (teams and quests will), the connector is a Tier-2 `ascension-compat-*` jar
that loads only when the mod is there.

Every entry was checked on **2026-10-18** against the build that exists for **Minecraft 1.21.1 on
NeoForge**, from Modrinth's API or FTB's own maven. A project listing "NeoForge" and "1.21.1"
separately does not mean the pairing exists (see the Zoomify row).

**Status, 2026-10-18: installed in the Ascension Dev instance** (Sanchit's go-ahead): Iris, Ok Zoomer,
FTB Quests, FTB Library, FTB Teams and Architectury. Every file was checked against the hash its
source publishes before it was copied. **FTB XMod Compat was left out:** Quests lists it as optional
and it wants JEI 19.53 where we run 19.51; add it when JEI is updated. The optional extras below
were not decided.

**Booted and checked, headless and in a dev client:** the dev server loads Library, Quests, Teams,
Architectury and Ok Zoomer with no new errors, answers `/ftbteams list`, and skips Iris (the dev mirror
sees it is client-only). The dev client loads Iris beside Sodium 0.8.13, Distant Horizons and
ImmediatelyFast (Iris binds Distant Horizons' events, ImmediatelyFast switches its Iris compat on), and
the scripted combat integration run still passes **55 of 55** with all six present. Not yet seen by
eye: a shader pack (none is installed; Iris only loads the pack files in `shaderpacks/`), our skies
and ships under shaders, Quests' screens, and Ok Zoomer's default key against ours. Sodium logs that it
force-disables its own sky mixins because Iris asks it to: Iris draws the sky, so check our custom
skies first.

## The set

| Mod | Build | File (size) | From | What it is for |
|---|---|---|---|---|
| **Iris Shaders** | `1.8.14-beta.1+1.21.1` | `iris-neoforge-1.8.14-beta.1+mc1.21.1.jar` (2.5 MB) | Modrinth | Shader packs. **The beta is the right one:** its changelog says "updates to Sodium 0.8", which is the Sodium we run (0.8.13). The last full release, 1.8.12, was built for Sodium 0.6 and will not load beside it. Client only. |
| **Ok Zoomer** | `10.0.0-beta.13` (NeoForge) | `ok_zoomer-neo-10.0.0-beta.13.jar` (208 KB) | Modrinth | The zoom key, with smooth scroll zoom and a cinematic camera, all configurable. No dependencies. Client only. |
| **FTB Quests** | `2101.1.36` | `ftb-quests-neoforge-2101.1.36.jar` (1.6 MB) | maven.ftb.dev | The quest book: chapters, tasks, rewards, **shared by a team**. |
| **FTB Library** | `2101.1.37` | `ftb-library-neoforge-2101.1.37.jar` (1.4 MB) | maven.ftb.dev | Required by Quests and Teams (screens, config, SNBT files). |
| **FTB Teams** | `2101.1.11` | `ftb-teams-neoforge-2101.1.11.jar` (308 KB) | maven.ftb.dev | Parties and teams: create, invite, a team id other mods read. Required by Quests. |
| **Architectury API** | `13.0.11` (NeoForge) | `architectury-13.0.11-neoforge.jar` (585 KB) | Modrinth | Required by Quests, Library and Teams. |
| **FTB XMod Compat** | `21.1.12` | `ftb-xmod-compat-neoforge-21.1.12.jar` (149 KB) | maven.ftb.dev | Small glue between the FTB mods and JEI. Listed as a dependency of Quests on CurseForge. |

FTB's mods are not on Modrinth. maven.ftb.dev is FTB's own release repository, the same files
CurseForge serves. Quests' pom asks for Architectury 13.0.8, Library and Teams 2101.1.x; the newer
patch versions above are the latest of each.

**Optional, not asked for, worth a decision:**

| Mod | Build | Why |
|---|---|---|
| **Xaero's Minimap** `26.6.0` and **World Map** `1.47.0` | 2.2 MB and 1.4 MB, Modrinth | A game about travelling needs a map; both handle custom dimensions. They draw a minimap on the HUD, so the HUD overhaul must make room. |
| **Mouse Tweaks** `2.26.1`, **Controlling** `19.0.5` with **Searchables** `1.0.2` | under 100 KB each | Inventory handling; searchable key bindings (we now have R, K, V and an ability key). |

## Shader packs: Complementary Reimagined and Unbound

Sanchit's choice (2026-10-18): bundle both, and leave any others to players. Installed in the
Ascension Dev instance's `shaderpacks/`, hash-checked against Modrinth:

| Pack | Build | File (size) | Licence |
|---|---|---|---|
| **Complementary Shaders: Reimagined** | `r5.9.3` (2026-09-15) | `ComplementaryReimagined_r5.9.3.zip` (553 KB) | Complementary License Agreement 1.7 |
| **Complementary Shaders: Unbound** | `r5.9.3` (2026-09-15) | `ComplementaryUnbound_r5.9.3.zip` (553 KB) | Complementary Agreement 1.3 |

Reimagined keeps the look of vanilla Minecraft at higher quality; Unbound is the more
transformed, atmospheric look. Players add any other pack themselves, in Video Settings > Shader Packs.

**Licence rules for bundling in a pack** (read from each pack's `License.txt`):
- **Credit them visibly** in the pack's description page(s). Reimagined asks for this when it is on by
  default for new users; Unbound asks for it always.
- **Do not modify them** (Reimagined: not even a variable).
- **Add them through Modrinth or CurseForge, never as a file we upload** (Reimagined 1.2d). So the
  zips are **not committed to this repository** and a distributed pack references them in its
  Modrinth or CurseForge manifest. The dev instance's copies are only for playing.
- The pack's author is responsible for any problems it causes in their pack (both licences).

**Checked:** a dev client with Reimagined switched on loads it beside Sodium 0.8.13 and Distant
Horizons, Iris builds the overworld pipeline, there are no shader compile errors, and the combat
integration run still passes 55 of 55. Iris warns about uniforms the pack defines for newer Minecraft
versions (Pale Garden, Sulfur Caves, the End flash), which do not exist in 1.21.1 and are harmless.
Flywheel (Create's renderer) falls back from `indirect` to `off` while a shader is on, which is
Flywheel's own choice. **Not yet seen by eye:** the Moon and space under either pack, and Unbound
at all. Shaders are off in the instance until a player turns one on.

## Left out on purpose

| Mod | Why |
|---|---|
| **Zoomify** | Its NeoForge builds start at Minecraft 26.1; there is **no 1.21.1 NeoForge build**. Ok Zoomer does the same job. |
| **AppleSkin, Neat and other health-bar and HUD mods** | They draw what the HUD overhaul (`plans/session-prompts/6-hud-overhaul.md`) is about to replace. Revisit after it. |
| **FTB Essentials, Waystones and other teleport mods** | They would short-cut the airship road to space (ADR-0015). |
| **FTB Chunks** | Claims and a map. Later, if co-op griefing becomes real; Xaero's covers the map. |
| **Mod Menu, Embeddium** and friends | Fabric plumbing, or a second copy of Sodium. |

## What to check once they are in

The client:

1. The game boots with the whole stack and no new errors in the log.
2. **Shaders:** a shader pack loads with Sodium 0.8.13 and Distant Horizons 2.4.5-b together. Then the
   things that draw their own way: our Moon, Earth and planets in the sky, the dust storm overlay,
   the lit-sphere planets in space, the gateway portals, and a **Sable / Aeronautics ship** (Sable is
   mixin-heavy; shaders change the render pipeline). Vanilla-looking fallbacks matter more than pretty.
3. **Keys:** none of the new keys collides with R (runes), K (skill tree), V (character sheet).
4. **FTB Quests** opens, a quest completes, and the team screen works.
5. Distant Horizons' `PRE_EXISTING_ONLY` setting is unchanged.

The server: boots with the FTB set, clients without Iris and Ok Zoomer can still join (both are
client-only), and the dev mirror (`syncDevMods`) skips the client-only jars on the server.

## A map that shows the team (FTB Chunks, installed 2026-10-18)

Sanchit: a minimap that works with FTB Teams so team members see each other, in the world too, even far
away; use a mod, do not build it. Everything below was read from the mods' own changelogs, Modrinth
and FTB's maven, for **Minecraft 1.21.1 on NeoForge**.

| Option | Teammates on the map | Far away | FTB Teams | Verdict |
|---|---|---|---|---|
| **FTB Chunks** `2101.1.22` (655,807 bytes, maven.ftb.dev) | Player heads on the minimap and the full map | **Any range**, not only inside the entity tracking range (its changelog) | **Built on it.** A team setting "Location Visibility" (default Allies: teammates and allied teams only; Public shows you to everyone) and a server setting that overrides it | **Recommended.** Also gives claims, a full-screen map and in-world waypoint dots visible at any range |
| Xaero's Minimap `26.6.0` + World Map `1.47.0` | Only players the game has loaded nearby | No, not without a tracker add-on | No (its party tracker is Open Parties and Claims, which replaces FTB Teams) | A better-looking map, but not a team map |
| JourneyMap `+` JourneyMap Teams (Apache-2.0, 212 KB) | Colours teammates' icons by team colour, and can hide non-teammates | Nothing found that shows far players | Lists FTB Teams as supported | Possible; unclear at range |
| Open Parties and Claims | Party heads on Xaero's maps | Yes | **Replaces** FTB Teams with its own party system | Rejected: we chose FTB Teams (ADR-0023) |

**Installed in Ascension Dev (Sanchit's go-ahead, 2026-10-18):** `ftb-chunks-neoforge-2101.1.22.jar`,
655,807 bytes, from maven.ftb.dev, sha1 `a53f1bb7965c6954a348d4f321f03d3971ad503f` (equal to the hash FTB
publishes, and again after the copy). Read from the jar without starting the game: no nested jars and no
native code; it needs Architectury 13.0.8+, FTB Library 2101.1.34+ and FTB Teams 2101.1.9+, and the instance has
13.0.11, 2101.1.37 and 2101.1.11. It carries **12 mixins** (armor stands, biomes, block states, the chunk map,
fire, flowing fluid, item frames, pistons, vehicles, and on the client the packet listener, `Gui` and a util
class), so it is the first thing to suspect if a boot misbehaves beside Lithium, ModernFix or our HUD. **Not yet
booted**: Sanchit will say when to test.

**Its keys** (read from the code, so they match the audit once it is rerun): Open Map **M**, minimap zoom in
**=** and out **-**, and four unbound: toggle minimap, claim manager, quick add waypoint, waypoint manager.
M is free in the pack. `=` and `-` are also FTB Quests' zoom keys, but those act only inside a screen, so
they do not clash. No change to `pack/config/defaultoptions/keybindings.txt` is needed.

**Its server settings** (`config/ftbchunks-world.snbt`; copy it to `world/serverconfig/` to survive pack
updates), names read from the jar, defaults to be read once it has written the file: `max_claimed_chunks`,
`hard_team_claim_limit`, `max_force_loaded_chunks`, `disable_protection`, `no_wilderness`, `pvp_mode`,
`ally_mode`, `location_mode_override`, `force_disable_minimap`, `long_range_tracker_interval` (how often
far-away teammates are re-sent, the knob if that traffic ever shows in the performance log) and
`waypoint_sharing` (server, party, players). Claims can be limited to nothing by setting the claim limits to 0
or switching protection off. **Two minimaps clash:** its own can be switched off in the
client config if a player prefers another.

**It brings claims with it.** Players can claim chunks and keep them loaded; the team's claim, PvP and
explosion rules are team properties. That is the "claims and base protection" decision `teams.md` left
open, answered by default: they exist unless the server config limits them (to be read from
`ftbchunks-world.snbt` once installed).

**Seeing a teammate as a character, far away, in the 3D world** is a different thing from a map marker,
and the map does not do it: the game stops sending players beyond its tracking range. The one mod found
for it is **SeeU** `0.9.5` (133,405 bytes, Modrinth; needed on the server too): it draws distant players
(pose, armour, held item, name) beyond that range and works beside Distant Horizons. Caveats: it is under
`1.0`, it shows every player, not only the team (each player can opt out of being shown), its licence
allows an unmodified build in a pack with credit and a link, and it has not been tried beside our
entity-culling mods. **Not part of the recommendation** until Sanchit says that is what he meant.

Left out on purpose: Locator Bar and Wayfarer (a direction bar of players, nothing team-aware), Ping Wheel
(pings, a separate wish).

## Loot of your own in every chest (Lootr, installed 2026-10-18)

Sanchit: add the mod that gives each player their own loot from a chest, so a chest is not emptied by whoever
reaches it first. **Lootr** is that mod.

**Installed in Ascension Dev (his explicit yes, 2026-10-18):** `lootr-neoforge-1.21.1-1.11.38.127.jar`, 1,075,929
bytes, from Modrinth (cdn.modrinth.com, project Lootr), sha1 `7a305b581abd90e1a8b7c1bb5eb4511d2653aa58` and sha512
equal to the hashes Modrinth publishes (checked after the download). MIT licence. Needed on **both** client and
server; it has no dependencies. **Booted once on a dedicated server beside the whole stack** (the quest check, 10 of 10, and
Lootr listed as loaded, with its Jade plug-in); the three mixin warnings in that log were already there before it. Not
yet seen in a client.

**What it does.** Every container with a loot table (chests, trapped chests, barrels, shulker boxes, minecarts) gives
each player a separate inventory of its own, rolled from the same loot table the vanilla chest would use, so our loot
modifiers (the Archive's and the colony's enchantment books) still apply. It does not touch chests filled by code
or by hand. Its settings are in `config/lootr-common.toml`, all left at their defaults: it works in every dimension (the
Moon and the Nether included), mineshafts are converted, chests already opened before the mod arrived are not, and
**team loot is off** (a party member gets their own copy, as asked). A `[team] team_loot` switch exists if a party should
share one chest instead; which team mods it reads has not been checked.

**Two things to know.** (1) **Removing Lootr later deletes every converted chest** and cannot be undone, so it is a
decision for the pack, not a toggle. (2) Chests we place that use no loot table are not converted; the Archive's store chests
and the colony chests are filled from loot tables (`ascension_worlds:archive/stores` and `ascension_worlds:nether_colony/stores`, which our enchantment loot modifiers target), so they should be; that is read from the data, not yet seen. Seen in a client:
not yet, and the quest book's lines about chests ("Take what you can carry") need no change.

## Thrusters for rockets (Create Simulated Thrusters, installed 2026-10-18)

Sanchit: add the thruster mod for Create Aeronautics, "that is how we will actually make the rockets for space travel". No
mod is called "Iron Thrusters"; he named **Create: Simulated Thrusters** from the candidates found (Create Propulsion: Simulated,
Gadgets & Gizmos, Linked Thrusters), and asked that it also be on CurseForge (it is). Decision record: ADR-0025.

**Installed in Ascension Dev (his explicit yes, 2026-10-18):** `create_simulated_thrusters-1.21.1-NeoForge-1.1.1.jar`,
298,484 bytes (the page says 291.5 KB), downloaded from CurseForge's CDN (project 1546743, file ID 8220627), sha1
`e8f480fa206e2f958b7fc6a04841ffde09d06ac7`, sha256 `bcaaf4bffa8df9e776a4b786efbdc4d24ce6fa0f8a54828093357a8afe297024`.
CurseForge publishes no hash to compare with, so only the size and a clean zip were checked. **Modrinth carries only 1.0.1
(207,778 bytes); the CurseForge 1.1.1 is the newer file and the one we use**, which is also what the CurseForge app would install.

**What it declares and brings** (read from the jar): requires NeoForge 21+, Create 6.0+, Sable 1.1.3+ and Simulated 1.1.0+
(the instance has Create 6.0.10, Sable 2.0.5 and Simulated 1.3.2 inside Aeronautics). Three blocks:

| Block | Recipe | What it does |
|---|---|---|
| Redstone Thruster | 2 electron tubes, a dropper, 2 repeaters, a shaft | Pushes along its shaft when spun and powered; costs stress |
| Blaze Thruster | 2 iron ingots, 2 engine assemblies, a blaze burner | Burns Create's blaze-burner fuels, costs no stress; redstone pauses it |
| Brass Thruster | 4 brass sheets, a shaft, 2 electron tubes, a Redstone Thruster, a precision mechanism | 3 by 3 by 3, steerable by two redstone inputs, costs stress |

Settings are in `config/create_simulated_thrusters-common.toml` (defaults: redstone 600, blaze 80 / 700 / 1500 for sticks / coal and
logs / blaze cake, brass 4800). **Helium** is made a superheated fuel (the 1500 grade) by one data file in `ascension-compat-create`.

**Cautions.** (1) **Licence: All Rights Reserved** on both CurseForge and Modrinth (its own `mods.toml` says MIT, which looks like
the template's default), no source link: install it in the dev instance, and reference it from a pack, never bundle the jar in
something we publish. (2) It carries a mixin config (`create_simulated_thrusters.mixins.json`), so it is the first suspect if a boot
misbehaves beside Sable. (3) One author, no docs or source: how it treats thin air is unknown. (4) Create Propulsion: Simulated
(MIT, data-driven fuels) is the fallback.

**Seen:** a dedicated server boots with it beside everything, and the quest check (10 of 10) passes with its items named. **Not
seen** in a client; no thruster has been fuelled yet.

## Storage, backpacks and waystones (installed 2026-10-21)

Sanchit, for the alpha: group play needs storage (Tom's Simple Storage lags with many chests), a backpack, and fast travel inside a
world but never between worlds. All six jars were downloaded from CurseForge's own file host on 2026-10-21, checked as intact
zips, and placed in the Ascension Dev instance (67 to 74 jars). Their CurseForge ids are in `pack/curseforge-extras.json`
(the launcher does not track a jar placed by hand). A dedicated server booted with all of them, loaded every one, and the
damage checks still passed (77 of 77). **Not seen in a client.**

| Mod | Version | Size | Needs | Why |
|---|---|---|---|---|
| Sophisticated Core | 1.5.16.2422 | 1.7 MB | NeoForge 21.1.229 or later | the base of the next two |
| Sophisticated Storage | 1.6.7.2193 | 1.8 MB | Core 1.5.12 or later | chests and barrels with upgrades, a controller |
| Sophisticated Backpacks | 3.26.13.2214 | 1.2 MB | Core 1.5.15 or later | a backpack; Curios' `back` slot when Curios is present |
| Tom's Simple Storage Mod | 2.4.2 | 0.8 MB | NeoForge | one terminal for every chest |
| Waystones | 21.1.46 | 1.2 MB | Balm 21.0.39 or later (the pack has 21.0.66) | fast travel inside a world |
| Mouse Tweaks | 2.26.1 | 0.07 MB | nothing | client only |

- **Tom's works with Sophisticated Storage on purpose, not by luck.** Tom's 2.4.2 declares Sophisticated Storage as an optional
  dependency (1.5.36 or later) and has its own class for Sophisticated's double chests (`SophisticatedDoubleBlocks`), so the
  two are meant to be used together. Sanchit's question was whether Tom's alone would then be enough. **It is: Refined Storage 2
  was installed for a few minutes on 2026-10-21 and taken out again at his word** ("we have Tom now"), and is not in the pack.
  If a very large group ever needs storage on disks, it is the one to add (2.0.9, CurseForge 243076, file 8211701).
- **AppleSkin was left out.** It draws on vanilla's food bar, and our HUD hides vanilla's hearts, armour, food and experience
  (`HudClient`, `docs/technical/hud.md`), so it would have nothing to draw on. A held-food preview is on the HUD's own list.
- **Waystones: inside a world, never between worlds.** Waystones decides a warp with an ordered list of requirements
  (`[teleports] warpRequirements` in `waystones-common.toml`); its default lets a warp between worlds through for 27 levels of
  experience. `pack/config/waystones-common.toml` keeps every default and ends with
  `[is_interdimensional] refuse(Waystones only carry you within one world)`. `refuse` and `is_interdimensional` were read from the
  jar's own code; the message is a literal (a leading `$` would be a translation key). **The refusal is configured and read from
  code, not yet seen:** place a waystone on Earth and one on another world, and try each kind of warp (a stone, a scroll, a plate, the
  inventory button). The reasoning is the gates' (ADR-0015, ADR-0018): a waystone on the Moon that reached one on Earth would be a gate nobody built.
  Waystones also generates wild waystones (in villages, and its allow list names the Overworld, the Nether and the End); travel inside
  a world is allowed, so they stay.
- **The server needs the same files.** The server pack must carry `pack/config/` as well as `serverpack/config/` (the Waystones rule
  is a server decision, and Waystones syncs its config to clients).

## Chat noise (2026-10-18)

Two lines kept appearing in the chat of the dev client. **"Distant Horizons: Chunky detected"** is real
and would reach players: it is turned off in the pack's `DistantHorizons.toml`
(`pack/config/`). **"Hello from FTB Library!"** is FTB Library's own greeting, hardcoded to show only in
a development environment, so no player sees it; the dev client hides it (`client/dev/DevChatFilter`).
Both were checked gone in a dev client that joined the test world.

## What this is for

Once these run beside our mods, the **team** and **quest** design can start. Facts that will shape it:

- **Teams today are the whole server.** Growth's shared milestones (a boss's first kill, a lit gate)
  go to every player on the server, late joiners included (ADR-0021; `TeamGrowthState` says it is
  "the whole server until Ascension has teams"). FTB Teams is the natural source of a real team.
  A Tier-2 `ascension-compat-ftbteams` could make those milestones per team, and Bulwark's
  team runes and share-air could use the same team id. **Designed 2026-10-18 as a proposal, awaiting
  Sanchit: [`docs/progression/teams.md`](../progression/teams.md)** (who shares what, joining and
  leaving, what a solo player is).
- **The quest book is built** (2026-10-18): pages, voice rules, the Planets chart and its live marker are in
  [`docs/progression/quests.md`](../progression/quests.md). Quests add no skill points; the rewards are items.
- **Quests can feed growth with no code.** Our milestones are advancements; an FTB Quests reward can
  run `/advancement grant @p only <id>`, and a quest task can be "have this advancement". So a
  quest chapter can award skill points, exactly as a boss kill does, from data.
- **Quest files are text** (SNBT in `config/ftbquests/`), so quest lines can be written and reviewed
  here, not only clicked together in the editor. **Settled 2026-10-18:** the source is `pack/quests/` (Python),
  generated into `pack/config/ftbquests/quests/`, which `tools/sync-pack-config.py` copies into a game.
- **No world is ever required** (ADR-0018): quests *reward* going somewhere, they never gate a path.
