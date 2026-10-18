# Session prompt 6: the HUD overhaul

Paste everything below the line into a new Claude Code session opened on this repository.

This **replaces `5-rune-hud.md`** (the rune HUD was going to be its own session; it is now one part
of this). Read that file too: its notes on what the rune system keeps server-side, and how to sync
it as deltas, still apply.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **the whole HUD**: what sits on the player's screen
while they play. Vanilla's HUD was made for 20 hearts and a full inventory of nothing; ours has
characters who gain health all game, wear armour that is a stat from 0 to 300, carry rune pages that
do things in a fight, and fly to planets with no air. The vanilla HUD does not fit that, and Sanchit
wants it replaced, not decorated.

## Sanchit's words (2026-10-18)

> actually hud overhaul. vanilla hud will not work for us. hearts will be bad as we will have much
> more hp gaining consistently. permanently showing oxygen as a sec also looks bad. and we don't
> even show the effects from the runes, active or not. also instead of having the stat screen we can
> use how League shows the stats in the side of the HUD area. so we need to make the whole HUD
> overhaul.

His standing wish for every screen applies: **professional level**, the same family as the orrery
(rune screen) and the skill tree. Not vanilla buttons and text. He decides the look from mockups
before anything is built.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These bind you:
  - **0003:** no Tier-1 module hard-depends on a third-party mod; mixins never in Tier 1; foreign
    integration is a Tier-2 `ascension-compat-*` jar.
  - **0007:** performance. A HUD runs **every frame**: no allocation per frame in the hot path, no
    world or entity scans, nothing static holding a Level, Player or Entity; read cached client state
    only. Sync is server-authoritative and delta-synced, **never a per-tick packet**.
  - **0008:** it is done when you have seen it in a running client; sync is checked on a dedicated
    server; anything persistent, across a reload.
  - **0011:** Tier-1 modules never depend on each other; they share contracts through `core`.
    **This is the central design problem here, see below.**
- `plans/session-prompts/5-rune-hud.md`: the rune state that exists server-side, and which attachments
  hold it.
- `docs/technical/rune-system.md` §10 (the screen) and §8 (what a rune shows when it fires);
  `design/runes/in-game/*.png` and `design/runes/kit.png` show the look to match.
- `docs/technical/item-stats.md` §3, §6, §7: the stats, `StatReader`, and the tooltip and sheet that
  show them today. `docs/technical/combat-api.md` §3: the defence curve (Armor 0 to 300; every 100
  halves damage).
- `docs/progression/growth.md` §10: the skill-tree screen; `docs/technical/atmosphere-api.md`: the
  oxygen model (lungs and tanks), and `modules/atmosphere/.../client/OxygenHudLayer.java`.
- `docs/technical/art-guide.md`: every texture is **original, made by a seeded script in `tools/`**,
  never copied. Extend `tools/generate-rune-art.py` (181 sprites, Pillow, seeded) rather than
  starting a third art kit.

## What is true today

- **Vanilla layers still draw:** hearts, armour icons, food, the XP bar and level, the air bubbles
  (hidden when our oxygen bar is on), status effects, the hotbar, the boss bar.
  NeoForge 1.21.1 lets a mod register its own layers and cancel a vanilla one
  (`RegisterGuiLayersEvent`, `RenderGuiLayerEvent.Pre`, `VanillaGuiLayers`), with no mixin.
- **Oxygen** (`ascension-atmosphere`): a bar with the time left **in seconds**, hidden in breathable
  air and shown in the vanilla air slot otherwise. It reads `ClientOxygenState`. Lungs (20 s,
  refill free in breathable air) and tanks (drained first, never self-refill) are two sources.
- **Health:** a player's maximum is vanilla 20 plus gear, growth and runes: 63 in a full netherite
  set with a few nodes, and it will climb far past that. Absorption exists. The vanilla heart row
  cannot show it.
