"""The Road Up: from a stack of iron to the top of the sky. How a ship is made and what to carry.

The ship parts are Create's, Simulated's and Aeronautics', not ours, so each step names the part, says what it
is made of (read from those mods' recipes), and leaves how it works to Create's own guide (hold W over an
item). Where those mods keep a real advancement for the step ("Bonk!", "Harnessed Hydraulics", "Head in the
Clouds"), the quest waits for that, so the book can tell progress from a guess.

Layout: one trunk down the middle (x 4.5), four branches under its last quest, and the sky at the bottom.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, dimension, give, item,
                          item_icon, picture)

CHAPTER = Chapter(
    slug='road_up',
    title='The Road Up',
    subtitle='Earth to the top of the sky.',
    icon=picture('textures/quests/icons/road_up.png'),
    shape=GOAL,
    quests=[
        # ---- The trunk: Create, from nothing ---------------------------------------------------
        Quest(
            slug='manual', x=4.5, y=0,
            title='The manual in your hand',
            subtitle='Create explains itself',
            icon=item_icon('create:goggles'), shape=LESSON, size=0.9,
            text=[
                'Create teaches its own machines. Hold W over any Create item, in your inventory or in JEI, '
                'and a short animated guide plays.',
                'A ship is made of Create parts, so this is where the way up begins.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:iron_nugget', 16)],
        ),
        Quest(
            slug='alloy', x=4.5, y=2,
            title='Andesite alloy',
            subtitle='Where Create begins',
            icon=item_icon('create:andesite_alloy'), shape=GOAL,
            deps=['manual'],
            text=[
                'Two andesite and two iron nuggets, set on a diagonal, make one alloy.',
                'Shafts, cogwheels, casings and nearly every part of a ship are made from it. You will want a dozen.',
            ],
            tasks=[advancement('create:andesite_alloy'), item('create:andesite_alloy', 12)],
            rewards=[give('minecraft:stripped_oak_log', 8)],
        ),
        Quest(
            slug='casing', x=4.5, y=4,
            title='Casing',
            subtitle='A log, wrapped in alloy',
            icon=item_icon('create:andesite_casing'), shape=GOAL,
            deps=['alloy'],
            text=[
                'Strip a log, then use andesite alloy on it. That is a casing.',
                'A ship\'s controls are built on casings: the physics assembler, the steering wheel, the sensors.',
            ],
            tasks=[advancement('create:andesite_casing'), item('create:andesite_casing', 4)],
            rewards=[give('minecraft:lever', 2), give('minecraft:redstone', 8)],
        ),
        Quest(
            slug='power', x=4.5, y=6,
            title='Spinning power',
            subtitle='Water, wind or a hand crank',
            icon=item_icon('create:water_wheel'), shape=LESSON, size=0.9,
            deps=['casing'],
            text=[
                'Create machines run on spinning power. A water wheel in a stream is the simplest source; '
                'a windmill or a hand crank also works.',
                'Hold W over each to see how it is built.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:oak_planks', 16)],
        ),
        Quest(
            slug='press', x=4.5, y=8,
            title='Pressed iron',
            subtitle='Sheets for everything',
            icon=item_icon('create:iron_sheet'), shape=GOAL,
            deps=['power'],
            text=[
                'A mechanical press flattens iron into sheets. It needs a casing, a shaft, an iron block '
                'and something to turn it.',
                'You need nine: one for the bearing, four for a propeller, four for a burner.',
            ],
            tasks=[advancement('create:mechanical_press'), item('create:iron_sheet', 9)],
            rewards=[give('minecraft:coal_block', 2)],
        ),

        # ---- Four branches ------------------------------------------------------------------
        Quest(
            slug='bearing', x=1.5, y=10,
            title='The bearing',
            subtitle='Titanium turns the blades',
            icon=item_icon('aeronautics:propeller_bearing'), shape=GOAL,
            deps=['press', 'first_days.titanium'],
            text=[
                'Three titanium ingots, an iron sheet and a wooden slab make the bearing that spins a propeller.',
                'The usual recipe asks for brass. Here, titanium does the job.',
            ],
            tasks=[item('aeronautics:propeller_bearing', 1)],
            rewards=[give('create:shaft', 2)],
        ),
        Quest(
            slug='thrust', x=1.5, y=12,
            title='Thrust',
            subtitle='Blades that push the sky',
            icon=item_icon('aeronautics:andesite_propeller'), shape=GOAL,
            deps=['bearing'],
            text=[
                'A propeller is four iron sheets round an alloy. A wooden slab and a shaft make it an andesite propeller.',
                'Set it on the bearing and give it power. A ship has to carry its own: Simulated\'s portable engine '
                'is one way, and Ponder shows how.',
            ],
            tasks=[advancement('aeronautics:for_every_action'), advancement('aeronautics:in_thrust_we_trust')],
            rewards=[give('minecraft:coal', 16)],
        ),
        Quest(
            slug='lift', x=4.5, y=10,
            title='Lift',
            subtitle='Hot air holds it up',
            icon=item_icon('aeronautics:adjustable_burner'), shape=GOAL,
            deps=['press'],
            text=[
                'Envelopes are wool and sticks. Seal them into a shell, put a burner under it, and feed the '
                'burner a coal block.',
                'A burner is four iron sheets, two alloy, redstone and a coal block.',
            ],
            tasks=[advancement('aeronautics:head_in_the_clouds')],
            rewards=[give('minecraft:white_wool', 16)],
        ),
        Quest(
            slug='thin_air', x=4.5, y=12,
            title='Thin air',
            subtitle='Both lift and thrust fade as you climb',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            deps=['lift'],
            text=[
                'Balloons and propellers push against air, and the air thins as you rise. By the top of the sky '
                'about a third is left.',
                'Build with more lift and more blades than seems fair.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='helm', x=7.5, y=10,
            title='A helm',
            subtitle='One block wakes the hull',
            icon=item_icon('simulated:steering_wheel'), shape=GOAL,
            deps=['press'],
            text=[
                'The physics assembler turns the blocks you built into one moving ship. The steering wheel turns it.',
                'Both are made on casings. Place the assembler where the hull begins and hold W over it.',
            ],
            tasks=[item('simulated:physics_assembler', 1), item('simulated:steering_wheel', 1)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='altitude', x=7.5, y=12,
            title='Know your height',
            subtitle='The sky ends at 320',
            icon=item_icon('simulated:altitude_sensor'), shape=GOAL,
            deps=['helm'],
            text=[
                'An altitude sensor is paper, an iron sheet and a casing.',
                'The sky ends at height 320. Past it, the world lets go.',
            ],
            tasks=[item('simulated:altitude_sensor', 1)],
            rewards=[give('minecraft:paper', 8)],
        ),
        Quest(
            slug='refill', x=11, y=10,
            title='A station to fill from',
            subtitle='Air, bottled',
            icon=item_icon('ascension_atmosphere:oxygen_refill_station'), shape=GOAL,
            deps=['first_days.first_breath'],
            text=[
                'A refill station fills a tank from the air around it, so it works only where the air is good.',
                'Fill every tank before you leave. In space there is nothing to compress.',
            ],
            tasks=[item('ascension_atmosphere:oxygen_refill_station', 1)],
            rewards=[give('minecraft:glass', 16)],
        ),
        Quest(
            slug='room', x=11, y=12,
            title='A room of air',
            subtitle='Seal it, and it fills',
            icon=item_icon('ascension_atmosphere:oxygen_emitter'), shape=GOAL,
            deps=['refill'],
            text=[
                'An oxygen emitter fills a sealed room with air. Any hole lets it out, so seal every gap.',
                'A refill station inside such a room works anywhere, even where there is no air outside.',
            ],
            tasks=[item('ascension_atmosphere:oxygen_emitter', 1)],
            rewards=[give('minecraft:iron_block', 1)],
        ),
        Quest(
            slug='range', x=11, y=14,
            title='How far can you go?',
            subtitle='Count minutes of air',
            icon=item_icon('minecraft:clock'), shape=LESSON, size=0.9,
            deps=['room'],
            text=[
                'Space is airless. A tank lasts five minutes and you can carry two, plus twenty seconds in your lungs.',
                'Work out your ship\'s speed, then plan the trip in minutes, not blocks.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),

        # ---- The sky, and what is past it --------------------------------------------------
        Quest(
            slug='sky_ends', x=6, y=15.5,
            title='Where the sky ends',
            subtitle='Past the top, space begins',
            icon=item_icon('minecraft:ender_eye'), shape=LANDMARK, size=1.2,
            deps=['thrust', 'thin_air', 'altitude', 'range'],
            text=[
                'Climb past the top of the sky in your ship and the world lets go. This is space, and it is empty.',
                'On foot you will get nowhere: only a ship can carry you past the top.',
                'Take two tanks. There is nothing to breathe.',
            ],
            tasks=[dimension('ascension_worlds:space')],
            rewards=[give('ascension_atmosphere:oxygen_tank', 2)],
        ),
        Quest(
            slug='chart_flight', x=3.5, y=18,
            title='Steer by the chart',
            subtitle='The Planets page follows you',
            icon=item_icon('minecraft:map'), shape=LESSON, size=0.9,
            deps=['sky_ends'], hide_until='complete',
            text=[
                'Open the Planets page. A point shows where you are, with an arrow for the way you face.',
                'Worlds you know are named. Keep the arrow pointed at one and fly.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:compass', 1)],
        ),
        Quest(
            slug='come_down', x=8.5, y=18,
            title='Coming down',
            subtitle='A world will take your ship',
            icon=item_icon('minecraft:end_rod'), shape=LESSON, size=0.9,
            deps=['sky_ends'], hide_until='complete',
            text=[
                'Fly near a world and your ship is carried down to it, crew and all.',
                'The first lies east of Earth, about four thousand blocks out.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 12)],
        ),
    ],
)
