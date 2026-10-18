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
| **Stat block** | Two columns of three, icon plus number only, in a glass panel bottom-aligned just **left of the hotbar** (League's block). Offence left (Attack Damage, Attack Speed, Omnivamp), defence right (Armor, Magic Resist, Move Speed, shown as a boot). A buff turns the number green, a debuff red. No names, no expanded view, no key to hold. Details live in the tooltip and the character sheet. |
| **Health** | One smooth bar above the hotbar, a subtle tick every 100 HP, numbers above its left end. **Green is health now, always, even when low.** After it, left to right: **white = shield**, **grey = health about to regenerate** (shown only when known, e.g. a Regeneration effect's remaining heals), **red = health just lost**, fading out. `+N` labels above the right end. Same bar from 20 to 1,000. |
| **Level** | A round badge on the bar's left end; its ring is the XP progress. Always visible (levels are spent). |
| **Hunger** | A round badge at the bar's right end, mirroring the level badge. Its ring is the food level; it glows green at 18+ (natural regeneration allowed) and reddens when low, so a player sees at a glance why they are or are not healing. |
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

## 6. Build status

Built and seen in a client (2026-10-18), each photographed at GUI scales 2 to 4:

1. **Shell:** the contract in `core` (`HudElement`, `HudAnchor`, `HudRegistry`, `HudTheme`, `HudIcon`),
   the `ascension-hud` module, a layout (`HudLayout`, 8 tests) and a client config (`enabled`, `opacity`).
2. **Health row** (hud): level badge, health bar (green now, white shield, grey regeneration, red just
   lost), hunger badge. Vanilla hearts, armour, food and XP are hidden while the HUD is on.
3. **Stat block** (combat): two columns, icon and number, left of the hotbar. Move Speed ignores the
   sprint boost; Attack Speed reads against the bare hand (1.00); a number turns green or red for three
   seconds after a buff or debuff.
4. **Oxygen emblem** (atmosphere): a big O2 that rises as air runs low, amber then red with the screen
   edge reddening; a quiet badge in good air. Atmosphere's plain bar remains as the fallback when no
   HUD host is installed.
5. **Rune chips** (combat): a round chip for each equipped rune with running state, right of the
   hotbar; delta-synced (`RuneHudSyncPayload`, sent only when the list changes, only to the owner).
   Stacks and charges fill a ring and carry a number; a cooldown refills its ring; a window drains it.
6. **Skill points note** (combat): gold, above the health row, only while points are unspent.

7. **Lungs and tank** on the oxygen emblem (atmosphere): the outer ring is the lungs, an inner amber ring
   is the tank, once the player carries one (`OxygenSyncPayload` now carries the lungs' share).
8. **Config** (`config/ascension_hud-client.toml`): `enabled`, `opacity`, `scale` (0.6 to 1.6) and
   `hiddenElements` (a list of element ids). Hiding the health row gives vanilla's hearts back.
9. **Smooth rune glyphs** for the chips (`tools/generate-hud-rune-glyphs.py`), tinted by path.

**Left alone on purpose, with reasons:** the **hotbar** (it is the player's inventory strip and other mods
hook it), the **boss bar** (Cataclysm and FTB draw their own at the top centre, clear of everything here),
**status effects** (vanilla's top-right, clear of everything here) and **item abilities** (they already show
their cooldown on the item itself, `AbilityCooldownDecorator`). Revisit if they collide in play.

**Not yet:** position offsets per element (only global scale and per-element hiding); a dedicated-server check
with **two players** (the server boots and registers the packet, but a second client has not been used); a
relog check by script; the frame-cost reading in `performance-log.md` (needs Spark with the HUD on and off);
smooth glyphs for runes that have no state to show (they have no chip).

## 7. How it is checked

Dormant dev scripts photograph the real HUD without a person playing. Each is armed by an empty file
in `run/client/` and deletes it when it starts, then opens the `runes-test` world, sets states on the
server, photographs at several GUI scales, and quits:

| Trigger file | Script | Photographs |
|---|---|---|
| `hud-screenshots.txt` | `ascension-hud`'s `HudScreenshots` | health row and oxygen emblem |
| `rune-hud-screenshots.txt` | combat's `RuneHudScreenshots` | rune chips |

Launch with `./gradlew :modules:hud:runClient` (it also loads combat and atmosphere; `-PhudOnly` runs
the HUD with nothing but core). Pictures land in `run/client/screenshots/hud-*.png`. Only one of the
two scripts should be armed at a time.
