"""Breath: air to carry, air to make, air to keep. The compressor, tanks, the helium tank, what holds air in a room or a ship, and the Air
Dispenser that keeps it.

Project 7 of 9.
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, LESSON, Quest, advancement, checkmark, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='breath',
    title='Breath',
    subtitle='Air to carry, air to make, air to keep.',
    icon=picture('textures/quests/icons/breath.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='refill', legacy='road_up',
            title='A machine that makes air',
            subtitle='Spin it, and it breathes out',
            icon=item_icon('ascension_compat_create:air_compressor'), shape=GOAL,
            deps=['first_days.first_breath'],
            text=[
                'An air compressor turns a spinning shaft into air. The faster it spins, the more air it makes.',
                'Use a tank on it to fill the tank. Fill every tank before you leave: in space nothing makes air.',
            ],
            tasks=[item('ascension_compat_create:air_compressor', 1)],
            rewards=[give('minecraft:glass', 16)],
        ),
        Quest(
            slug='range', legacy='road_up',
            title='How far can you go?',
            subtitle='Count minutes of air',
            icon=item_icon('minecraft:clock'), shape=LESSON, size=0.9,
            deps=['helium'],
            text=[
                'Space is airless. A tank lasts five minutes and you can carry two, plus twenty seconds in your lungs.',
                'Work out your ship\'s speed, then plan the trip in minutes, not blocks.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='room', legacy='road_up',
            title='A room of air',
            subtitle='Seal it, and it fills',
            icon=item_icon('ascension_atmosphere:air_dispenser'), shape=GOAL,
            deps=['seal', 'range'],
            text=[
                'An air dispenser keeps a sealed room breathable while it has air. Pipe air to it from a compressor and a tank.',
                'Any hole lets the air out, so seal every gap. A bigger room needs more air every second.',
            ],
            tasks=[advancement('ascension_compat_create:project/breath')],
            rewards=[give('minecraft:iron_block', 1)],
        ),
        Quest(
            slug='helium',
            title='A bigger breath',
            subtitle='The helium tank holds twice as much',
            icon=item_icon('ascension_atmosphere:oxygen_tank_helium'), shape=GOAL,
            deps=['refill'],
            text=[
                'A helium tank holds twice the air of a plain one. Fill it at the compressor the same way.',
                'Two helium tanks last twice as long as two plain ones, and the trip to the next world is a long one.',
            ],
            tasks=[item('ascension_atmosphere:oxygen_tank_helium', 1)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='seal',
            title='What holds air',
            subtitle='Walls, doors, slabs and panes',
            icon=item_icon('minecraft:glass_pane'), shape=LESSON, size=0.9,
            deps=['refill'],
            text=[
                'A closed room holds air if every side is a wall. Full blocks, shut doors and trapdoors, slabs and glass panes all hold it.',
                'Stairs, iron bars, fences and an open door let it out. A hot air balloon\'s burner and envelope hold it too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:glass_pane', 8)],
        ),
    ],
)

blueprint_page(CHAPTER, number=7, of=9, motif='breath', finale='room', points=1)
