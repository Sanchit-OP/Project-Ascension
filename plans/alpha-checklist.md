# Alpha launch checklist

First written 2026-10-19 at Sanchit's request ("lets list down what all we need to finish before the alpha launch"),
then **revised the same night with his answers** (below). Built from `todo.md`, `plans/m3-vertical-slice.md`,
`docs/progression/journey.md` section 5, `serverpack/README.md`, `docs/technical/pack-extras.md` and the CurseForge
instance's own manifest. Nothing here is new design except Planet 4, which is Sanchit's addition. Sizes are guesses.

## What the alpha is (decided by Sanchit, 2026-10-19)

| Question | Answer |
|---|---|
| Who plays it | Friends first, and **anyone can download it and play** |
| Platform | **CurseForge** |
| Server | **We run it ourselves** |
| Ship air | **A blocker** (see A4 for why) |
| First-launch defaults | **We bundle `options.txt`** |
| How far it goes | Earth, Moon, Planet 3 (the Nether), and **Planet 4: the End as an asteroid belt, with the Ender Dragon and the Elytra.** Reaching the dragon and the elytra is the alpha's good stopping point |

So the bar is: *a stranger can install it from CurseForge, start a world, play from Day 1 to the Ender Dragon without
us standing next to them, and when it breaks we hear about it.* Two things stay the alpha's job to find out, not ours
(Sanchit, 2026-10-06 and 2026-10-18): **two clients** and **whether a starter ship design is needed**. Planet 4 raises
the stakes on the second, because the End is the farthest place a ship has to reach (A4, A8).

## Status check, 2026-10-21 (main at `a7577ec`, Planet 4 merged)

**What a machine has proved today** (on `main`; none of it has been seen by a person):

