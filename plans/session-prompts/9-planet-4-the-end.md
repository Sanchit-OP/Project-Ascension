# Session prompt 9: Planet 4, the End as an asteroid belt (the Ender Dragon and the Elytra)

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **Planet 4: the vanilla End as the fourth world**,
with the Ender Dragon as its boss and the Elytra as its prize. It is the **stopping point of the
alpha** (the alpha is a free CurseForge modpack on a server we run: `plans/alpha-checklist.md`, A8).
Planet 3 (the Nether, ADR-0017) is your worked example: it is the same kind of job, and every file
it needed is listed in **`docs/technical/new-planet-checklist.md`**. Read that first; it is your
shopping list.

## Sanchit's words (2026-10-19, lightly corrected)

> I think we should add planet 4, which I am thinking an asteroid belt of the End from the normal
> game will be good. [...] With the Ender Dragon and the Elytra we can mark it as a good point for
> the alpha release.

My reading: the End's floating islands *are* the asteroid belt (you arrive from space, the islands
hang in the dark, the Dragon is the guardian of the belt, and the Elytra from the End cities is how
you cross the gaps). The planets are already planned to be reached by airship (ADR-0015) and lit by
a boss's gateway key (ADR-0017's pattern). **He has not yet answered four questions (below). Ask
him first; do not build until he has.**

## Read first

- `CLAUDE.md`, then `docs/decisions/` (the register). These bind you:
  - **0003:** no Tier-1 module hard-depends on a third-party mod; foreign integration (Cataclysm,
    Enderman Overhaul, Sable) lives in optional Tier-2 `ascension-compat-*` jars; mixins only in
    Tier 2 with an ADR. **0007:** performance (no per-tick scans, nothing static holding a
    `Level`, `Player`, `Entity` or `BlockEntity`, delta sync). **0008:** nothing is done until seen
    in a running client; world state on a dedicated server; anything saved across a reload; a
    refactor pass every third increment.
  - **0014:** vanilla Nether and End portals are dormant (read the *Consequences*: the End half of
    the recipe debt is yours to pay). **0015 and 0016:** the road to space is by airship. **0017:**
    Planet 3 is the real Nether, the template for this job. **0018:** no world is ever required;
    materials reward, they do not unlock. **0026:** air is a fluid. **0027:** Max Health's ceiling
    is raised, so a boss can have thousands of health.
