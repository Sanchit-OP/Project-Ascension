"""Skyline: read the sky, test the ship, leave. Air pressure, the altitude and velocity sensors, goggles and a navigation table, a first
hop up and back (the test flight), and the climb past the top of the sky.

Project 8 of 9.
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, LANDMARK, LESSON, Quest, advancement, checkmark, give, item_icon, picture)

CHAPTER = Chapter(
    slug='skyline',
    title='Skyline',
    subtitle='Read the sky, test the ship, leave.',
    icon=picture('textures/quests/icons/skyline.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='pressure', legacy='ship_fly',
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
            slug='altitude_sensor', legacy='ship_fly',
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
            slug='velocity', legacy='ship_fly',
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
            slug='goggles', legacy='ship_fly',
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
            slug='navigation', legacy='ship_fly',
            title='A navigation table',
            subtitle='A signal toward where you are going',
            icon=item_icon('simulated:navigation_table'), shape=GOAL,
            deps=['goggles', 'brass.precision'],
            text=[
                'A brass sheet over a precision mechanism over a brass casing. Give it a navigation item and it signals toward the destination, weaker when you do not face it.',
            ],
            tasks=[advancement('simulated:thataway')],
            rewards=[give('minecraft:map', 1)],
        ),
        Quest(
            slug='thin_air', legacy='road_up',
            title='Thin air',
            subtitle='Both lift and thrust fade as you climb',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            deps=['pressure'],
            text=[
                'Balloons and propellers push against air, and the air thins as you rise. By the top of the sky about a third is left.',
                'The sky ends at height 320. Past it, the world lets go. Build with more lift and more blades than seems fair.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='sky_ends', legacy='road_up',
            title='Where the sky ends',
            subtitle='Past the top, space begins',
            icon=item_icon('minecraft:ender_eye'), shape=LANDMARK, size=1.2,
            deps=['first_hop', 'velocity', 'navigation', 'wings.propellers', 'breath.range'],
            text=[
                'Climb past the top of the sky in your ship and the world lets go. This is space, and it is empty.',
                'On foot you will get nowhere: only a ship can carry you past the top.',
                'Take two tanks. There is nothing to breathe.',
            ],
            tasks=[advancement('ascension_worlds:project/skyline')],
            rewards=[give('ascension_atmosphere:oxygen_tank', 2)],
        ),
        Quest(
            slug='first_hop',
            title='A first hop',
            subtitle='Up a little, and back down',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            deps=['altitude_sensor', 'thin_air'],
            text=[
                'Before the long climb, take the ship up a few hundred blocks and bring it back. Watch the altitude sensor and the fuel.',
                'Mend what you saw: a ship that drifts, a burner that runs dry, a tank that was nearly empty. Then go.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:firework_rocket', 8)],
        ),
    ],
)

blueprint_page(CHAPTER, number=8, of=9, motif='skyline', finale='sky_ends', points=1)
