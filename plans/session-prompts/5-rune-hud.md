# Session prompt 5: the rune HUD

> **Superseded 2026-10-18 by `6-hud-overhaul.md`.** Sanchit wants the whole HUD redone, of which the
> rune HUD is one part. Start from prompt 6. This file stays for its notes on the rune state and
> how to sync it.

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **the HUD for the rune system**: the small things
on the player's screen, in a fight, that say what their runes are doing right now. The rune
system itself, with its screen (the orrery), 45 runes and their art, is built and was play-tested by
Sanchit on 2026-10-18; you add to it, you do not redesign it.

## Sanchit's words (2026-10-18)

> ok we need to make hud for this for sure. but that can come to a different session i think.

His standing wish for the rune screen applies to the HUD too: **professional level**, not vanilla
buttons and text. The first thing missing is the stack counter for Conqueror's Resolve.

## Where the work is

The rune work lives on the branch `combat-runes` (worktree `E:\code\Project-Ascension-runes`),
**not yet merged to main** as of 2026-10-18; Sanchit merges when he says. Check `git log main` for
the rune commits before you start: if they are there, branch from main; if not, ask Sanchit
whether to branch from `combat-runes`. Never push unless asked.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. ADRs 0003, 0007, 0008, 0011 and 0020 bind you:
  no Tier-1 third-party dependencies, no mixins, no static collections holding a Level, Player or
  Entity, no per-tick scans, server-authoritative and **delta-synced, never a per-tick packet**.
- `docs/technical/rune-system.md`, especially section 10 (the screen), section 4 (every rune) and
  "How the paths were built" with the two dev harness sections at the end.
- `docs/technical/art-guide.md`: every texture is **original, made by a seeded script in `tools/`**,
  never copied from vanilla, another mod or a pack. The rune art is made by
  `tools/generate-rune-art.py` (Pillow, seeded); extend it, do not hand-draw.
- `plans/session-prompts/2-rune-system.md` for how the rune system was asked for.

## What is true today

