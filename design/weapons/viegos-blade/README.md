# Viego-inspired sword

Minecraft Java 1.21.1 / NeoForge. Uses the existing item ID
`ascension_combat:veigos_blade` (the original spelling is preserved).

## Files

- `veigos_blade.bbmodel`: editable Blockbench Java Block/Item project, with embedded texture.
- `first-person.png`: screenshot from the Minecraft dev client after the final hand adjustment.
- Runtime model: `modules/combat/src/main/resources/assets/ascension_combat/models/item/veigos_blade_3d.json`
  (in hand). Since 2026-10-06 `veigos_blade.json` is a `neoforge:separate_transforms` wrapper:
  this 3D model in hand, and a flat 16 x 16 icon (`veigos_blade_icon`, from
  `tools/generate-combat-art.py`) in the inventory, on the ground and in item frames.
- Runtime texture: `modules/combat/src/main/resources/assets/ascension_combat/textures/item/veigos_blade_3d.png`.

The model follows the two supplied reference images: long narrow blade, small blade guard,
wrapped ricasso, large crescent guards, diamond socket, and an open crescent pommel.
The side profile is an interpretation because the references primarily show the broad face.
It has 373 cuboids in 11 named groups and a 64x64 material atlas. It uses standard Java item
geometry; no renderer, animation library, particles, or shader dependency is added.
The pale edges are textured metal, not an animated aura or a dynamic light source.

## Editing

Open the `.bbmodel` in Blockbench. Keep the Java Block/Item format and the 1.21.1 restrictions:
one rotation axis per cube, rotations in steps of 22.5 degrees between -45 and 45, and
element coordinates between -16 and 32. Export the Block/Item JSON to the runtime model path (`veigos_blade_3d.json`).
Keep the texture resource location `ascension_combat:item/veigos_blade_3d`.
Do not add `minecraft:item/handheld` as a parent: its generated geometry replaces cuboid elements.

`node tools/generate-viegos-blade.mjs` recreates the initial geometry, atlas, and display settings.
This overwrites the editable source and runtime assets; preserve any manual Blockbench edits first.
The existing red sword texture remains available at `textures/item/veigos_blade.png`.

## Verification — 2026-09-09

- Opened in Blockbench with all geometry and the embedded atlas visible.
- Inspected Blockbench third-person placement and inventory framing.
- `./gradlew :modules:combat:build -PnoDevMods --offline` passed.
- Launched `:modules:combat:runClient -PnoDevMods --offline` with combat and core only.
- Created the disposable creative superflat world `Viego sword visual test` in `run/client`.
- Equipped with `/give @s ascension_combat:veigos_blade` and observed in first and third person.
- Reloaded resources with F3+T and verified the revised first-person position, showing the large guard.
- Existing damage/stats behavior was not changed. Other modpack combinations and left-hand placement
  were not tested in the client; this was a visual asset check in the isolated dev client.

The built mod is `modules/combat/build/libs/ascension_combat-0.1.0.jar`. It has not been copied
into the CurseForge instance. The editable `.bbmodel` is an authoring file; Minecraft loads
the exported JSON and PNG packaged in the mod.
