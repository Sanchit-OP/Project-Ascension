"""Spin: make the world do the turning. Create from nothing: andesite alloy and casings, power from a hand, water or wind, stress, a press, a
mixer, a fan, belts and a spout. The finale is a working line, which is the first thing a base does by itself.

Project 2 of 9 (docs/progression/dr-stone-roadmap.md). Quests moved here from The Road Up, Create: Power and Create: Machines keep their ids.
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, LANDMARK, LESSON, Quest, advancement, checkmark, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='spin',
    title='Spin',
    subtitle='Make the world do the turning.',
    icon=picture('textures/quests/icons/spin.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='manual', legacy='road_up',
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
            slug='alloy', legacy='road_up',
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
            slug='casing', legacy='road_up',
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
            slug='power', legacy='road_up',
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
            slug='press', legacy='road_up',
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
        Quest(
            slug='shafts', legacy='create_power',
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
        Quest(
            slug='hand_crank', legacy='create_power', optional=True,
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
            slug='water_wheel', legacy='create_power',
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
            slug='windmill', legacy='create_power',
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
        Quest(
            slug='stress', legacy='create_power',
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
            slug='wrench', legacy='create_power', optional=True,
            title='Wrench and goggles',
            subtitle='Tune it, and see inside it',
            icon=item_icon('create:wrench'), shape=GOAL,
            deps=['press'],
            text=[
                'A wrench is three gold sheets, a cogwheel and a stick. Goggles are two glass, a gold sheet and string.',
                'The wrench turns and takes apart machines; goggles show speed, stress and what a block holds.',
            ],
            tasks=[advancement('create:wrench_goggles')],
            rewards=[give('minecraft:gold_ingot', 4)],
        ),
        Quest(
            slug='gearbox', legacy='create_power', optional=True,
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
            slug='belt', legacy='create_power',
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
            slug='steam', legacy='create_power',
            title='Steam',
            subtitle='A boiler, and a lot of power',
            icon=item_icon('create:steam_engine'), shape=GOAL,
            deps=['stress'],
            text=[
                'A steam engine is a gold sheet, an alloy and a copper block. It sits on a fluid tank with water in it.',
                'The tank needs heat under it, and a lit burner does that (the Brass page gives you three). Hold W over the engine for the rest.',
            ],
            tasks=[advancement('create:steam_engine')],
            rewards=[give('minecraft:copper_ingot', 8)],
        ),
        Quest(
            slug='millstone', legacy='create_craft', optional=True,
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
            slug='saw', legacy='create_craft', optional=True,
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
            slug='fan', legacy='create_craft',
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
            slug='fan_processing', legacy='create_craft',
            title='What the air passes through',
            subtitle='Wash, smoke, blast, haunt',
            icon=item_icon('minecraft:lava_bucket'), shape=GOAL,
            deps=['fan'],
            text=[
                'Items carried by a fan are changed by what its air passes through. Water washes, fire smokes, lava blasts and soul fire haunts.',
                'Put the block in front of the fan, with the items between.',
                'Soul fire burns only on soul sand or soul soil, and Earth has little of either, so here is one block to light it on.',
            ],
            tasks=[advancement('create:fan_processing')],
            rewards=[give('minecraft:bucket', 1), give('minecraft:soul_sand', 1)],
        ),
        Quest(
            slug='basin', legacy='create_craft',
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
            slug='mixer', legacy='create_craft',
            title='A mixer',
            subtitle='Combine ingredients',
            icon=item_icon('create:mechanical_mixer'), shape=GOAL,
            deps=['basin'],
            text=[
                'A cogwheel over a casing over a whisk. Set it over a basin and turn it.',
                'Some mixes need heat: a lit burner under the basin gives it. The Brass page gives you three.',
            ],
            tasks=[advancement('create:mechanical_mixer')],
            rewards=[give('minecraft:redstone', 8)],
        ),
        Quest(
            slug='compacting', legacy='create_craft',
            title='Compacting',
            subtitle='Fewer items from more',
            icon=item_icon('create:mechanical_press'), shape=GOAL,
            deps=['basin', 'press'],
            text=[
                'Set a mechanical press over a basin and it squeezes what the basin holds into something denser.',
            ],
            tasks=[advancement('create:compacting')],
            rewards=[give('minecraft:iron_ingot', 4)],
        ),
        Quest(
            slug='copper', legacy='create_craft',
            title='Copper casing',
            subtitle='A log, wrapped in copper',
            icon=item_icon('create:copper_casing'), shape=GOAL,
            deps=['casing'],
            text=[
                'Use a copper ingot on a stripped log. The spout and the item drain are built on copper casings.',
            ],
            tasks=[advancement('create:copper_casing')],
            rewards=[give('minecraft:copper_ingot', 4)],
        ),
        Quest(
            slug='chute', legacy='create_craft',
            title='Chutes',
            subtitle='Items, straight down',
            icon=item_icon('create:chute'), shape=GOAL,
            deps=['press'],
            text=[
                'Two iron sheets and an iron ingot make four. Items fall down a chute; a fan under it can blow them up.',
            ],
            tasks=[advancement('create:chute')],
            rewards=[give('minecraft:iron_nugget', 16)],
        ),
        Quest(
            slug='funnel', legacy='create_craft',
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
            slug='pipes', legacy='create_craft',
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
            slug='spout', legacy='create_craft',
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
        Quest(
            slug='line',
            title='A working line',
            subtitle='Press, mixer, fan, belt and spout',
            icon=item_icon('create:mechanical_press'), shape=LANDMARK, size=1.5,
            deps=['belt', 'steam', 'press', 'mixer', 'compacting', 'fan_processing', 'funnel', 'spout'],
            text=[
                'A press that flattens, a mixer that stirs, a fan that washes and smokes, a belt that carries, a spout that fills.',
                'With all of them turning off one shaft, a base builds itself while you are away.',
            ],
            tasks=[advancement('ascension_compat_create:project/spin')],
            rewards=[give('minecraft:iron_block', 2), give('create:andesite_casing', 8)],
        ),
    ],
)

blueprint_page(CHAPTER, number=2, of=9, motif='spin', finale='line', points=1)
