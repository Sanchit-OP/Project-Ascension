"""Create: Machines. Break things down, mix them, press them, put them together, and move them about.

Same rules as Create: Power: each quest names the part, says what it is made of (read from Create 6.0.10's
recipes) and what to watch for, then leaves the animation to Create's own guide (hold W over the item). Where
Create has a real advancement for the step, the quest waits for it. Nothing gates anything.

Layout: four columns, each one trade: breaking things down (x 0), air (x 3), mixing and pressing (x 6) and
building (x 9); moving things about is the bottom row.
"""

from quests.model import GOAL, Chapter, Quest, advancement, give, item, item_icon, picture

CHAPTER = Chapter(
    slug='create_craft',
    title='Create: Machines',
    subtitle='Grind, blow, mix, press, assemble, move.',
    icon=picture('textures/quests/icons/create_craft.png'),
    shape=GOAL,
    quests=[
        # ---- Breaking things down (x 0) ---------------------------------------------------------
        Quest(
            slug='millstone', x=0, y=0,
            title='A millstone',
            subtitle='Ore to crushed ore, grain to flour',
            icon=item_icon('create:millstone'), shape=GOAL,
            text=[
                'A cogwheel over a casing over a stone. Drop items on top while it turns and they come out ground.',
                'Crushed ore can be smelted, or washed into nuggets at a fan.',
            ],
            tasks=[advancement('create:millstone')],
            rewards=[give('minecraft:raw_iron', 4)],
        ),
        Quest(
            slug='saw', x=0, y=2,
            title='A saw',
            subtitle='Logs to planks, and more',
            icon=item_icon('create:mechanical_saw'), shape=GOAL,
            deps=['millstone'],
            text=[
                'Three iron sheets, an iron ingot and a casing. Stood upright, it cuts whatever you put on it.',
                'Turned to face a tree, it fells it.',
            ],
            tasks=[advancement('create:saw_processing')],
            rewards=[give('minecraft:oak_log', 8)],
        ),
        Quest(
            slug='crushers', x=0, y=4,
            title='Crushing wheels',
            subtitle='A pair, set side by side',
            icon=item_icon('create:crushing_wheel'), shape=GOAL,
            deps=['saw', 'deployer'],
            text=[
                'Crushing wheels are made two at a time at mechanical crafters, so they come after brass.',
                'Items that fall between a spinning pair are ground, faster than a millstone does it.',
            ],
            tasks=[advancement('create:crushing_wheel')],
            rewards=[give('minecraft:raw_copper', 8)],
        ),

        # ---- Air (x 3) ---------------------------------------------------------------------------
        Quest(
            slug='fan', x=3, y=0,
            title='An encased fan',
            subtitle='Moves air, and what rides on it',
            icon=item_icon('create:encased_fan'), shape=GOAL,
            text=[
                'A shaft over a casing over a propeller. It blows air one way when it spins one way, and sucks the other way when reversed.',
            ],
            tasks=[advancement('create:encased_fan')],
            rewards=[give('create:andesite_alloy', 4)],
        ),
        Quest(
            slug='fan_processing', x=3, y=2,
            title='What the air passes through',
            subtitle='Wash, smoke, blast, haunt',
            icon=item_icon('minecraft:lava_bucket'), shape=GOAL,
            deps=['fan'],
            text=[
                'Items carried by a fan are changed by what its air passes through. Water washes, fire smokes, lava blasts and soul fire haunts.',
                'Put the block in front of the fan, with the items between.',
            ],
            tasks=[advancement('create:fan_processing')],
            rewards=[give('minecraft:bucket', 1)],
        ),

        # ---- Mixing and pressing (x 6) -----------------------------------------------------------
        Quest(
            slug='basin', x=6, y=0,
            title='A basin',
            subtitle='A bowl for machines to work on',
            icon=item_icon('create:basin'), shape=GOAL,
            text=[
                'Five andesite alloy in a bowl shape. Machines above it work on what it holds.',
            ],
            tasks=[item('create:basin', 1)],
            rewards=[give('create:andesite_alloy', 5)],
        ),
        Quest(
            slug='mixer', x=6, y=2,
            title='A mixer',
            subtitle='Combine ingredients',
            icon=item_icon('create:mechanical_mixer'), shape=GOAL,
            deps=['basin'],
            text=[
                'A cogwheel over a casing over a whisk. Set it over a basin and turn it.',
                'Some mixes need heat: a lit blaze burner under the basin gives it. The Rot page shows where blazes live.',
            ],
            tasks=[advancement('create:mechanical_mixer')],
            rewards=[give('minecraft:redstone', 8)],
        ),
        Quest(
            slug='compacting', x=6, y=4,
            title='Compacting',
            subtitle='Fewer items from more',
            icon=item_icon('create:mechanical_press'), shape=GOAL,
            deps=['basin', 'road_up.press'],
            text=[
                'Set a mechanical press over a basin and it squeezes what the basin holds into something denser.',
            ],
            tasks=[advancement('create:compacting')],
            rewards=[give('minecraft:iron_ingot', 4)],
        ),

        # ---- Building (x 9) -----------------------------------------------------------------------
        Quest(
            slug='copper', x=9, y=0,
            title='Copper casing',
            subtitle='A log, wrapped in copper',
            icon=item_icon('create:copper_casing'), shape=GOAL,
            deps=['road_up.casing'],
            text=[
                'Use a copper ingot on a stripped log. The spout and the item drain are built on copper casings.',
            ],
            tasks=[advancement('create:copper_casing')],
            rewards=[give('minecraft:copper_ingot', 4)],
        ),
        Quest(
            slug='deployer', x=9, y=2,
            title='A deployer',
            subtitle='Your own hands, on a machine',
            icon=item_icon('create:deployer'), shape=GOAL,
            deps=['the_rot.brass'],
            text=[
                'An electron tube over a casing over a brass hand. It uses items on blocks, places blocks, and presses buttons.',
                'An electron tube is a polished rose quartz over an iron sheet.',
            ],
            tasks=[advancement('create:deployer')],
            rewards=[give('minecraft:gold_ingot', 4)],
        ),
        Quest(
            slug='crafter', x=9, y=4,
            title='Mechanical crafters',
            subtitle='Crafting without hands',
            icon=item_icon('create:mechanical_crafter'), shape=GOAL,
            deps=['deployer'],
            text=[
                'An electron tube, a brass casing and a crafting table make three. Set them in a grid and they craft together.',
            ],
            tasks=[advancement('create:mechanical_crafter')],
            rewards=[give('minecraft:crafting_table', 1)],
        ),
        Quest(
            slug='precision', x=9, y=6,
            title='A precision mechanism',
            subtitle='Built in loops',
            icon=item_icon('create:precision_mechanism'), shape=GOAL,
            deps=['crafter'],
            text=[
                'A gold sheet is passed along a line of machines, round and round, until it becomes a mechanism.',
                'Fine parts like this are made by passing one item through many machines. Hold W over it.',
            ],
            tasks=[advancement('create:precision_mechanism')],
            rewards=[give('minecraft:gold_nugget', 16)],
        ),

        # ---- Moving things about -------------------------------------------------------------------
        Quest(
            slug='chute', x=0, y=8,
            title='Chutes',
            subtitle='Items, straight down',
            icon=item_icon('create:chute'), shape=GOAL,
            deps=['road_up.press'],
            text=[
                'Two iron sheets and an iron ingot make four. Items fall down a chute; a fan under it can blow them up.',
            ],
            tasks=[advancement('create:chute')],
            rewards=[give('minecraft:iron_nugget', 16)],
        ),
        Quest(
            slug='funnel', x=3, y=8,
            title='Funnels',
            subtitle='Into a chest, or out of one',
            icon=item_icon('create:andesite_funnel'), shape=GOAL,
            deps=['chute'],
            text=[
                'An alloy over dried kelp makes two. A funnel takes items out of a container, or puts them in. Hold W over it for how it faces.',
            ],
            tasks=[advancement('create:funnel')],
            rewards=[give('minecraft:dried_kelp', 8)],
        ),
        Quest(
            slug='pipes', x=6, y=8,
            title='Pipes and a pump',
            subtitle='Moving fluid',
            icon=item_icon('create:mechanical_pump'), shape=GOAL,
            deps=['copper'],
            text=[
                'Two copper sheets and a copper ingot make four pipes. A cogwheel and a pipe make a pump, which pushes fluid one way as it spins.',
                'A fluid tank is a barrel between two copper sheets.',
            ],
            tasks=[advancement('create:mechanical_pump_0')],
            rewards=[give('minecraft:copper_ingot', 8)],
        ),
        Quest(
            slug='spout', x=9, y=8,
            title='A spout',
            subtitle='Fill what passes under it',
            icon=item_icon('create:spout'), shape=GOAL,
            deps=['pipes'],
            text=[
                'A copper casing over dried kelp. Set it over a belt or a depot and it fills buckets, bottles and the like from the pipe it is joined to.',
            ],
            tasks=[advancement('create:spout')],
            rewards=[give('minecraft:glass_bottle', 4)],
        ),
    ],
)
