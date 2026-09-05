# Todo

Open questions only. **Settled decisions live in [`docs/decisions/`](docs/decisions/)** — check
there before reopening anything.

Last reviewed: 2026-09-05.

---

## Resolved on 2026-09-05

Moved out of this list — see the register for reasoning:

- ~~Define what requires a custom mod~~ → ADR-0002
- ~~Target Minecraft and loader version~~ → ADR-0001
- ~~Planet framework approach~~ → ADR-0004
- ~~v1 scope~~ → ADR-0005
- ~~Whether oxygen is custom or mod-based~~ → ADR-0003 / M1

## Blocking the next milestones

These need answers during the milestone that touches them, not before.

**During M1.1 (atmosphere API design):**
- Does sprinting, combat, or hazard exposure increase oxygen consumption?
- Do sealed bases create local breathable zones, or only refill points?
- How visible should oxygen information be in the UI?
- Can players create temporary field refills?
- How do multiplayer rescue and recovery work?

**Before M2 (`ascension-worlds`):**
- **Is orbit a separate dimension or a high-Y band of the surface dimension?**
  (Deferred deliberately by ADR-0004 — real memory and chunk cost either way.)
- What is the exact order and identity of the seven mandatory off-world steps?
- Where do ancient gateways physically exist?
- How are failed landings handled?

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

## Missing documentation

- Quest and onboarding design — FTB Quests appears in the mod list with no design behind it. In
  a campaign this authored, the quest book is effectively the game's UI.
- Multiplayer and server operations
- Resource ledger — seven gating materials with no single page tracking what each is and which
  recipes consume it
- Testing and playtest plan
- `docs/technical/performance-log.md` — created in M0.5
- `docs/technical/atmosphere-api.md` — created in M1.1
