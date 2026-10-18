#!/usr/bin/env python3
"""Time to kill: how long each combat weapon takes to kill a target, abilities used correctly.

A small discrete simulation of the damage code in modules/combat (docs/progression/balance.md section 5).
It mirrors, one for one, what the effects classes do, so the numbers are the game's, not an estimate of
its feel. What it assumes, and what a real fight will change, is printed with the result.

  python tools/time-to-kill.py                       # the 1000 HP, 150 Armor, 90 Magic Resist boss
  python tools/time-to-kill.py 1000 150 90 --kit     # with a typical kit's extra Attack Damage and Lethality

The formulas (all from the code):
  Armor and Magic Resist: damage taken = 0.5 ** (stat / 100).
  Lethality comes off Armor after percent Penetration; Armor never goes below 0.
  Melee: every swing at full charge, so damage is the weapon's Attack Damage figure.
  Arrows: a full draw's 6 base damage plus the shooter's whole Attack Damage, as physical damage.
  Each ability's bonus is added where its class adds it (before Armor, after it, or true).
"""

import sys

STEP = 0.05          # seconds
MAX_SECONDS = 600.0
MAX_SWINGS_PER_SECOND = 8.0   # a player cannot land more; matters only for Ragefang's ramp


def mult(stat):
    return 0.5 ** (max(stat, 0.0) / 100.0)


class Boss:
    def __init__(self, hp, armor, mr):
        self.hp, self.max_hp, self.armor, self.mr = hp, hp, armor, mr
        self.shred = 0           # Darius's Axe stacks

    def effective_armor(self, lethality=0.0, pen=0.0, ignore_share=0.0):
        a = self.armor * (1 - 0.05 * self.shred)
        a = a * (1 - pen) - lethality
        return max(a, 0.0)

    def physical(self, raw, lethality=0.0, pen=0.0, ignore_share=0.0):
        """Physical damage through Armor. `ignore_share` is Mortal Reminder's: it scales the reduction itself."""
        taken = mult(self.effective_armor(lethality, pen))
        reduction = 1.0 - taken
        return raw * (1.0 - reduction * (1.0 - ignore_share))

    def magic(self, raw, pen=0.0):
        return raw * mult(self.mr * (1 - pen))


def melee(name, damage, speed, kit_ad=0.0, kit_leth=0.0, extra_leth=0.0, pen=0.0):
    """Returns a function that runs the fight and gives the time to kill."""
    def run(boss):
        ad = damage + kit_ad                      # the Attack Damage stat is the weapon's figure plus the kit's
        leth = kit_leth + extra_leth
        t, next_swing, hits, last_burst = 0.0, 0.0, 0, -99.0
        spd = speed
        sunder_ready = 0.0
        while boss.hp > 0 and t < MAX_SECONDS:
            if t >= next_swing:
                hits += 1
                raw = ad
                true_bonus = 0.0
                magic_bonus = 0.0
                if name == 'veigos_blade':
                    raw += boss.hp * (0.08 + 0.002 * ad)
                elif name == 'darius_axe':
                    pass
                elif name == 'spear_of_shojin':
                    if hits % 3 == 0:
                        true_bonus = boss.max_hp * (0.05 + 0.001 * ad)
                elif name == 'divine_sunderer':
                    if t >= sunder_ready:                         # right-click primes, every 5 s
                        sunder_ready = t + 5.0
                        raw += boss.max_hp * (0.04 + 0.001 * ad)
                        magic_bonus = boss.max_hp * (0.04 + 0.001 * ad)
                elif name == 'darken_blade':
                    if hits % 2 == 0 and t - last_burst >= 8.0:   # every second hit releases it, 8 s cooldown
                        last_burst = t
                        magic_bonus = boss.max_hp * (0.05 + 0.001 * ad)
                elif name == 'ragefang':
                    magic_bonus = (5 + 0.2 * ad) * (2 if hits % 3 == 0 else 1)
                boss.hp -= boss.physical(raw, leth, pen) + boss.magic(magic_bonus) + true_bonus
                if name == 'darius_axe':
                    boss.shred = min(12, boss.shred + 1)          # this hit's stack counts from the next
                if name == 'ragefang':
                    spd = speed * (1 + min(5.0, 0.05 * hits))
                if name == 'endless_hunger':
                    spd = speed * (1 + 0.02 * ad)
                next_swing = t + 1.0 / min(spd, MAX_SWINGS_PER_SECOND)
            t += STEP
        return t if boss.hp <= 0 else float('inf')
    return run


