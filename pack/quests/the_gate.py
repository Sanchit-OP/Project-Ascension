"""Build the Gate: how to raise Earth's ring, in order. Everything here is read from the frame's own data
(gateway.json and the block tags next to it), so the counts and the families are what the game checks.

The picture beside the quests is drawn from that same file (tools/generate-quest-art.py): a view of the ring,
a colour key, and every layer from the ground up. It is always drawn, because it is the whole point of the page.

Voice rules (docs/progression/quests.md section 1) hold: what and where, never why.

Layout: the steps in one column on the left, the picture to the right of them.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Image, Quest, advancement, checkmark, give, item,
                          item_icon, picture)

CHAPTER = Chapter(
    slug='the_gate',
    title='Build the Gate',
    subtitle="Earth's ring, stone by stone.",
    icon=picture('textures/quests/icons/the_gate.png'),
    shape=GOAL,
    images=[
        Image('plan', 'textures/quests/gate/plan.png', 8.6, 5.0, 11, 11 * 600 / 1024, order=0),
    ],
    quests=[
        Quest(
            slug='read_plan', x=0, y=0,
            title='Read the plan',
            subtitle='A view, a key and twenty layers',
            icon=item_icon('minecraft:paper'), shape=LESSON, size=0.9,
            text=[
                'The picture beside this shows the whole ring from the front, then every layer from the ground up.',
                'The ring is 15 wide, 7 deep and 20 high: 418 blocks, with an opening 5 wide and 7 high that you walk through.',
                'Each colour is a family of blocks. Any block in a family may stand in for another.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:paper', 8)],
        ),
        Quest(
            slug='ground', x=0, y=2,
            title='Level ground',
            subtitle='Room for the ring to stand',
            icon=item_icon('minecraft:grass_block'), shape=LESSON, size=0.9,
            deps=['read_plan'],
            text=[
                'Pick open, flat ground with room for 15 by 7 blocks and twenty of clear air above.',
                'The ring stands upright. The opening is in its middle, three layers up from the ground.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='stone', x=0, y=4,
            title='The stone',
            subtitle='Five families, 389 blocks',
            icon=item_icon('minecraft:deepslate_bricks'), shape=GOAL,
            deps=['ground'],
            text=[
                'Dark, 189: deepslate bricks or tiles, polished deepslate or blackstone, smooth basalt.',
                'Pale, 72: quartz blocks or bricks, smooth quartz, calcite, polished diorite. A gold block works too.',
                'Pillars, 46: quartz pillar, bone block or polished basalt.',
                'Accents, 26: chiselled stone bricks, quartz or deepslate, or a gold block.',
                'Steps, 56: slabs of deepslate, polished deepslate or polished blackstone.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:deepslate_bricks', 32)],
        ),
        Quest(
            slug='gatestones', x=-1.5, y=6,
            title='Gatestones',
            subtitle='Twenty-eight, carved',
            icon=item_icon('ascension_worlds:inscribed_gatestone'), shape=GOAL,
            deps=['stone'],
            text=[
                'Eight polished deepslate and a piece of sculk make eight gatestones. Take sculk with Silk Touch.',
                'They go round the keystone, down both sides of the opening and along the top.',
            ],
            tasks=[item('ascension_worlds:inscribed_gatestone', 28)],
            rewards=[give('minecraft:polished_deepslate', 16)],
        ),
        Quest(
            slug='keystone', x=1.5, y=6,
            title='The keystone',
            subtitle='One block wakes the ring',
            icon=item_icon('ascension_worlds:gateway_keystone'), shape=GOAL,
            deps=['stone'],
            text=[
                'Four diamonds, two echo shards, two sculk catalysts and a compass make the keystone.',
                'It sits in the third layer from the ground, at the very centre of the ring.',
            ],
            tasks=[item('ascension_worlds:gateway_keystone', 1)],
            rewards=[give('minecraft:echo_shard', 1)],
        ),
        Quest(
            slug='raise', x=0, y=8,
            title='Raise the ring',
            subtitle='From the ground up',
            icon=item_icon('minecraft:stone_brick_stairs'), shape=GOAL,
            deps=['gatestones', 'keystone'],
            text=[
                'Build from the bottom layer to the top, as the plan shows. Mix the stones of a family as you like.',
                'Leave the opening empty. The ring will not light if a block is wrong.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:bone_block', 8)],
        ),
        Quest(
            slug='wake', x=0, y=10,
            title='Wake it',
            subtitle='A tendril on the keystone',
            icon=item_icon('ascension_worlds:warden_tendril'), shape=LANDMARK, size=1.2,
            deps=['raise'],
            text=[
                'Use a Warden tendril on the keystone. When the opening fills, the ring is lit.',
                'Everyone on the server gains skill points from it. A gate on its own leads nowhere yet: see the Planets page.',
            ],
            tasks=[advancement('ascension_worlds:gates/overworld')],
            rewards=[give('minecraft:golden_apple', 2)],
        ),
    ],
)
