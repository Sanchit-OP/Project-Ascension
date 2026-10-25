"""Aeronautics: In Flight. Reading the sky, steering a ship, powering it, and the thrusters that make a rocket.

Same rules as the other teaching pages: the parts are Simulated's, Aeronautics', Create: Aeroworks', Create Propulsion's and
Create Crafts & Additions', each quest says what a part is made of (read from the mods' recipes) and what its own guide says,
and where the mods keep a real advancement the quest waits for it. Nothing gates anything.

The rocket half is ADR-0025 as changed by ADR-0030: a rocket is a ship with thrusters under it, carried to space the same way as
any ship (ADR-0015). The thrusters are Create Propulsion's. Helium, from the Moon, is a solid-fuel thruster fuel and a blaze
burner fuel (modules/compat/create, data/createpropulsion and data/create/data_maps). Steering is Aeroworks' (ADR-0030): the
first Moon flight of the alpha.2 test failed on the controls, so the page teaches a gyroscope and a control stand before a rocket.

The liquid fuel is crude oil from Create: Petrochem (ADR-0031): a pumpjack on Earth, an electrolyzer to desalt it, a distillation tower to split it, and
kerosene, diesel and gasoline for the liquid Thruster (modules/compat/create, data/createpropulsion/thruster_fuels/petrochem). Its steel and bronze
are mixed on a Blaze Burner, so the lane is a Nether-tier upgrade; coal and helium still reach the Moon.

Layout: the sky and its instruments on top (y 0 to 4); steering (y 7 to 9); thrusters and power (y 12 to 16); the oil lane beside them (x 13.5, y 12 to 22); the rocket at the bottom.
"""

from quests.model import (GOAL, LANDMARK, LESSON, Chapter, Quest, advancement, checkmark, dimension, give, item,
                          item_icon, picture)

