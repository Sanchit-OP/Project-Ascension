# The combat items' build paths

> **Built 2026-10-18, checked by the build only.** Not yet seen in a client or on a dedicated server
> (ADR-0008). Sanchit approved the 2026-10-12 draft as written. Every number is a placeholder to tune
> by playing. Everything below is written by `tools/generate-build-paths.py` from one table; edit the
> table, not the JSON.

**The rules.** Every recipe is shapeless at a crafting table, so JEI shows each step. Other modules'
materials are asked for by tag (`#c:ingots/titanium`, `#c:raw_materials/helium`), never by id.
Components have no stats of their own. No world is required (ADR-0018); the paths are only costly.

## Tiers

| Tier | Items | Made from |
|---|---|---|
| Basic (6) | Long Sword, Dagger, Cloth Armor, Null-Magic Mantle, Ruby Crystal, Boots | Earth: titanium, leather, amethyst, an echo shard, diamond, redstone |
| Epic, Moon (8) | Pickaxe, Caulfield's Warhammer, Giant's Belt, Recurve Bow, Chain Vest, Seeker's Armguard, Last Whisper, Noonquiver | basics plus raw helium |
| Epic, Nether (9) | B. F. Sword (netherite), Phage (blaze powder), Kindlegem (magma cream), Vampiric Scepter (ghast tears), Serrated Dirk (quartz), Negatron Cloak (glowstone), Spectre's Cowl (nether wart), Hearthbound Axe (blaze rods), Executioner's Calling (wither skeleton skull) | basics plus Nether materials |
| Legendary (20) | the non-boot combat items | two to four epics and basics, so every one touches Earth, the Moon and the Nether; six also take a heart |
| Boots (6) | Ninja Tabi, Mercury Treads, Swifty, Berserker's Greaves (Earth only); Wizard's Agony (helium); Tasted Blood (ghast tears) | Boots plus one basic or one material. Never a heart |

Darius's Axe is Phage plus Caulfield's Warhammer: 9 titanium, a diamond, 4 redstone, 4 blaze powder
and 6 helium. **Endless Hunger is the hardest weapon:** three epics and the Warden's heart.

## Hearts

One per boss, dropped only when a player kills it. Each finishes one item.

| Boss | Heart | Item | Drop lives in |
|---|---|---|---|
| The Warden | Heart of the Warden | Endless Hunger | `ascension-combat` |
| The Wither | Heart of the Wither | Unending Despair | `ascension-combat` |
| The Ender Guardian | Heart of the Ender Guardian | Guardian's Wing | `ascension-compat-cataclysm` |
| The Ender Golem | Heart of the Ender Golem | Sterak's Harness | `ascension-compat-cataclysm` |
| The Netherite Monstrosity | Heart of the Netherite Monstrosity | Zonia's Hourglass | `ascension-compat-cataclysm` |
| Ignis | Heart of Ignis | Divine Sunderer | `ascension-compat-cataclysm` |

The Cataclysm modifiers carry a `neoforge:conditions` `mod_loaded` check for `ascension_combat`.
Just Enough Resources will not show drops added by loot modifiers; `ascension-compat-jei`'s
"Where it's found" pages are the planned answer.

## In JEI

Every recipe shows in JEI as a crafting recipe. `ascension-compat-jei` adds a **Build path** page per combat
item: everything its whole tree costs, flattened, with counts. Hover a material to see which world (or boss)
it comes from. Built, compiled, not yet seen in a client.

## Still open

- **Seen in a client and on a dedicated server:** recipes in JEI, each heart dropping from its boss,
  and the conditional loot modifiers loading with and without Cataclysm (confirm that
  `neoforge:conditions` is honoured on a loot modifier file).
- **Art is a first pass:** the hearts share one silhouette in six palettes, and the Moon and Nether
  marks are three loose pixels in a corner. A design session should redraw them.
- **Balance:** costs against the stat numbers in `item-stats.md`, and how enchantments interact.
- Titanium and helium now have jobs; the helium lamp is still to do.
