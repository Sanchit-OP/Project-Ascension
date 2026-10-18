# The HUD

Status: **design started 2026-10-18, nothing built.** Branch `combat-hud`.
Brief: [`plans/session-prompts/6-hud-overhaul.md`](../../plans/session-prompts/6-hud-overhaul.md).
Decision: [ADR-0022](../decisions/0022-the-hud-is-its-own-module.md), `ascension-hud` is a Tier 1
module depending only on `core`; other modules contribute elements through a contract in `core`.

## 1. What is decided

- Who owns it: `ascension-hud` owns layout, theme, frame, config (Sanchit, 2026-10-18).
- It ships the vanilla-only elements (health, food, level); other modules register theirs.
- One client config switch gives vanilla's HUD back.

## 2. What is still open (ask Sanchit, do not decide)

- Does the stat column retire `CharacterSheetScreen` (V)?
- Which runes' state is shown, and does a full stack make a sound?
- Does food stay visible, and what does the oxygen gauge look like?
- Default positions, and what a pack author may switch off.

## 3. Research (to do)

League's bottom bar and stat panel, Souls-likes, Diablo-style globes, Terraria, Hades, Path of
Exile, and well-made Minecraft HUD mods: how they scale health, hide the irrelevant and animate
change. End with what we can add and what we should add.

## 4. Elements (to design, with mockups)

Health, Armor and Magic Resist, food and level, oxygen, runes, stat column, item abilities, status
effects, boss bar, unspent skill points, layout and config. Mockups are rendered by a seeded
script and shown to Sanchit before anything is built.

## 5. Build order

Shell and layout, health, stat panel, oxygen, runes (with delta sync), the rest; a refactor pass
every third increment. Proof per ADR-0008: scripted screenshots at GUI scales 2 to 4, a dedicated
server with two players, a relog, and a frame-cost reading in `performance-log.md`.
