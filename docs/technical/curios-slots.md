# Curios slots (`ascension-compat-curios`)

Built 2026-10-22, branch `curios-slots`, **compiled and unit-built only: not seen in a client, not checked on a server.**

| Slot | Holds | What it does |
|---|---|---|
| `ascension_tank` | oxygen tank, helium tank (tag `curios:ascension_tank`) | The worn tank is an oxygen source with no valve: wearing it is opening it. One slot, one worn tank. |
| `ascension_relic` | Zonia's Hourglass, Sterak's Harness (tag `curios:ascension_relic`) | Works as the off hand does: the ability key, the passive, the stats. |
| `back` | a backpack | Sophisticated Backpacks assigns it itself; nothing of ours. Gliders brings its own `glider` slot, so the two do not compete. |

## Decisions (they differ from prompt 11, on Sanchit's word 2026-10-22)

- **No valve API.** A worn tank counts as open. Consequence: the 3 s pressurise delay (ADR-0009 section 2) does not apply to a
  worn tank, and a worn tank is not counted in the carry limit of two (`TankRules` counts the inventory only). A player can
  carry two in the inventory and wear a third. Say so in ADR-0009 if this stays.
- **Extension points:** `com.ascension.atmosphere.api.OxygenTanks` (is it a tank, turn it into a source) and
  `com.ascension.combat.api.WornStacks` (the off hand is always worn; a provider adds other places). Zonia's and Sterak's read
  through `WornStacks`; Tier 1 has no Curios in it.
- **Relic stats:** Curios applies attributes through `CurioAttributeModifierEvent`; `RelicStats` copies the off-hand modifiers
  under `ascension_compat_curios:relic/...` ids (named "Relic slot" on the stat sheet). If the same item is also in the off hand
  the curio copy is skipped.
- **Art:** `tools/generate-curios-slot-art.py` (two 16 x 16 icons, seeded, original).

## Client checklist (one batched run)

1. Both slots appear on the Curios screen (G) with their icons; the tank slot refuses a sword, the relic slot refuses a tank.
2. Wear a filled tank in the tank slot on the Moon: the air bar drains from it with no valve opened; unequip it and it stops.
3. A helium tank in the slot lasts twice as long. Two tanks: one worn, one in the inventory both drain (expected).
4. Zonia's in the relic slot: the ability key starts stasis; the tooltip and stat sheet list its stats, labelled "Relic slot".
5. Sterak's in the relic slot: the attack-damage and lifesteal passives work; with Sterak's also in the off hand, stats count once.
6. Die with Soulbound and without; relog; reload the world: both slots keep their contents.
7. Wear a backpack in `back` and a glider in its slot at the same time.
8. Dedicated server: the same jars on the server (Curios is the example in `dev-environment.md` of a mod both sides must carry).
