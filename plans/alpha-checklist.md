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

- **Three mods disallow third-party distribution** (`allowModDistribution=false`): **Create Aeronautics** (the road to
  space depends on it), **Entity Culling** and **Keybind Atlas**. The CurseForge app can still install a pack that
  references them, but tools that download packs automatically cannot (this is what the search of CurseForge's own
  docs and launcher tools says; the exact submission rule for them is not confirmed). Two consequences: never put their
  jars in `overrides/`, and the **server** needs them copied by hand from the instance (we run it, so this is fine).
  **Do a trial export and install on a clean profile early**, because a refusal late would be expensive.
- **19 of the 67 jars are not tracked by the launcher**: our 17 `ascension_*` jars, plus **Create Simulated Thrusters**
  and **Lootr**, which were installed by hand. The last two need re-adding through the CurseForge app so they get
  project IDs (`pack-extras.md` says Thrusters is on CurseForge). Our own 17 jars go in `overrides/mods/` (they are
  ours), or become CurseForge projects of their own if we want separate update pages.
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

## Still open for Sanchit (needed before Planet 4's design starts)

1. **Air in the End: breathable or a vacuum?** Recommendation: breathable, as the Nether is. The dragon fight is hard
   enough, and a tank clock ticking during it will read as unfair; the asteroid-belt feel comes from the sky and the
   void. The alternative is a vacuum, which makes the End the first world with a pressurised arena.
2. **Does the predator world slide to Planet 5?** The proposal in A8 item 1. Or is the End a side planet (a bonus
   stop that is not numbered)?
3. **After the dragon dies, where does its exit portal go?** Recommendation: back to the End's gate or arrival point,
   no credits, and keep the credits for the real ending.
4. **Is a lore pass first, as for the Nether?** Recommendation: yes, one short `the-end.md`, because the Endermen are
   the Observers in our lore and the End is where players meet them in numbers.

Nothing on this list needs an ADR yet, except the licence and distribution choices and Planet 4's slot (record them as
ADR-0028 and ADR-0029 when decided).
