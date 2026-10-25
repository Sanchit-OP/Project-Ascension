# Session prompt 12: the team list on the HUD (teammates' health), and room for the minimap

Paste everything below the line into a new Claude Code session opened on this repository. **Alpha.3, "good to have"** (`plans/alpha-3.md`,
F14): not a blocker, but Sanchit asked for it after testing alpha.2 with friends. It needs a **design pass before code** (see "Designs").

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods, by Sanchit (player name Devil0701).
Your job is the HUD side of playing in a group: **a team list showing each teammate and their health**, and **a HUD layout that leaves room
for the Xaero's Minimap** the pack is adding. You build it in `ascension-hud`; it learns who is on your team from `core`'s team contract.

## Sanchit's words (2026-10-25, after the alpha.2 test with friends)

> for minimap this can work https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap and we maybe have to build the hud. let this
> be handled by a new session ... as it will need some designs as well.

Earlier in the same message: "for the mini map and the team HUD I felt there are other mods or some settings which felt better. To see
a player on the map was easy. We could also see their health and all in the HUD. ... the minimap also had levels where we can see the map
at different Y levels. (These two things are not necessary but good to have for alpha 3.)"

So the target is three things, in this order of importance:

1. **Teammates on the HUD with their health.** Who they are, how much health they have, whether they are near.
2. **Teammates on the map.** This is Xaero's own job once the mod is in (it shows nearby players; with a party mod it shows party
   members). **Do not write a map.** Check what Xaero's Minimap shows of a teammate with only FTB Teams installed, and report it; if it
   shows nothing without Open Parties and Claims, say so and do not add that mod yourself (it duplicates FTB Teams and FTB Chunks).
3. **Y-level slices of the map.** Xaero's has a cave mode that follows the player's depth. That is a setting, not code: find which
   setting, and put it in the pack's default config (see "Shipping defaults").

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These bind you: **0003** (Tier 1 never depends on a third-party mod; foreign integration is an
  optional Tier-2 `ascension-compat-*` jar; extension through provider registries), **0007** (performance: nothing ticks needlessly, no
  static holding a `Level`/`Player`/`Entity`, server-authoritative and delta-synced, **no per-tick packets**), **0008** (nothing is done
  until seen in a client; world state on a dedicated server), **0011** (Tier-1 modules never depend on each other), **0023** (teams).
- `docs/technical/hud.md`: the HUD module, its layout, what is built and how it is photographed (`hud-screenshots.txt`). Its section 4
  is the layout you must live beside: stat block left of the hotbar, health bar above it, level and hunger badges, the oxygen emblem,
  rune discs right of the hotbar. **Read how the HUD decides where things go before you add an element.**
- `docs/progression/teams.md`: the team model. `core`'s `TeamRegistry` says who is on which team (everyone is one team when nothing
  answers; a solo player is a party of one); `ascension-compat-ftbteams` answers it from FTB Teams. **T4, "the team list for the HUD",
  is this job**, deferred on 2026-10-18 because "a map mod shows teammates"; Sanchit has now asked for it.
- `docs/technical/pack-extras.md`: the optional extras, including the Xaero's note: "They draw a minimap on the HUD, so the HUD
  overhaul must make room." This is that.
- `docs/technical/art-guide.md`: every texture is original, made by a seeded script in `tools/`.

## What is true today (2026-10-25)

- The HUD module exists and was merged on 2026-10-18 (ADR-0024). It has a health bar that is the same from 20 to 1,000 HP, so a
  teammate's health must read the same way: a **fraction of their max health**, with the number only on request.
- Teams are known on the server. **Nothing sends a teammate's health to a client.** Vanilla sends other players' health only to the
  client through entity data of players it tracks (within the tracking range), not for a teammate far away. You will need a small
  payload: the server sends each online teammate's current and max health, on a **change** (and once at join), to the members of that
  team only. Not every tick; not to strangers. Cap the rate (a few times a second at most, only for a changed value).
- Max health on this server goes far past vanilla's 20 (boss HP 1,000 to 4,550; ADR-0027 raised the cap). A teammate bar must
  not overflow at 600 HP, and shields (the white part of our own bar) are a separate number you may leave out.
