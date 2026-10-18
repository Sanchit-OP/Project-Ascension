"""First Days: the Earth page. What a new player does, in the order that helps.

Voice rules (docs/progression/quests.md section 1): say what and where, never why; use only what the player
has already been shown; practical hints, in plain words; no answers. The build fails on a guarded word
(pack/quests/guarded-words.txt).

Shapes: LESSON, GOAL and LANDMARK (tools/quests/model.py).
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, give, item, item_icon,
                          picture)

CHAPTER = Chapter(
    slug='first_days',
    title='First Days',
    subtitle='Earth. Learn the ground you stand on.',
    icon=picture('textures/quests/icons/first_days.png'),
    shape=LESSON,
    quests=[
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
            slug='know_your_numbers', x=2.5, y=0,
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
            slug='titanium', x=2.5, y=2,
            title='Titanium',
            subtitle='The metal for leaving',
            icon=item_icon('ascension_worlds:titanium_ingot'), shape=GOAL,
            deps=['first_iron'],
            text=[
                'Titanium ore lies in the deeper rock. Smelt raw titanium into an ingot (a blast furnace is twice as fast).',
                'Three ingots, an iron plate and a wooden slab make the plain propeller bearing an airship needs.',
            ],
            tasks=[item('ascension_worlds:titanium_ingot', 3)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='first_breath', x=2.5, y=4,
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
            slug='the_valve', x=1.2, y=6,
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
            slug='the_dark_below', x=5, y=3,
            title='The dark below',
            subtitle='A city that is asleep',
            icon=item_icon('minecraft:sculk_catalyst'), shape=LANDMARK, size=1.2,
            deps=['keep_some_gold'], hide_until='complete',
            text=[
                'Far under the world there is a city, and everything in it is asleep.',
                'Go quietly. Bring good armour, food and a way back up.',
            ],
            tasks=[advancement('ascension_combat:growth/discovery/ancient_city')],
            rewards=[give('minecraft:golden_carrot', 8)],
        ),
        Quest(
            slug='a_tendril', x=5, y=5,
            title='A tendril',
            subtitle='What the keeper leaves behind',
            icon=item_icon('ascension_worlds:warden_tendril'), shape=GOAL,
            deps=['the_dark_below'], hide_until='complete',
            text=[
                'What waits in that city leaves a tendril behind when it falls.',
                'Keep it: a gateway will not wake without one.',
            ],
            tasks=[item('ascension_worlds:warden_tendril', 1)],
            rewards=[give('minecraft:echo_shard', 2)],
        ),
        Quest(
            slug='earth_gate', x=5, y=7,
            title="Earth's gate",
            subtitle='A frame of carved stone',
            icon=item_icon('ascension_worlds:inscribed_gatestone'), shape=LANDMARK, size=1.2,
            deps=['a_tendril'], hide_until='complete',
            text=[
                'Build a ring of inscribed gatestone, set a keystone in it, and wake it with a tendril.',
                'When it is lit, everyone on the server gains from it.',
                'A gate on its own has nowhere to lead yet.',
            ],
            tasks=[advancement('ascension_worlds:gates/overworld')],
            rewards=[give('minecraft:diamond', 2)],
        ),
        Quest(
            slug='look_up', x=3.8, y=6,
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
