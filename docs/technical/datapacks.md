# Datapacks

## Purpose

Defines how data-driven content — worldgen, loot, tags — is authored across a custom planet
versus a vanilla-owned dimension, and the pack structure that keeps the two from tangling.

## Two techniques, and which dimension decides which one applies

**A fully custom dimension (the Moon, Planet 3): edit its files directly.** Its biome, dimension
and dimension_type JSON are ours — `ascension_worlds` wrote them, per ADR-0004 — so a resource
exclusive to that world is one line in that biome's own `features` list. No indirection needed;
see `worldgen/biome/moon_surface.json`.

**Earth (or any vanilla-owned dimension): never edit vanilla's files. Use a NeoForge biome
modifier.** Overwriting `data/minecraft/worldgen/biome/*.json` wholesale is fragile — it silently
wins or loses against every other mod's own override of the same biome, depending on load order,
and it has to be kept in sync with vanilla by hand across every update. A biome modifier instead
*patches* an existing biome without replacing it, composes cleanly with whatever else touches
Earth, and is the same mechanism used later to prune a third-party mod's ores (`neoforge:
remove_features`) rather than fighting its worldgen directly.

```json
{
  "type": "neoforge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": "ascension_worlds:titanium_ore",
  "step": "underground_ores"
}
```

Lives at `data/<namespace>/neoforge/biome_modifier/<name>.json` — a datapack registry, same
mechanism as `worlds`' own `PLANET` registry, just owned by NeoForge instead of us. `step` takes
the same `GenerationStep.Decoration` name a custom biome's `features` array indexes by position
(`underground_ores` is index 6 — confirmed against vanilla's own `plains.json`).

**Removing a mod's ore later is the mirror image:** `neoforge:remove_features`, same shape, a
`features` list of the placed features to strip rather than add. Deferred until Create and
whichever other mods actually ship ores worth pruning — there is nothing to remove yet.

## The rest of the shared machinery

Every ore, custom or vanilla-biome-attached, is built from the same four pieces:

1. **`Block` + `Item`**, registered once (`AtmosphereContent`, `WorldsContent`) — reusable
   content, not a class per planet or per biome.
2. **`configured_feature`** (`minecraft:ore`, target list against
   `#minecraft:stone_ore_replaceables` / `#minecraft:deepslate_ore_replaceables`) and
   **`placed_feature`** (count, `in_square`, `height_range`, `biome`) — plain JSON, matched
   against vanilla's own `ore_iron.json` / `ore_iron_small.json` to keep the shape exactly right.
3. **`loot_table`** — what actually drops.
4. **Vanilla's own `mineable/pickaxe` and `needs_iron_tool` tags**, appended via
   `"replace": false` rather than a tag of our own — a tool-tier gate has to compose with every
   other mod's ores in the same tag, not sit beside it.

## Namespace strategy

Settled by using it twice: **our own namespace (`ascension_worlds`, `ascension_atmosphere`) for
everything we own — blocks, items, our own dimensions' JSON. Vanilla's own namespace
(`minecraft`) only for appending to a tag vanilla already defines, always with `"replace": false`
never `true`.** A biome modifier file is the one case that sits under neither: it is filed under
our namespace (it is our patch) but its registry folder (`neoforge/biome_modifier/`) is owned by
the mod that defines the mechanism, the same relationship `worldgen/biome/` has to vanilla.
