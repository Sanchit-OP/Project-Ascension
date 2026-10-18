"""Aeronautics: In Flight. Reading the sky, steering by signals, and the thrusters that make a rocket.

Same rules as the other teaching pages: the parts are Simulated's, Aeronautics' and Create Simulated Thrusters',
each quest says what a part is made of (read from the mods' recipes) and what its own guide says, and where the
mods keep a real advancement the quest waits for it. Nothing gates anything.

The rocket half is ADR-0025: a rocket is a ship with thrusters under it, carried to space the same way as any
ship (ADR-0015). Helium, from the Moon, is a thruster fuel through Create's own fuel list
(modules/compat/create, data/create/data_maps).

Layout: the sky and its instruments on top (y 0 to 4); thrusters below (y 7 to 11); the rocket at the bottom.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, dimension, give, item,
                          item_icon, picture)

CHAPTER = Chapter(
    slug='ship_fly',
    title='Aeronautics: In Flight',
    subtitle='Climb, steer, and leave.',
    icon=picture('textures/quests/icons/ship_fly.png'),
    shape=GOAL,
    quests=[
        # ---- The sky and its instruments ----------------------------------------------------------
        Quest(
            slug='pressure', x=4.5, y=0,
            title='Air pressure',
            subtitle='Climb, and everything weakens',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            text=[
                'Hot air burners lift less and propellers push less the higher you go, because the air thins.',
                'An altitude sensor shows your height, and with goggles on, the air pressure too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='altitude_sensor', x=1.5, y=2,
            title='The altitude sensor',
            subtitle='A signal that follows your height',
            icon=item_icon('simulated:altitude_sensor'), shape=GOAL,
            deps=['pressure'],
            text=[
                'Paper over an iron sheet over a casing. Climbing raises its signal by default, and you can set the lowest and highest heights, or invert it.',
            ],
            tasks=[advancement('simulated:no_pressure')],
            rewards=[give('minecraft:paper', 8)],
        ),
        Quest(
            slug='velocity', x=4.5, y=2,
            title='The velocity sensor',
            subtitle='A signal that follows your speed',
            icon=item_icon('simulated:velocity_sensor'), shape=GOAL,
            deps=['pressure'],
            text=[
                'A propeller over a barrel over a casing. It gives a signal by speed along the way it faces; set the speed that gives full strength.',
            ],
            tasks=[advancement('simulated:speed_is_key')],
            rewards=[give('minecraft:barrel', 1)],
        ),
        Quest(
            slug='goggles', x=7.5, y=2,
            title="Aviator's goggles",
            subtitle='A pilot sees more',
            icon=item_icon('aeronautics:aviators_goggles'), shape=GOAL,
            deps=['pressure'],
            text=[
                'Leather and a pair of goggles. They show what engineer\'s goggles show: speed, stress and what a block holds.',
            ],
            tasks=[advancement('aeronautics:high_fashion')],
            rewards=[give('minecraft:leather', 4)],
        ),
        Quest(
            slug='throttle', x=1.5, y=4,
            title='A throttle lever',
            subtitle='Set a signal by hand',
            icon=item_icon('simulated:throttle_lever'), shape=GOAL,
            deps=['altitude_sensor', 'the_rot.brass'],
            text=[
                'A stick on a brass casing. Drag it to set a signal from nothing to full, and use a wrench to invert it.',
            ],
            tasks=[item('simulated:throttle_lever', 1)],
            rewards=[give('minecraft:stick', 8)],
        ),
        Quest(
            slug='gimbal', x=4.5, y=4,
            title='A gimbal sensor',
            subtitle='A signal that follows the tilt',
            icon=item_icon('simulated:gimbal_sensor'), shape=GOAL,
            deps=['velocity', 'the_rot.brass'],
            text=[
                'A compass over a gyroscopic mechanism over a brass casing. It gives a signal by tilt, each axis on its own.',
                'By default the signal is strongest downhill; you can set the angle and invert it.',
            ],
            tasks=[advancement('simulated:the_definition_of_up')],
            rewards=[give('minecraft:compass', 1)],
        ),
        Quest(
            slug='navigation', x=7.5, y=4,
            title='A navigation table',
            subtitle='A signal toward where you are going',
            icon=item_icon('simulated:navigation_table'), shape=GOAL,
            deps=['goggles', 'create_craft.precision'],
            text=[
                'A brass sheet over a precision mechanism over a brass casing. Give it a navigation item and it signals toward the destination, weaker when you do not face it.',
            ],
            tasks=[advancement('simulated:thataway')],
            rewards=[give('minecraft:map', 1)],
        ),

        # ---- Thrusters ----------------------------------------------------------------------------
        Quest(
            slug='thrusters', x=4.5, y=7,
            title='Thrusters',
            subtitle='A block that pushes',
            icon=item_icon('create_simulated_thrusters:blaze_thruster'), shape=LESSON, size=0.9,
            deps=['navigation', 'gimbal'],
            text=[
                'A propeller pushes against air. A thruster is a block that pushes along the way it faces. Create Simulated Thrusters adds three.',
                'The redstone thruster turns shaft spin into push. The blaze thruster burns fuel. The brass thruster is large and can be steered.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='blaze_thruster', x=1.5, y=9,
            title='A blaze thruster',
            subtitle='Fuel in, push out',
            icon=item_icon('create_simulated_thrusters:blaze_thruster'), shape=GOAL,
            deps=['thrusters', 'the_rot.burner'],
            text=[
                'Two iron ingots, two engine assemblies and a blaze burner. It burns fuel and takes no shaft spin, so it costs no stress.',
                'Feed it by hand or from a belt. A redstone signal pauses it and stops it using fuel.',
            ],
            tasks=[item('create_simulated_thrusters:blaze_thruster', 1)],
            rewards=[give('minecraft:coal', 16)],
        ),
        Quest(
            slug='fuel', x=4.5, y=9,
            title='Fuel decides the push',
            subtitle='Sticks, coal, cake, helium',
            icon=item_icon('ascension_worlds:raw_helium'), shape=GOAL,
            deps=['thrusters'],
            text=[
                'A thruster burns what a blaze burner burns. Sticks push a little, coal and logs push hard, a blaze cake pushes hardest.',
                'Raw helium from the Moon burns like a blaze cake, and for longer. Bring a stack.',
            ],
            tasks=[item('ascension_worlds:raw_helium', 8)],
            rewards=[give('minecraft:coal_block', 2)],
        ),
        Quest(
            slug='redstone_thruster', x=7.5, y=9,
            title='A redstone thruster',
            subtitle='Spin in, push out',
            icon=item_icon('create_simulated_thrusters:redstone_thruster'), shape=GOAL,
            deps=['thrusters', 'create_power.stress'],
            text=[
                'Two electron tubes, a dropper, two repeaters and a shaft. It pushes along its shaft when spun and powered.',
                'More spin and a stronger signal give more push.',
            ],
            tasks=[item('create_simulated_thrusters:redstone_thruster', 1)],
            rewards=[give('minecraft:repeater', 2)],
        ),
        Quest(
            slug='brass_thruster', x=10.5, y=9,
            title='A brass thruster',
            subtitle='Large, and it can swing',
            icon=item_icon('create_simulated_thrusters:brass_thruster'), shape=GOAL,
            deps=['redstone_thruster'],
            text=[
                'Four brass sheets, a shaft, two electron tubes, a redstone thruster and a precision mechanism.',
                'It builds a 3 by 3 by 3 frame round itself and can swing its push left or right with redstone.',
            ],
            tasks=[item('create_simulated_thrusters:brass_thruster', 1)],
            rewards=[give('minecraft:redstone_block', 1)],
        ),

        # ---- A rocket -----------------------------------------------------------------------------
        Quest(
            slug='rocket', x=4.5, y=12,
            title='A rocket',
            subtitle='Thrusters under a hull',
            icon=item_icon('create_simulated_thrusters:blaze_thruster'), shape=LANDMARK, size=1.2,
            deps=['blaze_thruster', 'fuel'],
            text=[
                'A rocket is a ship with its thrusters at the bottom, pointing down, and fuel in each. Glue it, assemble it, fuel it, then give the thrusters a signal.',
                'Past the top of the sky it is carried into space like any ship. Take air.',
            ],
            tasks=[dimension('ascension_worlds:space')],
            rewards=[give('ascension_worlds:raw_helium', 4)],
        ),
    ],
)
