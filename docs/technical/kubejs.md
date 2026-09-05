# KubeJS

## Purpose

Defines what should be solved through scripting and what should not.

## Suitable For

- Recipe gating
- Loot tables
- Quest synchronization
- Item progression tags
- Simple progression checks

## Poor Fit

- Complex movement systems
- Deep custom UI systems
- Low-level dimension behavior
- Persistent mechanics that need tight performance control
- A fully custom oxygen system with heavy simulation logic

## Likely Custom-Mod Territory

- Oxygen systems with suit, zone, and vehicle integration
- Rift or gateway state tracking
- Planet progression capability checks beyond simple recipes or loot
- Special suit modules and advanced gear behaviors

## Open Questions

- Which progression checks must be script-driven?
- Which mechanics require a custom mod instead?
- How much logic are we comfortable maintaining in scripts?
