"""Crude: oil out of the ground, fuel out of the oil. Create: Petrochem (ADR-0031): steel, bronze, a pumpjack, an electrolyzer, a distillation
tower, and kerosene for the liquid Thruster. Every heated step wants a burner, and the Brass page gives three; none wants the Nether (ADR-0032).

Project 5 of 9.
"""

from quests.layout import blueprint_page
from quests.model import (Chapter, GOAL, LESSON, Quest, advancement, checkmark, give, item, item_icon, picture)

CHAPTER = Chapter(
    slug='crude',
    title='Crude',
    subtitle='Oil out of the ground, fuel out of the oil.',
    icon=picture('textures/quests/icons/crude.png'),
    shape=GOAL,
    quests=[
        Quest(
            slug='oil', legacy='ship_fly',
            title='Oil for the rocket',
            subtitle='Out of the ground, into a tank',
            icon=item_icon('petrochem:kerosene_bucket'), shape=LESSON, size=0.9,
            deps=[],
            text=[
                'The liquid Thruster burns kerosene, diesel or gasoline. All three come out of crude oil, and Create: Petrochem makes the whole chain.',
                'Oil lies under deserts, swamps, jungles and deep oceans of Earth, and nowhere else. Every step from steel to the tower wants a burner.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:coal', 16)],
        ),
        Quest(
            slug='steel', legacy='ship_fly',
            title='Steel',
            subtitle='Iron and coal, heated',
            icon=item_icon('petrochem:steel_ingot'), shape=GOAL,
            deps=['oil', 'brass.burner'],
            text=[
                'Every Petrochem machine is made of steel. Heat an iron ingot and a coal in a mixer over a burner, then press the ingot flat for sheets.',
                'Pipes, tanks and plates are all steel.',
            ],
            tasks=[item('petrochem:steel_sheet', 8)],
            rewards=[give('minecraft:iron_ingot', 16)],
        ),
        Quest(
            slug='pumpjack', legacy='ship_fly',
            title='A pumpjack',
            subtitle='A crank, a well and an arm',
            icon=item_icon('petrochem:pumpjack_arm'), shape=GOAL,
            deps=['steel', 'brass.precision'],
            text=[
                'The crank takes a rotation speed controller, the well is steel pipe, the arm is built on a mechanical crafter. Put the well in the ground, turn the crank.',
                'Desert and deep ocean give crude petroleum; swamp and jungle give oil that is already desalted. Any other ground gives nothing.',
            ],
            tasks=[item('petrochem:pumpjack_arm', 1)],
            rewards=[give('minecraft:chain', 8)],
        ),
        Quest(
            slug='electrolyzer', legacy='ship_fly',
            title='An electrolyzer',
            subtitle='Take the salt out',
            icon=item_icon('petrochem:electrolyzer'), shape=GOAL,
            deps=['pumpjack', 'bronze', 'current.alternator'],
            text=[
                'A mixer that runs on electricity, not spin. Wire it to an alternator or an accumulator and feed it petroleum: out come desalted oil and, half the time, salt.',
            ],
            tasks=[item('petrochem:electrolyzer', 1)],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='distillation', legacy='ship_fly',
            title='A distillation tower',
            subtitle='Split the oil by how hot it boils',
            icon=item_icon('petrochem:distillation_controller'), shape=GOAL,
            deps=['electrolyzer'],
            text=[
                'Steel tanks stacked over a controller, and an output block at each height. Three modes: flash, atmospheric and vacuum.',
                'Flash needs no heat; the other two want two lit burners under the base.',
                'The atmospheric mode gives diesel, kerosene and heavy naphtha. Pipe the kerosene away to a tank.',
            ],
            tasks=[item('petrochem:distillation_controller', 1)],
            rewards=[give('minecraft:bucket', 2)],
        ),
        Quest(
            slug='kerosene', legacy='ship_fly',
            title='Kerosene',
            subtitle='Fuel for a long climb',
            icon=item_icon('petrochem:kerosene_bucket'), shape=GOAL,
            deps=['distillation'],
            text=[
                'Kerosene gives the liquid Thruster more push for less fuel than diesel does. Gasoline pushes hardest and burns faster.',
            ],
            tasks=[advancement('ascension_compat_create:project/crude')],
            rewards=[give('minecraft:blaze_powder', 8)],
        ),
        Quest(
            slug='bronze',
            title='Bronze',
            subtitle='Copper and tin, heated',
            icon=item_icon('petrochem:bronze_sheet'), shape=GOAL,
            deps=['oil', 'brass.burner', 'spin.spout'],
            text=[
                'The tower and the engines use bronze plates. Heat a copper ingot and a tin nugget in a mixer over a burner, then press the ingot.',
                'Tin has no ore here. Petrochem fills a zinc nugget with a thick potion; two zinc nuggets and an iron nugget make one at a crafting table.',
            ],
            tasks=[item('petrochem:bronze_sheet', 4)],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='tank',
            title='A bigger tank',
            subtitle='Platinum holds twice the fuel',
            icon=item_icon('createpropulsion:platinum_fluid_tank'), shape=GOAL,
            deps=['distillation'], optional=True,
            text=[
                'Platinum is Create Propulsion\'s own metal, in stone and deepslate. A platinum fluid tank holds twice what a copper one does.',
                'A good place for kerosene: a rocket burns a lot of it.',
            ],
            tasks=[item('createpropulsion:platinum_fluid_tank', 1)],
            rewards=[give('minecraft:iron_ingot', 16)],
        ),
    ],
)

blueprint_page(CHAPTER, number=5, of=9, motif='crude', finale='kerosene', points=1)
