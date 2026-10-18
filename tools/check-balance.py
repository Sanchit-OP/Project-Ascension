#!/usr/bin/env python3
"""Checks the legendary power budget (docs/progression/balance.md section 3).

Every combat item's stat lines are scored with one table of weights, and every slot has one budget.
A legendary must land within TOLERANCE of its slot's budget, so no legendary is stronger than another
of its slot; and no Cataclysm item may reach more than CEILING of its slot's budget, so every legendary
stays significantly better than any boss gear. Abilities are not scored: they are the item's flavour,
and the budget is what its stats are worth.

Run:  python tools/check-balance.py            (prints the table)
      python tools/check-balance.py --check    (exits 1 on a violation; the build runs this)
"""

import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
STATS = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'data', 'ascension_combat',
                     'data_maps', 'item', 'item_stats.json')

# Points per unit of each stat. A weapon's own damage counts as damage per second (damage x speed).
WEIGHTS = {
    'health': 1.0, 'armor': 0.5, 'magic_resist': 0.5, 'attack_damage': 4.0, 'attack_speed': 0.8,
    'move_speed': 1.5, 'omnivamp': 6.0, 'lethality': 1.2, 'armor_penetration': 2.0, 'magic_penetration': 2.0,
}
DPS_WEIGHT = 3.0

BUDGETS = {'melee': 120, 'ranged': 120, 'helmet': 55, 'chest': 85, 'legs': 70, 'boots': 45, 'offhand': 55}
TOLERANCE = 3.0   # points, either way
CEILING = 0.65    # a Cataclysm item's share of its slot's budget, at most

SLOTS = {
    'melee': ['veigos_blade', 'darius_axe', 'spear_of_shojin', 'divine_sunderer', 'darken_blade', 'ragefang',
              'endless_hunger', 'wraithblade'],
    'ranged': ['mortal_reminder', 'heartwood_of_pallas', 'galeforce'],
    'helmet': ['protean_mask', 'edge_of_night'],
    'chest': ['gargoyle_stoneplate', 'guardian_wing'],
    'legs': ['rocketbelt', 'collectors_pants', 'unending_despair'],
    'boots': ['ninja_tabi', 'mercury_treads', 'swifty', 'berserkers_greaves', 'wizards_agony', 'tasted_blood'],
    'offhand': ['zonias_hourglass', 'steraks_harness'],
}

# Cataclysm's own numbers, measured 2026-10-16 (docs/technical/item-stats.md section 8): the slot, then
# Attack Damage, Armor, Magic Resist, Health. A weapon's speed is not measured; 1.6 is assumed.
CATACLYSM_SPEED = 1.6
CATACLYSM = {
    'soul_render': ('melee', 14, 0, 0, 0), 'ceraunus': ('melee', 15, 0, 0, 0), 'infernal_forge': ('melee', 13, 0, 0, 0),
    'void_forge': ('melee', 13, 0, 0, 0), 'the_incinerator': ('melee', 13, 0, 0, 0), 'brontes': ('melee', 13, 0, 0, 0),
    'astrape': ('melee', 9.5, 0, 0, 0), 'ancient_spear': ('melee', 8.5, 0, 0, 0),
    'gauntlet_of_guard': ('melee', 10, 10, 7, 7),
    'ignitium_helmet': ('helmet', 0, 21, 15, 10), 'ignitium_chestplate': ('chest', 0, 38, 27, 10),
    'ignitium_leggings': ('legs', 0, 31, 22, 10), 'ignitium_boots': ('boots', 0, 21, 15, 10),
    'cursium_helmet': ('helmet', 0, 17, 12, 10), 'cursium_chestplate': ('chest', 0, 35, 25, 10),
    'cursium_leggings': ('legs', 0, 28, 20, 10), 'cursium_boots': ('boots', 0, 17, 12, 10),
    'bone_reptile_chestplate': ('chest', 0, 38, 27, 6), 'monstrous_helm': ('helmet', 0, 10, 7, 7),
}
VANILLA = {'netherite_chestplate': ('chest', 0, 28, 20, 10), 'diamond_chestplate': ('chest', 0, 28, 20, 5),
           'netherite_sword': ('melee', 8, 0, 0, 0)}


def score_entry(entry):
    total = 0.0
    weapon = entry.get('weapon')
    if weapon:
        total += weapon['damage'] * weapon['speed'] * DPS_WEIGHT
    for stat, value in entry.get('stats', {}).items():
        total += WEIGHTS[stat] * value
    return total


def score_foreign(slot, ad, armor, mr, hp):
    base = armor * WEIGHTS['armor'] + mr * WEIGHTS['magic_resist'] + hp * WEIGHTS['health']
    if slot == 'melee':
        base += ad * CATACLYSM_SPEED * DPS_WEIGHT
    return base


def main():
    check = '--check' in sys.argv
    with open(STATS, encoding='utf-8') as f:
        values = json.load(f)['values']
    problems = []

    print('Legendaries (budget, points):')
    for slot, names in SLOTS.items():
        budget = BUDGETS[slot]
        for name in names:
            entry = values.get('ascension_combat:' + name)
            if entry is None:
                problems.append('%s has no entry in item_stats.json' % name)
                continue
            s = score_entry(entry)
            flag = '' if abs(s - budget) <= TOLERANCE else '   <-- off by %.1f' % (s - budget)
            print('  %-8s %-22s %6.1f / %d%s' % (slot, name, s, budget, flag))
            if flag:
                problems.append('%s scores %.1f, its %s budget is %d (+/-%.0f)' % (name, s, slot, budget, TOLERANCE))

    print('Other gear (share of its slot budget; ceiling %d%%):' % (CEILING * 100))
    for table in (CATACLYSM, VANILLA):
        for name, (slot, ad, armor, mr, hp) in table.items():
            s = score_foreign(slot, ad, armor, mr, hp)
            share = s / BUDGETS[slot]
            flag = '   <-- over the ceiling' if share > CEILING and table is CATACLYSM else ''
            print('  %-8s %-26s %6.1f  %3.0f%%%s' % (slot, name, s, share * 100, flag))
            if flag:
                problems.append('%s is %.0f%% of its slot budget, over %.0f%%' % (name, share * 100, CEILING * 100))

    for p in problems:
        print('balance: ' + p)
    print('%d problems' % len(problems))
    if check and problems:
        sys.exit(1)


if __name__ == '__main__':
    main()
