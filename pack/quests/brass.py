"""Brass: fire, metal and a hand that works for you. A burner (a captured blaze's, or one made at home), brass, an electron tube, a
deployer, mechanical crafters and a precision mechanism. Everything after this on the road is built on a crafter.

Project 3 of 9. The burner, the tube and brass by hand are Earth recipes of ascension-compat-create (ADR-0032).
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, Quest, advancement, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='brass',
    title='Brass',
    subtitle='Fire, metal and a hand that works for you.',
    icon=picture('textures/quests/icons/brass.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='burner', legacy='the_rot',
            title='A burner for the basin',
            subtitle='With a blaze, or without one',
            icon=item_icon('create:blaze_burner'), shape=GOAL,
            deps=['spin.press'],
            text=[
                'Heated recipes need a blaze burner under the basin. A captured blaze makes one: use an empty burner on a blaze, in the Nether.',
                'You can make the same burner at home: iron sheets round a magma block, a lava bucket on top. Feed it coal to light it.',
            ],
            tasks=[item('create:blaze_burner', 1)],
            rewards=[give('minecraft:copper_ingot', 8)],
        ),
        Quest(
            slug='brass', legacy='the_rot',
            title='Brass',
            subtitle='Create\'s next tier',
            icon=item_icon('create:brass_ingot'), shape=GOAL,
            deps=['burner', 'spin.mixer'],
            text=[
                'Copper and zinc, mixed over a lit burner, make brass. Three of each make two ingots by hand at a crafting table, dearer than the mixer.',
                'Brass is the upgrade: stronger casings, finer controls, better propellers.',
            ],
            tasks=[advancement('create:brass')],
            rewards=[give('minecraft:quartz', 8)],
        ),
        Quest(
            slug='deployer', legacy='create_craft',
            title='A deployer',
            subtitle='Your own hands, on a machine',
            icon=item_icon('create:deployer'), shape=GOAL,
            deps=['brass', 'tube'],
            text=[
                'An electron tube over a casing over a brass hand. It uses items on blocks, places blocks, and presses buttons.',
                'The brass hand is four brass sheets and an alloy. The electron tube has its own quest.',
            ],
            tasks=[advancement('create:deployer')],
            rewards=[give('minecraft:gold_ingot', 4)],
        ),
        Quest(
            slug='crafter', legacy='create_craft',
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
            slug='crushers', legacy='create_craft',
            title='Crushing wheels',
            subtitle='A pair, set side by side',
            icon=item_icon('create:crushing_wheel'), shape=GOAL,
            deps=['deployer', 'spin.saw'],
            text=[
                'Crushing wheels are made two at a time at mechanical crafters, so they come after brass.',
                'Items that fall between a spinning pair are ground, faster than a millstone does it.',
            ],
            tasks=[advancement('create:crushing_wheel')],
            rewards=[give('minecraft:raw_copper', 8)],
        ),
        Quest(
            slug='precision', legacy='create_craft',
            title='A precision mechanism',
            subtitle='Built in loops',
            icon=item_icon('create:precision_mechanism'), shape=GOAL,
            deps=['crafter', 'crushers'],
            text=[
                'A gold sheet is passed along a line of machines, round and round, until it becomes a mechanism.',
                'Fine parts like this are made by passing one item through many machines. Hold W over it.',
            ],
            tasks=[advancement('ascension_compat_create:project/brass')],
            rewards=[give('minecraft:gold_nugget', 16)],
        ),
        Quest(
            slug='tube',
            title='An electron tube',
            subtitle='A shard of light, bound in iron',
            icon=item_icon('create:electron_tube'), shape=GOAL,
            deps=['spin.press'],
            text=[
                'A deployer, a crafter and every fine control start with an electron tube. Create cuts it from polished rose quartz, which is Nether quartz and redstone.',
                'An amethyst shard from a geode, four redstone and an iron sheet make one just as well, at a crafting table.',
            ],
            tasks=[item('create:electron_tube', 4)],
            rewards=[give('minecraft:redstone', 16)],
        ),
    ],
)

blueprint_page(CHAPTER, number=3, of=9, motif='brass', finale='precision', points=1)
