# ADR-0009: Carry limits are enforced on the player, not on containers

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Oxygen tanks are a consumable that gates exploration. The intended pressure is: air runs out,
so you must build refill infrastructure, or better tanks, at the places you want to operate.

That pressure evaporates if a player can carry a bag of spare tanks and swap through them.
Sanchit's requirement was explicit: *"you can't just get 10 tanks to hack things out — so get
better tanks or have a way to keep refilling."* The proposed mechanism was to restrict where a
tank may be stored: a dedicated Curios slot to make it work, one tank in the inventory, and
tanks blocked from backpack mods.

The storage-restriction half of that does not survive contact with Minecraft.

- **Shulker boxes are vanilla.** Twenty-seven tanks fit in one, in the player's own inventory,
  with no mod involved at all. Whatever we do to backpacks, this hole remains.
- **Every backpack mod is a separate integration.** Some expose a blacklist, some need a mixin
  (Tier 2 only, ADR-0003 rule 2), and each one is a jar we have to keep alive across their
  updates. That is an unbounded maintenance commitment for a partial result.
- **Ender chests, item pipes, armour stands, other players.** The list of places an item can be
  parked does not end.

Chasing containers is a fight that costs a permanent maintenance burden and still loses.

## Decision

**Carry limits are properties of the player, not of any container.**

### 1. Only an open tank supplies air

A tank has a valve. Closed, it is inert: it supplies nothing and drains nothing. Only one tank
may be open at a time, enforced server-side.

This is a rule about the player's oxygen supply, so it lives in Tier 1 and works with no other
mod installed. A carried tank being inert is not a restriction on storage — it is the tank's
own state, and it travels with the item.

### 2. Opening a valve costs time

A freshly opened tank delivers nothing for `TANK_PRESSURISE_TICKS` (currently 3 seconds).

**This is the mechanism that actually closes the exploit**, and it is the reason the rest of it
is allowed to be leaky. A swap that costs real seconds inside a failure window does not care
whether the spare came from a backpack, a shulker box, an ender chest or a teammate's hand.
Ten spares stop being ten tanks of air and become ten swaps you have to survive.

The timer is stored on the player and **serialised**. A transient timer would be cleared by
logging out and back in, which is a bypass a player would find in an afternoon.

### 3. The inventory cap is a nudge, not a wall

At most `MAX_TANKS_CARRIED` (currently 2 — one on the valve, one spare) in the player's own 41
slots. Extras are dropped, and picking one up past the cap is refused.

This is honest about what it is: it makes carrying a stack of tanks *inconvenient*, and it
communicates the design intent at the moment a player first tries. It is not the thing keeping
the game honest. Rule 2 is.

### 4. Curios is an optional front-end, never the mechanism

`ascension-compat-curios` may add a dedicated tank slot where equipping opens the valve and
unequipping closes it. That is a nicer interface to the same rule — not a new rule.

**Tier 1 must never require it** (ADR-0003 rule 6). A version of this design where a tank only
works in a Curios slot would make `ascension-atmosphere` unusable standalone, which is exactly
backwards for the module we most want other people to adopt.

## Consequences

- The design goal is met without a single third-party dependency.
- We do not ship, and will not accept, per-backpack-mod blocking jars whose only job is to
  forbid storage. If a backpack mod exposes a blacklist, documenting it is fine; writing code to
  enforce it is not.
- A player who really wants to carry twenty tanks can. They will spend three seconds per swap
  doing it, which is the cost we actually wanted to charge.
- The valve doubles as the answer to a separate problem found in M1.7 testing: a tank quietly
  emptying itself because its owner swam across a river. Stow it and it is safe.

## Alternatives rejected

**Block tanks from every container.** Rejected above: unbounded maintenance, defeated by a
vanilla shulker box.

**Only the equipped tank exists; spares are impossible.** Would need a Tier-1 slot of our own,
which means a menu, a screen and sync for something Curios already does better in the packs that
have it. And it deletes the "carry a spare for a long trip" decision, which is a good decision.

**Make tanks heavy — slowness for carrying more than one.** Mod-agnostic in appearance only: it
reads the same 41 slots and is defeated by the same shulker box, while also being a worse feel.
