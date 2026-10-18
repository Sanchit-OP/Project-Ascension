# ADR-0022: The HUD is its own Tier 1 module, `ascension-hud`

- **Status:** Accepted, 2026-10-18 (Sanchit chose this option over a registry living in `core` alone,
  and over combat owning everything)
- **Date:** 2026-10-18
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md),
  [ADR-0007](0007-performance-contract.md), [ADR-0011](0011-tier-1-modules-share-contracts-through-core.md)

## Context

Sanchit wants the whole vanilla HUD replaced (prompt `plans/session-prompts/6-hud-overhaul.md`): a
health bar that scales, Armor and Magic Resist as stats, oxygen that appears only when it matters,
rune state, and a League-style stat column. That is one screen drawing things that belong to three
Tier 1 modules (atmosphere: oxygen; combat: stats, runes, growth; worlds later: the rot, dust
storms), and **Tier 1 modules may not depend on each other** (ADR-0011).

A HUD is also a single piece of layout. Anchors, scale, safe areas, opacity, a frame, a theme and a
"turn it all off" switch cannot be decided by three modules separately without three HUDs that
overlap and disagree.

## Decision

**1. A new Tier 1 module, `ascension-hud`, owns the layout, the theme and the frame.** It depends
only on `core`. It decides where every element sits, at what scale and opacity, draws the shared
frame and backing, and handles the client config, the edit-free anchors and the vanilla-layer
switch.

**2. The contract lives in `core`** (ADR-0011): a small `HudElement` interface and a provider
registry. An element has an id, a preferred anchor, a size, a "wants to be visible now" check, and
a draw call that reads **its own module's cached client state**. Atmosphere registers the oxygen
gauge, combat registers the stat column, rune cluster and growth marker, and a third party can
register theirs without depending on `ascension-hud` at all (ADR-0003 rule 3). `core` holds the
interface and registry only; it draws nothing and changes nothing in the game alone.

**3. The HUD module ships the elements that need nothing but vanilla**: health (current, maximum,
absorption), food, level, hotbar surroundings, the low-health warning. Those read the player's
vanilla attributes, so `hud` works with only `core` present. Anything that needs another module's
state (Armor stat, Magic Resist, oxygen, runes) is that module's element.

**4. Every module still works alone** (ADR-0003 rule 5). With `hud` absent, atmosphere falls back
to its own simple oxygen bar and combat to its existing screens; with only `hud` and `core`, the
player gets the health, food and level elements. Neither module needs the other to load.

**5. A pack that wants vanilla back gets it from one client config switch** that stops `hud` from
cancelling vanilla layers and stops it drawing. Elements that fall back (oxygen) stay.

**6. Performance (ADR-0007):** draw calls run every frame, so an element reads cached client state
only, allocates nothing in the hot path, scans no world and holds no `Level`, `Player` or `Entity`
in a static. Sync is server-authoritative and delta-synced by the module that owns the data; `hud`
adds no packets of its own. Teardown hooks to disconnect.

**7. No mixins** (ADR-0003 rule 2). NeoForge's `RegisterGuiLayersEvent` and
`RenderGuiLayerEvent.Pre` are enough to add our layers and cancel vanilla's.

## Consequences

- A fifth Tier 1 module: `core` → `atmosphere`, `worlds`, `combat`, `hud` → `compat/*`.
  `architecture.md` and the Gradle module list change with the first code.
- `core` gains a HUD contract with a semver obligation. It is added when the first two elements
  need it (oxygen and health), not earlier, per ADR-0011.
- Atmosphere's `OxygenHudLayer` becomes an element behind the contract, with its current layer kept
  as the fallback when `hud` is not installed.
- Other mods' overlays (Jade, JEI, boss bars, FTB Quests) are not ours to move; the layout keeps
  room for them and the config can shift or hide each of ours.
- The character sheet (V) is not decided here. Whether the stat column retires it is a separate
  question to Sanchit (prompt 6).

## Built (2026-10-18)

The contract in `core` grew to what the first elements needed, and no more: `HudElement` (anchor, size,
visible, render), `HudAnchor`, `HudRegistry`, and **`HudTheme`**, which the host hands every element each
frame (panel, divider, icons, ring badge, ring, glow, screen-edge glow, the two typefaces). Elements draw
with the theme and never ship their own look, so the whole HUD is one family and a different host could
restyle all of it. `HudIcon` is the icon vocabulary and may only be appended to.

Elements by owner: the health row is `ascension-hud`'s (vanilla state only); the stat block, rune chips and
skill-points note are combat's; the oxygen emblem is atmosphere's. Each registers through core and works
with or without a host (atmosphere keeps its plain bar as the fallback). Neither combat nor atmosphere
depends on the HUD module.

## Alternatives rejected

**Registry in `core`, elements drawn by their owners, layout split between them.** Each module
would place its own element, so layout, scale and the config would live in three places and
overlap. Rejected for that reason.

**Combat owns everything, atmosphere keeps a small fallback.** Simplest, but oxygen would look
different whenever combat is absent, and a pack author wanting only the HUD would have to take
combat's items, stats and runes with it. Rejected on the compatibility priority.
