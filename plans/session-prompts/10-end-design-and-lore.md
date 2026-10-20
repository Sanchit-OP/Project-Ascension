# Session prompt 10: the End (Planet 4), design and lore, done properly

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods, by Sanchit (player name Devil0701).
**Planet 4 is the vanilla End as an asteroid belt** (ADR-0028). Its logic works and was checked on a dedicated server and by Sanchit
in a client: the gate (the Dragon's exit portal), the Dragon's stats, the fall out of the bottom back to space, ship landing, the quest page.
**The design and the lore are bad, and are yours to redo.** Sanchit, after playing it (2026-10-20), verbatim: *"the concept and all is working
good for you but you suck at design. there are trash archives. and also no lore. and wtf is this, it is not an asteroid belt at all."*
He sent a screenshot of the End from space: about 160 grey cratered spheres in a loose blob, all the same texture, like a bunch of
moons stuck together, lit from one side. It reads as a cloud of small planets, not a belt.

## Work in your own worktree

The logic branch is **`planet-4-end`** (worktree `E:\code\Project-Ascension-end`), **not merged to main and not pushed; Sanchit said to hold it
until he has finished testing**. Branch from it, do not touch main:

```
git worktree add -b end-design ../Project-Ascension-end-design planet-4-end
```

Commit with explicit paths (never `git add -A`); do not push; merge nothing. Java is Temurin JDK 21 (`org.gradle.java.home` is pinned in
`gradle.properties`; the system `java` is a Java 8 JRE). `./gradlew build` runs `check-data.py`, `check-balance.py`, the quest generator and the
resource ledger and must pass.

## Read first

- `CLAUDE.md`, then `docs/decisions/` (the register). Binding here: **0003** (no Tier-1 module hard-depends on a third-party mod; mixins only in
  Tier 2), **0007** (performance: no per-tick scans, nothing static holding a `Level`/`Player`/`Entity`, delta sync), **0008** (nothing is done until
  seen in a running client), **0028** (the End is Planet 4). `docs/technical/art-guide.md`: **original art only, every texture from a seeded script
  in `tools/`, reproducible byte for byte** (never copy or trace vanilla, another mod or a pack).
- `docs/technical/end-art-format.md` (the art contract; section 1 now says a belt exists), `docs/technical/worlds-api.md` ("Space bodies", "The End is
  a belt, not a sphere", "Planet 4"), `docs/technical/planet-4-client-checklist.md`, `docs/technical/new-planet-checklist.md`.
- **Lore, read all of it before writing a word:** `docs/lore/what-the-warden-keeps.md` (the whole story), `the-mark.md`, `the-moon.md`, `the-nether.md`,
  `the-ending-and-names.md`, `lore-spread-proposal.md`, and the outline `docs/lore/the-end.md`. The rules from Sanchit: lore must **explain vanilla
  Minecraft** (what the mobs and structures are in the story), use **plain names**, and be **outlined before any long prose** (the outline is
  approved: write from it, and show him any change to it).
- The Nether is your worked example of a good result: `docs/technical/nether-art-format.md`, `nether-art-design.md`, `tools/generate-nether-colony.py`
  (three real ruins with murals and eight tablets), `design/nether/`. Study what made it work: several distinct ruins, murals that disagree with the
  tablets, places that tell the story without text, previews that were looked at.

## What is wrong, and what to do about each

**1. The belt (what he saw is not a belt).** Today: `assets/ascension_worlds/ascension_space/end.json` has a `belt` (160 identical lit spheres,
one texture `end_rock.png` from `tools/generate-end-belt-art.py`, scattered through a disc of the planet's body radius, drawn by
`client/SpaceSkyRenderer.renderBelt`, positions from `client/BeltRocks`). The result is a blob of moons. An asteroid belt reads as: a **wide, thin,
flat band** of **many tiny irregular rocks** in a range of sizes, with a few large ones, **dust and a faint glow**, **irregular (not spherical)
shapes**, rocks that **tumble**, and a **bright core** where the central island is. You are free to change the renderer (it is client-only, Tier 1;
keep it cheap: no per-rock world objects, no allocation per frame, a handful of draw calls) and the format. Ideas, not orders: instanced
irregular rock meshes or several rock sprites chosen per rock (not one repeated disc), a dust band drawn as a soft additive ring, many more rocks
at one or two pixels from afar, a flatter distribution with a ring-like density, a bright central rock for the Dragon's island, violet
glow on the night side. Look at it **in a client in space** next to the Moon and the Nether to judge it (ADR-0008); the planet is at
`[-10945, 30070]` in the `ascension_worlds:space` dimension and you arrive at `[-10945, 30618]` after falling out of the End. The planet's
`body_radius` is 320 and flying into it carries a ship down. Keep that rule.

**2. The archive tablets are trash.** Today: `tools/generate-end-ruin.py` makes two tiny templates (`end_ruin/ruin.nbt`: end-stone-brick floor, a broken
wall and five identical plinths in a row; `end_ruin/outpost.nbt`: one plinth among four pillars), generated by worldgen as `end_ruin` (fixed at
chunk `[40, 0, -8]`, which put it at about x 16..32, z -28..-16) and `end_outpost` (random, on outer islands, rare). The text is six short entries in
`tools/archive-lore.py` (`the_end` folder). All of it is a placeholder. Redo it as the Nether's colony ruins were done: **several distinct ruins**
with a character each (not one rectangle), made of the End's own materials (end stone, purpur, obsidian, end rods, chorus) and our decorative
blocks, **murals and carvings** (see `lore-art-format.md` and the Nether's `colony_mural`, `sealer_carving`, `NetherDecorBlocks`) that **disagree with
the tablets**, plinths **placed where they mean something** (the first near the arena, one by each pillar's foot? the last at the edge of the belt), on
the central island and the outer islands, and **findable** (a ruin a player will actually come across, not a lone block).

**3. There is no lore.** The tablets are one-paragraph stubs and the world tells no story. Write the End's lore from the approved outline
`docs/lore/the-end.md` as a proper document in the voice of the others (`the-end.md` grows into prose; keep the outline's beats): what the belt was, what
the Endermen are (the Observers showing through), who the Dragon was, why the crystals feed her, what the exit portal is (the belt's own gate), what
the End cities and shulkers and the Elytra are (the ancients' last outpost and its wings), what chorus is, why there is **no floor** (the fold that holds
the gates; below it the dark between worlds; Slow Falling, then space: ADR-0028 explains the mechanism, the lore must explain the reason), and where the
**predator world (Planet 5)** begins. Then the in-world tablets (about six to eight, **the voice**: the Observers speaking in Tacita's name, flattering and upside
down on purpose, signed with the glyph row; see the docstring of `tools/archive-lore.py` for how every other tablet is signed) **plus at least one that is
not the voice** (the true record, the way the Nether's Forge Record is), and **the in-world details that carry the story without words**. Each page
must fit a book page (about 14 lines of 19 characters, `ArchiveLoreTest` checks). Show Sanchit the prose before wiring it in.

## Boundaries

- **Do not touch** the logic: `EndGate`, `VoidFall`, `EndChecks`, the Dragon's balance, the quest page's structure (you may fix its text), growth, runes.
  If the design needs logic changed (a new space-body field, another structure type, a new plinth block, loot), write it down in
  `docs/technical/end-art-format.md` and build the small piece yourself in `ascension-worlds` when it is client-only or data; ask Sanchit for anything larger.
- Vanilla End worldgen (the islands, End cities, ships, the Elytra) is vanilla's; your structures sit on it (`ascension_worlds:on_dry_ground` with
  `min_ground`, `fixed_position`, `random_spread`; check the structure files above). The End is a fresh-worlds-only dimension.
- Original art only. Every `.nbt` and `.png` from a seeded script, deterministic (sorted palette, fixed block order, gzip with no name and mtime 0: the
  Lunar Archive generator's writer, imported not copied).

## How to prove it

Headless first (`dev-environment.md`, "Checking generated structures without a client": throwaway world, `forceload`, `structure_starts.py`,
`find_blocks.py`, `end-checks.txt` still passes), then **one batched client run** (a launch per tweak is wasted: Sanchit wants one combined run):
the belt from space at three distances, each ruin, every tablet read, the murals, the outer-island ruin, a reload. The instance's mods folder
(`C:\Users\sanch\curseforge\minecraft\Instances\Ascension Dev\mods`) is shared: compare before overwriting a jar, and a backup of the old
jars is in `mods-backup-before-planet4`. Operators: on a throwaway dev server `op Devil0701`, then `/execute in minecraft:the_end run tp @s ...` and
`/execute in ascension_worlds:space run tp @s -10945 0 30618` get you around.

## Hand back with

The new belt (renderer, format, textures and the script), the redone ruins and their generators, the lore document and the tablets (text in
`tools/archive-lore.py`), previews of every piece in `design/end/`, the headless results (numbers), what you could only check headless, and the
client checklist. Update `docs/technical/end-art-format.md`, `worlds-api.md`, `docs/lore/the-end.md`, `CLAUDE.md` (one line) and `todo.md`.
