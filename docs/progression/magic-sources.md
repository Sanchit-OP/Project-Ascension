# Magic sources

Sanchit, 2026-10-22: "we have a big thriving system for Magic Resist and Magic Penetration and everything, we need more sources
of magic damage." Chosen scope: **more items and enchantments, and more magic mobs and bosses.** Branch `magic-sources`.

## Where magic damage came from before

Items: Darken Blade, Ragefang, Heartwood of Pallas, Divine Sunderer (half), Unending Despair, Test Magic Blade (dev).
Runes: Starfall, Arc, Brace. Enchantment: Sweeping Edge (extra targets, added 2026-10-22). Vanilla: blasts, fire, lightning, potions,
Evoker fangs, guardian beams, Wither, dragon breath, sonic boom. Cataclysm: see `damage-kinds.md`.

## Added 2026-10-22 (built, compiled, **not yet proved on a server or seen in a client**)

| What | Does | Numbers (placeholders) |
|---|---|---|
| **Arcane Edge** (enchantment, melee weapons, I-III) | each melee hit adds magic damage | `level x (4 + 12% of Attack Damage)` |
| **Aether Tip** (enchantment, bows and crossbows, I-III) | each arrow that hits adds magic damage (level read from the weapon that fired it) | same |
| **Spellpiercing** (enchantment, weapons, I-III) | your magic damage ignores part of the target's Magic Resist (plain data: Magic Penetration) | +5% a level |
| **Shulker bullets** | now a magic attack (the levitation bolt), so Magic Resist answers it | tag entry |

Arcane Edge and Aether Tip add to the hit that is landing (`MagicDamage.apply`), so invulnerability frames cannot swallow them and the
Aetherwright boosters (Attunement, Clarity, Gathering Charge, Siphon) count them. `damage-checks` has a check for Arcane Edge; Aether
Tip has none yet (an arrow needs a stand-in shooter).

## Still to decide (the bigger half of what Sanchit asked)

- **Items:** which of the 26 give magic damage or Magic Penetration. Today only one item (Wizard's Agony) gives Magic Penetration, and
  only five deal magic. The balance tools (`tools/check-balance.py`, `time-to-kill.py`) hold item numbers, so each change needs a
  deliberate retune. Candidates: Magic Penetration on Darken Blade, Heartwood, Ragefang and Unending Despair; a magic on-hit for one
  more weapon of each kind (a bow, an axe).
- **Mobs and bosses:** which of a boss's attacks are magic. Cataclysm's four are sorted; the Ender Dragon's breath is magic and its
  wings physical. The Moon, the Nether and the End have no custom mobs of ours yet; every magic attacker beyond the lists above needs
  a mob or an attack to exist first.

## Enchantment rules, 2026-10-22 (Sanchit)

- **Exclusive sets:** Keen Edge now goes with Sharpness (it gives Lethality, Sharpness Attack Damage; they complement). Warding is still
  exclusive with Protection. Arcane Edge, Spellpiercing and every other enchantment of ours exclude nothing.
- **Limit:** 6 enchantments on an ordinary item, 7 on a legendary one (`EnchantmentLimits`). The anvil gives no result when a combine
  would add a new enchantment past it; raising a level is always allowed. Books, commands and creative are not limited.
- **Richer table:** after vanilla's roll the table adds extra enchantments the item can take and that fit (75% for a first, 15% less
  for each further), up to the limit. Placeholders. **Proved:** the compatibility and the anvil limit (damage-checks, 296 checks). **Not
  proved:** the table's extra rolls (needs the enchanting menu in a client).
- **Library (not built):** see the question to Sanchit.
