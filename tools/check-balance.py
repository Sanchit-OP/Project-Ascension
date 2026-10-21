#!/usr/bin/env python3
"""Checks the legendary balance (docs/progression/balance.md section 3).

Weapons are balanced by time to kill, not points: a weapon's worth is mostly its ability, which no stat
sum can say. Each legendary weapon must kill the standard boss (1000 HP, 150 Armor, 90 Magic Resist) in
its written number of seconds, give or take 6%, and the fastest and slowest may not be further apart than
1.7 times (it was 2.2 before the 2026-10-22 buff), so nobody runs one weapon. A weapon that carries utility
(Darius's shred, Wraithblade's blink) is a little slower on purpose.

The same is held at the Wither (4225 HP, 180 Armor, 110 Magic Resist), the last boss of the ladder, where
the weapons that deal flat damage fall behind the ones that deal a share of the boss's health: each weapon's
written time there, and a wider spread limit that says how far behind a flat weapon may fall.

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
# Narrower than the first spread (Sanchit, 2026-10-19: lessen the split): strong weapons are a little
# faster, the ones that carry utility for a team (Darius's shred, Mortal Reminder's anti-heal) a little
# slower, and the two dual-wield weapons are slow alone and strong as a pair.
# 2026-10-19 (Sanchit: "dont want to keep the kill time intact"): Ragefang's attack-speed limit went to 7.5 with its
# damage left as it was, so it is the fastest weapon, and Endless Hunger swings half as fast and hits harder.
# 2026-10-22 (Sanchit: "currently the player is weak against the fuking bosses, we need them all some buffs"): every
# weapon's base damage and scaling went up, so each kills the standard boss in about two thirds of the time it did
# (the old figures were 52 to 110 s) and the Wither in about half. The flat-damage weapons (Darius's Axe, Wraithblade,
# Mortal Reminder, Endless Hunger) were raised furthest, since a share-of-health weapon grows with the boss and they do not.
TTK_TARGET = {
    'spear_of_shojin': 46, 'veigos_blade': 48, 'divine_sunderer': 54, 'darken_blade': 55, 'galeforce': 55,
    'heartwood_of_pallas': 53, 'ragefang': 36, 'mortal_reminder': 44, 'darius_axe': 47, 'wraithblade': 42,
    'endless_hunger': 51,
}
PAIR_TARGET = {'ragefang': 28, 'endless_hunger': 24}   # worn one in each hand
TTK_TOLERANCE = 0.06
TTK_SPREAD = 1.7
BOSS = (1000.0, 150.0, 90.0)

# The same weapons against the Wither (the 2026-10-19 numbers, balance.md section 5), alone. A flat weapon falls behind
# here (Endless Hunger most: its damage is flat and its sustain does little against 180 Armor), which Sanchit parked until
# a real fight says it matters, so the spread limit is wide and written down rather than closed.
WITHER_TARGET = {
    'spear_of_shojin': 105, 'veigos_blade': 111, 'divine_sunderer': 116, 'darken_blade': 174, 'galeforce': 144,
    'heartwood_of_pallas': 175, 'ragefang': 100, 'mortal_reminder': 194, 'darius_axe': 209, 'wraithblade': 219,
    'endless_hunger': 270,
}
WITHER_PAIR_TARGET = {'ragefang': 71, 'endless_hunger': 125}
WITHER_SPREAD = 2.8
WITHER = (4225.0, 180.0, 110.0)

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


# The skill tree (docs/progression/growth.md section 8): what all 56 nodes add up to, written down. Raised on 2026-10-22
# (Sanchit: "+0.5 AD or 1 lethality or 1 HP is nothing in the bigger picture"): the nodes were sized against a 7-damage
# sword and a 20-health player, and the legendary weapons are 12 to 34. Nobody holds the whole web (41 points of 56
# nodes), so a committed player has about two corners of it. Attack speed was left as it was: 3% a node is already 3%
# more damage.
GROWTH = os.path.join(BASE, 'combat', 'src', 'main', 'resources', 'data', 'ascension_combat', 'ascension_combat', 'growth_node', 'web')
WEB_TOTAL = {
    'max_health': 44, 'armor': 30, 'magic_resist': 25, 'attack_damage': 13.5, 'lethality': 12, 'armor_penetration_percent': 0.13,
    'magic_penetration_percent': 0.10, 'attack_speed': 0.54, 'omnivamp': 0.19, 'movement_speed': 0.43,
}
GROWTH_POINTS = 41


def check_growth(problems):
    totals, by_theme = {}, {}
    nodes = 0
    for name in sorted(os.listdir(GROWTH)):
        with open(os.path.join(GROWTH, name), encoding='utf-8') as f:
            node = json.load(f)
        nodes += 1
        for stat in node['stats']:
            key = stat['attribute'].split(':')[-1].replace('generic.', '')
            totals[key] = totals.get(key, 0.0) + stat['amount']
            theme = by_theme.setdefault(node['arm'], {})
            theme[key] = theme.get(key, 0.0) + stat['amount']
    print('Skill tree: %d nodes, %d points to spend; the whole web adds up to:' % (nodes, GROWTH_POINTS))
    for key, written in WEB_TOTAL.items():
        got = totals.get(key, 0.0)
        flag = '' if abs(got - written) < 1e-6 else '   <-- off'
        print('  %-28s %8.2f %8.2f%s' % (key, got, written, flag))
        if flag:
            problems.append('the skill tree gives %s %.3f in all, written %.3f' % (key, got, written))
    for theme in sorted(by_theme):
        shown = ', '.join('%s %.2f' % (k, v) for k, v in sorted(by_theme[theme].items())
                          if k in WEB_TOTAL)
        print('  %-9s %s' % (theme, shown))


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

    print('The Wither (%d HP, %d Armor, %d Magic Resist), alone:' % WITHER)
    print('  %-22s %8s %8s %8s' % ('weapon', 'alone', 'target', 'kit'))
    late = {}
    for name in WITHER_TARGET:
        a = ttk.fight(name, ttk.Boss(*WITHER), False)
        k = ttk.fight(name, ttk.Boss(*WITHER), True)
        late[name] = a
        flag = '' if abs(a - WITHER_TARGET[name]) <= WITHER_TARGET[name] * TTK_TOLERANCE else '   <-- off'
        print('  %-22s %8.0f %8d %8.0f%s' % (name, a, WITHER_TARGET[name], k, flag))
        if flag:
            problems.append('%s kills the Wither in %.0f s, written %d s' % (name, a, WITHER_TARGET[name]))
    for name, target in WITHER_PAIR_TARGET.items():
        a = ttk.fight(name, ttk.Boss(*WITHER), False, None, True)
        k = ttk.fight(name, ttk.Boss(*WITHER), True, None, True)
        flag = '' if abs(a - target) <= target * 0.08 else '   <-- off'
        print('  %-22s %8.0f %8d %8.0f%s' % (name + ' (a pair)', a, target, k, flag))
        if flag:
            problems.append('%s as a pair kills the Wither in %.0f s, written %d s' % (name, a, target))
    late_spread = max(late.values()) / min(late.values())
    print('  spread slowest / fastest: %.2f (limit %.1f)' % (late_spread, WITHER_SPREAD))
    if late_spread > WITHER_SPREAD:
        problems.append('the weapons are %.2f times apart at the Wither, limit %.1f' % (late_spread, WITHER_SPREAD))

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

    check_growth(problems)

    for p in problems:
        print('balance: ' + p)
    print('%d problems' % len(problems))
    if check and problems:
        sys.exit(1)


if __name__ == '__main__':
    main()
