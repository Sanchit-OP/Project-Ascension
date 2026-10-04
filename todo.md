# Todo

Open questions only. **Settled decisions live in [`docs/decisions/`](docs/decisions/)** — check
there before reopening anything. Milestone work in flight lives in [`plans/`](plans/) — currently
[`plans/m3-vertical-slice.md`](plans/m3-vertical-slice.md).

Last reviewed: 2026-10-05.

---

## Resolved — moved out of this list

See the register or the linked doc for reasoning:

- ~~Define what requires a custom mod~~ → ADR-0002
- ~~Target Minecraft and loader version~~ → ADR-0001
- ~~Planet framework approach~~ → ADR-0004
- ~~v1 scope~~ → ADR-0005
- ~~Whether oxygen is custom or mod-based~~ → ADR-0003 / M1
- ~~Atmosphere API review questions (UI visibility, rescue, `drainMultiplier`, units)~~ →
  settled when the API froze, 2026-09-05; [`atmosphere-api.md`](docs/technical/atmosphere-api.md)
- ~~Optimisation mods at the M2 boundary, and Spark~~ → installed;
  [`optimisation-stack.md`](docs/technical/optimisation-stack.md)
- ~~Is orbit a separate dimension or a high-Y band?~~ → ADR-0010, one shared space dimension
- ~~`ascension-compat-chunky`~~ → built and confirmed live 2026-09-06; pre-generates every planet
  on server start rather than per arrival (see `architecture.md`)
- ~~Does a custom dimension work with Distant Horizons?~~ → yes, with
  `distantGeneratorMode = PRE_EXISTING_ONLY`; see `optimisation-stack.md`
- ~~Where do ancient gateways physically exist?~~ → one portal per planet at a fixed coordinate,
  M3.2 in `plans/m3-vertical-slice.md`
- ~~Moon terrain tuning~~ → crater clipping and mountain pillars fixed and confirmed live;
  [`moon-terrain-tuning.md`](docs/technical/moon-terrain-tuning.md)

## Still open from milestone planning

- **What is the exact order and identity of the seven mandatory off-world steps?**
  (`planets.md`, `tech-tree.md`.) The Moon covers one; Planet 3 (M3.3) will be the second.
- **How are failed landings handled?** Deliberately deferred to v2 at M2.6 — landing damage
  belongs to whichever module ends up owning ships/landing, not `worlds` (see
  `SpaceMechanics.descendTo`'s javadoc).
- **Why are the ancient gateways present?** Where they are is settled (M3.2); the lore is not
  (`docs/lore/world.md`). The portal blocks are still unnamed placeholders until this is answered.

## Outstanding before calling the server multiplayer-ready

- **Two clients has never been tested.** It is in M1's definition of done
  ([`plans/m1-atmosphere.md`](plans/m1-atmosphere.md)) and still unticked. Oxygen is
  per-player state with per-player sync, and every session so far has had exactly one player.
  Two bars that drain independently is not a thing we have observed.
- **`online-mode=false` on the dev server must not survive onto a real one.** Offline mode
  authenticates nobody — anyone can connect as any username, an operator's included. See
  [`serverpack/README.md`](serverpack/README.md).
- **Share-air rescue is designed and not built.** Decided at the M1.1 review, it is the reason
  `accept` exists on `OxygenSource`, and it is the one atmosphere feature that only means
  anything with two players. Small — an `interactLivingEntity` on the tank.

## Design questions, not yet blocking

**Progression:**
- Define mandatory tech families on the critical path versus optional optimisation
- Define late-game planets after the gun unlock step
- Guns currently land at planet 6 of 7 — only one world left to use them in. Move earlier, or
  make planets 7+ substantial?
- Define the repair and gateway lore arc
- Define gun-world identity beyond industrialisation
- Define the exact scope of advanced suit modules

**Combat:**
- Define late-game boss anti-cheese rules in implementable terms
- Which boss pressure types are mandatory across the campaign?
- Should some planet completions use non-boss victory conditions?

**Movement:**
- **Gravity per planet.** Noticed but not designed — excluded from the M2 planet schema on
  purpose (`worlds-api.md`'s "no field lands here until something reads it") and listed as an
  open question in `movement.md`. Natural shape once `gear` exists: the same pattern as
  atmosphere — `worlds` publishes a per-planet multiplier through a `core` contract, `gear`
  reads it and applies the movement effect, and the two never depend on each other directly
  (ADR-0011). Not a blocker for M2 v0.1, which is Earth-gravity Moon and Planet 3 throughout.

**Worldgen:**
- **Curate Earth's modded ore spawns** once Create and whichever other mods actually land add
  their own overworld ores. Mechanism is already proven: `neoforge:remove_features` biome
  modifiers, the mirror image of the `add_features` one that placed Titanium Ore — see
  `docs/technical/datapacks.md`. Deliberately not started now, per Sanchit's call 2026-09-06:
  nothing to remove yet.
- **Real textures.** Every Ascension block and item still reuses a vanilla texture as a
  placeholder (each model file's `_comment` says which). Sanchit is sourcing CC0 packs; swapping
  is one line per model.

**Systems gaps identified 2026-09-05:**
- **Nether removal has an uncosted recipe debt.** Cutting Nether access means re-sourcing
  blaze, nether quartz, netherite and ancient debris wherever our progression needs them. This
  is concrete authoring work, not a policy line.
- **The late-joiner gap.** "Joins at the team's current tier" collides with "unique planetary
  resources gate progression" — they get the dimension unlocked and no gear to survive it.
  Needs a catch-up mechanism, not just a permission flag.
- **Fallback ship experience without Sable** — created by ADR-0006. What do players who do not
  install the optional physics jar actually get?

## Pack-level polish

- **First-launch defaults for players.** The dev build seeds `options.txt` via
  `seedDevGameOptions`, but that only covers our run directories. The shipped pack should
  ship the same defaults so players never see the accessibility onboarding prompt before the
  main menu. Decide whether this is a bundled `options.txt`, a config mod, or our own module.

## Atmosphere follow-ups

Captured during M1.6 testing; none block v0.1.

- **Emitter becomes a three-block Create chain** — generator (rotation or addon electricity)
  feeds a buffer tank, tank feeds a pressuriser that holds the volume. Direct generator-to-
  pressuriser is the cheap early setup. Belongs in `ascension-compat-create` (Tier 2); the
  standalone emitter stays so the module works with no Create installed. Design recorded in
  `docs/technical/atmosphere-api.md` section 7b.
- **Airlocks need solid blocks.** Doors, trapdoors, slabs and panes are not full collision cubes,
  so they leak by the current sealing rule. Either accept it, or add a sealing door block.
- **No general block-change event** means external world edits are not seen until something else
  near the volume changes. Fails safe, but worth revisiting if it bites.

## Missing documentation

- Quest and onboarding design — FTB Quests appears in the mod list with no design behind it. In
  a campaign this authored, the quest book is effectively the game's UI.
- Multiplayer and server operations
- Resource ledger — seven gating materials with no single page tracking what each is and which
  recipes consume it
- Testing and playtest plan
