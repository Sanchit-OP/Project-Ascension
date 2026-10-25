"""Current: spin becomes power, and power keeps. An alternator, wires, an accumulator, and a motor to turn it back. Pays nothing in skill points:
it is a short step between Brass and Crude, which need its power.

Project 4 of 9.
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, LESSON, Quest, checkmark, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='current',
    title='Current',
    subtitle='Spin becomes power, and power keeps.',
    icon=picture('textures/quests/icons/current.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='power_aboard', legacy='ship_fly',
            title='Power aboard',
            subtitle='Electricity, stored in the hull',
            icon=item_icon('createaddition:modular_accumulator'), shape=LESSON, size=0.9,
            deps=['spin.stress'],
            text=[
                'Create Crafts & Additions turns spin into electricity and electricity back into spin. Wires carry it, accumulators store it.',
                'A ship can carry its own power: with the Sable addon the wires follow the ship, so a charged accumulator in the hull runs anything on board.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='alternator', legacy='ship_fly',
            title='An alternator',
            subtitle='Spin in, power out',
            icon=item_icon('createaddition:alternator'), shape=GOAL,
            deps=['power_aboard', 'brass.crafter'],
            text=[
                'Andesite alloy, iron plates, an iron rod and copper spools, on a mechanical crafter. A shaft turns it and it gives energy to whatever is wired to it.',
            ],
            tasks=[item('createaddition:alternator', 1)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='accumulator', legacy='ship_fly',
            title='An accumulator',
            subtitle='Power kept for later',
            icon=item_icon('createaddition:modular_accumulator'), shape=GOAL,
            deps=['alternator', 'wires'],
            text=[
                'A brass casing, two capacitors, a copper rod and electrum wire. Connect it to a generator, and it feeds machines when the generator is stopped.',
            ],
            tasks=[item('createaddition:modular_accumulator', 1)],
            rewards=[give('minecraft:redstone', 16)],
        ),
        Quest(
            slug='wires',
            title='Wires',
            subtitle='Power travels along copper',
            icon=item_icon('createaddition:connector'), shape=GOAL,
            deps=['alternator'],
            text=[
                'A connector on each end and a copper wire between them carry power across a base.',
                'With the Sable addon the wire stays joined when the ship moves, so a ship can have its own wiring.',
            ],
            tasks=[item('createaddition:connector', 2)],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='motor',
            title='An electric motor',
            subtitle='Power back into spin',
            icon=item_icon('createaddition:electric_motor'), shape=GOAL,
            deps=['alternator'], optional=True,
            text=[
                'The alternator turns spin into power; the electric motor does the opposite. Power kept in an accumulator can turn a shaft again.',
                'A motor is a quiet way to turn a propeller: no fuel, no smoke.',
            ],
            tasks=[item('createaddition:electric_motor', 1)],
            rewards=[give('minecraft:redstone', 16)],
        ),
    ],
)

blueprint_page(CHAPTER, number=4, of=9, motif='current', finale='accumulator', points=0)
