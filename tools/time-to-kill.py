#!/usr/bin/env python3
"""Time to kill: how long each combat weapon takes to kill a target, abilities used correctly.

A small discrete simulation of the damage code in modules/combat (docs/progression/balance.md section 5).
It mirrors what the effects classes do, and it reads each weapon's numbers from the game's own data
(`item_stats.json`), so the table is the game's, not a copy that can drift.

  python tools/time-to-kill.py                          # the 1000 HP, 150 Armor, 90 Magic Resist boss
  python tools/time-to-kill.py 4000 150 90              # another boss
  python tools/time-to-kill.py --tune endless_hunger 100 # the damage that makes it take 100 s alone

"Kit" is a typical full set of legendary armour worn with the weapon: together its pieces add about +8
Attack Damage and +8 Lethality to the wielder. The kit column is what a player nearer the end of the
game would see; the first column is the weapon on its own, which is what the weapon is worth by itself.

The formulas (all from the code):
  Armor and Magic Resist: damage taken = 0.5 ** (stat / 100).
  Lethality comes off Armor after percent Penetration; Armor never goes below 0.
  Melee: every swing at full charge, so damage is the weapon's Attack Damage figure.
  Arrows: a full draw's 6 base damage plus the shooter's whole Attack Damage, as physical damage.
  Percent-of-health procs are held to 1 x the wielder's Attack Damage a go (AbilityScaling).
  Each ability's bonus is added where its class adds it (before Armor, after it, or true).
Assumed: the target stands still, every swing lands, a bow's full draw takes 1 s, a player lands at most
8 swings a second. A real fight is slower, which lengthens every figure and the weapons that need
positioning (the dashes, the blink) most.
"""

import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
STATS = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'data', 'ascension_combat',
                     'data_maps', 'item', 'item_stats.json')

STEP = 0.05
MAX_SECONDS = 900.0
MAX_SWINGS_PER_SECOND = 8.0
CAP_PER_AD = 1.0
GALE_CAP, HUNT_CAP = 3.0, 5.0      # Galeforce's and Hunt's missing-health bonuses, in Attack Damage
ENDLESS_AS_PER_AD = 0.012          # Endless Hunger: attack speed per point of Attack Damage
KIT_AD, KIT_LETHALITY = 8.0, 8.0
RAGE_MAGIC, RAGE_SCALE, RAGE_PER_HIT, RAGE_CAP = 3.0, 0.1, 0.03, 1.0   # Ragefang's on-hit magic (flat, per Attack Damage), attack speed per hit, and its limit

MELEE = ['veigos_blade', 'darius_axe', 'spear_of_shojin', 'divine_sunderer', 'darken_blade', 'ragefang',
         'endless_hunger', 'wraithblade']
BOWS = ['mortal_reminder', 'heartwood_of_pallas', 'galeforce']
NAMES = {'veigos_blade': "Veigo's Blade", 'darius_axe': "Darius's Axe", 'spear_of_shojin': 'Spear of Shojin',
         'divine_sunderer': 'Divine Sunderer', 'darken_blade': 'Darken Blade', 'ragefang': 'Ragefang',
         'endless_hunger': 'Endless Hunger', 'wraithblade': 'Wraithblade', 'mortal_reminder': 'Mortal Reminder',
         'heartwood_of_pallas': 'Heartwood of Pallas', 'galeforce': 'Galeforce'}


def mult(stat):
    return 0.5 ** (max(stat, 0.0) / 100.0)


class Boss:
    def __init__(self, hp, armor, mr):
        self.hp, self.max_hp, self.armor, self.mr = hp, hp, armor, mr
        self.shred = 0

    def physical(self, raw, lethality=0.0, ignore_share=0.0):
        armor = max(self.armor * (1 - 0.05 * self.shred) - lethality, 0.0)
        reduction = 1.0 - mult(armor)
        return raw * (1.0 - reduction * (1.0 - ignore_share))

    def magic(self, raw):
        return raw * mult(self.mr)


def entry(name, overrides=None):
    with open(STATS, encoding='utf-8') as f:
        values = json.load(f)['values']
    e = dict(values['ascension_combat:' + name])
    e = json.loads(json.dumps(e))
    for k, v in (overrides or {}).items():
        if k == 'damage':
            e['weapon']['damage'] = v
        elif k == 'speed':
            e['weapon']['speed'] = v
        elif k == 'ad':
            e.setdefault('stats', {})['attack_damage'] = v
    return e