- **Armour:** vanilla's icons show vanilla armour points, which no longer matter. The real number is
  our Armor stat (netherite is 74, a boss has 100). Magic Resist is a second stat with no HUD at all.
- **Runes:** 45, with running state held server-side (stacks, charges, cooldowns, windows) and **none
  of it sent to the client**. Only the saved page and its growth counts are synced. Item abilities
  already sync their cooldowns (`AbilityCooldownSyncPayload`, `AbilityCooldownDecorator`).
- **Stats:** the character sheet (`CharacterSheetScreen`, key V) is plain vanilla-looking text with a
  hover card showing each stat's sources. Sanchit wants League's way instead: a compact stat panel
  beside the HUD.
- **Levels** (vanilla XP) are a currency now: enchanting (`docs/progression/enchantments.md`), a
  rune change (3 levels) and a skill-tree respec (20 levels). The level number must stay visible.
- **Growth** has unspent skill points. Nothing on screen says so except a chat line.
- **Other HUDs and overlays in the pack:** Jade (tooltips), the damage-indicator compat jar, JEI,
  boss bars from Cataclysm, and soon Iris shaders, Zoomify and FTB Quests/Teams (the logic session is
  adding them). Yours must sit beside them, not on top.

## The design problem to settle first: who owns the HUD

The HUD shows things from **three Tier-1 modules that may not depend on each other**: oxygen
(atmosphere), health, stats, runes and growth (combat), and later planet hazards (worlds: the rot,
dust storms). Options, for you to weigh and put to Sanchit with a recommendation:

- **Contracts in `core`, elements from their owners.** Core defines a small HUD registry (extension
  by provider, ADR-0003 rule 3): an element has an id, an anchor, a size and a draw call reading its
  own module's cached client state. Atmosphere registers the oxygen gauge; combat registers health,
  stats and runes; the layout and the frame live in one place. Every module still works alone.
- **A new Tier-1 `ascension-hud` module** that depends only on core and owns layout, themes and the
  frame, with the other modules contributing elements to it.
- **Combat owns everything,** and atmosphere keeps a small fallback gauge. Simplest, but then oxygen
  looks different when combat is not installed.

Whichever it is: **every module must still load and be usable with only core present** (ADR-0003
rule 5), and a pack that wants vanilla's HUD back must get it from one client config switch.
Write the choice as an ADR.

## The job

1. **Research, then design in a doc** (`docs/technical/hud.md`, linked from the rune and growth
   docs). Cover what good HUDs do: League's bottom bar and stat panel, Souls-likes, Diablo-style
   globes, Terraria, Hades, Path of Exile, well-made Minecraft HUD mods (look at how they scale
   health, how they hide what is irrelevant, how they animate change). End with what we can add
   and what we should add. Decide, with mockups:
   - **Health that scales.** One bar, whose number and length stay readable from 20 to 1,000: a
     smoothed fill, a pale "just lost" trail, a healing flash, absorption and shield as their own
     colour, the numbers current / max. A warning at low health that does not need hearts.
   - **Armour and Magic Resist.** Show the stat, not vanilla's icons. A defence curve makes the
     number meaningful ("takes 40% less damage"); decide whether to show that.
   - **Food** (keep, restyle or move: it still gates sprinting and regeneration) and **the level**
     (visible, since levels are spent).
   - **Oxygen that is not a permanent number of seconds.** It should say nothing when air is fine,
     rise into view as it drops, and show lungs and tank as one picture that reads at a glance:
     colour and shape change before the number matters, a tank's charge, a warning that hits the
     whole screen edge, not only a bar. Sealed rooms and share-air also have state worth a mark.
   - **Runes.** What each equipped rune is doing right now, active or not (**replaces
     `5-rune-hud.md`**). The first thing missing is Conqueror's Resolve's stack count. Decide a rule
     (for example: show what the equipped page makes live, dim when idle, brighten when it fires;
     stacks as numbers or pips, charges as a filling ring, cooldowns as a sweep).
   - **League's stat panel at the side:** a compact column of the stats (Attack Damage, Attack
     Speed, Armor, Magic Resist, Move Speed, Lethality and Penetration, Omnivamp), each with its
     icon and number, changing colour when a buff or debuff moves it. The mouse is grabbed in
     play, so decide how to see the sources: a held key that expands the panel, or only the screen.
     **Decide whether this retires `CharacterSheetScreen`** (V).
   - **Item abilities, status effects, the boss bar, unspent skill points** and the hotbar's
     surroundings: restyled, moved or left alone, each with a reason.
   - **Layout:** anchors that adapt to GUI scales 2 to 4 and to ultrawide and 4K, safe areas, and
     a client config for scale, position, opacity and each element's visibility.
