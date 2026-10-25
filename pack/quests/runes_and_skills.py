"""Runes and Skill Points: a guide to the two screens that make a character, written for a player who has just
earned their first point and does not know where it goes.

Two columns that end in the same place: skill points and the Skill Web on the left (K), runes and the rune page on the
right (R), and one quest at the bottom that ties them to gear and the character sheet (H). Every quest is a lesson with
a checkmark: there is nothing to detect in reading, and nothing here gates a path (ADR-0018).

Numbers quoted are the ones in docs/progression/growth.md and docs/technical/rune-system.md (41 points from seven
bosses, four gates, six places and six projects; a respec costs 20 levels; a rune change one Rune Chalk and 3 levels; the second
path opens at the first milestone and the three shards at the next three). If a number there moves, move it here.
The places are described by kind, not named, so the page never names a place the player has not been to (the book's
rule: say what and where, never why).
"""

from quests.model import (GOAL, LESSON, Chapter, Quest, checkmark, give, item_icon)

CHAPTER = Chapter(
    slug='runes_and_skills',
    title='Runes and Skill Points',
    subtitle='Two screens, one character.',
    icon=item_icon('minecraft:amethyst_shard'),
    shape=LESSON,
    quests=[
        # ---- Skill points and the web (K) -------------------------------------------------------
        Quest(
            slug='what', x=0, y=0,
            title='What skill points are',
            subtitle='Permanent, and spent on you',
            icon=item_icon('minecraft:experience_bottle'), shape=LESSON, size=0.9,
            text=[
                'A skill point makes you stronger for good. You earn them for firsts: never for kills or hours played, so '
                'nothing can be farmed.',
                'There are 41 in all. You will not fill the whole web, so choose.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='bosses', x=0, y=2,
            title='Points from bosses',
            subtitle='3 each, once, for the whole team',
            icon=item_icon('minecraft:wither_skeleton_skull'), shape=LESSON, size=0.9,
            deps=['what'],
            text=[
                'The first time your team beats each boss, every player on it gains 3 points.',
                'The Warden, the Wither, the Ender Golem, the Ender Guardian, the Netherite Monstrosity, Ignis and the Ender Dragon.',
                'Beating the same boss again gives nothing. A friend who joins later is given the points too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='gates', x=0, y=4,
            title='Points from gates',
            subtitle='2 each, once, for the whole team',
            icon=item_icon('minecraft:ender_eye'), shape=LESSON, size=0.9,
            deps=['bosses'],
            text=[
                'Every gate you light gives every player 2 points: Earth\'s, the Moon\'s, the Nether\'s and the End\'s.',
                'A lit gate stays lit, so these come once too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='places', x=0, y=6,
            title='Places and projects',
            subtitle='1 each, once',
            icon=item_icon('minecraft:spyglass'), shape=LESSON, size=0.9,
            deps=['gates'],
            text=[
                'The first time you find each kind of place, you gain a point: a few ruins, and every new world you land on. These are yours alone.',
                'Each project sheet (Spin, Brass, Crude, Wings, Breath, Skyline) pays one point to the whole team when its finale is done.',
                'A hundred of the same kind give one point. Bosses, gates and projects are shared.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='web', x=0, y=8,
            title='The Skill Web',
            subtitle='K',
            icon=item_icon('minecraft:amethyst_cluster'), shape=GOAL,
            deps=['places'],
            text=[
                'Press K. A gold note above your health row tells you when you have points to spend.',
                'Click a node to mark it, click again to unmark, then press Confirm. Hover a node for what it gives.',
                'A node costs one point and must touch one you already hold, or sit in the ring round the centre.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:experience_bottle', 8)],
        ),
        Quest(
            slug='corners', x=0, y=10,
            title='What each corner gives',
            subtitle='Power, Fury, Guard, Wayfarer',
            icon=item_icon('minecraft:iron_chestplate'), shape=LESSON, size=0.9,
            deps=['web'],
            text=[
                'Power: Attack Damage, Lethality and Penetration. Fury: Attack Speed and Omnivamp.',
                'Guard: Health, Armor and Magic Resist. Wayfarer: Move Speed, mining speed, reach and safe falls.',
                'A small node is about 3% of what a geared player has: +1 Attack Damage, +3 Health or +5 Armor.',
                'Notables and the outer ring give several times that. The corners shade into one another, so you can drift.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='respec', x=0, y=12,
            title='Change your mind',
            subtitle='Twenty levels, out of a fight',
            icon=item_icon('minecraft:experience_bottle'), shape=LESSON, size=0.9,
            deps=['corners'],
            text=[
                'The Respec button returns every point to spend again. It works anywhere out of combat and costs 20 '
                'experience levels.',
                'It moves points; it never makes them. Death takes nothing from the web.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:experience_bottle', 16)],
        ),

        # ---- Runes and the rune page (R) --------------------------------------------------------
        Quest(
            slug='page', x=5, y=0,
            title='The rune page',
            subtitle='R',
            icon=item_icon('minecraft:amethyst_shard'), shape=GOAL,
            text=[
                'Press R. Runes change how you fight, not how strong you are: skill points are numbers, runes are behaviours.',
                'Your page holds a main path, a second path and three stat shards. Nothing is kept until you press Save, '
                'and Reset takes you back to the page you saved.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 6)],
        ),
        Quest(
            slug='paths', x=5, y=2,
            title='Four paths',
            subtitle='Pick how you play',
            icon=item_icon('minecraft:netherite_sword'), shape=LESSON, size=0.9,
            deps=['page'],
            text=[
                'Vanguard keeps hitting: damage that grows the longer a fight lasts. Reaver strikes first and finishes fast.',
                'Aetherwright makes the magic do more. Bulwark stands in front: defence, pets and the team.',
                'Every rune can be tried from the first minute; only the slots are locked.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='keystones', x=5, y=4,
            title='Keystones',
            subtitle='The rune that sets your style',
            icon=item_icon('minecraft:nether_star'), shape=LESSON, size=0.9,
            deps=['paths'],
            text=[
                'Your main path gives one keystone and one small rune from each of its two rows. Each path has three '
                'keystones: two for fighting and a third that stands apart.',
                'The third one stands apart on the dial: vein mining, Lethality as damage, more experience, or healing that becomes a shield.',
                'Your second path gives two small runes and no keystone.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='slots', x=5, y=6,
            title='Slots that open',
            subtitle='The page grows as you do',
            icon=item_icon('minecraft:ender_eye'), shape=LESSON, size=0.9,
            deps=['keystones'],
            text=[
                'The main path is open from the start. Your team\'s first boss kill or lit gate opens the second path; the '
                'next three open the offence, flex and defence shards.',
                'Points from places do not open slots. A locked slot says what opens it.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='chalk', x=5, y=8,
            title='Changing a rune',
            subtitle='One chalk and three levels',
            icon=item_icon('ascension_combat:rune_chalk'), shape=GOAL,
            deps=['slots'],
            text=[
                'Filling an empty slot is free. Removing or replacing a rune costs one Rune Chalk and 3 levels, once per '
                'save however many runes change, and only out of combat.',
                'Amethyst and calcite make chalk, and both come from geodes. Here are two to start.',
            ],
            tasks=[checkmark()],
            rewards=[give('ascension_combat:rune_chalk', 2)],
        ),
        Quest(
            slug='grow', x=5, y=10,
            title='Runes that grow',
            subtitle='New kinds of enemy',
            icon=item_icon('minecraft:sculk_catalyst'), shape=LESSON, size=0.9,
            deps=['chalk'],
            text=[
                'Four runes grow: Blooded, Trophy Hunter, Gathering Charge and Deep Roots.',
                'Each new kind of enemy you defeat while one is on your page adds to it, up to 20 kinds.',
                'A farm cannot fill it, since a zombie is one kind however many fall. Swap the rune out and it starts again.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),

        # ---- Together ---------------------------------------------------------------------------
        Quest(
            slug='together', x=2.5, y=14,
            title='Putting it together',
            subtitle='H',
            icon=item_icon('minecraft:book'), shape=GOAL,
            deps=['respec', 'grow'],
            text=[
                'Gear is what you carry, skill points are who you are for good, and runes are how you fight today.',
                'The character sheet (H) shows all three and where every number comes from, so you can see what a node or a '
                'rune is really doing.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:experience_bottle', 16)],
        ),
    ],
)
