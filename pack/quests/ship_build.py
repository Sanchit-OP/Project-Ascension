"""Aeronautics: A Ship. How a pile of blocks becomes something that floats and moves.

The parts are Simulated's and Aeronautics', not ours. Each quest names a part, says what it is made of (read from
those mods' recipes) and what the mods' own guides say about it (their Ponder text), and leaves the animation to the
game (hold W over the item). Where the mods keep a real advancement for the step, the quest waits for that.
Nothing gates anything. The Road Up page is the short way; this page is the long one.

Layout: a trunk down the middle (x 4.5) and four columns: balloons (x 1.5), propellers and an engine (x 4.5),
steering (x 7.5) and the small parts that make a ship yours (x 10.5).
"""

from quests.model import (GOAL, LESSON, Chapter, Quest, advancement, checkmark, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='ship_build',
    title='Aeronautics: A Ship',
    subtitle='From a pile of blocks to something that flies.',
    icon=picture('textures/quests/icons/ship_build.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='contraption', x=4.5, y=0,
            title='A ship is a contraption',
            subtitle='Glued blocks, one moving body',
            icon=item_icon('simulated:physics_assembler'), shape=GOAL,
            text=[
                'A physics assembler is a lever on a casing between two alloy. It turns a glued group of blocks into one moving body.',
                'Glue what should move, then hold use on the assembler and pull its lever. Any assembler can take the ship apart again.',
            ],
            tasks=[advancement('simulated:applied_kinematics')],
            rewards=[give('create:andesite_alloy', 4)],
        ),

        # ---- The hull -----------------------------------------------------------------------------
        Quest(
            slug='glue', x=1.5, y=2,
            title='Glue',
            subtitle='What is glued moves together',
            icon=item_icon('create:super_glue'), shape=GOAL,
            deps=['contraption'],
            text=[
                'Two slimeballs, an iron sheet and an iron nugget make super glue. Click two corners to glue the blocks between them.',
                'Honey glue (an iron sheet filled with honey at a spout) is the kind made for ships: it joins only ships.',
            ],
            tasks=[item('create:super_glue', 1)],
            rewards=[give('minecraft:slime_ball', 4)],
        ),
        Quest(
            slug='weight', x=7.5, y=2,
            title='Weight',
            subtitle='Lift and thrust must beat it',
            icon=item_icon('minecraft:anvil'), shape=LESSON, size=0.9,
            deps=['contraption'],
            text=[
                'Every block has weight. A ship rises only when its lift and thrust beat the total, so build light where you can.',
                'A few blocks are light, super heavy, slippery or fragile. Hold W over the assembler for the list.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:feather', 8)],
        ),

        # ---- Lift: a balloon (x 1.5) -------------------------------------------------------------
        Quest(
            slug='balloon', x=1.5, y=4,
            title='A balloon',
            subtitle='A shell that holds hot air',
            icon=item_icon('aeronautics:white_envelope'), shape=GOAL,
            deps=['contraption'],
            text=[
                'Two wool and two sticks make four envelope blocks. Enclose a space with them: hot air must be held by airtight blocks.',
                'Encase a shaft with an envelope block to pass it through the wall. Blocks inside use up room, and dye paints them.',
            ],
            tasks=[item('aeronautics:white_envelope', 16)],
            rewards=[give('minecraft:white_dye', 4)],
        ),
        Quest(
            slug='burner', x=1.5, y=6,
            title='The hot air burner',
            subtitle='It fills the balloon above it',
            icon=item_icon('aeronautics:adjustable_burner'), shape=GOAL,
            deps=['balloon'],
            text=[
                'Four iron sheets, two alloy, redstone and a coal block. Power it with redstone and it fills the balloon above.',
                'A panel sets the most hot air it may give, and the signal strength scales that. Several burners can share a balloon.',
            ],
            tasks=[advancement('aeronautics:head_in_the_clouds')],
            rewards=[give('minecraft:redstone', 8)],
        ),

        # ---- Thrust: propellers and an engine (x 4.5) ---------------------------------------------
        Quest(
            slug='propellers', x=4.5, y=4,
            title='Propellers',
            subtitle='Sails on a bearing',
            icon=item_icon('aeronautics:andesite_propeller'), shape=GOAL,
            deps=['contraption', 'road_up.bearing'],
            text=[
                'A propeller bearing attaches to the block in front of it. Anything with at least two sail-like blocks counts as a propeller; more sails push harder.',
                'Reversing the spin, or using a wrench, reverses the push. A propeller moving through air pushes less.',
            ],
            tasks=[advancement('aeronautics:in_thrust_we_trust')],
            rewards=[give('create:shaft', 4)],
        ),
        Quest(
            slug='engine', x=4.5, y=6,
            title='A portable engine',
            subtitle='The ship carries its own power',
            icon=item_icon('simulated:red_portable_engine'), shape=GOAL,
            deps=['propellers'],
            text=[
                'A blast furnace, an engine assembly and an iron sheet make one. It burns fuel to turn a shaft, wherever it is.',
                'Fuel goes in by hand or by machine. A blaze cake superheats it for more power.',
            ],
            tasks=[advancement('simulated:steamless_engine')],
            rewards=[give('minecraft:coal', 16)],
        ),

        # ---- Steering (x 7.5) ---------------------------------------------------------------------
        Quest(
            slug='wheel', x=7.5, y=4,
            title='The steering wheel',
            subtitle='Grab it and turn',
            icon=item_icon('simulated:steering_wheel'), shape=GOAL,
            deps=['contraption'],
            text=[
                'A large cogwheel over a casing over a shaft. Hold use to grab it and move the mouse to turn it; sneaking snaps it to 45 degrees.',
                'A bearing driven by the wheel must be told not to take the ship apart when it turns.',
            ],
            tasks=[advancement('simulated:unpowered_steering')],
            rewards=[give('minecraft:oak_planks', 8)],
        ),
        Quest(
            slug='swivel', x=7.5, y=6,
            title='A swivel bearing',
            subtitle='A part of the ship that turns',
            icon=item_icon('simulated:swivel_bearing'), shape=GOAL,
            deps=['wheel'],
            text=[
                'A slab over an industrial iron block over a cogwheel. It attaches to the block in front and, when powered through the cog, assembles the glued blocks there.',
                'Spin passes straight through along its shaft, so the turning part can be driven from the hull.',
            ],
            tasks=[advancement('simulated:you_spin_me_right_round')],
            rewards=[give('create:cogwheel', 2)],
        ),

        # ---- Small parts (x 10.5) ------------------------------------------------------------------
        Quest(
            slug='handle', x=10.5, y=4,
            title='Something to hold',
            subtitle='Handles keep you on board',
            icon=item_icon('simulated:iron_handle'), shape=GOAL,
            deps=['contraption'],
            text=[
                'An iron nugget over an alloy makes a handle. Grab one and you stay put while the ship moves, and a fall can be broken by catching one.',
            ],
            tasks=[advancement('simulated:get_a_grip')],
            rewards=[give('minecraft:iron_nugget', 8)],
        ),
        Quest(
            slug='nameplate', x=10.5, y=6,
            title='A name',
            subtitle='Every ship has one',
            icon=item_icon('simulated:white_nameplate'), shape=LESSON, size=0.9,
            deps=['handle'],
            text=[
                'Paper, a stick and an alloy make four nameplates. Name your ship with one.',
            ],
            tasks=[advancement('simulated:i_declare_thee')],
            rewards=[give('minecraft:paper', 8)],
        ),
    ],
)