- `docs/technical/new-planet-checklist.md` (every item a planet needs, with the Nether's files),
  `docs/progression/planets.md` (the planet template and required fields), `docs/technical/worlds-api.md`
  (the planet schema, "Sky bodies", "Space bodies", "Planet 3", "Hazards", "Planet 3's gate and
  keeper", "Vehicles crossing the line"), `docs/technical/gateway-frame-format.md`.
- `docs/lore/what-the-warden-keeps.md`, `the-nether.md`, `the-moon.md`, `the-ending-and-names.md`,
  `lore-spread-proposal.md`. Lore rules from Sanchit: it must **explain vanilla Minecraft** (what the
  mobs and structures are in the story), use plain names, and be **outlined before any long prose**.
- `docs/progression/balance.md` (section 5: the boss ladder, Normal HP after the 2026-10-19 cut:
  Warden 975, Ender Golem 1,625, Ender Guardian 2,080, Netherite Monstrosity 2,730, Ignis 3,380, Wither
  4,225), `docs/technical/damage-kinds.md` (physical or magic is decided by the attack; the Dragon's
  breath is already magic), `docs/progression/growth.md` (milestones feed skill points and rune slots),
  `docs/progression/build-paths.md` (boss hearts), `docs/progression/journey.md`,
  `docs/technical/dev-environment.md` (how to check worldgen without a client, the command-built
  ship, the `*-checks.txt` pattern), `docs/technical/art-guide.md`.
- The vanilla source for 1.21.1 is in the Gradle cache; **read it** for `EndDragonFight`,
  `EnderDragon` (part damage, crystal healing), `EndPortalBlock`, `EndGatewayBlockEntity`,
  `EndCityStructure` and the `the_end` dimension type and noise settings, instead of trusting memory.

## What is true today (checked 2026-10-20)

- The End is **dormant**: `ascension-worlds` cancels any portal-driven trip into it (ADR-0014). It is
  not a planet; there is no `planet/end.json`, no Sable `dimension_physics` for it
  (`compat/sable` has `overworld`, `moon`, `nether`, `space`), no sky, no space look, no gate.
- `minecraft:ender_dragon` is already in `ascension_combat:tier/boss`, so the boss tier's
  difficulty scaling reaches it (Easy -30% health, Hard +75%), but it has **no `mob_stats` row** (no
  Armor, no Magic Resist) and **no `types` row** in `mob_difficulty/*.json`. Vanilla Dragon: 200 health.
- Planet 4's slot is laid out: **32,000 blocks out, bearing 110°, `[-10945, 30070]`**, 26,215 from
  Planet 3 (`worlds-api.md`). Planets are data (`planet/<name>.json`), `order` is 20 (Moon), 30
  (Nether), so the End is 40.
- `docs/progression/planets.md` and `lore-spread-proposal.md` still say **Planet 4 is a "predator
  world" where the keepers were made.** This job conflicts with that; resolving it is part of the ADR.
- The Moon and the Nether set the pattern: a gate ruin pre-generated in the world, **unbreakable**,
  lit by the key the boss drops; a survival rule (the Moon's vacuum, the Nether's rot); seven or eight
  Archive tablets in the voice; art made by a separate design session through a contract doc.
- Ships: a ship climbing past a planet's build height reaches space; coming down, it arrives
  40 blocks below the build height over the spot the pilot last rose from, or over the world's centre
  (`SpaceTravel.arrivalAbove`). **Over the End's centre is the Dragon's arena.**
- Air aboard a ship (Oxygen v2) is not yet verified, and a survival-built ship has never flown.
  You depend on both for the client test but do not own them (`plans/alpha-checklist.md` A2, A4).

## The four questions to put to Sanchit first (use AskUserQuestion; recommendations in bold)

1. **Air in the End: breathable or vacuum?** **Breathable, as the Nether is.** The Dragon fight is hard
   enough and a tank clock during it will read as unfair; the asteroid-belt feel comes from the sky and
   the void. Vacuum is possible (`environment.breathable: false`) and would make the End the first world
   needing a pressurised arena.
2. **Does the predator world slide to Planet 5, or is the End an unnumbered bonus world?** **Slide it to
   Planet 5**, and note it in `planets.md` and the lore proposal.
3. **Where does the End's exit portal go after the Dragon dies?** Vanilla sends the player to world
   spawn and plays the credits on the first kill. **Back to the End's gate or arrival point, no credits
   (keep them for the real ending).**
4. **A lore pass first (`docs/lore/the-end.md`), as for the Nether?** **Yes**, outlined short first.
   Our lore makes the Endermen the Observers showing through, and the End is where players meet them in
   numbers.

Also ask, once, before drawing anything: **does the Dragon give a heart for build paths** (a seventh)
and which item takes it, and **what is the End's own resource** (candidates: shulker shells, chorus,
purpur, End stone; the Elytra is the prize, not a material). Both touch other designs.

## Increments (ADR-0008: a refactor pass, no new features, after every third)

**P0. Decide and document. Stop and show Sanchit before P1.**
- His four answers. Then **an ADR** (the next free number; `docs/decisions/README.md` lists them) that
  gives Planet 4 to the End and supersedes the older plan in `planets.md` in writing, a Planet 4
  section in `worlds-api.md`, the lore outline `docs/lore/the-end.md`, and the per-planet design
  fields (fantasy, threat, constraint, objective, prize, mastery loop; orbit purpose, survival
  mechanic, movement wrinkle, resource, core structure, boss, permanent travel improvement). Suggested:
  movement wrinkle = the Elytra over the void; core structure = the End cities; boss = the Dragon;
  permanent improvement = the lit gate.

**P1. The planet exists.** `planet/end.json` (surface `minecraft:the_end`, position
`[-10945, 30070]`, order 40, radii like the Nether's unless the asteroid-belt look wants other numbers,
the survival rule from answer 1). Dimension overrides only if you need them (the End already has no
ceiling and 256 height; check, do not assume). `compat/sable/.../dimension_physics/the_end.json`
(copy the Nether's shape, check the pressure at the End's build height). **A command-built ship round
trip**: space, down onto the End, up again, cargo intact (dedicated server). Decide where a ship
**lands**, because the default is over the Dragon's arena; the session must make it safe (a landing
island, or an arrival offset) and say why.

**P2. The gate and the exit.** A gate ruin (`gateway_site`, `gateway_frame_end`, structure set, biome
tag) on ground that exists: **not inside the Dragon's arena, not over void**; decide fixed position or
seeking. The key: a loot modifier on `minecraft:entities/ender_dragon` giving a key with
`gateway_key_for_planet: minecraft:the_end`, in `ascension-worlds` (the Dragon is vanilla, so this is
Tier 1, as `warden_tendril` is). Gate and discovery advancements and growth milestones. **Redirect the
exit portal** (ADR-0014 only blocks trips *into* the End; leaving by the exit portal still ends in the
credits) per answer 3. Who is in the arena when the fight starts, and does the fight begin when you
arrive by gate: read `EndDragonFight`, test it, write down what you find.

