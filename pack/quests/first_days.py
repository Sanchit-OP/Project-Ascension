"""First Days: the Earth page. What a new player does, in the order that helps.

Voice rules (docs/progression/quests.md section 1): say what and where, never why; use only what the player
has already been shown; practical hints, in plain words; no answers. The build fails on a guarded word
(pack/quests/guarded-words.txt).

Shapes: LESSON, GOAL and LANDMARK (tools/quests/model.py).

Layout: two tracks. The left one (x 0) is the way down to the Warden and the gate; the right one (x 3) is
iron, diamonds, titanium and air, which the gate and the ship both need.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Image, Quest, advancement, checkmark, give, item,
                          item_icon, picture)

CHAPTER = Chapter(
    slug='first_days',
    title='First Days',
    subtitle='Earth. Learn the ground you stand on.',
    icon=picture('textures/quests/icons/first_days.png'),
    shape=LESSON,
    # The plan of Earth's gate, drawn from the frame's own data (tools/generate-quest-art.py). It sits beside the gate
    # quests and is always drawn: a plan nobody can find teaches nothing (the first version hid it until the dark city
    # was found and put it far to the right; Sanchit could not find it). The Build the Gate page has it too.
    images=[
        Image('gate_plan', 'textures/quests/gate/plan.png', 7, 14, 9, 9 * 600 / 1024, order=0),
    ],
    quests=[
        # ---- The ledger itself, and the basics -------------------------------------------------
        Quest(
            slug='open_ledger', x=0, y=0,
            title='Open the Ledger',
            subtitle='It keeps count, and it points',
            icon=item_icon('minecraft:book'), shape=LESSON, size=0.9,
            text=[
                'This book remembers what you do and points at what to do next.',
                'It will say where to look. It will not say why.',
                'Open it any time with J. Press the button below to carry on.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:bread', 4)],
        ),
        Quest(
            slug='know_your_numbers', x=3, y=0,
            title='Know your numbers',
            subtitle='Everything you carry has them',
            icon=item_icon('minecraft:iron_sword'), shape=LESSON, size=0.9,
            deps=['open_ledger'],
            text=[
                'What you wear and hold changes your numbers: damage, armour, speed and more.',
                'Open your character sheet (H by default) to see each number and where it comes from.',
                'Growth (K) and runes (R) feed the same sheet.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='no_portals', x=5.5, y=0,
            title='Dead doors',
            subtitle='Obsidian will not open them',
            icon=item_icon('minecraft:obsidian'), shape=LESSON, size=0.9,
            deps=['know_your_numbers'],
            text=[
                'The old portals are dead. Lighting an obsidian frame does nothing here.',
                'Other worlds are reached by air, and later by rings you wake yourself.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_porkchop', 6)],
        ),
        Quest(
            slug='first_iron', x=0, y=2,
            title='First iron',
            subtitle='Nothing starts without it',
            icon=item_icon('minecraft:iron_ingot'), shape=LESSON, size=0.9,
            deps=['open_ledger'],
            text=[
                'Smelt your first iron. Almost everything after this is built on it.',
            ],
            tasks=[advancement('minecraft:story/smelt_iron')],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='diamonds', x=3, y=2,
            title='Diamonds',
            subtitle='Deep, and worth the dark',
            icon=item_icon('minecraft:diamond'), shape=GOAL,
            deps=['first_iron'],
            text=[
                'Titanium needs a diamond pickaxe, and the first gateway wants a handful more.',
                'Look near the bottom of the world. Take light, food and a bucket of water.',
            ],
            tasks=[advancement('minecraft:story/mine_diamond')],
            rewards=[give('minecraft:torch', 32)],
        ),

        # ---- The left track: gold, the dark, the gate -----------------------------------------
        Quest(
            slug='keep_some_gold', x=0, y=4,
            title='Keep some gold',
            subtitle='It matters more than it looks',
            icon=item_icon('minecraft:gold_ingot'), shape=GOAL,
            deps=['first_iron'],
            text=[
                'Gather four gold ingots and keep them on you, not left in a chest.',
                'You will be glad of them before long. Where, you will see for yourself.',
            ],
            tasks=[item('minecraft:gold_ingot', 4)],
            rewards=[give('minecraft:golden_apple', 1)],
        ),
        Quest(
            slug='the_dark_below', x=0, y=6,
            title='The dark below',
            subtitle='A city that is asleep',
            icon=item_icon('minecraft:sculk_catalyst'), shape=LANDMARK, size=1.2,
            deps=['keep_some_gold'], hide_until='complete',
            text=[
                'Far under the world there is a city, and everything in it is asleep.',
                'It lies inside the deep dark: caves lined with sculk, near the bottom of the world.',
                'Go quietly. Sneak, and put wool under your feet where you must cross open floor.',
                'Bring good armour, food and a way back up.',
            ],
            tasks=[advancement('ascension_combat:growth/discovery/ancient_city')],
            rewards=[give('minecraft:golden_carrot', 8)],
        ),
        Quest(
            slug='the_warden', x=0, y=8,
            title='The Warden',
            subtitle='It hears, and nothing else',
            icon=item_icon('minecraft:sculk_shrieker'), shape=LANDMARK, size=1.2,
            deps=['the_dark_below'], hide_until='complete',
            text=[
                'It cannot see you. It hears your steps, your digging and your fall.',
                'Wear what you can afford to lose, and know where the way up is.',
                'The first time your team beats it, everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_combat:growth/warden')],
            rewards=[give('minecraft:golden_apple', 2)],
        ),
        Quest(
            slug='a_tendril', x=0, y=10,
            title='A tendril',
            subtitle='A gate will want one',
            icon=item_icon('ascension_worlds:warden_tendril'), shape=GOAL,
            deps=['the_warden'], hide_until='complete',
            text=[
                'The Warden leaves a tendril behind when it falls, if a player struck the blow.',
                'Keep it: a gateway will not wake without one.',
            ],
            tasks=[item('ascension_worlds:warden_tendril', 1)],
            rewards=[give('minecraft:echo_shard', 2)],
        ),
        Quest(
            slug='gatestone', x=-1.5, y=12,
            title='Gatestone',
            subtitle='Carved, and veined with sculk',
            icon=item_icon('ascension_worlds:inscribed_gatestone'), shape=GOAL,
            deps=['a_tendril'], hide_until='complete',
            text=[
                'Eight polished deepslate and one piece of sculk make eight gatestones.',
                'The frame takes twenty-eight. Take sculk from the dark city with Silk Touch.',
            ],
            tasks=[item('ascension_worlds:inscribed_gatestone', 28)],
            rewards=[give('minecraft:deepslate_bricks', 32)],
        ),
        Quest(
            slug='keystone', x=1.5, y=12,
            title='The keystone',
            subtitle='Four diamonds and a compass',
            icon=item_icon('ascension_worlds:gateway_keystone'), shape=GOAL,
            deps=['a_tendril'], hide_until='complete',
            text=[
                'Diamonds at the corners, echo shards above and below, sculk catalysts at the sides, '
                'a compass in the middle.',
                'It goes in the base of the ring.',
            ],
            tasks=[item('ascension_worlds:gateway_keystone', 1)],
            rewards=[give('minecraft:bone_block', 16)],
        ),
        Quest(
            slug='earth_gate', x=0, y=14,
            title="Earth's gate",
            subtitle='A frame of carved stone',
            icon=item_icon('ascension_worlds:inscribed_gatestone'), shape=LANDMARK, size=1.2,
            deps=['gatestone', 'keystone'], hide_until='complete',
            text=[
                'Build the ring on level ground from the plan beside these quests, and set the keystone in its base. The Build the Gate page walks through it.',
                'Use a tendril on the keystone to wake it. When it is lit, everyone on the server gains from it.',
                'A gate on its own has nowhere to lead yet. Look at the Planets page.',
            ],
            tasks=[advancement('ascension_worlds:gates/overworld')],
            rewards=[give('minecraft:diamond', 2), give('minecraft:spyglass', 1)],
        ),

        # ---- The right track: titanium and air -------------------------------------------------
        Quest(
            slug='titanium', x=3, y=4,
            title='Titanium',
            subtitle='The metal for leaving',
            icon=item_icon('ascension_worlds:titanium_ingot'), shape=GOAL,
            deps=['diamonds'],
            text=[
                'Titanium ore lies in the deepest rock and needs a diamond pickaxe. '
                'Smelt the ore or the raw titanium it drops; a blast furnace is twice as fast.',
                'Three ingots, an iron sheet and a wooden slab make the plain propeller bearing an airship needs.',
            ],
            tasks=[item('ascension_worlds:titanium_ingot', 3)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='first_breath', x=3, y=6,
            title='A breath of your own',
            subtitle='Where the air runs out',
            icon=item_icon('ascension_atmosphere:oxygen_tank'), shape=GOAL,
            deps=['titanium'],
            text=[
                'Some places have no air. Craft an oxygen tank and you carry your own.',
                'Your lungs hold about twenty seconds and refill wherever the air is good. A tank is drained first.',
            ],
            tasks=[item('ascension_atmosphere:oxygen_tank', 1)],
            rewards=[give('minecraft:iron_ingot', 4), give('minecraft:coal', 16)],
        ),
        Quest(
            slug='the_valve', x=1.8, y=8,
            title='The valve',
            subtitle='A tank only helps when it is open',
            icon=item_icon('minecraft:lever'), shape=LESSON, size=0.9,
            deps=['first_breath'],
            text=[
                'Use the tank to open its valve. It takes three seconds to pressurise, and you can carry two.',
                'Use it on another player to share your air with them.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='look_up', x=4.2, y=8,
            title='Look up',
            subtitle='The Planets page has changed',
            icon=item_icon('minecraft:spyglass'), shape=LESSON, size=0.9,
            deps=['first_breath'], hide_until='complete',
            text=[
                'Open the Planets page. A world you had only heard of now has a name.',
                'The page also shows where you are, on the ground and between the worlds.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
    ],
)
