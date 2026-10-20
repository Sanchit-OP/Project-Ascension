"""The Belt: the End, as a fourth world. A long flight, no floor, the Dragon, and the gate her portal becomes.

Everything here is a rule of the world that the game states or the player can see for themselves: how far it is, what
the ground does, what the Dragon is made of. Nothing says why.

Layout: the flight and the landing across the top, then three columns: the small thing worth knowing on the left, the
Dragon and the gate in the middle, the outer islands on the right.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, give, item, item_icon,
                          picture)

CHAPTER = Chapter(
    slug='the_belt',
    title='The Belt',
    subtitle='Thirty-two thousand blocks south. No floor.',
    icon=picture('textures/quests/icons/the_belt.png'),
    shape=GOAL,
    quests=[
        # ---- The flight and the landing -------------------------------------------------------
        Quest(
            slug='belt_flight', x=3, y=0,
            title='The longest flight',
            subtitle='Thirty-two thousand blocks, south',
            icon=item_icon('minecraft:clock'), shape=LESSON, size=0.9,
            deps=[],
            text=[
                'The End is the farthest world yet, about thirty-two thousand blocks south of Earth. Its air is fine.',
                'Plan the trip in fuel and food.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 16)],
        ),
        Quest(
            slug='belt_landing', x=3, y=2,
            title='Landing on the Belt',
            subtitle='Islands of pale stone, hung in the dark',
            icon=item_icon('minecraft:end_stone'), shape=LANDMARK, size=1.2,
            deps=['belt_flight'],
            text=[
                'A ship comes down over empty air, a little way from the middle. Fly in toward the bright island.',
                'Landing here earns a skill point, once.',
            ],
            tasks=[advancement('ascension_worlds:discovery/the_end')],
            rewards=[give('minecraft:ender_pearl', 4)],
        ),

        # ---- Small things --------------------------------------------------------------------
        Quest(
            slug='no_floor', x=0, y=4,
            title='No floor',
            subtitle='Falling out leads back to space',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            deps=['belt_landing'], hide_until='complete',
            text=[
                'Step off an island and you drift down slowly, taking no harm. Past the bottom of the world you are '
                'carried back into space beside the planet, with nothing lost.',
                'Wings are the way to cross the gaps.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:feather', 8)],
        ),

        # ---- The Dragon and the gate ------------------------------------------------------------
        Quest(
            slug='dragon', x=3, y=4,
            title='The Ender Dragon',
            subtitle='Ten pillars, each with a crystal',
            icon=item_icon('minecraft:dragon_head'), shape=LANDMARK, size=1.2,
            deps=['belt_landing'], hide_until='visible',
            text=[
                'The crystals on the pillars heal her. Break them first.',
                'Her head takes a full hit and the rest of her takes a quarter. The first time your team beats her, '
                'everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_combat:growth/ender_dragon')],
            rewards=[give('minecraft:golden_apple', 4)],
        ),
        Quest(
            slug='belt_gate', x=3, y=6,
            title='The way out',
            subtitle='Her portal becomes a gate',
            icon=item_icon('ascension_worlds:gateway_keystone'), shape=LANDMARK, size=1.2,
            deps=['dragon'], hide_until='complete',
            text=[
                'When she falls, the portal in the middle of the arena wakes. Step into it and pick where to go.',
                'When it lights, everyone on the server gains skill points. It joins the gates you have already lit.',
            ],
            tasks=[advancement('ascension_worlds:gates/the_end')],
            rewards=[give('minecraft:diamond', 3)],
        ),

        # ---- The outer islands ---------------------------------------------------------------------
        Quest(
            slug='belt_gateway', x=6, y=4,
            title='A small gateway',
            subtitle='A short road to the outer islands',
            icon=item_icon('minecraft:ender_pearl'), shape=GOAL,
            deps=['dragon'], hide_until='visible',
            text=[
                'Each time she falls, a small gateway opens on a ring around the arena. Throw an ender pearl at one '
                'and it carries you a long way out.',
            ],
            tasks=[advancement('minecraft:end/enter_end_gateway')],
            rewards=[give('minecraft:ender_pearl', 4)],
        ),
        Quest(
            slug='belt_city', x=6, y=6,
            title='An End city',
            subtitle='Tall towers on the outer islands',
            icon=item_icon('minecraft:purpur_block'), shape=LANDMARK, size=1.2,
            deps=['belt_gateway'], hide_until='complete',
            text=[
                'Shulkers guard the towers and their chests. A shulker shell makes a shulker box.',
            ],
            tasks=[advancement('minecraft:end/find_end_city')],
            rewards=[give('minecraft:chorus_fruit', 4)],
        ),
        Quest(
            slug='belt_wings', x=6, y=8,
            title='Wings',
            subtitle='In a ship, in an item frame',
            icon=item_icon('minecraft:elytra'), shape=GOAL,
            deps=['belt_city'], hide_until='complete',
            text=[
                'Some cities have a ship moored beside them. The wings are in its frame.',
                'They do not carry you into space. The sky still ends at the top of the world.',
            ],
            tasks=[advancement('minecraft:end/elytra')],
            rewards=[give('minecraft:firework_rocket', 16)],
        ),
        Quest(
            slug='shells', x=9, y=6,
            title='Shulker shells',
            subtitle='Boxes that keep what is inside',
            icon=item_icon('minecraft:shulker_shell'), shape=GOAL,
            deps=['belt_city'], hide_until='complete', optional=True,
            text=[
                'Two shells and a chest make a shulker box.',
            ],
            tasks=[item('minecraft:shulker_shell', 2)],
            rewards=[give('minecraft:chorus_fruit', 8)],
        ),
    ],
)
