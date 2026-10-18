"""The Moon: landing, living without air, the Archive, the fortress and its two guardians, and the first ring.

Where things stand is stated as coordinates read from the structure placement files (the Archive at
-160, -256; the ring at -160, 0; the fortress at 800, 0; the Moon's middle is 0, 0). The page says what and
where, never why: the tablets in the Archive speak for themselves.

Layout: three columns. The left follows the Archive, the middle the fortress and the ring (the main line, down
to the shortcut and the next world), the right the small things worth knowing before wandering off.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, give, item, item_icon,
                          picture, structure)

CHAPTER = Chapter(
    slug='the_moon',
    title='The Moon',
    subtitle='Four thousand blocks east, and no air.',
    icon=picture('textures/quests/icons/the_moon.png'),
    shape=GOAL,
    quests=[
        # ---- Before, and the landing ------------------------------------------------------------
        Quest(
            slug='pack', x=3, y=0,
            title='Before you go',
            subtitle='Pack for a world with no air',
            icon=item_icon('ascension_atmosphere:oxygen_tank'), shape=GOAL,
            deps=[],
            text=[
                'Two tanks, full. Food, a pickaxe and light. Spare iron for more tanks.',
                'Fly east from Earth. When your ship nears the Moon it is carried down to the surface.',
            ],
            tasks=[item('ascension_atmosphere:oxygen_tank', 2)],
            rewards=[give('minecraft:cooked_beef', 16)],
        ),
        Quest(
            slug='touchdown', x=3, y=2,
            title='Touchdown',
            subtitle='The first step is a long one',
            icon=item_icon('minecraft:end_stone'), shape=LANDMARK, size=1.2,
            deps=['pack'],
            text=[
                'Open a tank\'s valve before anything else. It takes three seconds to pressurise.',
                'Landing here earns a skill point, once.',
            ],
            tasks=[advancement('ascension_worlds:discovery/moon')],
            rewards=[give('minecraft:torch', 16)],
        ),

        # ---- Three lessons, once you are down -------------------------------------------------
        Quest(
            slug='air', x=0, y=4,
            title='No air',
            subtitle='Lungs, tanks and a room',
            icon=item_icon('ascension_atmosphere:oxygen_emitter'), shape=LESSON, size=0.9,
            deps=['touchdown'], hide_until='complete',
            text=[
                'Everything outside a sealed room is airless. Your lungs hold twenty seconds; a tank, five minutes.',
                'Build a room, seal it and put an emitter in it. A refill station inside works here too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='dust', x=3, y=4,
            title='Dust',
            subtitle='When you can see a dozen blocks',
            icon=item_icon('minecraft:gravel'), shape=LESSON, size=0.9,
            deps=['touchdown'], hide_until='complete',
            text=[
                'Dust storms roll through now and then, and sight drops to about a dozen blocks.',
                'They pass in about a minute. Stop, dig in or wait; do not wander blind.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='earthrise', x=6, y=4,
            title='Home in the sky',
            subtitle='It does not move',
            icon=item_icon('minecraft:compass'), shape=LESSON, size=0.9,
            deps=['touchdown'], hide_until='complete',
            text=[
                'Earth hangs in the sky and stays where it is.',
                'Use it to find your way back to where you came down.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),

        # ---- The Archive, on the left ---------------------------------------------------------
        Quest(
            slug='archive', x=0, y=6,
            title='The Lunar Archive',
            subtitle='A hall under the ground',
            icon=item_icon('ascension_worlds:archive_tablet'), shape=LANDMARK, size=1.2,
            deps=['air'], hide_until='visible',
            text=[
                'Under the surface, at about -160, -256, with a tall pylon over it. The Moon\'s middle is 0, 0.',
                'A great hall of things to read. Bring light, and take your time.',
            ],
            tasks=[advancement('ascension_worlds:discovery/lunar_archive')],
            rewards=[give('minecraft:torch', 32)],
        ),
        Quest(
            slug='moon_stone', x=0, y=8,
            title='Moon stone',
            subtitle='It builds rings, and looks the part',
            icon=item_icon('ascension_worlds:anorthosite_bricks'), shape=GOAL,
            deps=['archive'], hide_until='complete',
            text=[
                'Anorthosite and lunar basalt both build gateway frames; every kind of frame block accepts them.',
                'Haul some home: a ring of Moon stone matches the one waiting here.',
            ],
            tasks=[item('ascension_worlds:anorthosite', 16), item('ascension_worlds:moon_basalt', 16)],
            rewards=[give('minecraft:stonecutter', 1)],
        ),

        # ---- The right: small things worth knowing -----------------------------------------
        Quest(
            slug='tall_ones', x=6, y=6,
            title='Tall visitors',
            subtitle='Do not meet their eyes',
            icon=item_icon('minecraft:carved_pumpkin'), shape=GOAL,
            deps=['earthrise'], hide_until='visible',
            text=[
                'The Moon\'s Endermen are taller and older than Earth\'s. Looking at one is an invitation.',
                'A carved pumpkin on your head hides your eyes from them.',
            ],
            tasks=[item('minecraft:carved_pumpkin', 1)],
            rewards=[give('minecraft:ender_pearl', 2)],
        ),

        # ---- The main line: fortress, guardians, the ring -------------------------------------
        Quest(
            slug='citadel', x=3, y=6,
            title='The Ruined Citadel',
            subtitle='A broken fortress to the east',
            icon=item_icon('minecraft:chiseled_stone_bricks'), shape=LANDMARK, size=1.2,
            deps=['dust'], hide_until='visible',
            text=[
                'About eight hundred blocks east of the Moon\'s middle, at 800, 0. Two things guard it.',
                'Bring the best armour you have, food, and a way home.',
            ],
            tasks=[advancement('ascension_compat_cataclysm:growth/ruined_citadel')],
            rewards=[give('minecraft:golden_apple', 2)],
        ),
        Quest(
            slug='golem', x=3, y=8,
            title='The Ender Golem',
            subtitle='A huge guardian',
            icon=item_icon('minecraft:dragon_head'), shape=LANDMARK, size=1.2,
            deps=['citadel'], hide_until='complete',
            text=[
                'Keep moving, and fight where you have room. It is tougher here than anywhere else it is found.',
                'The first time your team beats it, everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_compat_cataclysm:growth/ender_golem')],
            rewards=[give('minecraft:golden_carrot', 12)],
        ),
        Quest(
            slug='moon_ring', x=6, y=8,
            title='The ring on the plain',
            subtitle='Dark, and it will not break',
            icon=item_icon('ascension_worlds:inscribed_gatestone'), shape=LANDMARK, size=1.2,
            deps=['earthrise'], hide_until='visible',
            text=[
                'On the plain, about a hundred and sixty blocks west of the Moon\'s middle, at -160, 0, a ring of '
                'pale stone stands dark.',
                'It cannot be broken, and it will not wake without a key.',
            ],
            tasks=[structure('ascension_worlds:gateway_frame_moon')],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='guardian', x=3, y=10,
            title='The Ender Guardian',
            subtitle='When the first falls, it wakes',
            icon=item_icon('ascension_worlds:gateway_key'), shape=LANDMARK, size=1.2,
            deps=['golem'], hide_until='complete',
            text=[
                'It rises when the golem falls, and it drops a gateway key.',
                'Take the key. It fits the Moon\'s ring and nothing else.',
            ],
            tasks=[advancement('ascension_compat_cataclysm:growth/ender_guardian')],
            rewards=[give('minecraft:golden_apple', 2)],
        ),
        Quest(
            slug='moon_gate', x=3, y=12,
            title='Wake the ring',
            subtitle='Key to the keystone',
            icon=item_icon('ascension_worlds:gateway_keystone'), shape=LANDMARK, size=1.2,
            deps=['guardian', 'moon_ring'], hide_until='complete',
            text=[
                'Use the key on the keystone at the ring\'s foot. When it lights, everyone on the server gains skill points.',
                'Watch your hand.',
            ],
            tasks=[advancement('ascension_worlds:gates/moon')],
            rewards=[give('ascension_atmosphere:oxygen_tank', 2), give('minecraft:diamond', 2)],
        ),
        Quest(
            slug='both_lit', x=3, y=14,
            title='Two rings',
            subtitle='The way home, and the way back',
            icon=item_icon('minecraft:ender_pearl'), shape=LESSON, size=0.9,
            deps=['moon_gate'], hide_until='complete',
            text=[
                'With Earth\'s ring and the Moon\'s both lit, stepping into either offers the other.',
                'Ships are still how you reach worlds that have no lit ring.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 12)],
        ),
        Quest(
            slug='look_out', x=3, y=16,
            title='Look out',
            subtitle='Another world is named',
            icon=item_icon('minecraft:spyglass'), shape=LESSON, size=0.9,
            deps=['both_lit'], hide_until='complete',
            text=[
                'Open the Planets page. Another world is named, and it is far.',
                'The Rot page says what to bring.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:golden_boots', 1)],
        ),
    ],
)
