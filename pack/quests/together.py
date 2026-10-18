"""Together: parties, the shared book, the map, and a breath for a friend.

All of it is FTB Teams, FTB Chunks and our own tank: no new rules here, just where the player finds them
(docs/progression/teams.md). Every quest is a lesson, because none of it can be detected from outside a
party of two.
"""

from quests.model import LESSON, Chapter, Quest, checkmark, give, item_icon, picture

CHAPTER = Chapter(
    slug='together',
    title='Together',
    subtitle='Friends make the road shorter.',
    icon=picture('textures/quests/icons/together.png'),
    shape=LESSON,
    quests=[
        Quest(
            slug='party', x=3, y=0,
            title='A party',
            subtitle='Friends share a team',
            icon=item_icon('minecraft:player_head'), shape=LESSON, size=0.9,
            text=[
                'Open the team screen from the button beside your inventory, make a party and invite your friends.',
                'A boss one of you beats counts for all of you, and so do the quests.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='catch_up', x=0, y=2,
            title='Catching up',
            subtitle='Join late, miss nothing',
            icon=item_icon('minecraft:clock'), shape=LESSON, size=0.9,
            deps=['party'],
            text=[
                'Join a party and you receive what it already holds: its boss kills and its lit rings.',
                'The party receives what you hold. Leaving takes nothing back.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='friends', x=3, y=2,
            title='No friendly fire',
            subtitle='Swing wide',
            icon=item_icon('minecraft:shield'), shape=LESSON, size=0.9,
            deps=['party'],
            text=[
                'Party members cannot hurt each other: not with swords, arrows, fuses or pets.',
                'Anyone else can, wherever the server allows fighting.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='one_book', x=6, y=2,
            title='One book, many hands',
            subtitle='A task done is done for all',
            icon=item_icon('minecraft:writable_book'), shape=LESSON, size=0.9,
            deps=['party'],
            text=[
                'A quest one of you finishes counts for the whole party.',
                'Each of you claims your own rewards.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='map', x=1.5, y=4,
            title='The map',
            subtitle='M',
            icon=item_icon('minecraft:filled_map'), shape=LESSON, size=0.9,
            deps=['catch_up'],
            text=[
                'Press M for the map. Your party shows on it, however far away they are.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:compass', 1)],
        ),
        Quest(
            slug='share_air', x=4.5, y=4,
            title='A breath for a friend',
            subtitle='Use a tank on a player',
            icon=item_icon('ascension_atmosphere:oxygen_tank'), shape=LESSON, size=0.9,
            deps=['friends'],
            text=[
                'Use an oxygen tank on another player to fill their lungs from it.',
                'It is a rescue, not a refill: their lungs only, and only as much as they can hold.',
            ],
            tasks=[checkmark()],
            rewards=[give('ascension_atmosphere:oxygen_tank', 1)],
        ),
    ],
)
