# The HUD

Status: **design and mockups, 2026-10-18; nothing built.** Branch `combat-hud`.
Brief: [`plans/session-prompts/6-hud-overhaul.md`](../../plans/session-prompts/6-hud-overhaul.md).
Decision: [ADR-0022](../decisions/0022-the-hud-is-its-own-module.md), `ascension-hud` is a Tier 1
module depending only on `core`; other modules contribute elements through a contract in `core`.
Mockups: [`design/hud/`](../../design/hud/), made by `tools/render-hud-mockups.py` (seeded, our own
colours, glyphs and stand-in font from the rune art kit).

## 1. What is decided

- Who owns it: `ascension-hud` owns layout, theme, frame, config (Sanchit, 2026-10-18).
- It ships the vanilla-only elements (health, food, level); other modules register theirs.
- One client config switch gives vanilla's HUD back.

## 2. Still open (Sanchit decides)

- Does the stat column retire `CharacterSheetScreen` (V)? Recommendation: **yes for reading, no for
  the hover sources**: the column shows numbers, a held key (default Alt) expands it with names and
  what each number means; the sheet's per-stat source list moves into that expanded view.
- Which runes' state is shown, and does a full stack make a sound? Recommendation: show only what the
  equipped page makes live; a soft tick on a stack topping out, off by default.
- Does food stay visible? Recommendation: keep, as a short bar, not ten icons.
- Default positions, and what a pack author may switch off. Recommendation: every element has its own
  on/off, anchor and opacity in the client config.

## 3. Research

Sources were thin on the web (searches turned up engine forums and mod pages, not UX write-ups), so
this leans on the games and mods themselves.

- **League of Legends:** the stat column is the model Sanchit named: icon plus number, colour by stat,
  full detail on hover. Its target frame lists health, resource, AD, AP, armor, MR, attack speed,
  haste, crit and move speed ([wiki](https://wiki.leagueoflegends.com/en-us/Target_Frame)).
- **Overflowing Bars (Minecraft):** stacks extra health into layers with one highlight colour and a
  counter, instead of more rows; adds an armour-toughness bar
  ([moddex](https://moddex.gg/mod/overflowing-bars)). Right idea (one row), but layers hide the
  magnitude; we prefer one bar with ticks.
- **Absorption in HUD mods:** eXc BetterInfo gives it its own row; Njol's HUD draws it in or behind
  the bar with a cap on stacked bars ([moddex](https://moddex.gg/mod/njols-hud)). We draw it as a gold
  strip on the bar's top half.
- **AppleSkin-style food:** a saturation outline and a held-food preview. Worth a later increment,
  not a first one.
- **Health-scale plugins** render a fixed number of hearts regardless of real health
  ([Modrinth](https://modrinth.com/mod/healthscale)). Fine for servers, wrong for us: it hides how
  much health a player has.
- **Diablo globes, Souls bars, Hades, Terraria, Path of Exile:** the habits worth borrowing, from
  general knowledge and not checked against sources here: a bar that **grows in length** with the
  maximum (Souls), a **pale "lost" trail** that drains after a hit, flashing at low health, and
  showing resources as shapes before numbers.

**What we add:** a trail, ticks per 100 HP, absorption strip, one oxygen gauge with two sources,
rune chips, a stat column with buff and debuff colours, a damage-reduction readout.
**What we should add (later):** food preview, a held-key expand, a low-health edge pulse.
**What we leave alone:** the hotbar and the boss bar (Cataclysm draws its own), Jade, JEI.

## 4. The proposed layout (see the mockups)

| Element | Proposal |
|---|---|
| **Health** | One bar above the hotbar's left end, 100 GUI px wide, ticks every 100 HP, numbers **beside** it (text on a fill is unreadable). Pale trail = health just lost; gold strip on top = absorption, with `+N` above; red below 30%. Same bar from 20 to 1,000. |
| **Level** | A small gem on the XP line with the number, always visible (levels are spent: enchanting, rune change, respec). |
| **XP** | A thin line across the hotbar's top edge. |
| **Food** | A short bar right of the hotbar, above the rune chips. |
| **Armor, Magic Resist** | In the stat column. Vanilla's armour icons are hidden. Expanded view adds "-29% physical" and "-21% magic" from the defence curve. |
| **Stat column** | Left edge, vertically centred: Attack Damage, Attack Speed, Armor, Magic Resist, Move Speed. Number coloured by stat; green when a buff lifts it, red when a debuff cuts it. Hold a key to expand it with names and sources. Lethality, Armor Penetration and Omnivamp appear only when non-zero. |
| **Oxygen** | Absent in breathable air. Appears as one gauge when air is short: lungs (blue, first half) and tank (amber, second half), drained tank-first. The number appears only under about 10 s, and the screen edge reddens as it drops (the mockup `vacuum-scale3`). Pressurising and share-air get a mark on the gauge. |
| **Runes** | A chip per equipped rune right of the hotbar, using the orrery's glyphs and path colours. Live runes are bright with a top rule; idle ones dim. Stacks are a number, charges fill a line along the chip's bottom, cooldowns sweep a dark cap down. |
| **Unspent skill points** | A small mark on the level gem (not drawn in the mockups yet). |
| **Item abilities, status effects, boss bar** | Not redesigned in the first pass; moved only if they collide. |

Layout is in GUI pixels from the hotbar and the screen edges, so it holds at scales 2 to 4
(`fight-scale2/3/4`) and on ultrawide. The mockups are 1920×1080; 4K needs the same check in game.

## 5. Mockups

`design/hud/`: `calm-scale3`, `fight-scale3` (low health, absorption, three runes), `vacuum-scale3`
(oxygen critical), `vacuum-ok-scale3` (lungs and tank full, no number), `expanded-scale3` (stat
column opened), and `fight-scale2` / `fight-scale4`. Known gaps: the stand-in font is not Minecraft's;
the rune cooldown chip is still too dark; skill points, status effects and share-air are not drawn.

## 6. Build order

Shell and layout, health, stat panel, oxygen, runes (with delta sync), the rest; a refactor pass
every third increment. Proof per ADR-0008: scripted screenshots at GUI scales 2 to 4, a dedicated
server with two players, a relog, and a frame-cost reading in `performance-log.md`.