- **Every rune's running state is server-side only.** It sits in attachments, in
  `modules/combat/src/main/java/com/ascension/combat/internal/rune/RuneAttachments.java`, and none
  of it is sent to the client. Only the saved page and its growth counts are synced
  (`RunePageSyncPayload`). The attachments, by rune:
  - **Conqueror's Resolve:** `CONQUERORS_RESOLVE` (`CombatStackState`): a stack that builds one a
    second in combat. *This is the one Sanchit named.*
  - **Cadence:** `CADENCE` (`CadenceState`): the run of charged hits toward the hard third hit.
  - **Relentless Strikes, Momentum:** `RELENTLESS_STRIKES`, `MOMENTUM` (`StackingChargeState`).
  - **Second Wind, Killing Surge:** `SECOND_WIND_COOLDOWN`, `KILLING_SURGE_BURST`.
  - **Arc:** `ARC` (`ArcState`): hits counted toward the charged weapon, and whether it is charged.
  - **Starfall:** `STARFALL` (`StarfallState`): the cooldown and the star in the sky.
  - **Brace:** `BRACE` (`BraceState`), **Plating:** `PLATING` (`PlatingState`),
    **Riposte:** `RIPOSTE`, **Challenge:** `CHALLENGED` (`ChallengeState`), **Pack Leader:**
    `PACK_SAVE`.
  - **Drop In:** `DROP_IN` (its window). **Vanish:** `VANISH_WINDOW` and `VANISH_COOLDOWN`.
  - Exposed (Cadence's mark on an enemy) is on the *target*: `EXPOSED` / `ExposedState`.
  - Item abilities already sync their cooldowns to the client (`AbilityCooldownSyncPayload`, in
    `internal/net/`): look at how before inventing a second way.
- **A HUD layer example:** `modules/atmosphere/.../client/AtmosphereClient.java` registers one with
  `RegisterGuiLayersEvent` (the oxygen bar). Combat's own client code is in `client/` and
  `client/rune/` (`RuneKit` draws sprites with blending on; vanilla text and fills switch GL blending
  off, so every translucent sprite goes through `RuneKit.blit`).
- **The art kit:** `tools/generate-rune-art.py` exports 181 GUI sprites (`sprite_set()`,
  `export_gui()`, `kit_sheet()`), including each path's colours (`RuneKit.ramp`) and every rune's
  glyph (`rune/<name>` and `rune/<name>_open`). The HUD should look like the same family as the
  orrery: same path colours, same glyphs, same frame language.
- **The mod's other HUDs:** the oxygen bar (atmosphere) and whatever Jade and the damage-indicator
  compat jars draw. The rune HUD must not sit on top of them or the hotbar.

## The job

1. **Design it first, in a doc** (extend `docs/technical/rune-system.md`, or add
   `docs/technical/rune-hud.md` and link it), and show Sanchit mockups before you build:
   - *What* is worth showing. Not every rune: a stack that builds (Conqueror's, Relentless Strikes,
     Momentum), a charge that fills (Arc, Cadence), a cooldown that matters (Starfall, Brace, Second
     Wind, Vanish), a window that is open (Drop In, Vanish, Plating). Decide a rule, e.g. "show only
     what the equipped page makes active, fading when idle".
   - *How*: one compact cluster per equipped rune, near the crosshair or beside the hotbar? A ring
     around the rune's glyph filling as the charge builds? Numbers for stacks? Motion and a sound when
     a stack tops out? Position and scale configurable (a client config), and readable at GUI scales
     2, 3 and 4 and on a 1080p and a 4K window.
   - **Do not break the screen's family look.** Read section 10 and look at
     `design/runes/in-game/*.png`.
2. **Sync it properly.** The client needs each shown rune's state. Per ADR-0007 that is a **delta**:
   send when a value changes (a stack going up or down, a window opening or closing, a cooldown
   starting), carrying the *end game tick* for anything that counts down so the client animates it
   alone. Never one packet per tick. Only to the owning player. Dropping out of the world must clear
   the client's copy (no stale state, no static holding an entity).
3. **Build it** in `client/` with new sprites from `tools/generate-rune-art.py`. Keep edits to shared
   files small (`AscensionCombat.java`, the lang file, `CombatContent.java`). Add lang keys for every
   string.
4. **Prove it works**, ADR-0008 style, in a **running client**, not by compiling:
   - `client/dev/RuneEffectChecks` plays real fights and logs them; `client/dev/RuneScreenshots`
     photographs screens; both run on `client/dev/DevScript`. Extend one for the HUD: put a page on
     the player, start a fight, photograph the HUD with a stack at 0, mid, and full, at GUI scales
     2 to 4. Read the screenshots yourself and send them to Sanchit.
   - On a **dedicated server** too, since this is sync: a second player's HUD must show only their
     own runes.
   - Across a **relog**: the HUD must come back right, not stuck or empty.
5. **Unit-test the pure parts** (ratios, fill amounts, what to show for which page), and run
   `./gradlew build` (it runs `python tools/resource-ledger.py --check`).

## Rules that bind you

- Commit each step, ending the message with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- **Do not push**, and do not merge to main until Sanchit says.
- Original art only. Read stats only through `StatReader`.
- Only one dev server at a time (ports 25565 and 25575); close it when you are done.
- This shell has a guard that rejects complex commands in a worktree: write Python scripts to your
  scratchpad and run them, or use the file tools. Line-ending warnings are harmless.
- Plain language to Sanchit, recommendation first, and send screenshots and docs to his phone.

## Questions to put to Sanchit (do not decide these yourself)

- Which runes' state he wants on screen at all (he named Conqueror's stacks).
- Whether the HUD may be turned off or moved, and where by default.
- Whether a stack topping out should make a sound.
