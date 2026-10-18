# Todo

Open questions only. **Settled decisions live in [`docs/decisions/`](docs/decisions/)** — check
there before reopening anything. Milestone work in flight lives in [`plans/`](plans/) — currently
[`plans/m3-vertical-slice.md`](plans/m3-vertical-slice.md).

Last reviewed: 2026-10-06.

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
- **What ended the gateway civilisation?** The premise is settled (an interplanetary civilisation,
  something happened, the Moon is the lore anchor; `docs/lore/world.md`). The answer itself, and
  how lore reaches the player, are not.

## Outstanding before calling the server multiplayer-ready

- **Two clients has never been tested.** Deferred by Sanchit (2026-10-06) to the alpha release. It is in M1's definition of done
  ([`plans/m1-atmosphere.md`](plans/m1-atmosphere.md)) and still unticked. Oxygen is
  per-player state with per-player sync, and every session so far has had exactly one player.
  Two bars that drain independently is not a thing we have observed.
- **`online-mode=false` on the dev server must not survive onto a real one.** Offline mode
  authenticates nobody — anyone can connect as any username, an operator's included. See
  [`serverpack/README.md`](serverpack/README.md).
- **Share-air rescue is built (2026-10-05) and unverified.** Right-click a player with an oxygen
  tank to fill their lungs from it (`AirSharing`). It only means anything with two players, so it
  is verified by the same two-client session as the item above, not before.

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
- **Real textures.** `ascension-worlds` is done: original art, generated in-house by
  `tools/generate-gateway-art.py` and `tools/generate-moon-materials-art.py`, never copied from
  another mod or pack (Sanchit, 2026-10-06). Combat's items got theirs on 2026-10-07, in League
  likeness, worn armour included (`combat-art.md`), and a held 3D model for Veigo's Blade
  (redone, drawn by combat's own weapon renderer), not yet seen in a client; the Darkin Blade,
  the Black Cleaver and the other swords are next (`plans/combat-art-3d.md`). Still on vanilla
  placeholders:
  atmosphere's oxygen tank, emitter and refill station.

**Systems gaps identified 2026-09-05:**
- **Portal removal had a recipe debt (ADR-0014, 2026-10-06). The Nether half is paid
  (2026-10-11):** Planet 3 is the real Nether, reached by airship, and a fresh world's roofless
  Nether has blaze spawners in its fortresses, nether wart, glowstone (under overhangs and in
  caves, y 32 to 100+), ancient debris, quartz, soul sand and magma, checked headless. **The End
  half is still open:** End-only items stay unobtainable until the End is a planet. Our own recipes
  were never in debt: the gateway keystone uses Deep Dark materials (`gateway-portal-design.md`).
  Still to watch: any later recipe that reaches for End items, or puts a Nether item on the road
  before Planet 3 (eyes of ender need blaze powder).
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

## Oxygen v2 (Sanchit, 2026-10-18: starting soon)

First item, decided: **a ship is a closed space, so the Oxygen Emitter aboard a ship pressurises it.** Today that is unbuilt and
unseen (journey.md, "Decided by Sanchit"). Needs: the Sable side of `AtmosphereAnchor.Region` / `AnchorSpace` (the seam exists in
the API, nothing registers it); an emitter and a refill station that work aboard; a cabin that stays valid as the ship's blocks
change; doors and hatches that seal (see "Airlocks need solid blocks" above). With it, tanks are only for walking outside and the
ship-speed limit on reaching the Nether goes away. **The quest book's "A long flight" and "Share your air" lines change when it
lands.** Also queued by Sanchit: helium as a better tank or as a fuel (undecided; build paths also use helium).

**Sanchit's Oxygen v2 list (2026-10-19), written up as `plans/session-prompts/8-oxygen-v2.md`:** (1) redraw the tank, the refill
station and the room block (they use vanilla textures today); (2) helium as the upgrade material for a level-2 tank that holds more
(the fuel half is done, ADR-0025); (3) the oxygen station becomes a Create-powered machine, more rotation giving more air and more
area; (4) air becomes a fluid Create's tanks and pipes can carry, with a new Air Dispenser (fed by a mechanical pump) that
pressurises the room instead of the station; (5) the same working aboard a ship. His session starts from that prompt.

## Missing documentation

- Quest and onboarding design — FTB Quests appears in the mod list with no design behind it. In
  a campaign this authored, the quest book is effectively the game's UI.
- Multiplayer and server operations
- ~~Resource ledger~~ → **built 2026-10-12:**
  [`docs/progression/resource-ledger.md`](docs/progression/resource-ledger.md), generated by
  `tools/resource-ledger.py` and checked by every build. Why it exists:
  [`resource-economy.md`](docs/progression/resource-economy.md).
- Testing and playtest plan
