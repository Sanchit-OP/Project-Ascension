"""Create: Power. How to make things spin, and what it costs: the first thing every Create player needs.

Create teaches its own machines (hold W over an item), so each quest names the part, says what it is made of
(read from Create 6.0.10's recipes) and what to watch for, and leaves the animation to the game. Where Create keeps
a real advancement for the step, the quest waits for that. Nothing here gates anything: a player may do these in any
order, or none.

Layout: shafts at the top, three ways to make spin under them, what it costs in the middle, and what to do with
spin at the bottom.
"""

from quests.model import (GOAL, LESSON, Chapter, Quest, advancement, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='create_power',
    title='Create: Power',
    subtitle='Make things spin, and learn what it costs.',
    icon=picture('textures/quests/icons/create_power.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='shafts', x=3, y=0,
            title='Shafts and cogs',
            subtitle='Spin travels along them',
            icon=item_icon('create:cogwheel'), shape=GOAL,
            text=[
                'Two andesite alloy make eight shafts. A shaft and a plank make a cogwheel; one more plank makes it large.',
                'Cogs that touch turn each other, the opposite way. A large cog driving a small one makes the small one '
                'spin twice as fast.',
            ],
            tasks=[advancement('create:shifting_gears')],
            rewards=[give('create:andesite_alloy', 4)],
        ),

        # ---- Three ways to make spin ------------------------------------------------------------
        Quest(
            slug='hand_crank', x=0, y=2,
            title='A hand crank',
            subtitle='Slow, and needs nothing',
            icon=item_icon('create:hand_crank'), shape=LESSON, size=0.9,
            deps=['shafts'],
            text=[
                'Three planks and an alloy make a crank. Hold use on it to spin whatever it is attached to.',
                'Good for testing a machine before you build a real source.',
            ],
            tasks=[item('create:hand_crank', 1)],
            rewards=[give('minecraft:oak_planks', 8)],
        ),
        Quest(
            slug='water_wheel', x=3, y=2,
            title='A water wheel',
            subtitle='A stream does the work',
            icon=item_icon('create:water_wheel'), shape=GOAL,
            deps=['shafts'],
            text=[
                'Eight planks round a shaft. Set it so moving water pushes its paddles.',
                'Eight planks round a water wheel make a large one, which gives more.',
            ],
            tasks=[advancement('create:water_wheel')],
            rewards=[give('create:shaft', 4)],
        ),
        Quest(
            slug='windmill', x=6, y=2,
            title='A windmill',
            subtitle='Sails catch the wind',
            icon=item_icon('create:windmill_bearing'), shape=GOAL,
            deps=['shafts'],
            text=[
                'A windmill bearing is a slab, a stone and a shaft. It turns whatever structure you attach to its front.',
                'A wool, two sticks and an alloy make two sails. The more sail blocks it carries, the faster it turns.',
            ],
            tasks=[advancement('create:windmill')],
            rewards=[give('minecraft:white_wool', 8)],
        ),

        # ---- What it costs ----------------------------------------------------------------------
        Quest(
            slug='stress', x=3, y=4,
            title='Stress',
            subtitle='Every machine has a price',
            icon=item_icon('create:stressometer'), shape=GOAL,
            deps=['water_wheel', 'windmill'],
            text=[
                'Each machine costs stress, and each power source can give only so much. Ask for more than it has '
                'and the whole network stops.',
                'A speedometer is a compass over a casing; a stressometer is made from one. Goggles let you read them.',
            ],
            tasks=[advancement('create:stressometer')],
            rewards=[give('minecraft:compass', 1)],
        ),
        Quest(
            slug='wrench', x=6, y=4,
            title='Wrench and goggles',
            subtitle='Tune it, and see inside it',
            icon=item_icon('create:wrench'), shape=GOAL,
            deps=['road_up.press'],
            text=[
                'A wrench is three gold sheets, a cogwheel and a stick. Goggles are two glass, a gold sheet and string.',
                'The wrench turns and takes apart machines; goggles show speed, stress and what a block holds.',
            ],
            tasks=[advancement('create:wrench_goggles')],
            rewards=[give('minecraft:gold_ingot', 4)],
        ),

        # ---- What to do with spin ----------------------------------------------------------------
        Quest(
            slug='gearbox', x=0, y=6,
            title='Turning corners',
            subtitle='Gearboxes, clutches, gearshifts',
            icon=item_icon('create:gearbox'), shape=GOAL,
            deps=['stress'],
            text=[
                'A gearbox is four cogwheels round a casing: it sends spin round corners and splits it.',
                'A clutch (casing, shaft, redstone) cuts spin with a signal. A gearshift (casing, cogwheel, redstone) reverses it.',
            ],
            tasks=[item('create:gearbox', 1), item('create:clutch', 1), item('create:gearshift', 1)],
            rewards=[give('minecraft:redstone', 8)],
        ),
        Quest(
            slug='belt', x=3, y=6,
            title='Belts',
            subtitle='Link two shafts, carry what is on top',
            icon=item_icon('create:belt_connector'), shape=GOAL,
            deps=['stress'],
            text=[
                'Six dried kelp make a belt. Use it on two shafts to join them; the belt carries items and anything standing on it.',
            ],
            tasks=[advancement('create:belt')],
            rewards=[give('minecraft:dried_kelp', 12)],
        ),
        Quest(
            slug='steam', x=6, y=6,
            title='Steam',
            subtitle='A boiler, and a lot of power',
            icon=item_icon('create:steam_engine'), shape=GOAL,
            deps=['stress'],
            text=[
                'A steam engine is a gold sheet, an alloy and a copper block. It sits on a fluid tank with water in it.',
                'The tank needs heat under it, and a lit blaze burner does that. Hold W over the engine for the rest.',
            ],
            tasks=[advancement('create:steam_engine')],
            rewards=[give('minecraft:copper_ingot', 8)],
        ),
    ],
)