- Xaero's Minimap is **client-side only** (it ships a server-optional part). It draws the minimap at the top-right by default and can
  be moved. Our HUD currently leaves the top-right free except for the boss bar and status effects; **check that, in a client**, with
  Xaero's installed.
- Keys already taken: R (runes), K (growth), V (character sheet), G (Curios), Z/X and mouse 4/5 (item abilities), plus Xaero's own
  (it binds several: look at what it takes and avoid them; the Keybind Atlas mod lists conflicts). Do not add a key if you can avoid one.

## Designs (do this before any code)

Sanchit said it needs designs. Produce, as pictures in the repo's mockup style (`docs/technical/hud.md` section 5 shows how the v2
mockups were made), **three layouts** for the team list and have him pick:

1. A **column on the left edge**, one row per teammate: small head, name, a thin health bar under it, a distance when far.
2. A **compact strip above the hotbar's right end**, heads only with a health ring around each (like the level badge).
3. A **corner panel under the minimap**, so the map and the people sit together.

For each, show 1, 3 and 6 teammates, one at low health, one far away, one dead, and the minimap in its default corner. Say what
each costs in screen space on a 1080p window and on a small one. Ask him which before you build; do not build all three.

## Increments (ADR-0008: a refactor pass, no new features, after every third)

1. **The data, no drawing.** The payload and its server side: on a health change of a team member, send it to the other online members
   of that team; at login and when someone joins or leaves a team, send the whole list. Rate-limited. Held on the client as plain
   values (a UUID, a name, two numbers), cleared on disconnect and on dimension change if the data would be stale. Unit tests for the
   pure parts (who receives what, the rate limit, a team of one receives nothing). A dedicated-server check with stand-in players, in
   the style of `TeamChecks` (`modules/compat/ftbteams/.../dev/TeamChecks`).
2. **The list, in the layout he picked.** Drawn by `ascension-hud`, off by a client config switch, hidden with F1 like the rest. A
   teammate in another dimension is shown dimmed with the dimension's name; a dead one is crossed out. No names longer than the
   layout allows (cut with an ellipsis, never wrap).
3. **Room for the map.** With Xaero's installed, nothing of ours overlaps its minimap in its default position or the corner it can be
   moved to, at the three common GUI scales. If our layout must move for it, move ours, never ask the player to move the map.
4. **Proof.** The HUD's screenshot script (`hud-screenshots.txt`, `HudScreenshots`) takes the new pictures; one batched client list
   for Sanchit. Do not launch the game client per tweak; he wants one combined run.

## Shipping defaults (config the pack carries, once the map is in)

The pack's config lives in `pack/config/` and `tools/sync-pack-config.py` merges it into the test instance. When Xaero's is added,
decide and write down, with the reason, its defaults for: minimap size and corner, cave mode (the depth slices) on, showing other
players on, waypoint sharing. Only the settings the pack decides; leave the rest to the mod.

## How to work

- **Work in your own git worktree** and merge only when Sanchit says:
  `git worktree add -b team-hud ../Project-Ascension-team-hud main`. Commit each step with explicit paths (never `git add -A`); do not
  push. **No `Co-Authored-By` or "Generated with Claude Code" lines** in commits (CLAUDE.md).
- Java is Temurin JDK 21 (system `java` is Java 8). A full `./gradlew build` must pass before you hand back.
- The CurseForge instance (`C:/Users/sanch/curseforge/minecraft/Instances/Ascension Dev/mods`) is shared with other sessions:
  **compare before overwriting any jar there**. Xaero's is installed by the alpha.3 mods session, not by you; if it is not in the
  instance when you need it, say so and work from the screenshots of its layout.
- If you disagree with a locked decision, supersede it with an ADR. Do not work around it.
- Do not touch: combat, atmosphere, worlds, the quest pages, the Curios slots.

## Hand back with

Which layout he picked and the pictures, the payload's size and rate (numbers), the dedicated-server results, what Xaero's does and
does not show of a teammate on its own, the config defaults you chose and why, and the batched client checklist. Say plainly what was
only checked headless.
