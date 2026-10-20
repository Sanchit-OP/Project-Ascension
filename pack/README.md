# pack/

What the finished pack ships **besides** its mods: configuration that players get on a fresh install.
It is separate from `run/` (throwaway dev state, never committed) and from `config/` inside a world.

| Path | What it is |
|---|---|
| `config/defaultoptions/keybindings.txt` | Our default keys, applied by the Default Options mod to a fresh install and to any key a player has not yet had saved. One `key_<binding name>:<key>` line each; `key.keyboard.unknown` means unbound. The reasoning is in `docs/technical/controls.md`. |
| `config/DistantHorizons.toml` | **Only the settings the pack decides**, not the whole file: no chat warning about Chunky (or any other mod) on startup, and far terrain built only from chunks that exist (`PRE_EXISTING_ONLY`, `docs/technical/optimisation-stack.md`). Distant Horizons fills in every other setting, so a player's own render distance and quality stay theirs. |
| `config/` (the rest) | `cataclysm-common.toml` (the bosses' Cataclysm multipliers, moved here from `serverpack/config` on 2026-10-21 so a single-player world and the server agree), `waystones-common.toml` (refuses any warp between worlds), `defaultoptions/options.txt` (first-launch options: no accessibility prompt, no tutorial toasts), `ftbquests/` (the generated quest book). Partial files are merged by `tools/sync-pack-config.py` and filled in by the mod on first start. |
| `pack.json` | The pack's name, version and loader, and what is left out of its manifest. |
| `curseforge-extras.json`, `curseforge-pins.json` | The mods the launcher does not track (by CurseForge id) and the files the pack names instead of ones CurseForge no longer has. |
| `launch/` | The text for the CurseForge page, the changelog, the known issues and the install notes. |
| Building it | `./gradlew build`, then `python tools/build-curseforge-pack.py` writes `build/pack/<name>-<version>.zip`, then `python tools/check-curseforge-ids.py` asks CurseForge whether every file the manifest names still exists. Run the check again right before uploading. |

`python tools/sync-pack-config.py` copies everything under `pack/config/` into the dev client
(`run/client/config`) and the CurseForge test instance. A `.toml` file that already exists there is
**merged**, not replaced: only the lines the pack file names are set. Add `--options <path to options.txt>` to also
move the saved keys in an existing game, which Default Options deliberately does not do.

Not here, on purpose: mods and shader packs. Their licences ask to be added through Modrinth or
CurseForge, not as files we upload (`docs/technical/pack-extras.md`).
