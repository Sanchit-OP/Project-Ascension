# The Mark

Sanchit's idea, 2026-10-07. A mark on the player's hand that grows with every gate they light. It
is the spine of the trap: it makes the player feel they are getting closer to solving the
mystery, while they are really being played into opening the gates. Read with
what-the-warden-keeps.md.


## What the player sees

The first time you light a ring, on Earth, something appears on the back of your hand. A small
glyph, in the same glowing teal script that is cut into the rings. It does not wash off. It does
not hurt. You can see it in first person whenever you hold something, and other players can see
it on you.

At the same moment, if you look up at the Moon through a spyglass, a light has appeared on it
that was not there before. Something up there answered.

Every gate you light after that adds one more glyph to your hand. The glyphs sit in a ring, around
a closed eye. With each new glyph, the eye opens a little wider.

There is one glyph for every gate on the road: Earth's, the Moon's, and one for each world after.
With seven gates, the full mark is seven glyphs around an open eye.


## Where else the glyphs appear

The same glyphs are carved everywhere the ancients built.

On the Moon, the tall pylon over the Archive has a band of them near its top. When you lit your
Earth ring, the first glyph on the pylon began to glow. That is the light you saw from Earth.
Every time you light another gate, another glyph on the pylon lights, matching your hand.

Every world's ruins carry the glyph that belongs to that world, over its gate.

The illagers carry the full mark, all seven glyphs around a wide open eye, on their crossed
forearms. You see it from your very first raid. You have no idea what it means. Halfway through
the game, you look at your own hand and realise it is becoming theirs.


## What the player is told

The voice, through the tablets, explains the mark warmly and in detail, and every explanation
makes you feel chosen.

It calls them the Heir's Marks. One for each world reclaimed. Each glyph gets a proud name and a
meaning, something like Awakening for Earth, Return for the Moon, Strength for the burning world.
When all seven shine on your hand, it says, you are the Heir of the ancients, and their Vault will
open to you: everything they built, everything they knew, all their power.

It explains the eye too. The ancients carved an eye on every gate, it says, because they watched
over all the worlds. The eye on your hand is their gaze, passed on to you.

And it explains the illagers: traitors, branded by the destroyer with a false copy of the mark.

So the mark becomes the thing the player chases. It is a progress bar on your own body, and every
glyph feels like a clue solved.


## What it really is

The glyphs are not the ancients' writing at all. They are the Observers' count.

The Sealers tied the seal in the Ancient City to the gates. As long as every gate stayed dark, the
seal held, and each gate was one of its locks. Every gate you light undoes one lock. The mark is
the Observers keeping score, written on the hand of the one doing their work for them.

The eye is not the ancients' gaze. It is theirs. When it is fully open, they can see through you,
and your hand is the key that fits the seal. The Vault the voice keeps promising is the seal
itself.

The pylon on the Moon was not a monument. The Sealers carved that band of glyphs on every pillar
as a warning, the way you would paint a hazard sign. The Sealers' own key to it, hidden in pictures,
reads it plainly: one lock broken, two locks broken, all the way to the last glyph, which is carved
over a picture of the seal open and the world turning to dust. Beside it is a carved hand bearing
the mark, and the hand is crossed out. If you see this on someone, do not let them reach the seal.

That is why the keepers fight you. They were never the destroyer's monsters. They were made to
stop a marked hand.

The symbols never changed. Only the words explaining them did. The Observers can rewrite anything
written in the script, so they rewrote every tablet that explained the pillars, and left the
carvings alone, because a carving means only what someone tells you it means.


## How the horror grows with the mark

The mark is not only a symbol. As the eye opens, the Observers see more of the world through you,
and the world reacts.

- Cats can see the Observers. Early on, cats love you. Around the middle of the game, they start
  watching you strangely. Late in the game, they hiss at you and run, and a cat you tamed may
  leave you. They are not afraid of you. They are afraid of what is looking out of your hand.
- Zombie sieges come more often, and villagers in places you have saved begin to turn.
- Raids find you more easily. The illagers can feel the mark, and they think you are carrying
  the key they have been hunting for.
- The tablets you already read change their words, and they get less careful.

Two things make the mark go dark, and they are clues. In a Moon dust storm, and in the darkness of
the Deep Dark, the mark stops glowing, because those are the two places the Observers cannot see.
A sharp player notices that the mark sleeps exactly where the Sealers hid their truth.


## How it ends

If you follow the voice, you walk into the Ancient City with all seven glyphs lit, put your hand to
the seal, and the eye opens all the way.

If you found Tacita's true signature, you know the order the Sealers meant the glyphs to be read
in. It is not the order you earned them, and it is not the order the voice signs her name in on
every tablet. Trace her name on the seal her way, and the eye closes. The seal shuts from every side at once, because every gate is lit and tied to it. The
gates go dark again, and the glyphs fade from your hand one by one, until it is just your hand.

This is a knowledge puzzle in the Outer Wilds sense. Nothing in your inventory unlocks the good
ending. Only what you have seen does.


## Can we build it

**Built 2026-10-10, and seen live the same day:** the mark on players and illagers, fed by the gate-progress sync. The pylon,
the cats and the orrery come next. The original note:

- The mark is a layer drawn over the player's arm. NeoForge has hooks for both the first-person
  hand and the third-person model, so no mixins are needed.
- The illagers' mark is the same kind of layer on the vindicator, evoker and pillager models.
- How many glyphs to show comes from how many gates are lit, sent from the server to the client.
  The orrery's hologram needs exactly the same data, so it is built once and used twice.
- Cats avoiding a marked player is a behaviour added to cats when they spawn, also without mixins.
- The glyphs and the eye are original art, drawn by our own scripts like everything else.
- Which module owns it is an open question. It needs the gate state, which lives in worlds.
