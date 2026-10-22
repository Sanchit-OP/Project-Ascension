#!/usr/bin/env python3
"""Planet loot (docs/progression/loot.md): the bonus every chest on another planet carries.

One table of numbers in this file, written out as plain data:

  * for each planet, a loot table per module (`ascension_worlds:planet/<planet>` holds the materials and
    vanilla treasure, `ascension_combat:planet/<planet>` the components and enchantment books), and
  * a `neoforge:add_table` loot modifier per table, carrying `ascension_core:chest_table` (any chest, from
    any mod, Lootr's included) and a dimension check, so no chest table is ever named, and
  * one `ascension_core:strip` modifier that takes the junk tag out of chests on those planets, and a milder one
    (seeds, rotten flesh, poisonous potatoes) for Earth's chests, which get no bonus.

Edit the numbers here, run the script, commit the JSON. `--check` fails if the files on disk differ.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

PLANETS = {
    'moon': 'ascension_worlds:moon',
    'nether': 'minecraft:the_nether',
    'end': 'minecraft:the_end',
}


def count(low, high=None):
    if high is None or high == low:
        return {'type': 'minecraft:constant', 'value': low}
    return {'type': 'minecraft:uniform', 'min': low, 'max': high}


def item(name, weight, low=None, high=None, enchant=None):
    entry = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    functions = []
    if low is not None:
        functions.append({'function': 'minecraft:set_count', 'count': count(low, high)})
    if enchant is not None:
        levels = {'type': 'minecraft:uniform', 'min': enchant[0], 'max': enchant[1]}
        functions.append({'function': 'minecraft:enchant_with_levels', 'levels': levels, 'options': '#minecraft:on_random_loot'})
    if functions:
        entry['functions'] = functions
    return entry


def own_book(weight, options):
    return {'type': 'minecraft:item', 'name': 'minecraft:book', 'weight': weight,
            'functions': [{'function': 'minecraft:enchant_randomly', 'options': options}]}


def pool(entries, rolls, chance=None):
    p = {'rolls': count(*rolls) if isinstance(rolls, tuple) else rolls, 'entries': entries}
    if chance is not None:
        p['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
    return p


def table(pools):
    return {'type': 'minecraft:chest', 'pools': pools}


def gear(names, weight, levels):
    return [item('minecraft:' + n, weight, enchant=levels) for n in names]


DIAMOND_GEAR = ['diamond_sword', 'diamond_pickaxe', 'diamond_helmet', 'diamond_chestplate', 'diamond_leggings', 'diamond_boots']
NETHERITE_GEAR = ['netherite_sword', 'netherite_pickaxe', 'netherite_helmet', 'netherite_chestplate', 'netherite_leggings',
                  'netherite_boots']

# ---------------------------------------------------------------------------------------------------------------
# ascension-worlds: materials and vanilla treasure. Every chest gets one or two staples, and three in ten a find.
# ---------------------------------------------------------------------------------------------------------------
WORLDS = {
    'moon': table([
        pool([
            item('ascension_worlds:raw_titanium', 10, 2, 6),
            item('ascension_worlds:raw_helium', 10, 2, 5),
            item('ascension_worlds:inscribed_gatestone', 4, 2, 5),
            item('minecraft:diamond', 7, 1, 3),
            item('minecraft:emerald', 5, 2, 6),
            item('minecraft:gold_ingot', 6, 3, 8),
            item('minecraft:iron_ingot', 6, 4, 10),
            item('minecraft:redstone', 5, 6, 14),
            item('minecraft:leather', 4, 3, 8),
            item('minecraft:amethyst_shard', 4, 3, 8),
            item('minecraft:experience_bottle', 8, 3, 8),
            item('minecraft:golden_apple', 4, 1),
            item('minecraft:ender_pearl', 3, 1, 3),
            item('minecraft:echo_shard', 3, 1, 2),
        ], (1, 2)),
        pool(gear(DIAMOND_GEAR, 2, (20, 30)) + [
            item('minecraft:book', 6, enchant=(22, 30)),
            item('minecraft:enchanted_golden_apple', 2, 1),
            item('minecraft:totem_of_undying', 1, 1),
        ], 1, 0.30),
    ]),
    'nether': table([
        pool([
            item('minecraft:netherite_scrap', 6, 1, 2),
            item('minecraft:ancient_debris', 4, 1, 2),
            item('minecraft:gold_ingot', 8, 4, 10),
            item('minecraft:diamond', 7, 1, 3),
            item('minecraft:blaze_rod', 8, 2, 5),
            item('minecraft:ghast_tear', 6, 1, 3),
            item('minecraft:magma_cream', 6, 2, 5),
            item('minecraft:nether_wart', 6, 3, 8),
            item('minecraft:glowstone_dust', 5, 4, 10),
            item('minecraft:quartz', 5, 4, 10),
            item('minecraft:crying_obsidian', 3, 2, 5),
            item('minecraft:experience_bottle', 8, 4, 10),
            item('minecraft:golden_apple', 4, 1, 2),
            item('ascension_worlds:raw_titanium', 4, 2, 5),
            item('ascension_worlds:raw_helium', 4, 2, 5),
        ], (1, 2)),
        pool(gear(DIAMOND_GEAR, 2, (24, 32)) + [
            item('minecraft:netherite_ingot', 2, 1),
            item('minecraft:wither_skeleton_skull', 2, 1),
            item('minecraft:book', 5, enchant=(24, 32)),
            item('minecraft:enchanted_golden_apple', 2, 1),
            item('minecraft:totem_of_undying', 1, 1),
        ], 1, 0.30),
    ]),
    'end': table([
        pool([
            item('minecraft:diamond', 8, 2, 4),
            item('minecraft:emerald', 5, 3, 8),
            item('minecraft:gold_ingot', 5, 4, 10),
            item('minecraft:ender_pearl', 8, 2, 5),
            item('minecraft:shulker_shell', 4, 1, 2),
            item('minecraft:netherite_scrap', 5, 1, 3),
            item('minecraft:dragon_breath', 2, 1, 3),
            item('minecraft:experience_bottle', 8, 6, 14),
            item('minecraft:golden_apple', 4, 1, 2),
            item('ascension_worlds:raw_titanium', 5, 3, 6),
            item('ascension_worlds:raw_helium', 5, 3, 6),
        ], (1, 2)),
        pool(gear(NETHERITE_GEAR, 1, (28, 38)) + gear(DIAMOND_GEAR, 1, (26, 34)) + [
            item('minecraft:netherite_ingot', 3, 1),
            item('minecraft:book', 5, enchant=(30, 39)),
            item('minecraft:enchanted_golden_apple', 3, 1),
            item('minecraft:totem_of_undying', 2, 1),
        ], 1, 0.35),
    ]),
}

# ---------------------------------------------------------------------------------------------------------------
# ascension-combat: the build paths' components (the epics are the rare finds) and our own enchantment books.
# ---------------------------------------------------------------------------------------------------------------
BASICS = [('long_sword', 10), ('dagger', 10), ('cloth_armor', 10), ('ruby_crystal', 8), ('null_magic_mantle', 6), ('basic_boots', 6)]
MOON_EPICS = ['pickaxe', 'caulfields_warhammer', 'giants_belt', 'recurve_bow', 'chain_vest', 'seekers_armguard', 'last_whisper',
              'noonquiver']
NETHER_EPICS = ['bf_sword', 'phage', 'kindlegem', 'vampiric_scepter', 'serrated_dirk', 'negatron_cloak', 'spectres_cowl',
                'hearthbound_axe', 'executioners_calling']


def comp(name, weight):
    return item('ascension_combat:' + name, weight)


def books(chance, soulbound_chance):
    return [
        pool([own_book(1, '#ascension_combat:own')], 1, chance),
        pool([own_book(1, 'ascension_combat:soulbound')], 1, soulbound_chance),
    ]


COMBAT = {
    'moon': table([
        pool([comp(n, w) for n, w in BASICS] + [comp(n, 1) for n in MOON_EPICS], 1, 0.45),
    ] + books(0.35, 0.05)),
    'nether': table([
        pool([comp(n, max(2, w // 2)) for n, w in BASICS] + [comp(n, 1) for n in MOON_EPICS]
             + [comp(n, 1 if n in ('bf_sword', 'executioners_calling') else 2) for n in NETHER_EPICS], 1, 0.45),
    ] + books(0.40, 0.08)),
    'end': table([
        pool([comp(n, 2) for n in MOON_EPICS + NETHER_EPICS], 1, 0.50),
    ] + books(0.50, 0.12)),
}


def location(dimension):
    return {'condition': 'minecraft:location_check', 'predicate': {'dimension': dimension}}


def modifier(namespace, planet):
    return {
        'type': 'neoforge:add_table',
        'conditions': [{'condition': 'ascension_core:chest_table'}, location(PLANETS[planet])],
        'table': '%s:planet/%s' % (namespace, planet),
    }


STRIP = {
    'type': 'ascension_core:strip',
    'conditions': [
        {'condition': 'ascension_core:chest_table'},
        {'condition': 'minecraft:any_of', 'terms': [location(d) for d in PLANETS.values()]},
    ],
    'tag': 'ascension_core:chest_junk',
}


STRIP_EARTH = {
    'type': 'ascension_core:strip',
    'conditions': [{'condition': 'ascension_core:chest_table'}, location('minecraft:overworld')],
    'tag': 'ascension_core:chest_junk_mild',
}


def outputs():
    """Every file this script owns: path (from the repo root) -> JSON value."""
    files = {}
    for namespace, module, tables in (('ascension_worlds', 'worlds', WORLDS), ('ascension_combat', 'combat', COMBAT)):
        base = 'modules/%s/src/main/resources/data/%s/' % (module, namespace)
        for planet, body in tables.items():
            files[base + 'loot_table/planet/%s.json' % planet] = body
            files[base + 'loot_modifiers/planet_%s.json' % planet] = modifier(namespace, planet)
    files['modules/worlds/src/main/resources/data/ascension_worlds/loot_modifiers/planet_chest_junk.json'] = STRIP
    files['modules/worlds/src/main/resources/data/ascension_worlds/loot_modifiers/earth_chest_junk.json'] = STRIP_EARTH
    return files


def registered():
    """The `entries` each module's global_loot_modifiers.json must carry for these files."""
    return {
        'modules/worlds/src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json':
            ['ascension_worlds:planet_%s' % p for p in PLANETS] + ['ascension_worlds:planet_chest_junk', 'ascension_worlds:earth_chest_junk'],
        'modules/combat/src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json':
            ['ascension_combat:planet_%s' % p for p in PLANETS],
    }


