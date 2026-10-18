#!/usr/bin/env python3
"""Checks the legendary balance (docs/progression/balance.md section 3).

Weapons are balanced by time to kill, not points: a weapon's worth is mostly its ability, which no stat
sum can say. Each legendary weapon must kill the standard boss (1000 HP, 150 Armor, 90 Magic Resist) in
its written number of seconds, give or take 6%, and the fastest and slowest may not be further apart than
2.2 times, so nobody runs one weapon. A weapon that carries utility (Darius's shred, Endless Hunger's
sustain, Wraithblade's blink) is slower on purpose.

Armour, boots and the off-hand are balanced by points: every stat is worth some points and every slot has
one budget; a piece must land within 3 points of its slot's (or its own written target, for the few that
are set by hand). No Cataclysm armour may pass 70% of its slot's budget, so a legendary stays well clear.

Also rules a number cannot say: melee damage is one figure in `weapon`, bows carry damage only, Penetration
only on Wizard's Agony, Omnivamp on five items.

Run:  python tools/check-balance.py            (prints the tables)
      python tools/check-balance.py --check    (exits 1 on a violation; the build runs this)
"""

import importlib.util
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
BASE = os.path.join(ROOT, 'modules')
STATS = os.path.join(BASE, 'combat', 'src', 'main', 'resources', 'data', 'ascension_combat', 'data_maps', 'item', 'item_stats.json')
CATACLYSM_STATS = os.path.join(BASE, 'compat', 'cataclysm', 'src', 'main', 'resources', 'data', 'ascension_combat',
                               'data_maps', 'item', 'item_stats.json')

spec = importlib.util.spec_from_file_location('ttk', os.path.join(ROOT, 'tools', 'time-to-kill.py'))
ttk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ttk)

# Seconds to kill the standard boss alone, written down: the design, not an output.
# Wide on purpose (Sanchit, 2026-10-19): some weapons are strong, some are middling, the ones that carry
# utility are slower, and the two dual-wield weapons are slow alone and strong as a pair.
TTK_TARGET = {
    'spear_of_shojin': 65, 'veigos_blade': 70, 'mortal_reminder': 75,
    'divine_sunderer': 90, 'darken_blade': 95, 'galeforce': 95, 'heartwood_of_pallas': 100,
    'ragefang': 97, 'endless_hunger': 120,
    'darius_axe': 120, 'wraithblade': 135,
}
PAIR_TARGET = {'ragefang': 71, 'endless_hunger': 65}   # worn one in each hand
TTK_TOLERANCE = 0.06
TTK_SPREAD = 2.2
BOSS = (1000.0, 150.0, 90.0)

# Points per unit of a defensive piece's stats. Omnivamp and Penetration are set by hand, not scored.
WEIGHTS = {
    'health': 1.0, 'armor': 0.5, 'magic_resist': 0.5, 'attack_damage': 4.0, 'attack_speed': 0.8,
    'move_speed': 1.5, 'lethality': 1.2, 'omnivamp': 0.0, 'armor_penetration': 0.0, 'magic_penetration': 0.0,
}
BUDGETS = {'helmet': 37, 'chest': 57, 'legs': 47, 'boots': 30, 'offhand': 37}
TOLERANCE = 3.0
CEILING = 0.70

SLOTS = {
    'helmet': ['protean_mask', 'edge_of_night'],
    'chest': ['gargoyle_stoneplate', 'guardian_wing'],
    'legs': ['rocketbelt', 'collectors_pants', 'unending_despair'],
    'boots': ['ninja_tabi', 'mercury_treads', 'swifty', 'berserkers_greaves', 'wizards_agony', 'tasted_blood'],
    'offhand': ['zonias_hourglass', 'steraks_harness'],
}
# Written by hand: the two defensive boots sit below their slot (they felt strong), and the two
# identity boots have no scored stats at all (Wizard's Agony: Penetration; Tasted Blood: Omnivamp).
TARGET = {'ninja_tabi': 24, 'mercury_treads': 24}
HAND_SET = {'wizards_agony', 'tasted_blood'}

MELEE = ttk.MELEE
BOWS = ttk.BOWS
OMNIVAMP = {'veigos_blade': 4, 'endless_hunger': 7, 'spear_of_shojin': 3, 'divine_sunderer': 3, 'tasted_blood': 8}
PENETRATION = {'wizards_agony': 12}
NO_PENETRATION = ('armor_penetration', 'magic_penetration')
BOW_STATS = {'attack_damage', 'move_speed'}
MELEE_NOT_IN_STATS = {'attack_damage', 'attack_speed'}

# Cataclysm armour as the mod made it, measured 2026-10-16 (item-stats.md section 8): slot, Armor, Magic Resist, Health.
CATACLYSM = {
    'ignitium_helmet': ('helmet', 21, 15, 10), 'ignitium_chestplate': ('chest', 38, 27, 10),
    'ignitium_leggings': ('legs', 31, 22, 10), 'ignitium_boots': ('boots', 21, 15, 10),
    'cursium_helmet': ('helmet', 17, 12, 10), 'cursium_chestplate': ('chest', 35, 25, 10),
    'cursium_leggings': ('legs', 28, 20, 10), 'cursium_boots': ('boots', 17, 12, 10),
    'bone_reptile_chestplate': ('chest', 38, 27, 6), 'monstrous_helm': ('helmet', 10, 7, 7),
}