**P3. The Dragon as a balanced boss.** A `mob_stats` row (Armor, Magic Resist), `types` rows in
`mob_difficulty/{easy,normal,hard}.json`, a row in `balance.md` section 5, a check in `DamageChecks`.
**Propose, do not decide, the numbers**: the ladder ends at the Wither (4,225 HP, 180 Armor, 110 Magic
Resist on Normal); the Dragon sits at or just above it. Run `tools/time-to-kill.py` for the kill times.
Things to find in the source and write down: the **end-crystal healing** (a flat amount that matters at
200 health and not at 4,000), the **part damage** (the Dragon takes reduced damage on its body and body
parts: confirm our Armor, Magic Resist and percent-of-health weapons act on the right number), the
Dragon's own attacks and their damage kinds (breath is magic; the body is physical), and respawn by
crystals (the key and the heart must not be farmable by respawning, or must be a decision).

**P4. The Elytra and the outer islands.** Vanilla puts the Elytra in an item frame on the **End city
ships** on the outer islands, which only appear after the Dragon dies (the End gateways, which also
spawn at the first death). Verify, with arrival by gate and not by the vanilla portal: gateways open,
outer islands generate, End cities and ships generate with their item frames and loot (Lootr), and
the Elytra works in the pack as installed (Caelus API, an elytra API, is in the stack) and with our fall rules. The outer ring generates on demand when players
fly over it at speed: measure the cost (`spark`) and write the pre-generation advice for the server
(`serverpack/README.md`; Chunky's background pre-generation is off by default after a crash, so say
what to do by hand). **Pay the End half of ADR-0014's recipe debt** (Elytra, shulker boxes, End-only
items) and record it in `todo.md` and the ledger. Check the compat jars: `endermanoverhaul`,
`distanthorizons` (`DimensionClouds`), `chunky`, `cataclysm` (does anything of Cataclysm's belong on
the End?), and say "nothing needed" where that is the finding.

**P5. Survival rule and movement.** Whatever answer 1 gave, as data (`environment` or `hazard`), with
its message and ward tag, plus the void: falling is death, so say what stops a stranger from dying
on the first step. Check what Oxygen v2 needs, if vacuum.

**P6. How it looks, by contract.** Do **not** draw. Write `docs/technical/end-art-format.md`
(the contract, in the style of `nether-art-format.md`: what the design session owns, the file names,
the placeholder files, the handover checklist) and a prompt `design/end/design-session-prompt.md`
(in the style of `design/nether/design-session-prompt.md`) for: the End's sky and haze (the asteroid belt
under a dark sky), its face from space (a belt of rocks rather than a sphere? the contract must say what
the space-body format can and cannot do), the gate ruin, the quest-book planet art, and any tablets.
Placeholders keep the logic working meanwhile. Original art only, generated by seeded scripts in `tools/`.

**P7. The pack around it.** A Planet 4 section of the quest book (`pack/quests/`, via
`tools/generate-quests.py`, which the build checks), the Planets page's "you are here" marker for the
new planet, `docs/progression/journey.md`, `resource-economy.md` and the ledger
(`tools/resource-ledger.py --check` runs in every build), the growth documentation if the milestones
shift the rune-slot schedule (`GrowthProgress`).

**P8. Proof and hand-back.** A dedicated-server check file (extend `damage-checks.txt` for the Dragon's
numbers; add `end-checks.txt` if the exit portal, gate and arrival need one), world reload, a
performance-log entry for the new dimension and the fight, a refactor pass, and **one batched client
checklist** for Sanchit: arrival, the sky, the gate, the Dragon, the exit portal, the outer islands, the
Elytra, a flight across the gap, a reload. Do not launch the game client for each tweak; he wants one
combined run. Write the checklist into the design note, update `CLAUDE.md`, `worlds-api.md`,
`new-planet-checklist.md` (add what it missed), `todo.md` and `plans/alpha-checklist.md`.

## How to work

- **Work in your own git worktree** and merge only when Sanchit says:
  `git worktree add -b planet-4-end ../Project-Ascension-end main`. Commit each step (explicit paths,
  never `git add -A`); do not push.
- Java is Temurin JDK 21; system `java` is a Java 8 JRE. The build pins `org.gradle.java.home`. A
  full `./gradlew build` runs `tools/check-balance.py`, `check-data.py`, the ledger and the quest
  generator, and must pass before you hand back.
- The CurseForge instance (`C:/Users/sanch/curseforge/minecraft/Instances/Ascension Dev/mods`) is
  shared with other sessions: **compare before overwriting any jar there**.
- Data over code (ADR-0007): adding a planet must not mean adding a class. If you need Java, it is for
  the Dragon fight and the exit portal, and it goes in `ascension-worlds` (Tier 1) unless it names a
  third-party mod.
- Do not touch: build paths' recipes (ask first), the combat item stats, the quest pages that are not
  Planet 4's, Oxygen v2, the distribution work (`plans/alpha-checklist.md` A5, A6).
- If you disagree with a locked decision, supersede it with an ADR. Do not work around it.

## Hand back with

The ADR, the design note, the lore, the art contract and design prompt, the dedicated-server
results (numbers, not adjectives), the open items you could not close, and the batched client
checklist. Say plainly what was only checked headless.