def render(value):
    return json.dumps(value, indent=2) + '\n'


def main():
    check = '--check' in sys.argv
    problems = []

    # Every combat item named here must be the result of a build-path recipe, so a rename cannot leave a dead entry.
    results = set()
    for recipe in (ROOT / 'modules/combat/src/main/resources/data/ascension_combat/recipe/build_path').glob('*.json'):
        results.add(json.loads(recipe.read_text(encoding='utf-8'))['result']['id'])
    for body in COMBAT.values():
        for p in body['pools']:
            for entry in p['entries']:
                name = entry['name']
                if name.startswith('ascension_combat:') and name not in results:
                    problems.append('%s is not the result of any build-path recipe' % name)

    for path, value in outputs().items():
        target = ROOT / path
        text = render(value)
        if check:
            if not target.exists() or target.read_text(encoding='utf-8').replace('\r\n', '\n') != text:
                problems.append('%s is out of date (run tools/generate-planet-loot.py)' % path)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8', newline='\n')

    for path, wanted in registered().items():
        target = ROOT / path
        listed = json.loads(target.read_text(encoding='utf-8')).get('entries', [])
        for entry in wanted:
            if entry not in listed:
                problems.append('%s does not list %s' % (path, entry))

    if problems:
        for p in problems:
            print('planet loot: ' + p)
        sys.exit(1)
    print('planet loot: %d files %s' % (len(outputs()), 'match' if check else 'written'))


if __name__ == '__main__':
    main()
