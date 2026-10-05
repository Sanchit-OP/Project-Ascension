# ADR-0014: Vanilla Nether and End portals are dormant

- **Status:** Accepted
- **Date:** 2026-10-06

## Context

The campaign's travel is spaceflight between planets (ADR-0010) and, later, the gateway network
(`plans/m3-vertical-slice.md`, M3.2). Vanilla's Nether and End portals are a second, older travel
system that skips all of it: a few obsidian blocks and a flint and steel open a whole other
world on day one.

Sanchit's call, 2026-10-05: the Nether and the End are meant to become **planets** in this
campaign, reached the way every other planet is, not through vanilla's portals. The dedicated
server already had `allow-nether=false`, but that is a server-only switch, does nothing in
single-player, and does not touch the End.

## Decision

`ascension-worlds` makes vanilla's Nether and End portals dormant, behind a server config switch
`vanilla.disableVanillaPortals` (default **on**), in `internal/VanillaPortals`:

- A Nether portal frame never lights: `BlockEvent.PortalSpawnEvent` is cancelled.
- Any **portal-driven** trip into the Nether or the End is cancelled
  (`EntityTravelToDimensionEvent`), with a short message to players. "Portal-driven" means the
  entity's `portalProcess` is set, which vanilla only does while it stands in a portal block. That
  catches the End portal (which has no creation event to cancel), pre-existing Nether portals,
  and portals other mods create.

Operator commands (`/execute in`, `/tp`) and this project's own travel never set
`portalProcess`, so they are unaffected. The day the Nether becomes a planet, planet travel into
it is not caught by this.

## Why a config switch, not a hard removal

Same reasoning as `AtmosphereConfig.waterIntegration`: these modules are meant to be adoptable by
people not building Project-Ascension (ADR-0003, and Sanchit's first stated priority), and silently
removing a core vanilla mechanic would make the module untrustworthy. Default on for us; one line
off for anyone else.

## Consequences

- **Everything only the Nether or the End provides is unobtainable in survival until those planets
  exist.** This is the "Nether removal has an uncosted recipe debt" item in `todo.md`, now live
  rather than hypothetical. Known instances so far:
  - the **gateway keystone's recipe** uses a lodestone, i.e. a netherite ingot
    (`gateway-portal-design.md`), so Earth's player-built gateway cannot currently be crafted;
  - the frame's "Moon-faithful" trim uses nether quartz (the "budget" materials avoid it);
  - blaze rods (brewing, Eyes of Ender), and End-only items such as elytra and shulker boxes.
- Strongholds still generate and their End portal frames can still be filled; the portal simply
  does not carry anyone. Not worth suppressing structure generation for.
- Any future mod content that travels to the Nether/End through its own portal block is blocked
  too, which is the intent.

## Alternatives rejected

- **`allow-nether=false` alone.** Server-only, ignores the End, does nothing in single-player.
- **Cancel all travel into those dimensions, commands included.** Locks operators out of
  dimensions they may need to inspect, and would have to be unpicked when they become planets.
- **Remove the dimensions entirely.** Their terrain and mobs are useful raw material for the
  planets they are meant to become.
