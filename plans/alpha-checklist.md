# Alpha launch checklist

Written 2026-10-19 at Sanchit's request ("lets list down what all we need to finish before the alpha launch").
Built from `todo.md`, `plans/m3-vertical-slice.md`, `docs/progression/journey.md` section 5, `serverpack/README.md`
and `docs/technical/pack-extras.md`. Nothing here is new design: it is the open items already written down in
those files, sorted by whether an alpha can go out without them. Sizes are guesses, not measurements.

## What the alpha is for (proposed, Sanchit to confirm)

An alpha is where outside players find what we cannot find alone. Two things are already decided to be the alpha's
job, not ours (Sanchit, 2026-10-06 and 2026-10-18):

- **Two clients** (a party, share-air, friendly fire, a late joiner) was deferred "to the alpha release".
- **Whether a survival-built ship can reach space**, and so whether a starter ship design or a schematic ruin is
  needed. "Leave it. Players try it in the alpha."

So the bar is **not** "everything verified". It is: *a stranger can install it, start a world, play the spine from
Day 1 to the Moon and Planet 3 without us standing next to them, and when it breaks we hear about it.*

## A. Blockers: do not release without these

### A1. The one combined client run (nothing below has been seen by a person)

ADR-0008 says nothing is done until seen in a client. Sanchit's own rule (journey.md 5.7): **one run at the end,
covering everything**, not a launch per tweak. Fresh world, current jars (compare the instance before overwriting).

- Quest book pages 3 to 12 (only First Days and Build the Gate were ever seen); the welcome line on first login;
  `QuestScreenChecks` to be extended last.
- Planet 3 whole: the roofless Nether, the rot and gold ward, the gate ruin, the Monstrosity's key, the seven tablets.
- Magic-hit tint (drawn in a client for the first time), Ragefang and Endless Hunger in real fights, the 26 item
  tooltips, gold names, sounds, light beams, boss bars and Max Health readouts above 1,024.
- Create's removed air gear is gone from the creative menu and JEI (server-proven only).
- Build paths: recipes, the JEI page, a boss heart in a forge chain (merged 2026-10-18, never seen).
- Lootr (one chest per player), Create Simulated thrusters burning helium, enchantment tooltips, Soulbound, tomes.
- Oxygen v2 aboard a ship (see A4).
- A save, quit and reload in the middle of all of it.

### A2. The spine is walkable with only what the book teaches

The risk that matters most: a stranger gets stuck at step N with no way forward. Three steps have never been done
by hand:

- **Hand-build the gate in survival** from the book's plan, block by block, and see it pass every rule.
- **Warden tendril drop, then craft the keystone** (built, never crafted in a client).
- **A ship built from survival parts that climbs past y 320 and reaches the Moon** (journey.md 5.1). Every crossing
  seen so far was command-built. This is the biggest unknown in the pack. Even if the alpha is meant to *discover*
  it, one of us should try it first, so we know whether the answer is "works", "works badly" or "impossible"; the
  last of those would make the whole alpha pointless.

Then one straight playthrough: first day, gate, ship, Moon, Archive, Moon bosses, key, Planet 3.

### A3. The 26 combat items must be obtainable and not break the game

Build paths exist but have never been seen. The Moon and Planet 3 bosses are the only sources of hearts.
Confirm a player can actually finish one item, and that the balance numbers survive a real fight (Cataclysm's
weapons are untouched and out of balance with ours: decide whether that matters for alpha).

### A4. Oxygen v2 leftovers

Built and seen in singleplayer except ships. Still owed:

- Ship air: Sable plot transform, block-edit events inside a plot, carry to space (all assumptions in
  `docs/technical/oxygen-v2.md`).
- A dedicated-server check (`OxygenChecks` was never written) and a world reload with a filled dispenser.
- A performance-log entry (`docs/technical/performance-log.md`).
- Quest book lines "A long flight" and "Share your air" rewritten once a ship is seen holding air.

### A5. Distribution: how a stranger gets the pack

Nothing is decided or built here, and it is the longest tail.

- **Format:** CurseForge and/or Modrinth modpack, or a plain zip? Mods are referenced, not bundled
  (`pack-extras.md`: Create Simulated Thrusters is All Rights Reserved; shaders also come from the platforms).
- **Our own jars** need a home (a GitHub release, or the platform upload) and a version number (`mod_version` is
  `0.1.0` today; an alpha should be something like `0.1.0-alpha.1`, with a changelog).
- **Licence:** `gradle.properties` says `mod_license=MIT` but the repo has **no `LICENSE` file**. Needs one, and a
  decision on whether it is really MIT.
