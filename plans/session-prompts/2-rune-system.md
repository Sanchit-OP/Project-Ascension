# Session prompt 2: the rune system, designed properly

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **the rune system**:
- research what it should contain;
- design its content;
- give it a screen and art at a professional level.

## Sanchit's words (2026-10-15)

> our game looks good with new planets, new lore, items etc. but the runes system having minecraft
> buttons and text is so depressing. we need professional level designing there so it is
> appealing. then that agent needs to start the actual research: what we can add and what we
> should add in the runes.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. ADRs 0003, 0007, 0008, 0011, 0012 and 0018 bind you.
- `docs/technical/combat-api.md`:
  - **§7:** the four paths and the page structure as built.
  - **§9:** the open problem of keeping a two-tree rune page from reading as a League clone.
  - **"A note on names and licensing".**
- `docs/progression/player-power.md` §3.4: proposals made 2026-10-15. Treat them as starting
  ideas, not decisions:
  - three keystones per path;
  - stat shards;
  - unlocks tied to the player's growth;
  - changing runes freely out of combat.
- `docs/technical/art-guide.md`: how art is made here. Original only, made by seeded scripts in
  `tools/`, never copied. Read `docs/technical/combat-art-reference.md` for the League-likeness
  rules combat follows.

## What is true today

- **Four paths, in `modules/combat/.../api/rune/RunePath.java`:** Vanguard (sustained damage),
  Reaver (burst), Aetherwright (amplifies items' magic procs) and Bulwark (defence and team; its
  keystone is meant to tie into oxygen drain).
- **Only Vanguard is built:** one keystone and four minors, in `internal/rune/RuneCatalog.java`
  and `internal/rune/vanguard/`.
- **The page:** a primary path grants its keystone plus up to two of its minors; a secondary path
  gives up to two of its minors.
  - Saved per player server-side (`RunePageState`).
  - Synced by `SetRunePagePayload` and `RunePageSyncPayload`.
- **The screen** (`client/RuneScreen.java`) is plain vanilla buttons with rune names only.
  - Its javadoc mentions a stylized mockup as the visual target.
  - The keybind is unbound by default.
- **No spells** exist in this game. Effects work through on-hit, movement, shields, stasis,
  omnivamp, oxygen, mobs and co-op. Know what the stat model supports (`combat-api.md` §3).

## The job

1. **Research.** In a doc, cover:
   - League's whole rune system: every path, keystone, minor, and the stat shards. What makes it
     fun.
   - Other games' talent and rune systems, and their screens. What makes a rune screen feel
     good: layout, icons, hover detail, sound, motion.
   - What Minecraft combat can actually support.
   - What fits Ascension: planets, oxygen, the rot, co-op, the lore's Observers and mark.

   End with a list of what we *can* add and what we *should* add.
2. **Design** in `docs/technical/rune-system.md`:
   - the content of all four paths;
   - how runes unlock;
   - how they change;
   - the numbers as placeholders;
   - the screen.

   Put structural choices in an ADR. Ask Sanchit the decisions, each as a plain question with your
   recommendation first.
3. **The screen: professional, not vanilla buttons.**
   - A custom panel, path emblems and colours, an icon for every rune, hover cards with what each
     rune does and its numbers, and selection feedback with motion and sound.
   - It must read well at GUI scales 2 to 4.
   - Make **rendered mockups first** (PNG previews from a script, as the art guide shows) and send
     them to Sanchit's phone. Build only after he likes one.
   - All textures come from seeded generator scripts. Never draw by hand or copy.
4. **Build** in increments: content, unlocks, the screen, the art.

## Working beside other sessions

Two other sessions start at the same time.
- **Stats (prompt 1)** owns the stat definitions and the API for reading stats, including base
  versus bonus. Read stats only through that API. If a rune needs a new stat, ask for it through
  Sanchit or its "Requested stats" list in `combat-api.md` §3. Don't add attributes yourself.
- **Growth (prompt 3)** designs how players grow, possibly a skill tree. Keep the boundary clear:
  - **runes:** swappable build choices;
  - **growth:** permanent progress;
  - **unlocks:** if runes unlock with growth, agree the hook with that session through Sanchit.

  A skill tree screen and the rune screen should feel like one game. Share your visual language
  early.

How to work alongside them:
- **Work in your own git worktree and branch**, for example `combat-runes`. If the app did not make
  one, run `git worktree add`. Merge into `main` only when Sanchit says.
- **Keep edits to shared files small:** `AscensionCombat.java`, the lang file, `CombatContent.java`.
- **Only one dev server can run at a time** (same ports). Check before `./gradlew runDevServer`.
- **Leave the original combat session's work alone:** the 3D weapon models and Better Combat
  animations.

## How Sanchit works

- **Commit each step.** Never push unless he asks.
- **ADR-0008:** a feature is done only when seen in his client; check world state and sync on a
  dedicated server. A refactor pass every third increment.
- **Every build runs `tools/resource-ledger.py --check`** (Python on PATH).
- **Plain language.** Keep docs handoff-ready, and send docs and mockups to his phone
  (SendUserFile).