def bow(name, ad_stat, kit_ad=0.0, kit_leth=0.0, draw=1.0):
    def run(boss):
        ad = ad_stat + 1.0 + kit_ad                  # a bare hand's 1, the bow's stat, the kit's
        t, shot_ready, stacks = 0.0, 0.0, 0
        hunt_ready, dash_ready = 0.0, 0.0
        busy_until = 0.0
        while boss.hp > 0 and t < MAX_SECONDS:
            # Galeforce's dash shot is free of the draw: sneak and right-click, every 8 s
            if name == 'galeforce' and t >= dash_ready:
                dash_ready = t + 8.0
                missing = boss.max_hp - boss.hp
                raw = 6.0 + ad + missing * (0.20 + 0.005 * ad)
                boss.hp -= boss.physical(raw, kit_leth)
            if t >= busy_until:
                raw = 6.0 + ad
                magic = 0.0
                ignore = 0.6 if name == 'mortal_reminder' else 0.0
                took = draw
                if name == 'heartwood_of_pallas':
                    magic = 3.0 + 0.15 * ad
                    if t >= hunt_ready and stacks > 0:           # Hunt: a 1.75 s draw, 15 s cooldown
                        hunt_ready = t + 15.0
                        took = 1.75
                        execute = (boss.max_hp - boss.hp) * stacks / 100.0
                        magic += execute
                        stacks = 0
                    else:
                        stacks += 1
                boss.hp -= boss.physical(raw, kit_leth, 0.0, ignore) + boss.magic(magic)
                busy_until = t + took
            t += STEP
        return t if boss.hp <= 0 else float('inf')
    return run


def lineup(kit_ad, kit_leth):
    return [
        ('Veigo\'s Blade', melee('veigos_blade', 17.9, 1.6, kit_ad, kit_leth)),
        ('Darius\'s Axe', melee('darius_axe', 30.0, 1.0, kit_ad, kit_leth)),
        ('Spear of Shojin', melee('spear_of_shojin', 21.9, 1.4, kit_ad, kit_leth)),
        ('Divine Sunderer', melee('divine_sunderer', 21.4, 1.2, kit_ad, kit_leth)),
        ('Darken Blade', melee('darken_blade', 22.6, 1.4, kit_ad, kit_leth)),
        ('Ragefang', melee('ragefang', 18.3, 2.0, kit_ad, kit_leth)),
        ('Endless Hunger', melee('endless_hunger', 5.7, 4.0, kit_ad, kit_leth)),
        ('Wraithblade', melee('wraithblade', 14.2, 1.6, kit_ad, kit_leth, extra_leth=20.0)),
        ('Mortal Reminder', bow('mortal_reminder', 27.5, kit_ad, kit_leth)),
        ('Heartwood of Pallas', bow('heartwood_of_pallas', 27.5, kit_ad, kit_leth)),
        ('Galeforce', bow('galeforce', 23.0, kit_ad, kit_leth)),
        # for scale
        ('(netherite sword)', melee('plain', 8.0, 1.6, kit_ad, kit_leth)),
        ('(a boss weapon, 14 at 1.6)', melee('plain', 14.0, 1.6, kit_ad, kit_leth)),
    ]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    hp, armor, mr = (float(args[0]), float(args[1]), float(args[2])) if len(args) >= 3 else (1000.0, 150.0, 90.0)
    kit = '--kit' in sys.argv
    kit_ad, kit_leth = (12.0, 10.0) if kit else (0.0, 0.0)
    print('Target: %.0f HP, %.0f Armor (takes %.0f%%), %.0f Magic Resist (takes %.0f%%)'
          % (hp, armor, mult(armor) * 100, mr, mult(mr) * 100))
    print('Wielder: %s' % ('the weapon plus a kit: +%.0f Attack Damage, +%.0f Lethality' % (kit_ad, kit_leth)
                           if kit else 'the weapon alone'))
    print('%-28s %10s %10s' % ('weapon', 'seconds', 'vs naked'))
    for name, run in lineup(kit_ad, kit_leth):
        t = run(Boss(hp, armor, mr))
        dummy = run(Boss(hp, 0.0, 0.0))
        print('%-28s %10.1f %10.1f' % (name, t, dummy))


if __name__ == '__main__':
    main()