- **First-launch defaults:** `options.txt` is seeded only for our dev runs (`seedDevGameOptions`). Players would see
  the accessibility onboarding before the main menu. Decide: bundled `options.txt`, a config mod, or ours.
- **Recommended RAM and Java** (21), launcher instructions, which shaders are optional.

### A6. A real server, if the alpha includes one

`serverpack/` has config only: no start script, no pinned JVM flags file, no server-pack zip.

- `online-mode=true` and `white-list=true` must be set (the dev server runs `false`; see `serverpack/README.md`).
- **Chunky's pre-generation crash is unexplained** (a fresh server filled a 6 GB heap in six minutes). It is off
  by default, so a server is safe but a new world generates every chunk live; fine for alpha if we say so, and note
  the cause is still open.
- "Multiplayer and server operations" is on todo.md's missing-documentation list.

### A7. A bug-report path and a known-issues page

Where do testers send crash logs, which files to attach (`latest.log`, `crash-reports/`), and what we already know
about. Without it the alpha produces rumour, not bug reports.

## B. Should have (an alpha can go out without these, but it will hurt)

- **Two-client smoke test before release**, even if the alpha is also one. Oxygen is per-player state and no
  session has ever had two players. A twenty-minute try with a friend catches the embarrassing class of bug.
- **Late-joiner gap:** a player who joins a team that is ahead gets the dimension and none of the gear to survive it.
  Needs a catch-up mechanism (todo.md "Systems gaps"); at minimum, say so in known issues.
- **Easy and Hard boss totals** (only Normal was run on a server). Players will pick Hard.
- **Quest book Gear and Growth page:** the forge chain and FTB reward tables ("choice of one of three"); the
  generator does not write reward tables yet.
- **Performance measurements** owed by ADR-0008: two players on a dedicated server, Oxygen v2 in a room, a spark
  profile of a full session with the whole stack. The performance log's newest reading predates build paths,
  balance and Oxygen v2.
- **Distant Horizons** dimension-change oddities on the way to space: confirm they are not worse than before.
- **Parked balance questions:** one-legendary carry limit (Sanchit decides after testing), flat versus
  percent weapons against late bosses, Cataclysm stat extras. State the current behaviour in known issues.

## C. Can wait until after the alpha

- Planets 4 to 7, the End as a planet (End-only recipes stay unobtainable, a known debt).
- The ending (A the Eye opens / B the seal / C the mirror), faction names, the lone human on the beach.
- HUD chart for pilots in space (needs a `core` contract and a packet); the HUD team list (T4).
- Starter ship design and a sky-dock schematic ruin (the alpha decides).
- The no-Sable fallback ship, airlocks and sealing doors, the helium lamp, the orrery hologram, cats, the keeper
  shrine placement, a spinning-shaft visual on the Air Compressor.
- Curating Earth's modded ore spawns (mechanism proven, nothing to remove yet).
- Gravity per planet, failed-landing handling.
- Cleanup that is Sanchit's to call: deleting the remote branch `origin/combat-hud`.

## Suggested order

1. **Decide the five questions below** (no work, an hour of talking).
2. **A5 and A7 in parallel with testing:** none of it needs the game. Licence file, version, changelog, manifest
   and install notes, known issues. Can be one session while Sanchit plays.
3. **A2, step by step, one short client session each** (gate, keystone, ship), because each can reveal a fix, and
   a fix after the big run costs a second big run.
4. **The remaining unverified server-side work:** Oxygen v2 `OxygenChecks`, Easy and Hard runs.
5. **A1, the one combined run**, fresh world, with a save and reload at the end.
6. **Fix list from A1**, a refactor pass (ADR-0008: every third increment), the performance-log entry, then A6 and
   the two-client smoke test.
7. **Tag, build the release, install it on a clean machine or a clean launcher profile** as a stranger would.
   That last step is the one most likely to find a missing dependency or config.

## Decisions needed from Sanchit

1. **Alpha audience and size.** Friends on a shared server, or a public download? It changes A5, A6 and A7 from
   "tidy" to "must be right".
2. **Platform.** CurseForge, Modrinth, GitHub zip? And the licence on our own jars (MIT as in the properties file?).
3. **Is a server part of the alpha?** If players host their own, A6 is mostly documentation. If we host, we also
   need pre-generation or a plan for the Chunky crash, and an op/whitelist process.
4. **Which unverified items are blockers?** This list says: A1 to A3 yes, ship air (A4) only if the alpha is
   supposed to fly with a crew; if not, say "ships have no air yet" in known issues.
5. **The first-minutes experience:** bundled options, or leave default and accept the accessibility prompt.

Nothing on this list needs a new ADR yet, except possibly the licence and distribution choices (record them as
ADR-0028 when decided).
