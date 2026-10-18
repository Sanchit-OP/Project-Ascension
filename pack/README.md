# pack/

What the finished pack ships **besides** its mods: configuration that players get on a fresh install.
It is separate from `run/` (throwaway dev state, never committed) and from `config/` inside a world.

| Path | What it is |
|---|---|
| `config/defaultoptions/keybindings.txt` | Our default keys, applied by the Default Options mod to a fresh install and to any key a player has not yet had saved. One `key_<binding name>:<key>` line each; `key.keyboard.unknown` means unbound. The reasoning is in `docs/technical/controls.md`. |
| `config/DistantHorizons.toml` | **Only the settings the pack decides**, not the whole file: no chat warning about Chunky (or any other mod) on startup, and far terrain built only from chunks that exist (`PRE_EXISTING_ONLY`, `docs/technical/optimisation-stack.md`). Distant Horizons fills in every other setting, so a player's own render distance and quality stay theirs. |

`python tools/sync-pack-config.py` copies everything under `pack/config/` into the dev client
(`run/client/config`) and the CurseForge test instance. A `.toml` file that already exists there is
**merged**, not replaced: only the lines the pack file names are set. Add `--options <path to options.txt>` to also
move the saved keys in an existing game, which Default Options deliberately does not do.

Not here, on purpose: mods and shader packs. Their licences ask to be added through Modrinth or
CurseForge, not as files we upload (`docs/technical/pack-extras.md`).
