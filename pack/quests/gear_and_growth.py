"""Gear and Growth: what you wear, what you earn, what you choose. Three threads that all end on the same
character sheet (H): gear on the left, growth in the middle, runes on the right.

Nothing here says where the strongest weapons come from, because no path to them exists yet; this page teaches
only what the player can do today (docs/progression/journey.md lists what is missing).

Numbers quoted are the ones in docs/progression/growth.md and docs/technical/rune-system.md (a respec costs 20
levels, a rune change one chalk and 3 levels, the second path opens at the first milestone).
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, give, item, item_icon,
                          picture)

CHAPTER = Chapter(
    slug='gear_and_growth',
    title='Gear and Growth',
    subtitle='What you wear, earn and choose.',
    icon=picture('textures/quests/icons/gear_and_growth.png'),
    shape=LESSON,
    quests=[
        # ---- Gear -----------------------------------------------------------------------------
        Quest(
            slug='armour', x=0, y=0,
            title='Armour counts',
            subtitle='Any armour feeds your numbers',
            icon=item_icon('minecraft:iron_chestplate'), shape=LESSON, size=0.9,
            text=[
                'Iron, diamond, gold and any other mod\'s armour all feed your numbers: Armor, Magic Resist and Health.',
                'Open the sheet (H) to see what each piece gives.',
            ],
            tasks=[advancement('minecraft:story/obtain_armor')],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='enchant', x=0, y=2,
            title='Enchanting',
            subtitle='It counts toward your numbers',
            icon=item_icon('minecraft:enchanting_table'), shape=GOAL,
            deps=['armour'],
            text=[
                'Enchant something. What it adds is listed on its tooltip, and it counts toward your numbers.',
                'Bookshelves around the table make it stronger. Lapis is the cost.',
            ],
            tasks=[advancement('minecraft:story/enchant_item')],
            rewards=[give('minecraft:lapis_lazuli', 16)],
        ),
        Quest(
            slug='tomes', x=0, y=4,
            title='Tomes',
            subtitle='Unmake an enchantment, or move it',
            icon=item_icon('ascension_combat:tome_of_extraction'), shape=GOAL,
            deps=['enchant'],
            text=[
                'A Tome of Scrapping wipes an item\'s enchantments at an anvil. A Tome of Extraction moves them '
                'onto a book in a crafting grid.',
                'Both are a book, lapis and a pinch of stone: flint for scrapping, an amethyst shard for extraction.',
            ],
            tasks=[item('ascension_combat:tome_of_scrapping', 1), item('ascension_combat:tome_of_extraction', 1)],
            rewards=[give('minecraft:book', 3), give('minecraft:lapis_lazuli', 8)],
        ),
        Quest(
            slug='ability', x=0, y=6,
            title='One key for abilities',
            subtitle='Z',
            icon=item_icon('minecraft:blaze_powder'), shape=LESSON, size=0.9,
            deps=['tomes'],
            text=[
                'Some items carry an ability. Z uses the one you wear or hold.',
                'Its cooldown shows on the item itself.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),

        # ---- Growth ---------------------------------------------------------------------------
        Quest(
            slug='points', x=4, y=0,
            title='Skill points',
            subtitle='Earned, never farmed',
            icon=item_icon('minecraft:experience_bottle'), shape=LESSON, size=0.9,
            text=[
                'Nothing here comes from kills or hours played. Bosses, lit rings and new kinds of place each give '
                'points, once.',
                'Open the web with K. The page "Runes and Skill Points" walks through it.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='tree', x=4, y=2,
            title='The web',
            subtitle='Four directions, no dead ends',
            icon=item_icon('minecraft:amethyst_cluster'), shape=GOAL,
            deps=['points'],
            text=[
                'Power, Fury, Guard and Wayfarer shade into one another. Every node costs a point and must touch '
                'one you already hold.',
                'Change your mind anywhere out of a fight, for twenty levels. Every point comes back.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:experience_bottle', 8)],
        ),
        Quest(
            slug='firsts', x=4, y=4,
            title='Firsts',
            subtitle='A hundred cities, one point',
            icon=item_icon('minecraft:spyglass'), shape=LESSON, size=0.9,
            deps=['tree'],
            text=[
                'The first time you find each kind of place, you gain a point: a deep city, a mansion, a monument, '
                'a fortress, a bastion, and more.',
                'A second one of the same kind gives nothing, so go somewhere new.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),

        # ---- Runes ----------------------------------------------------------------------------
        Quest(
            slug='runes', x=8, y=0,
            title='The rune page',
            subtitle='R',
            icon=item_icon('minecraft:amethyst_shard'), shape=LESSON, size=0.9,
            text=[
                'Pick a path, a keystone and two small runes. They change how you fight, not how strong you are.',
                'Open the page with R. Nothing is kept until you press Save.',
                'The page "Runes and Skill Points" walks through it.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='chalk', x=8, y=2,
            title='Rune chalk',
            subtitle='The price of a change',
            icon=item_icon('ascension_combat:rune_chalk'), shape=GOAL,
            deps=['runes'],
            text=[
                'Removing or replacing a rune costs one chalk and a few levels. Filling an empty slot is free.',
                'Amethyst and calcite make chalk, and both come from geodes.',
            ],
            tasks=[item('ascension_combat:rune_chalk', 1)],
            rewards=[give('minecraft:experience_bottle', 8)],
        ),
        Quest(
            slug='slots', x=8, y=4,
            title='More room',
            subtitle='The page grows as you do',
            icon=item_icon('minecraft:ender_eye'), shape=LESSON, size=0.9,
            deps=['chalk'],
            text=[
                'Your team\'s first boss kill or lit ring opens a second path. The next three open the shards, one at a time.',
                'A locked slot says what opens it.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
    ],
)
