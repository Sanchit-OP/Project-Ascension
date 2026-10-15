# Session prompt 3: how a player grows

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **player growth**: the many ways a player gains
stats over the game, and possibly a skill tree so different players grow in different directions.

## Sanchit's words (2026-10-15)

> how we gain stats. we need to add a lot of different ways to get stats, as these stats will be
> used in the items' scaling as well. few items will get like extra damage based on your attack
> damage and all those types of things. so we need legit ways to grow different types of stats.
> and we also know that different types of stats are better for different players, so maybe we
> add a skill tree or something so players go in different directions.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These ADRs bind you:
  - **0003, 0007, 0008, 0011, 0012.**
  - **0018:** no world is ever required; players go where they choose, and materials reward rather
    than unlock.
- `docs/progression/player-power.md`: the audit, and a first proposal for growth (§3.1).
  Treat these as starting ideas, not decisions:
  - Milestones: the first kill of each boss, and each lit gate, raise base stats for good.
  - Vanilla XP and kill-XP levels were turned down, because both can be farmed or spent.
  - The co-op question: each player who fought, or the whole team.
- `docs/technical/combat-api.md` §3: the stats, and the defence curve.
- `docs/vision.md`: co-op first; team progress is shared; bosses unlock capability; exploration
  must return value.
- `docs/lore/the-mark.md`: the mark on the player's hand, which grows with every lit gate.
- `todo.md`, "The late-joiner gap".

## What is true today

- **A player never grows.** HP is vanilla's 20 all game; every other stat comes only from items.
- **The bosses in v1:**
  - Earth: the Warden; the Wither (anywhere).
  - The Moon: the Ender Golem and the Ender Guardian (Cataclysm).
  - The Nether: the Netherite Monstrosity and Ignis (Cataclysm).
- **Gates:** each planet's gate is lit by a key, and gate progress is already synced to clients.

## The job

1. **Research.** In a doc, cover:
   - How games and mods let players grow: League's levels, Souls-like levelling, Path of Exile's
     passive tree, Diablo's paragon board, Terraria's life crystals, Minecraft skill mods.
   - What makes growth feel earned rather than grindy.
   - What fits Ascension's planets, bosses, exploration and co-op.

   End with what we *can* add and what we *should* add.
2. **Design** in `docs/progression/growth.md`:
   - **Every source of stats:** milestones, exploration finds, a skill tree and where its points
     come from, and anything else your research supports.
   - **How each source avoids being farmed.**
   - **Co-op and late joiners.**
   - **The numbers, sized against items:** items will scale off these stats.
   - **Storage and sync:** per player, kept across world reload.
   - **The skill tree screen.**

   Put structural choices in an ADR. Ask Sanchit the decisions, each as a plain question with your
   recommendation first.
3. **Build** in increments: the data first (a new boss's or planet's growth must be a data file,
   not code), then the sources, then the screen.

## Working beside other sessions

Two other sessions start at the same time.
- **Stats (prompt 1)** owns the stat definitions and the API for reading stats, including base
  versus bonus. Growth adds to a player's base stats through that API. Agree with that session,
  through Sanchit, how growth's stats count, and ask it for any new stat rather than adding one.
- **Runes (prompt 2)** builds the rune system and a professional rune screen. Keep the boundary
  clear:
  - **growth:** permanent;
  - **runes:** a swappable build;
  - **unlocks:** if rune slots unlock with growth, agree the hook through Sanchit.

  A skill tree screen should look like the same game as the rune screen. Share visual language.

How to work alongside them:
- **Work in your own git worktree and branch**, for example `combat-growth`. If the app did not make
  one, run `git worktree add`. Merge into `main` only when Sanchit says.
- **Keep edits to shared files small:** `AscensionCombat.java`, the lang file, `CombatContent.java`.
  Put new code in its own package, for example `internal/growth`.
- **Only one dev server can run at a time** (same ports). Check before `./gradlew runDevServer`.

## How Sanchit works

- **Commit each step.** Never push unless he asks.
- **ADR-0008:** a feature is done only when seen in his client. Check world state and sync on a
  dedicated server, and check anything persistent across a world reload. A refactor pass every
  third increment.
- **Every build runs `tools/resource-ledger.py --check`** (Python on PATH). New items need a source
  and, if they are materials, a use.
- **Original art only** (`docs/technical/art-guide.md`).
- **Plain language.** Keep docs handoff-ready, and send docs to his phone as `.md` (SendUserFile).