| Check | Result |
|---|---|
| `./gradlew build` (every module's tests, balance, data, ledger and quest checks) | passes |
| Quest generator tests and `--check` | 41 tests OK; 13 pages, 151 quests, no problems |
| Dedicated server, `damage-checks.txt` (damage kinds, the six bosses and the Dragon's health) | 77 checks, 0 failures |
| Dedicated server, `end-checks.txt` (the Dragon, the gate, the exit portal, travel, the fall to space) | 18 checks, 0 failures |
| Dedicated server, `quest-checks.txt` (the real FTB Quests loads the book) | 10 checks, 0 failures; 13 chapters, 493 ids, 152 dependencies (the dev server's copy of the book was a chapter behind until synced) |
| Dedicated server, `create-checks.txt` (Create's air gear gone) | 22 checks, 0 failures |

| Item | Status | What is left |
|---|---|---|
| **A1** combined client run | **not done** (the Planets page was seen once on 2026-10-20: it found the spacing bug, fixed in `2746f74`) | everything on the list |
| **A2** gate by hand, keystone, a survival ship to the Moon | **not done** | all three; the ship is the biggest unknown |
| **A3** a combat item finished, balance in a real fight | **not done** | |
| **A4** ship air | **not done**; `OxygenChecks` still not written | a real ship ride that holds air; a server check; a reload |
| **A5** CurseForge pack | **not started.** Found today: CurseForge ids for the two hand-installed mods (Lootr 361276, file 8968561; Create: Simulated Thrusters 1546743, file 8220627). Still missing: `LICENSE`, a pack `options.txt` (`pack/config/defaultoptions` has only keybindings), a manifest builder, a config audit, a changelog | all of it |
| **A6** our server | **not started** (`serverpack/` is configuration only) | start script, mod set, whitelist, backups, pre-generation plan |
| **A7** bug reports and known issues | **not started** | |
| **A8** Planet 4 | **built and merged** (ADR-0028; the design redo: a real belt, five ruins, eight tablets, the lore; the Planets page now draws the belt) and **headless-checked**; **never seen in a client** | the client run (`docs/technical/planet-4-client-checklist.md`), the outer islands' cost, the Elytra in a real End city, a ship with a real pilot |

**Can it be released today?** A *friends-only* alpha today is possible only if two things go right in the next few hours: one
client session (A1, A2 and A4 together, with Planet 4 at the end) finds nothing that stops a stranger, and the pack and the server
are built in parallel (A5, A6, A7: nothing there needs the game). A *public CurseForge listing* today is unlikely: a new modpack
project goes through CurseForge's own review, which we do not control, and publishing is Sanchit's action. The first step that
decides everything is the ship (A2, A4): if a survival ship cannot reach space and hold air, there is no alpha to release.

## The rule for this alpha: no dead ends (Sanchit, 2026-10-21)

The alpha is itself the test of many things, so **an intended mechanism may fail; a player must still have a way through.** If a
player cannot give air to another, they carry their own tanks. This table is that rule applied to every step that could stop a
stranger. "Verified" means checked by a machine; none of it has been tried by a stranger.

| If this fails | The way through | State |
|---|---|---|
| **A survival-built ship cannot reach space** (the one real dead end: the road up is closed on foot while a ship mod is installed) | The server setting `ascent.vehicleOnly = false` (`ascension_worlds-server.toml`; in a single-player world, its `serverconfig` copy) lets a player rise past the build height on foot (a tower, fireworks) and be carried to space. On our server we flip it for everyone. For downloaders it is one line in the known-issues page | The setting is read in code (`AscentCarriers.onFootAllowed`, whose closed cases are unit-tested). Not tried in a client |
| **A ship does not hold air** | **Tanks.** Lungs 20 s plus two tanks of 300 s is 620 s (a helium tank holds twice: 1,220 s with two). At 4,000 blocks the Moon needs 7 blocks a second; the Nether (18,000) 29, or 15 on helium; the End (32,000) 52, or 26 on helium. Or hop by planet and refill at each landing with an Air Compressor, which cuts every leg | Numbers from `AtmosphereTuning` and `worlds-api.md`; no ship has been timed |
| **Players cannot give each other air** | Each carries their own tanks | Share-air is built, never seen with two players |
| **The gate cannot be built or the key does not drop** | Gates are shortcuts, not requirements (ADR-0018): a ship goes to any world, and one can also fly home. On our server an operator can light one: `/ascension worlds gateway light <planet>` | Gates seen working 2026-10-06; the Dragon's gate checked by `end-checks` |
| **Quests are wrong or one cannot be finished** | The book never gates a path | `progression_mode: "flexible"` is tested on every build |
| **A combat item cannot be made** | Items reward, they do not unlock (ADR-0018): vanilla and netherite gear, growth points and runes still carry a player | Recipes checked by machine, never crafted by hand |
| **The titanium bearing is not craftable in survival** (brass needs a Nether blaze, and the Nether needs a ship: a circle) | `ascent.vehicleOnly = false`, as in the first row | The recipe is proven loaded and its tags resolve; not crafted by hand |
| **Falling out of the End** | Slow Falling, then the game carries you into space beside the planet | `end-checks`, 18 of 18 |
| **Shaders break the sky or planets** | Shaders are optional and ship **off**; take the pack out | See "Seen in Sanchit's test" |
| **Chunk generation lags the server** | Slower, not stuck; pre-generate a planet by hand if it hurts | Chunky's background pre-generation is off (crash unexplained) |
| **Air Compressor or Create power is broken** | The old refill station has no recipe, so this is a real dependency: lungs only on Earth, tanks as they are | The compressor was seen working in Sanchit's client |

Our own server has a safety net the public does not: an operator can fix any single stuck player with a command, so the
real test of the rule is a **single-player download**, where only the rows above help.

## Seen in Sanchit's test, 2026-10-21

He has played Planet 4 and saw two faults, both with a shader pack on:

1. **Clouds in space.** `SpaceSpecialEffects` removes vanilla's clouds and `ascension-compat-distanthorizons` removes Distant
   Horizons', but a shader pack draws its **own** clouds in any dimension it does not know. We cannot reach those from here.
2. **The planets' edges look like a chainsaw blade.** Our lit planets are custom sky meshes (`LitSphere`: a 48-segment ring
   mesh textured with a circle, plus an additive halo); a shader pack redraws sky geometry through its own program, and the
   halo's soft edge or the blend is the likely cause. Needs a screenshot with shaders on and the same view with them off.

**For the alpha:** ship no shader pack selected, say shaders are **not supported in space** in the known-issues page (Create
Aeronautics' own page already warns of Iris problems), and look at the planets' edges with shaders on after the alpha starts. Both are
cosmetic; neither blocks a player. **To confirm:** do the planets look right with shaders off?

## Added to the alpha, 2026-10-21 (Sanchit)

| Want | What was done | State |
|---|---|---|
| **Group storage that does not lag** | **Sophisticated Storage** (with Core), **Tom's Simple Storage Mod** and **Refined Storage 2** are installed. Tom's declares Sophisticated Storage as an optional dependency and has its own class for its double chests, so Tom's alone would have worked with the Sophisticated chests; Sanchit chose to have Refined Storage too. It is the first thing to cut if the alpha shows trouble | In the instance (67 to 74 jars); a dedicated server loads them; **not seen in a client** |
| **A backpack** | **Sophisticated Backpacks**, worn in Curios' `back` slot. ADR-0009 already accepts backpacks | Same |
| **Fast travel inside a world, never between worlds** | **Waystones**, with `pack/config/waystones-common.toml` refusing any warp between worlds (the rule is read from the jar's own code) | Configured; **the refusal is not yet seen**: it is on the next client list |
| **Curios slots**: the tank, Zonia's Hourglass and Sterak's Harness instead of the left hand, and `back` | `ascension-compat-curios`, prompt [`plans/session-prompts/11-curios-slots.md`](session-prompts/11-curios-slots.md). **Not a blocker**: the tank works in the inventory and the two relics in the off hand today | Not built |
| **Quality of life** | **Mouse Tweaks** only. **AppleSkin is out**: it draws on vanilla's food bar and our HUD replaces it. Not now: a minimap (FTB Chunks has a map), dynamic lights, Inventory Profiles Next | Mouse Tweaks installed |

The CurseForge ids of every mod the launcher does not track (these seven, Lootr and the thrusters) are in `pack/curseforge-extras.json`.

## A. Blockers: do not release without these

### A1. The one combined client run (nothing below has been seen by a person)

ADR-0008 says nothing is done until seen in a client. Sanchit's own rule (journey.md 5.7): **one run at the end,
covering everything**, not a launch per tweak. Fresh world, current jars (compare the instance before overwriting).

- Quest book pages 3 to 12 (only First Days and Build the Gate were ever seen); the welcome line on first login;
  `QuestScreenChecks` to be extended last.
- Planet 3 whole: the roofless Nether, the rot and gold ward, the gate ruin, the Monstrosity's key, the seven tablets.
- Magic-hit tint (drawn in a client for the first time), Ragefang and Endless Hunger in real fights, the 26 item
  tooltips, gold names, sounds, light beams, boss bars and Max Health readouts above 1,024 (bosses were cut by 35% on
  2026-10-19: Warden 975 to Wither 4,225 on Normal).
- Create's removed air gear is gone from the creative menu and JEI (server-proven only).
- Build paths: recipes, the JEI page, a boss heart in a forge chain (merged 2026-10-18, never seen).
- Lootr (one chest per player), Create Simulated thrusters burning helium, enchantment tooltips, Soulbound, tomes.
- Oxygen v2 aboard a ship (A4), and Planet 4 (A8) once it exists.
- A save, quit and reload in the middle of all of it.

### A2. The spine is walkable with only what the book teaches

The risk that matters most: a stranger gets stuck at step N with no way forward. Three steps have never been done
by hand:

- **Hand-build the gate in survival** from the book's plan, block by block, and see it pass every rule.
- **Warden tendril drop, then craft the keystone** (built, never crafted in a client).
- **A ship built from survival parts that climbs past y 320 and reaches the Moon** (journey.md 5.1). Every crossing
  seen so far was command-built. This is the biggest unknown in the pack. The alpha is meant to *decide* whether a
  starter ship is needed, but one of us should try it first, so we know the answer is "works", "works badly" or
  "impossible"; the last would make the whole alpha pointless.

Then one straight playthrough: first day, gate, ship, Moon, Archive, Moon bosses, key, Planet 3, Planet 4.

### A3. The 26 combat items must be obtainable and not break the game

Build paths exist but have never been seen. Six bosses each give a heart today (the Warden, the Wither and Cataclysm's
four); the Dragon (A8) would be a seventh, and whether it gives one is Planet 4's call. Confirm a player can actually
finish one item, and that the balance numbers survive a real fight (Cataclysm's weapons are untouched and out of balance with ours: decide whether that matters for alpha).

### A4. Ship air (Sanchit: blocker)

**Why it blocks.** Space has no air. A player lives on lungs (20 s) plus two tanks, about **620 seconds** in all
(journey.md 5.2). The trip to Planet 4 is 32,000 blocks from Earth (26,215 from Planet 3, `worlds-api.md`), so a ship
needs to do roughly 52 blocks a second just to arrive with nothing left, and a round trip is impossible on tanks.
Nobody has ever measured a survival ship's speed. A ship that is itself a sealed room with an Air Dispenser aboard
(Oxygen v2) removes the clock: the crew breathes for the whole flight. Without it, the Nether is borderline and the End
may be unreachable. That makes this the thing standing between the alpha and its stopping point.

**Why it is not done.** Oxygen v2 works in singleplayer on the ground (Sanchit saw it). Aboard a ship it is three
unproven guesses about Sable, the mod that moves ships: that a ship's blocks keep their own coordinates while the ship
turns, that placing or breaking a block inside a ship is reported to our code, and that the room survives the crossing
into space. If any is wrong, the cabin silently stops holding air and a crew dies mid-flight. The only test is a real
ship ride (`docs/technical/oxygen-v2.md` lists the three).

Still owed:

- A ship ride that holds air for the whole flight, Earth to the Moon and back, then to Planet 3.
- A dedicated-server check (`OxygenChecks` was never written) and a world reload with a filled dispenser.
- A performance-log entry (`docs/technical/performance-log.md`).
- Quest book lines "A long flight" and "Share your air" rewritten once a ship is seen holding air.

### A5. Distribution on CurseForge (decided: CurseForge, free download)

A CurseForge modpack is a zip with `manifest.json` (every mod by CurseForge project and file ID) and an `overrides/`
folder (our configs, quests, `options.txt`, and our own jars). Found by reading the dev instance's manifest
(`minecraftinstance.json`) on 2026-10-19:

- **Three mods have the CurseForge "third-party distribution" switch off** (`allowModDistribution=false`): **Create
  Aeronautics**, **Entity Culling** and **Keybind Atlas**. **This does not stop us using them** (rechecked
  2026-10-20; the first version of this list made it sound worse than it is). The switch only stops *other* tools
  (server hosts, non-CurseForge launchers) from fetching those files through CurseForge's API. What was checked:
  - The launcher's own record for all three says a download link exists, the file is available, and nothing blocks
    exporting a pack (`downloadUrl` set, `isAvailable` true, `exportDisabledReason` 0).
  - There is already a modpack on CurseForge built around Create Aeronautics ("Aeronautics Unleashed", 2026), so
    packs that list it are accepted. It has been **listed by project ID, not bundled**, which is also our plan.
  - Create Aeronautics' licence: code MIT, art and other assets All Rights Reserved. A pack that *lists* it is the
    normal route; a pack that *copies its jar into itself* would be the questionable one. Same for Lootr (code MIT,
    assets All Rights Reserved) and Keybind Atlas (All Rights Reserved).
  - Entity Culling's licence ("tr7zw Protective License") lets you use it freely but not for "commercial advantage or
    monetary compensation". A free pack is fine; if we ever enrol the pack in CurseForge's rewards programme, check
    that first, or drop it (it is a small optimisation).
  - **What it does change:** never put those three jars in `overrides/`, and **our own server needs them copied by
    hand** from the instance (we run it, so this is fine). Players using the CurseForge app are unaffected; players on
    another launcher may be asked to download them by hand.
  - Still worth a **trial export and install on a clean profile early**: it costs an hour and proves all of the above.
  - **New from Create Aeronautics' own page: it lists "visual issues when used with Iris Shaders."** Our pack ships
    Iris and two Complementary shader packs, and ships are the road to space. Test a ship under shaders in the A1
    client run, and decide whether the default profile has shaders off.
- **19 of the 67 jars are not tracked by the launcher**: our 17 `ascension_*` jars, plus **Create Simulated Thrusters**
  and **Lootr**, which were installed by hand. Both are on CurseForge (checked 2026-10-20: "Create: Simulated
  Thrusters", about 96K downloads, and "Lootr (Forge & NeoForge)", about 170M), so re-add them through the CurseForge
  app and they get project IDs. Our own 17 jars go in `overrides/mods/` (they are ours), or become CurseForge projects
  of their own if we want separate update pages.
- **Licence:** `gradle.properties` says `mod_license=MIT` but the repo has **no `LICENSE` file**. Add one and decide
  whether MIT is really the choice (it is the property today; the pack is free to download).
- **Version and changelog:** `mod_version` is `0.1.0`. Use `0.1.0-alpha.1` or similar, and a changelog.
- **Bundled `options.txt` (decided)** so nobody meets the accessibility prompt first: `seedDevGameOptions` only covers
  our dev runs, so the same defaults must go in `overrides/` (or use Default Options, which is already in the stack).
  Include sensible video settings for the shader and Distant Horizons setup.
- **Recommended RAM and Java 21**, which shaders are optional (Complementary Reimagined and Unbound are in the
  instance), and what the pack needs on first launch (it generates a world with custom dimensions).
- CurseForge needs a **description crediting any mod hosted elsewhere** and pack art/icon; plan for the review wait.

### A6. The server we run ourselves

`serverpack/` has config only: no start script, no pinned JVM flags file, no server-pack zip.

- Build the real server: NeoForge 21.1.x on Java 21, the same mods as the pack, including the three
  distribution-disabled jars, started with generational ZGC (`serverpack/README.md`), a restart script, and backups.
- `online-mode=true` and `white-list=true` (the dev server runs `false`; see `serverpack/README.md`).
- **Chunky's pre-generation crash is unexplained** (a fresh server filled a 6 GB heap in six minutes). It is off by
  default, so a server is safe but new areas generate live. A busy server with several players travelling to a new
  planet will feel it. Find the cause, or pre-generate each planet by hand in small steps before opening.
- Who is operator, how players are whitelisted, how bugs reach us, and how we update the world after a pack update
  (a world keeps the dimensions it was created with, so changes to worldgen need a new world: say so in the notes).
- "Multiplayer and server operations" is on todo.md's missing-documentation list; write it from this.

### A7. A bug-report path and a known-issues page

Where do testers send crash logs, which files to attach (`latest.log`, `crash-reports/`), and what we already know
about (late-joiner gap, ships, Cataclysm weapons). Without it the alpha produces rumour, not bug reports. A Discord
channel or a CurseForge comments link is enough.

### A8. Planet 4: the End as an asteroid belt, with the Ender Dragon and the Elytra (Sanchit's addition)

**The idea.** The vanilla End, as the fourth planet: islands floating in the void are the asteroid belt, the Ender
Dragon is the boss, and the End cities' elytra are the prize and the way to cross the gaps. Reaching the dragon and the
elytra is the alpha's stopping point.

**What is already in our favour.**

- ADR-0014 always intended the End to become a planet; its portal is dormant now, and **ADR-0017 (the real Nether,
  opened to the sky) is the template**: a vanilla dimension overridden by data, a gate ruin, a key dropped by the
  boss, a survival rule, an art session.
- The planet table already has the slot: Planet 4 at 32,000 blocks, bearing 110°, `[-10945, 30070]`
  (`worlds-api.md`). Planets are data (`planet/*.json`); a ship needs a `dimension_physics` file in
  `ascension-compat-sable`, as Earth, the Moon and the Nether have.
- Elytra answers the rule that every mandatory world has a movement wrinkle (`planets.md`); ADR-0015 only says elytra
  are useless for leaving a planet, which stays true.
- It pays the End half of the recipe debt (ADR-0014: elytra, shulker boxes and other End-only items are unobtainable
  until the End is a planet; todo.md "Systems gaps").

**The session prompt for it is [`plans/session-prompts/9-planet-4-the-end.md`](session-prompts/9-planet-4-the-end.md); what every planet needs
(the Nether's files, as a checklist) is [`docs/technical/new-planet-checklist.md`](../docs/technical/new-planet-checklist.md).**

**What has to be built or decided.** The first item is a conflict that needs Sanchit:

1. **An ADR that gives Planet 4 to the End.** `planets.md` and `lore-spread-proposal.md` currently make Planet 4 a
   *predator world* where "the keepers were made" (the Warden's makers, tied to armour materials). The proposal here
   is that the predator world slides to Planet 5 and the End takes slot 4. ADR-0005's "v1 is Earth, Moon, Planet 3" also
   grows by one world for the alpha.
2. **The planet itself:** `planet/end.json`, the planet's name on the gate screen, a space look (`ascension_space`), a
   `dimension_physics` file for ships, and whatever sky or fog art "asteroid belt" means (art session, like the Nether).
3. **Arrival:** a ship coming down onto the End must land on rock, never above the void, and the gate ruin needs an
   island to stand on (not inside the dragon's arena at 0,0). Falling into the void is death.
4. **A survival rule.** The Nether has the rot and breathable air. The End is open: is the air breathable, or is it a
   vacuum that needs tanks (the Moon's rule)? See the question below.
5. **The Ender Dragon as a balanced boss:** a row in `balance.md` section 5 (Health, Armor, Magic Resist, damage per
   difficulty, Normal / Easy / Hard), a tier tag, a check in `damage-checks.txt`, and a decision on the healing crystals.
   The balance doc already reserves the space ("a later world's boss goes below the Wither"). The Dragon's kill drops a
   gateway key naming `minecraft:the_end`, as the Moon and the Nether bosses do.
6. **What the End's exit portal does.** Vanilla sends the player to world spawn and plays the credits after the first
   kill. That must not happen (ADR-0014 only stops trips *into* the End). It should return the player to the planet's
   gate or arrival point, with no credits (keep them for the real ending).
7. **The Elytra:** vanilla places them in End city ships on the outer islands, which only exist after the dragon dies
   (the End gateways). Confirm that works when players arrive by gate and not by the vanilla portal, that Lootr
   converts those chests, and that loading far-away islands does not hurt a server (A6, the Chunky memory problem).
8. **The quest book, `journey.md` and the lore:** a Planet 4 page, a lore pass (`docs/lore/the-end.md`, like
   `the-nether.md`: the Endermen are tied to the Observers, so the End is the place to say something about them), and
   tablets if the Archive pattern is reused.
9. **Verification** (ADR-0008): a dedicated server with a stand-in player for the numbers, then one client run from the
   Nether's gate to the dragon's death, the elytra and back, and a reload; a performance-log entry for the new
   dimension and the dragon fight.

**Effort (guess).** Less terrain work than the Nether (the End's generator is vanilla's), but a new boss, an exit
portal, arrival and survival to design. About an ADR and lore session, a logic session, an art session, then the client
test. It sits behind A2 and A4: nobody can reach it until a survival ship flies and holds air. **Planet 4 is the
largest item on this list and the one most likely to move the alpha date.**

**Status, 2026-10-21 (merged to `main`):** built and checked on a dedicated server: the ADR (0028), the planet entry, Sable
physics, the arrival point, the gate (the Dragon's exit portal), the fall back to space, the Dragon's stats, the quest page "The Belt", the
lore prose (`the-end.md`), and the design redo (a real asteroid belt from space, five ruins, three carvings, eight tablets). The Planets
page draws the End as its belt (2026-10-21). **Left:** the End's sky and haze (still vanilla's), a client run
(`docs/technical/planet-4-client-checklist.md`), a spark/pre-generation measurement, the Elytra in a real End city, and a ship with a real
pilot. The growth total rose from 35 to 41 points (`growth.md` section 1).

## B. Should have (an alpha can go out without these, but it will hurt)

- **A two-client smoke test before release**, even though the alpha is itself that test. Oxygen is per-player state and
  no session has ever had two players; twenty minutes with a friend catches the embarrassing class of bug. The server
  will have several players from day one.
- **Late-joiner gap:** a player who joins a team that is ahead gets the dimension and none of the gear to survive it.
  Needs a catch-up mechanism (todo.md "Systems gaps"); at minimum say so in known issues. With a public download this
  will happen on the first weekend.
- **Easy and Hard boss totals** (only Normal was run on a server).
- **Quest book Gear and Growth page:** the forge chain and FTB reward tables ("choice of one of three"); the generator
  does not write reward tables yet.
- **Performance measurements** owed by ADR-0008: two players on a dedicated server, Oxygen v2 in a room, a spark
  profile of a full session with the whole stack, and the dragon fight. The log's newest reading predates build paths,
  balance and Oxygen v2.
- **Distant Horizons** dimension-change oddities on the way to space.
- **Shaders and ships:** Create Aeronautics' page lists visual issues with Iris Shaders (A5); see a ship under a
  Complementary shader before deciding the default.
- **Parked balance questions:** one-legendary carry limit (Sanchit decides after testing), flat versus percent weapons
  against late bosses, Cataclysm stat extras. State the current behaviour in known issues.

## C. Can wait until after the alpha

- Planets 5 to 7 (the predator world, the forge world, the war-machine world, the farthest gate).
- The ending (A the Eye opens / B the seal / C the mirror), faction names, the lone human on the beach.
- HUD chart for pilots in space (needs a `core` contract and a packet); the HUD team list (T4).
- A starter ship design and a sky-dock schematic ruin (the alpha decides).
- The no-Sable fallback ship, airlocks and sealing doors, the helium lamp, the orrery hologram, cats, the keeper shrine
  placement, a spinning-shaft visual on the Air Compressor.
- Curating Earth's modded ore spawns (mechanism proven, nothing to remove yet).
- Gravity per planet, failed-landing handling.
- Cleanup that is Sanchit's to call: deleting the remote branch `origin/combat-hud`.

## Suggested order

1. **Prove the road first (A2, then A4).** A survival ship that flies, then a ship that holds air. Everything after
   depends on them, including Planet 4, and either could turn out to need design work. One short client session each.
2. **In parallel, no game needed:** the trial CurseForge export and clean-profile install (A5), the licence file,
   version and changelog, the bundled options, the real server on a spare machine or profile (A6), known issues (A7).
3. **Planet 4's ADR and lore pass (A8 steps 1 and 8), then its logic and art sessions,** while the road is proven. The
   design sessions do not need the road; the client test of Planet 4 does.
4. **Server-side leftovers:** `OxygenChecks`, Easy and Hard boss runs, the dragon's balance row and checks.
5. **A1, the one combined run**, fresh world, from Day 1 to the dragon's death, with a save and reload.
6. **Fix list from A1**, a refactor pass (ADR-0008: every third increment), the performance-log entry, the two-client
   smoke test.
7. **Tag, build the release, install it from CurseForge on a clean machine or profile** as a stranger would, on the
   real server. That last step is the one most likely to find a missing dependency or config.

## Planet 4's four questions: answered (ADR-0028, 2026-10-20)

Breathable air in the End; the predator world slides to Planet 5; the Dragon's exit portal is the End's gate (it opens the
destination screen, no credits); a lore pass first (`docs/lore/the-end.md`, now prose).

## Still open for Sanchit

1. **The licence**: add a `LICENSE` file (MIT is what `gradle.properties` already says) or choose another.
2. **The pack's name, version string and CurseForge description** (suggested version: `0.1.0-alpha.1`).
3. **Friends first, or the public listing at once?** A zip the CurseForge app can import reaches friends today; the listing waits for review.
4. **Which of `docs/technical/planet-4-client-checklist.md` has he already seen?** (Nothing in it is ticked.)
5. **Which dev-instance configs ship.** The instance has about fifty files in `config/` (Better Combat, Aeronautics, the thrusters, Entity
   Culling, our own `ascension_*` files). The pack should ship the deliberate ones and none of the per-player ones; this needs a pass.

Nothing on this list needs an ADR except the licence and distribution choices (ADR-0029 when decided).
