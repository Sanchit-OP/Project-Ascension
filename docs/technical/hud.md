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

- Does the stat block retire `CharacterSheetScreen` (V)? The block is now deliberately simple, so
  recommendation: **keep the sheet** for sources and names, reached from the block or key V.
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

## 4. The proposed layout (v2, after Sanchit's notes of 2026-10-18)

Sanchit's direction: **premium, smooth, not pixel art; no bars for oxygen; a League-style stat block
just left of the hotbar, simple and with no detail.** Sizes below are GUI units.

| Element | Proposal |
|---|---|
| **Stat block** | Two columns of three, icon plus number only, in a glass panel bottom-aligned just **left of the hotbar** (League's block). Offence left (Attack Damage, Attack Speed, Omnivamp), defence right (Armor, Magic Resist, Move Speed). A buff turns the number green, a debuff red. No names, no expanded view, no key to hold. Details live in the tooltip and the character sheet. |
| **Health** | One smooth bar above the hotbar, hotbar-wide, subtle tick every 100 HP, numbers centred on it in a clean face. Pale trail = health just lost; gold strip = absorption, `+N` above its right end; red below 30%. Same bar from 20 to 1,000. |
| **Level** | A round badge on the bar's left end; its ring is the XP progress. Always visible (levels are spent). |
| **Food** | A short capsule right of the health bar. |
| **Oxygen** | **No bar.** A large **O₂** emblem rises above the hotbar only as air runs low: the outer ring is the lungs, an inner amber ring is the tank. It reddens and glows as it empties, shows seconds beneath only when low, and the screen edge reddens. In fine air in space it is a small quiet badge. Pressurising and share-air are marks on the emblem. |
| **Runes** | Round glass discs right of the hotbar, the orrery's glyphs and path colours. Live = bright with a glow; idle = dim. Stacks are a number badge, a charge is a ring that fills, a cooldown is a sweep. |
| **Armor, Magic Resist numbers** | In the stat block. Vanilla's armour icons are hidden. |
| **Unspent skill points, item abilities, status effects, boss bar** | Not in the mockups yet; moved only if they collide. |

**How we get "premium" in Minecraft** (decided in principle, to prove in a client): no mod is
needed. Panels, rings, icons and glows are **high-resolution textures drawn smoothly** by our seeded
generator (rounded 9-slice panels, ring and glow sprites, vector icons) and shown with linear
filtering; the text uses a **bundled open-licence TTF** through Minecraft's own font system, so it is
smooth rather than blocky; round gauges are drawn by a small number of textured fans or a shader,
and the shader route is tested with Iris first. A helper mod would break ADR-0003 (Tier 1 takes no
third-party dependency), so if one proves necessary it goes in a Tier 2 compat jar, optional.
**Risk to check early:** the orrery and skill-tree screens are pixel-styled; the HUD will be smooth,
so the two should be put side by side and Sanchit asked if the screens follow later.

## 5. Mockups (v2)

`design/hud/`, made by `tools/render-hud-mockups.py` (vector, drawn at 3x, stand-in font):
`calm-scale3`, `fight-scale3` (low health, absorption, buffed stats, three runes), `vacuum-scale3`
(O₂ critical, screen edge red), `vacuum-mid-scale3` (O₂ at 42%), `vacuum-ok-scale3` (the quiet
badge), `fight-scale2` / `fight-scale4`. Known gaps: the rune glyphs are still the pixel art
smoothed up, so they should be redrawn as vector; the real font is not chosen; skill points, status
effects and share-air are not drawn.

## 6. Build order

Shell and layout, health, stat panel, oxygen, runes (with delta sync), the rest; a refactor pass
every third increment. Proof per ADR-0008: scripted screenshots at GUI scales 2 to 4, a dedicated
server with two players, a relog, and a frame-cost reading in `performance-log.md`.