2. **Show mockups before building.** Render PNGs from a script (the art guide shows how), at several
   GUI scales and with a sample scene (full health, low health, in vacuum, a rune page mid-fight),
   and send them to Sanchit's phone. Build only after he likes one.
3. **Build in increments,** each seen in a client: the shell and the layout; health; the stat panel;
   oxygen; the runes (with their delta sync); the rest. A refactor pass every third increment.
4. **Prove it, ADR-0008 style:**
   - `client/dev/DevScript` is the shared spine for scripted client runs (`RuneScreenshots` photographs
     screens, `RuneEffectChecks` plays fights, `IntegrationChecks` checks the merged combat systems and
     is described in `docs/technical/dev-environment.md`). Extend one for the HUD: photograph every
     element in its states at GUI scales 2 to 4, and read the pictures yourself.
   - On a **dedicated server**: a second player's HUD shows only their own runes.
   - After a **relog**: nothing stale, nothing missing.
   - A **frame-cost reading** in `docs/technical/performance-log.md` (Spark, with the HUD on and
     off), because it runs every frame.
   - Compatibility: with Sodium, ImmediatelyFast and Distant Horizons as in the current pack; vanilla
     text and fills switch GL blending off, so every translucent sprite needs
     `RenderSystem.enableBlend()` first (the rune screen already learned this: `RuneKit.blit`).

## Working beside other sessions

- **Enchantments (prompt 7)** is a separate session that feeds enchantment effects into the stat
  system. You only read stats, through `com.ascension.combat.api.stat.StatReader`; never read around
  it, and ask the stats owner (`docs/technical/combat-api.md` §3, "Requested stats") for a new stat.
- **Build paths (prompt 4)** may start later and add items; the HUD must not hardcode the item list.
- **The logic session** (the one that wrote this) is adding quality-of-life and team mods to the
  pack. Keep your layout room for FTB Quests' and others' own overlays.
- **Work in your own git worktree and branch** (for example `combat-hud`). If the app did not make
  one, run `git worktree add`. Merge into `main` only when Sanchit says.
- **Keep edits to shared files small:** `AscensionCombat.java`, the lang file, `CombatContent.java`.
- **Only one dev server can run at a time** (ports 25565 and 25575). Check before
  `./gradlew runDevServer`, and never stop another session's.

## How Sanchit works

- **Commit each step,** ending the message with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
  **Never push** unless he asks.
- **Original art only.** Seeded generator scripts in `tools/`.
- **Plain language.** Keep docs handoff-ready, send docs and mockups to his phone (`SendUserFile`),
  and when he must choose, ask a clear question with your recommendation first.
- Every build runs `tools/resource-ledger.py` and `tools/check-data.py` (Python on PATH).
- This shell's guard may reject complex commands: write Python scripts to your scratchpad and run
  them, or use the file tools.

## Questions to put to Sanchit (do not decide these yourself)

- Who owns the HUD (the three options above).
- Does the stat panel retire the character sheet?
- Which runes' state he wants on screen, and whether a stack topping out should make a sound.
- Whether food stays visible, and the oxygen gauge's look.
- Where everything sits by default, and what a pack author can switch off.
