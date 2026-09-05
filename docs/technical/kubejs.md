# KubeJS and Scripting

> **Reframed 2026-09-05 by [ADR-0002](../decisions/0002-custom-mods-not-curated-modpack.md).**
> This document previously treated "which mechanics require a custom mod?" as an open
> question. It is now settled: we write Java for core systems. Scripting is a pack-level
> tuning layer, not a foundation.

## Purpose

Define the boundary between scripted pack tuning and authored Java systems.

## The boundary

**Java (Ascension modules)** — anything that owns state, ticks, syncs, or defines an API:

- the atmosphere and oxygen system (`ascension-atmosphere`)
- planets, orbit, and gateway state (`ascension-worlds`)
- suit modules and gear behaviour (`ascension-gear`)
- team capability and progression state (`ascension-progression`)
- boss behaviour, wherever authored combat is required

**Scripting / datapacks** — pack-level content and tuning that must be editable without a
rebuild:

- recipe gating and progression recipe trees
- loot tables
- quest wiring
- tags
- balance numbers exposed deliberately by our own modules

## Rule

If a behaviour needs to persist, tick, sync across the network, or be extended by another mod,
it is Java. If it is content that a pack author might reasonably want to retune, it is data.

Our own modules should **expose data hooks** (JSON, tags, recipe types) rather than forcing
consumers into scripts — that is also what makes them reusable per
[ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md).

## Open questions

- Do we ship KubeJS in the final pack at all, or expose our own datapack surface and skip it?
- Which balance numbers do we deliberately expose as data versus keep authored in code?
