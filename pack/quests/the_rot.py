"""The Rot: the Nether, opened to the sky. Gold, the long flight, the ring in the warped forest, and what its
fire opens (netherite, and brass for Create).

Everything here is a rule of the world that the game states or the player can see for themselves: the air
harms you, gold keeps it off (the game says so on arrival), piglins take gold. Nothing says why.

The gold from "Keep some gold" (First Days) is what this page's first quest asks for: four ingots make boots.

Layout: the first two lessons across the top, the landing, then three columns: wealth on the left, the ring in
the middle, fire on the right.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, give, item, item_icon, picture,
                          structure, checkmark)

CHAPTER = Chapter(
    slug='the_rot',
    title='The Rot',
    subtitle='Far to the south-east. Wear gold.',
    icon=picture('textures/quests/icons/the_rot.png'),
    shape=GOAL,
    quests=[
        # ---- Before you go -------------------------------------------------------------------
        Quest(
            slug='ward', x=1.5, y=0,
            title='Wear gold',
            subtitle='This is what the gold was for',
            icon=item_icon('minecraft:golden_boots'), shape=GOAL,
            deps=[],
            text=[
                'The air of the Nether harms you. A single piece of gold armour, worn in any slot, keeps it off.',
                'Four ingots make boots, and you kept four. Netherite armour counts too, and gold calms piglins.',
            ],
            tasks=[item('minecraft:golden_boots', 1)],
            rewards=[give('minecraft:golden_apple', 1)],
        ),
        Quest(
            slug='far_flight', x=4.5, y=0,
            title='A long flight',
            subtitle='Eighteen thousand blocks, south-east',
            icon=item_icon('minecraft:clock'), shape=LESSON, size=0.9,
            deps=[],
            text=[
                'The Nether lies about eighteen thousand blocks south-east of Earth. Count the trip in minutes of '
                'air: a tank is five minutes and you can carry two.',
                'Light a ring there and you need not fly back.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 12)],
        ),
        Quest(
            slug='landing', x=3, y=2,
            title='Landing on the Nether',
            subtitle='No roof, and a floor of fire',
            icon=item_icon('minecraft:netherrack'), shape=LANDMARK, size=1.2,
            deps=['ward', 'far_flight'],
            text=[
                'The sky is open here, and the ground is lava seas, cliffs and caves.',
                'Wear your gold. Landing earns a skill point, once.',
            ],
            tasks=[advancement('ascension_worlds:discovery/nether')],
            rewards=[give('minecraft:golden_carrot', 8)],
        ),

        # ---- Wealth: piglins, a bastion, debris, netherite ---------------------------------------
        Quest(
            slug='piglins', x=0, y=4,
            title='Oh, shiny',
            subtitle='Gold buys peace',
            icon=item_icon('minecraft:gold_ingot'), shape=GOAL,
            deps=['landing'], hide_until='visible',
            text=[
                'Piglins strike anyone who is not wearing gold. Wear it and they leave you alone.',
                'Drop a gold ingot where one can see it and it stops to pick it up. Throw gold at it and it trades.',
            ],
            tasks=[advancement('minecraft:nether/distract_piglin')],
            rewards=[give('minecraft:gold_ingot', 8)],
        ),
        Quest(
            slug='bastion', x=0, y=6,
            title='A Bastion',
            subtitle='A stronghold of gold',
            icon=item_icon('minecraft:gilded_blackstone'), shape=LANDMARK, size=1.2,
            deps=['piglins'], hide_until='visible',
            text=[
                'Bastions hold gold, a great many piglins, and chests worth the trouble.',
                'Bring a shield and more gold than you think. The template that upgrades diamond to netherite '
                'can turn up in their chests.',
            ],
            tasks=[advancement('ascension_combat:growth/discovery/bastion')],
            rewards=[give('minecraft:shield', 1)],
        ),
        Quest(
            slug='debris', x=0, y=8,
            title='Ancient debris',
            subtitle='Dark, heavy, and deep',
            icon=item_icon('minecraft:ancient_debris'), shape=GOAL,
            deps=['bastion'], hide_until='complete',
            text=[
                'It hides deep in the rock below the lava and needs a diamond pickaxe.',
                'Smelt it to scrap.',
            ],
            tasks=[advancement('minecraft:nether/obtain_ancient_debris')],
            rewards=[give('minecraft:cooked_porkchop', 12)],
        ),
        Quest(
            slug='netherite', x=0, y=10,
            title='Netherite',
            subtitle='The top of the ladder',
            icon=item_icon('minecraft:netherite_ingot'), shape=GOAL,
            deps=['debris'], hide_until='complete',
            text=[
                'Four scrap and four gold make one ingot.',
                'At a smithing table, an ingot and an upgrade template turn diamond gear into netherite. '
                'Netherite armour keeps the rot off as gold does.',
            ],
            tasks=[item('minecraft:netherite_ingot', 1)],
            rewards=[give('minecraft:gold_ingot', 8)],
        ),

        # ---- The ring: the colony, the smith, the gate ------------------------------------------
        Quest(
            slug='warped_gate', x=3, y=4,
            title='The gate ruin',
            subtitle='A ring in the teal forest',
            icon=item_icon('minecraft:warped_nylium'), shape=LANDMARK, size=1.2,
            deps=['landing'], hide_until='visible',
            text=[
                'Find the warped forest, the teal one. A ring stands in it, within about a mile of the world\'s middle.',
                'It cannot be broken, and it will not wake without this world\'s own key.',
            ],
            tasks=[advancement('ascension_worlds:discovery/nether_gate')],
            rewards=[give('minecraft:cooked_porkchop', 12)],
        ),
        Quest(
            slug='colony', x=3, y=6,
            title='Old halls',
            subtitle='Half-buried, and not empty',
            icon=item_icon('ascension_worlds:colony_mural'), shape=GOAL,
            deps=['warped_gate'], hide_until='complete',
            text=[
                'Broken halls lie scattered across the wastes and the red forests.',
                'Their chests hold old things and enchanted books. Take what you can carry.',
            ],
            tasks=[structure('ascension_worlds:nether_colony')],
            rewards=[give('minecraft:lapis_lazuli', 16)],
        ),
        Quest(
            slug='monstrosity', x=3, y=8,
            title='The Netherite Monstrosity',
            subtitle='A smith that never stopped',
            icon=item_icon('minecraft:netherite_block'), shape=LANDMARK, size=1.2,
            deps=['colony'], hide_until='complete',
            text=[
                'It works a forge in the soul sand valleys, and it hits like a falling building.',
                'It drops this world\'s key. The first time your team beats it, everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_compat_cataclysm:growth/netherite_monstrosity')],
            rewards=[give('minecraft:golden_apple', 3)],
        ),
        Quest(
            slug='nether_gate', x=3, y=10,
            title='Wake the ring',
            subtitle='Key to the keystone',
            icon=item_icon('ascension_worlds:gateway_keystone'), shape=LANDMARK, size=1.2,
            deps=['monstrosity'], hide_until='complete',
            text=[
                'Use the key on the keystone. When it lights, everyone on the server gains skill points.',
                'With Earth\'s ring lit as well, you can walk home.',
            ],
            tasks=[advancement('ascension_worlds:gates/the_nether')],
            rewards=[give('minecraft:diamond', 3)],
        ),

        # ---- Fire: blazes, brass, better blades ----------------------------------------------------
        Quest(
            slug='fortress', x=6, y=4,
            title='A Nether Fortress',
            subtitle='Long bridges of dark brick',
            icon=item_icon('minecraft:nether_bricks'), shape=LANDMARK, size=1.2,
            deps=['landing'], hide_until='visible',
            text=[
                'Blazes spawn in fortresses, and so do wither skeletons.',
                'Bring fire resistance, a bow, and a way out.',
            ],
            tasks=[advancement('ascension_combat:growth/discovery/nether_fortress')],
            rewards=[give('minecraft:arrow', 16)],
        ),
        Quest(
            slug='blaze', x=6, y=6,
            title='Blaze rods',
            subtitle='Fuel, brewing, and more',
            icon=item_icon('minecraft:blaze_rod'), shape=GOAL,
            deps=['fortress'], hide_until='complete',
            text=[
                'Blazes drop rods. Keep a few: Create\'s blaze burner is made from a blaze and needs netherrack to hold it.',
            ],
            tasks=[advancement('minecraft:nether/obtain_blaze_rod')],
            rewards=[give('minecraft:netherrack', 16)],
        ),
        Quest(
            slug='burner', x=6, y=8,
            title='A blaze, kept',
            subtitle='A burner that never goes out',
            icon=item_icon('create:blaze_burner'), shape=GOAL,
            deps=['blaze'], hide_until='complete',
            text=[
                'Make an empty blaze burner, then use it on a blaze. Hold W over it for how.',
                'A mixer set over a lit burner is hot enough to make brass.',
            ],
            tasks=[item('create:blaze_burner', 1)],
            rewards=[give('minecraft:copper_ingot', 8)],
        ),
        Quest(
            slug='brass', x=6, y=10,
            title='Brass',
            subtitle='Create\'s next tier',
            icon=item_icon('create:brass_ingot'), shape=GOAL,
            deps=['burner'], hide_until='complete',
            text=[
                'Copper and zinc, mixed over a lit burner, make brass.',
                'Brass is the upgrade: stronger casings, finer controls, better propellers.',
            ],
            tasks=[advancement('create:brass')],
            rewards=[give('minecraft:quartz', 8)],
        ),
        Quest(
            slug='smart_propeller', x=6, y=12,
            title='A propeller that tips itself',
            subtitle='Brass, and a little quartz',
            icon=item_icon('aeronautics:smart_propeller'), shape=GOAL,
            deps=['brass'], hide_until='complete',
            text=[
                'A smart propeller makes thrust when spun and tips itself to face upward.',
                'It needs brass and quartz. Hold W over it in JEI to see the rest.',
            ],
            tasks=[item('aeronautics:smart_propeller', 1)],
            rewards=[give('minecraft:quartz', 16)],
        ),
        Quest(
            slug='the_wither', x=9, y=4,
            title='The Wither',
            subtitle='Optional, and it fights back from range',
            icon=item_icon('minecraft:nether_star'), shape=LANDMARK, size=1.2,
            deps=['fortress'], hide_until='complete', optional=True,
            text=[
                'Wither skeletons drop skulls, rarely. Three skulls on a T of four soul sand call it.',
                'Fight it in a tunnel with a low roof. Bring a shield, and milk to cure the withering.',
                'The first time your team beats it, everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_combat:growth/wither')],
            rewards=[give('minecraft:golden_apple', 3)],
        ),
        Quest(
            slug='ignis', x=9, y=6,
            title='Ignis',
            subtitle='Optional, and no easier',
            icon=item_icon('minecraft:magma_block'), shape=LANDMARK, size=1.2,
            deps=['fortress'], hide_until='complete', optional=True,
            text=[
                'A second fire fights in an arena of its own, somewhere in this world.',
                'The first time your team beats it, everyone on the team gains skill points.',
            ],
            tasks=[advancement('ascension_compat_cataclysm:growth/ignis')],
            rewards=[give('minecraft:golden_apple', 3)],
        ),
    ],
)