CHAPTER = Chapter(
    slug='ship_fly',
    title='Aeronautics: In Flight',
    subtitle='Climb, steer, and leave.',
    icon=picture('textures/quests/icons/ship_fly.png'),
    shape=GOAL,
    quests=[
        # ---- The sky and its instruments ----------------------------------------------------------
        Quest(
            slug='pressure', x=4.5, y=0,
            title='Air pressure',
            subtitle='Climb, and everything weakens',
            icon=item_icon('minecraft:feather'), shape=LESSON, size=0.9,
            text=[
                'Hot air burners lift less and propellers push less the higher you go, because the air thins.',
                'An altitude sensor shows your height, and with goggles on, the air pressure too.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:cooked_beef', 8)],
        ),
        Quest(
            slug='altitude_sensor', x=1.5, y=2,
            title='The altitude sensor',
            subtitle='A signal that follows your height',
            icon=item_icon('simulated:altitude_sensor'), shape=GOAL,
            deps=['pressure'],
            text=[
                'Paper over an iron sheet over a casing. Climbing raises its signal by default, and you can set the lowest and highest heights, or invert it.',
            ],
            tasks=[advancement('simulated:no_pressure')],
            rewards=[give('minecraft:paper', 8)],
        ),
        Quest(
            slug='velocity', x=4.5, y=2,
            title='The velocity sensor',
            subtitle='A signal that follows your speed',
            icon=item_icon('simulated:velocity_sensor'), shape=GOAL,
            deps=['pressure'],
            text=[
                'A propeller over a barrel over a casing. It gives a signal by speed along the way it faces; set the speed that gives full strength.',
            ],
            tasks=[advancement('simulated:speed_is_key')],
            rewards=[give('minecraft:barrel', 1)],
        ),
        Quest(
            slug='goggles', x=7.5, y=2,
            title="Aviator's goggles",
            subtitle='A pilot sees more',
            icon=item_icon('aeronautics:aviators_goggles'), shape=GOAL,
            deps=['pressure'],
            text=[
                'Leather and a pair of goggles. They show what engineer\'s goggles show: speed, stress and what a block holds.',
            ],
            tasks=[advancement('aeronautics:high_fashion')],
            rewards=[give('minecraft:leather', 4)],
        ),
        Quest(
            slug='throttle', x=1.5, y=4,
            title='A throttle lever',
            subtitle='Set a signal by hand',
            icon=item_icon('simulated:throttle_lever'), shape=GOAL,
            deps=['altitude_sensor', 'the_rot.brass'],
            text=[
                'A stick on a brass casing. Drag it to set a signal from nothing to full, and use a wrench to invert it.',
            ],
            tasks=[item('simulated:throttle_lever', 1)],
            rewards=[give('minecraft:stick', 8)],
        ),
        Quest(
            slug='gimbal', x=4.5, y=4,
            title='A gimbal sensor',
            subtitle='A signal that follows the tilt',
            icon=item_icon('simulated:gimbal_sensor'), shape=GOAL,
            deps=['velocity', 'the_rot.brass'],
            text=[
                'A compass over a gyroscopic mechanism over a brass casing. It gives a signal by tilt, each axis on its own.',
                'By default the signal is strongest downhill; you can set the angle and invert it.',
            ],
            tasks=[advancement('simulated:the_definition_of_up')],
            rewards=[give('minecraft:compass', 1)],
        ),
        Quest(
            slug='navigation', x=7.5, y=4,
            title='A navigation table',
            subtitle='A signal toward where you are going',
            icon=item_icon('simulated:navigation_table'), shape=GOAL,
            deps=['goggles', 'create_craft.precision'],
            text=[
                'A brass sheet over a precision mechanism over a brass casing. Give it a navigation item and it signals toward the destination, weaker when you do not face it.',
            ],
            tasks=[advancement('simulated:thataway')],
            rewards=[give('minecraft:map', 1)],
        ),

        # ---- Steering -----------------------------------------------------------------------------
        Quest(
            slug='steering', x=4.5, y=7,
            title='Steering a ship',
            subtitle='Keep it level, point it where you go',
            icon=item_icon('aeroworks:control_stand'), shape=LESSON, size=0.9,
            deps=['navigation', 'gimbal'],
            text=[
                'A ship turns on three axes and wants to roll over. Two blocks make it calm: a gyroscope keeps it level, and a control stand holds the levers.',
                'Both come from Create: Aeroworks. They send signals over redstone links, so the stand can sit anywhere on the ship.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='gyroscope', x=1.5, y=9,
            title='A gyroscope',
            subtitle='A ship that stays level',
            icon=item_icon('aeroworks:gyroscope'), shape=GOAL,
            deps=['steering', 'gimbal'],
            text=[
                'A flywheel and a gimbal sensor, nothing else. Spin it with a shaft: the faster it spins, the harder it pushes the ship upright.',
                'Its tooltip says Off, Unstable, Steady or Level. A redstone signal on one side eases that axis, so you can tilt and then come back to level.',
            ],
            tasks=[item('aeroworks:gyroscope', 1)],
            rewards=[give('minecraft:iron_ingot', 4)],
        ),
        Quest(
            slug='control_stand', x=4.5, y=9,
            title='A control stand',
            subtitle='Levers, a joystick, a wheel',
            icon=item_icon('aeroworks:control_stand'), shape=GOAL,
            deps=['steering', 'the_rot.brass'],
            text=[
                'A copper casing, a redstone link, a transmitter, an electron tube and a precision mechanism.',
                'Click modules onto it: a lever, a joystick, a wheel, a yoke, a throttle.',
                'Each module sends its own signal on the link frequencies you set. Wire the signals to your thrusters and propellers and you have a cockpit.',
            ],
            tasks=[item('aeroworks:control_stand', 1)],
            rewards=[give('minecraft:redstone', 16)],
        ),
        Quest(
            slug='joystick', x=7.5, y=9,
            title='A joystick',
            subtitle='Two axes, one hand',
            icon=item_icon('aeroworks:joystick'), shape=GOAL,
            deps=['control_stand'],
            text=[
                'Brass, a redstone link, a precision mechanism and a shaft, built on a mechanical crafter. Push it and it signals 0 to 15 in each direction.',
                'It has a keyboard mode for steering without the mouse, and a button that locks it where it is.',
            ],
            tasks=[item('aeroworks:joystick', 1)],
            rewards=[give('minecraft:repeater', 2)],
        ),

        # ---- Thrusters ----------------------------------------------------------------------------
        Quest(
            slug='thrusters', x=4.5, y=12,
            title='Thrusters',
            subtitle='A block that pushes',
            icon=item_icon('createpropulsion:solid_fuel_thruster'), shape=LESSON, size=0.9,
            deps=['control_stand', 'gyroscope'],
            text=[
                'A propeller pushes against air. A thruster is a block that pushes along the way it faces, in air or out of it.',
                'Create Propulsion adds several: one that burns solid fuel, one that burns a liquid, and an ion thruster that runs on electricity.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='solid_thruster', x=1.5, y=14,
            title='A solid fuel thruster',
            subtitle='Coal in, push out',
            icon=item_icon('createpropulsion:solid_fuel_thruster'), shape=GOAL,
            deps=['thrusters'],
            text=[
                'Copper sheets, iron sheets, andesite alloy, a chute and a fluid pipe: no Nether needed. It burns what you feed it and costs no stress.',
                'Drop fuel in from a chute or a belt. A redstone signal pauses it and stops it using fuel.',
            ],
            tasks=[advancement('createpropulsion:fuel_propulsion')],
            rewards=[give('minecraft:coal', 16)],
        ),
        Quest(
            slug='fuel', x=4.5, y=14,
            title='Fuel decides the push',
            subtitle='Wood, coal, cake, helium',
            icon=item_icon('ascension_worlds:raw_helium'), shape=GOAL,
            deps=['thrusters'],
            text=[
                'Wood pushes a little, coal and charcoal push more, a blaze cake pushes hardest of the common fuels.',
                'Raw helium from the Moon pushes like a blaze cake and burns twice as long. Bring a stack.',
            ],
            tasks=[item('ascension_worlds:raw_helium', 8)],
            rewards=[give('minecraft:coal_block', 2)],
        ),
        Quest(
            slug='thruster', x=7.5, y=14,
            title='A thruster',
            subtitle='A liquid in, a hard push out',
            icon=item_icon('createpropulsion:thruster'), shape=GOAL,
            deps=['thrusters', 'the_rot.brass'],
            text=[
                'Iron and copper sheets, a fluid pipe, two mechanical pumps and a brass casing. It takes a liquid fuel from a pipe or a tank: kerosene, diesel or gasoline.',
                'Brass can be crafted by hand from copper and zinc; a heated mixer makes it far cheaper.',
            ],
            tasks=[item('createpropulsion:thruster', 1)],
            rewards=[give('minecraft:bucket', 1)],
        ),

        # ---- Power aboard -------------------------------------------------------------------------
        Quest(
            slug='power_aboard', x=10.5, y=12,
            title='Power aboard',
            subtitle='Electricity, stored in the hull',
            icon=item_icon('createaddition:modular_accumulator'), shape=LESSON, size=0.9,
            deps=['create_power.stress'],
            text=[
                'Create Crafts & Additions turns spin into electricity and electricity back into spin. Wires carry it, accumulators store it.',
                'A ship can carry its own power: with the Sable addon the wires follow the ship, so a charged accumulator in the hull runs anything on board.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='alternator', x=10.5, y=14,
            title='An alternator',
            subtitle='Spin in, power out',
            icon=item_icon('createaddition:alternator'), shape=GOAL,
            deps=['power_aboard'],
            text=[
                'Andesite alloy, iron plates, an iron rod and copper spools, on a mechanical crafter. A shaft turns it and it gives energy to whatever is wired to it.',
            ],
            tasks=[item('createaddition:alternator', 1)],
            rewards=[give('minecraft:iron_ingot', 8)],
        ),
        Quest(
            slug='accumulator', x=10.5, y=16,
            title='An accumulator',
            subtitle='Power kept for later',
            icon=item_icon('createaddition:modular_accumulator'), shape=GOAL,
            deps=['alternator'],
            text=[
                'A brass casing, two capacitors, a copper rod and electrum wire. Connect it to a generator, and it feeds machines when the generator is stopped.',
            ],
            tasks=[item('createaddition:modular_accumulator', 1)],
            rewards=[give('minecraft:redstone', 16)],
        ),
        Quest(
            slug='ion_thruster', x=7.5, y=16,
            title='An ion thruster',
            subtitle='No fuel, only power',
            icon=item_icon('createpropulsion:ion_thruster'), shape=GOAL,
            deps=['thruster', 'accumulator'],
            text=[
                'Needs redstone parts from Simulated, brass and sturdy sheets, a precision mechanism and a chute. It pushes gently for as long as it has power.',
                'It never runs dry, so it is what a ship uses to keep station in space.',
            ],
            tasks=[advancement('createpropulsion:energy_propulsion')],
            rewards=[give('minecraft:redstone_block', 1)],
        ),

        # ---- Oil -----------------------------------------------------------------------------------
        Quest(
            slug='oil', x=13.5, y=12,
            title='Oil for the rocket',
            subtitle='Out of the ground, into a tank',
            icon=item_icon('petrochem:kerosene_bucket'), shape=LESSON, size=0.9,
            deps=['thrusters'],
            text=[
                'The liquid Thruster burns kerosene, diesel or gasoline. All three come out of crude oil, and Create: Petrochem makes the whole chain.',
                'Oil lies under deserts, swamps, jungles and deep oceans of Earth, and nowhere else. A burner under the mixer and tower is needed at every step.',
            ],
            tasks=[checkmark()],
            rewards=[give('minecraft:coal', 16)],
        ),
        Quest(
            slug='steel', x=13.5, y=14,
            title='Steel',
            subtitle='Iron and coal, heated',
            icon=item_icon('petrochem:steel_ingot'), shape=GOAL,
            deps=['oil'],
            text=[
                'Every Petrochem machine is made of steel. Heat an iron ingot and a coal in a mixer over a burner, then press the ingot flat for sheets.',
                'Bronze is the second metal: copper and a tin nugget. A tin nugget is a zinc nugget filled with a thick potion by a spout.',
            ],
            tasks=[item('petrochem:steel_sheet', 8)],
            rewards=[give('minecraft:iron_ingot', 16)],
        ),
        Quest(
            slug='pumpjack', x=13.5, y=16,
            title='A pumpjack',
            subtitle='A crank, a well and an arm',
            icon=item_icon('petrochem:pumpjack_arm'), shape=GOAL,
            deps=['steel'],
            text=[
                'The crank takes a rotation speed controller, the well is steel pipe, the arm is built on a mechanical crafter. Put the well in the ground, turn the crank.',
                'Desert and deep ocean give crude petroleum; swamp and jungle give oil that is already desalted. Any other ground gives nothing.',
            ],
            tasks=[item('petrochem:pumpjack_arm', 1)],
            rewards=[give('minecraft:chain', 8)],
        ),
        Quest(
            slug='electrolyzer', x=13.5, y=18,
            title='An electrolyzer',
            subtitle='Take the salt out',
            icon=item_icon('petrochem:electrolyzer'), shape=GOAL,
            deps=['pumpjack', 'alternator'],
            text=[
                'A mixer that runs on electricity, not spin. Wire it to an alternator or an accumulator and feed it petroleum: out come desalted oil and, half the time, salt.',
            ],
            tasks=[item('petrochem:electrolyzer', 1)],
            rewards=[give('minecraft:copper_ingot', 16)],
        ),
        Quest(
            slug='distillation', x=13.5, y=20,
            title='A distillation tower',
            subtitle='Split the oil by how hot it boils',
            icon=item_icon('petrochem:distillation_controller'), shape=GOAL,
            deps=['electrolyzer'],
            text=[
                'Steel tanks stacked over a controller, a blaze burner under it, and an output block at each height. Three modes: flash, atmospheric and vacuum.',
                'The atmospheric mode gives diesel, kerosene and heavy naphtha. Pipe the kerosene away to a tank.',
            ],
            tasks=[item('petrochem:distillation_controller', 1)],
            rewards=[give('minecraft:bucket', 2)],
        ),
        Quest(
            slug='kerosene', x=13.5, y=22,
            title='Kerosene',
            subtitle='Fuel for a long climb',
            icon=item_icon('petrochem:kerosene_bucket'), shape=GOAL,
            deps=['distillation', 'thruster'],
            text=[
                'Kerosene gives the liquid Thruster more push for less fuel than diesel does. Gasoline pushes hardest and burns faster.',
            ],
            tasks=[item('petrochem:kerosene_bucket', 1)],
            rewards=[give('minecraft:blaze_powder', 8)],
        ),

        # ---- A rocket -----------------------------------------------------------------------------
        Quest(
            slug='rocket', x=4.5, y=18,
            title='A rocket',
            subtitle='Thrusters under a hull',
            icon=item_icon('createpropulsion:solid_fuel_thruster'), shape=LANDMARK, size=1.2,
            deps=['solid_thruster', 'fuel', 'joystick'],
            text=[
                'A rocket is a ship with its thrusters at the bottom, pointing down, and fuel in each. Glue it, assemble it, fuel it, then give the thrusters a signal.',
                'Put a gyroscope in the hull and a joystick at the stand, and it will climb straight. Past the top of the sky it is carried into space like any ship. Take air.',
            ],
            tasks=[dimension('ascension_worlds:space')],
            rewards=[give('ascension_worlds:raw_helium', 4)],
        ),
    ],
)