def fight(name, boss, kit, overrides=None):
    """Seconds to kill, or infinity."""
    e = entry(name, overrides)
    stats = e.get('stats', {})
    kit_ad, kit_leth = (KIT_AD, KIT_LETHALITY) if kit else (0.0, 0.0)
    leth = stats.get('lethality', 0.0) + kit_leth
    t = 0.0

    if name in MELEE:
        damage, speed = e['weapon']['damage'], e['weapon']['speed']
        ad = damage + kit_ad
        next_swing, hits, last_burst, sunder_ready, spd = 0.0, 0, -99.0, 0.0, speed
        while boss.hp > 0 and t < MAX_SECONDS:
            if t >= next_swing:
                hits += 1
                raw, true_bonus, magic_bonus = ad, 0.0, 0.0
                cap = CAP_PER_AD * ad
                if name == 'veigos_blade':
                    raw += min(boss.hp * (0.08 + 0.002 * ad), cap)
                elif name == 'spear_of_shojin' and hits % 3 == 0:
                    true_bonus = min(boss.max_hp * (0.05 + 0.001 * ad), cap)
                elif name == 'divine_sunderer' and t >= sunder_ready:
                    sunder_ready = t + 5.0
                    share = min(boss.max_hp * (0.04 + 0.001 * ad), cap)
                    raw += share
                    magic_bonus = share
                elif name == 'darken_blade' and hits % 2 == 0 and t - last_burst >= 8.0:
                    last_burst = t
                    magic_bonus = min(boss.max_hp * (0.05 + 0.001 * ad), cap)
                elif name == 'ragefang':
                    magic_bonus = (RAGE_MAGIC + RAGE_SCALE * ad) * (2 if hits % 3 == 0 else 1)
                boss.hp -= boss.physical(raw, leth) + boss.magic(magic_bonus) + true_bonus
                if name == 'darius_axe':
                    boss.shred = min(12, boss.shred + 1)
                if name == 'ragefang':
                    spd = speed * (1 + min(RAGE_CAP, RAGE_PER_HIT * hits))
                if name == 'endless_hunger':
                    spd = speed * (1 + ENDLESS_AS_PER_AD * ad)
                next_swing = t + 1.0 / min(spd, MAX_SWINGS_PER_SECOND)
            t += STEP
    else:
        ad = 1.0 + stats.get('attack_damage', 0.0) + kit_ad
        shot_ready, busy_until, hunt_ready, dash_ready, hunt_stacks = 0.0, 0.0, 0.0, 0.0, 0
        while boss.hp > 0 and t < MAX_SECONDS:
            if name == 'galeforce' and t >= dash_ready:
                dash_ready = t + 8.0
                missing = boss.max_hp - boss.hp
                raw = 6.0 + ad + min(missing * (0.20 + 0.005 * ad), GALE_CAP * ad)
                boss.hp -= boss.physical(raw, leth)
            if t >= busy_until:
                raw, magic = 6.0 + ad, 0.0
                ignore = 0.6 if name == 'mortal_reminder' else 0.0
                took = 1.0
                if name == 'heartwood_of_pallas':
                    magic = 3.0 + 0.15 * ad
                    if t >= hunt_ready and hunt_stacks > 0:
                        hunt_ready, took = t + 15.0, 1.75
                        magic += min((boss.max_hp - boss.hp) * hunt_stacks / 100.0, HUNT_CAP * ad)
                        hunt_stacks = 0
                    else:
                        hunt_stacks += 1
                boss.hp -= boss.physical(raw, leth, ignore) + boss.magic(magic)
                busy_until = t + took
            t += STEP
    return t if boss.hp <= 0 else float('inf')


def plain(damage, speed, kit, hp, armor, mr):
    boss = Boss(hp, armor, mr)
    ad = damage + (KIT_AD if kit else 0.0)
    leth = KIT_LETHALITY if kit else 0.0
    t, nxt = 0.0, 0.0
    while boss.hp > 0 and t < MAX_SECONDS:
        if t >= nxt:
            boss.hp -= boss.physical(ad, leth)
            nxt = t + 1.0 / speed
        t += STEP
    return t


def tune(name, target, hp, armor, mr, kit):
    key = 'ad' if name in BOWS else 'damage'
    lo, hi = 0.5, 400.0
    for _ in range(40):
        mid = (lo + hi) / 2
        if fight(name, Boss(hp, armor, mr), kit, {key: mid}) > target:
            lo = mid
        else:
            hi = mid
    return hi


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if '--tune' in sys.argv:
        name, target = args[0], float(args[1])
        hp, armor, mr = (float(args[2]), float(args[3]), float(args[4])) if len(args) >= 5 else (1000.0, 150.0, 90.0)
        print('%s: %s of %.1f takes %.0f s alone' % (name, 'attack damage' if name in BOWS else 'damage',
                                                    tune(name, target, hp, armor, mr, False), target))
        return
    hp, armor, mr = (float(args[0]), float(args[1]), float(args[2])) if len(args) >= 3 else (1000.0, 150.0, 90.0)
    print('Target: %.0f HP, %.0f Armor (takes %.0f%%), %.0f Magic Resist (takes %.0f%%)'
          % (hp, armor, mult(armor) * 100, mr, mult(mr) * 100))
    print('Alone = the weapon only. Kit = with a full legendary set: +%.0f Attack Damage, +%.0f Lethality.'
          % (KIT_AD, KIT_LETHALITY))
    print('%-26s %9s %9s' % ('weapon', 'alone (s)', 'kit (s)'))
    for name in MELEE + BOWS:
        a = fight(name, Boss(hp, armor, mr), False)
        k = fight(name, Boss(hp, armor, mr), True)
        print('%-26s %9.0f %9.0f' % (NAMES[name], a, k))
    for label, dmg, spd in (('(netherite sword)', 8.0, 1.6), ('(a boss weapon, 14 at 1.6)', 14.0, 1.6)):
        print('%-26s %9.0f %9.0f' % (label, plain(dmg, spd, False, hp, armor, mr), plain(dmg, spd, True, hp, armor, mr)))


if __name__ == '__main__':
    main()