def score(entry):
    return sum(WEIGHTS[s] * x for s, x in entry.get('stats', {}).items())


def main():
    check = '--check' in sys.argv
    with open(STATS, encoding='utf-8') as f:
        values = json.load(f)['values']
    with open(CATACLYSM_STATS, encoding='utf-8') as f:
        extras = json.load(f)['values']
    problems = []

    print('Weapons: seconds to kill the standard boss (%d HP, %d Armor, %d Magic Resist)' % BOSS)
    print('  %-22s %8s %8s %8s' % ('weapon', 'alone', 'target', 'kit'))
    alone = {}
    for name in TTK_TARGET:
        a = ttk.fight(name, ttk.Boss(*BOSS), False)
        k = ttk.fight(name, ttk.Boss(*BOSS), True)
        alone[name] = a
        flag = '' if abs(a - TTK_TARGET[name]) <= TTK_TARGET[name] * TTK_TOLERANCE else '   <-- off'
        print('  %-22s %8.0f %8d %8.0f%s' % (name, a, TTK_TARGET[name], k, flag))
        if flag:
            problems.append('%s kills in %.0f s, written %d s' % (name, a, TTK_TARGET[name]))
    for name, target in PAIR_TARGET.items():
        a = ttk.fight(name, ttk.Boss(*BOSS), False, None, True)
        k = ttk.fight(name, ttk.Boss(*BOSS), True, None, True)
        flag = '' if abs(a - target) <= target * 0.08 else '   <-- off'
        print('  %-22s %8.0f %8d %8.0f%s' % (name + ' (a pair)', a, target, k, flag))
        if flag:
            problems.append('%s as a pair kills in %.0f s, written %d s' % (name, a, target))
    spread = max(alone.values()) / min(alone.values())
    print('  spread slowest / fastest: %.2f (limit %.1f)' % (spread, TTK_SPREAD))
    if spread > TTK_SPREAD:
        problems.append('the weapons are %.2f times apart in time to kill, limit %.1f' % (spread, TTK_SPREAD))

    print('Armour, boots and off-hand (points, target):')
    for slot, names in SLOTS.items():
        for name in names:
            entry = values.get('ascension_combat:' + name)
            if entry is None:
                problems.append('%s has no entry in item_stats.json' % name)
                continue
            if name in HAND_SET:
                print('  %-8s %-22s set by hand' % (slot, name))
                continue
            target = TARGET.get(name, BUDGETS[slot])
            s = score(entry)
            flag = '' if abs(s - target) <= TOLERANCE else '   <-- off by %.1f' % (s - target)
            print('  %-8s %-22s %6.1f / %d%s' % (slot, name, s, target, flag))
            if flag:
                problems.append('%s scores %.1f, its target is %d (+/-%.0f)' % (name, s, target, TOLERANCE))

    for name in list(TTK_TARGET) + [n for ns in SLOTS.values() for n in ns]:
        entry = values.get('ascension_combat:' + name, {})
        stats = entry.get('stats', {})
        for stat in NO_PENETRATION:
            if stat in stats and PENETRATION.get(name) != stats[stat]:
                problems.append('%s has %s: Penetration is only on %s' % (name, stat, sorted(PENETRATION)))
        if 'omnivamp' in stats and OMNIVAMP.get(name) != stats['omnivamp']:
            problems.append('%s has omnivamp %s; only %s may' % (name, stats['omnivamp'], OMNIVAMP))
        if name in OMNIVAMP and stats.get('omnivamp') != OMNIVAMP[name]:
            problems.append('%s should have omnivamp %s' % (name, OMNIVAMP[name]))
        if name in BOWS and not set(stats) <= BOW_STATS:
            problems.append('%s (a bow) has %s; bows carry %s only' % (name, sorted(set(stats) - BOW_STATS), sorted(BOW_STATS)))
        if name in MELEE and (set(stats) & MELEE_NOT_IN_STATS or 'weapon' not in entry):
            problems.append('%s: damage and speed belong in `weapon` as one figure' % name)

    print('Cataclysm armour (share of its slot budget; ceiling %d%%):' % (CEILING * 100))
    for name, (slot, armor, mr, hp) in CATACLYSM.items():
        named = extras.get('cataclysm:' + name, {}).get('stats', {})
        a, m, h = named.get('armor', armor), named.get('magic_resist', mr), named.get('health', hp)
        s = a * WEIGHTS['armor'] + m * WEIGHTS['magic_resist'] + h * WEIGHTS['health']
        share = s / BUDGETS[slot]
        flag = '   <-- over the ceiling' if share > CEILING else ''
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
