# Project-Ascension — working context

A handcrafted Minecraft progression campaign built on **custom mods we write ourselves**.
Primitive Earth survival → engineered spaceflight → planet-by-planet conquest, repairing an
ancient gateway network underneath it all.

## Read this first

**[`docs/decisions/`](docs/decisions/) is the decision register.** Read it before making any
structural choice. It records what is settled *and why*. `todo.md` records what is still open.

If you disagree with a locked decision, do not silently work around it — supersede it with a
new ADR.

## Locked facts

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x |
| Java | Temurin JDK 21 — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Modid prefix | `ascension` |
| v1 scope | Earth + Moon + Planet 3, finished |

**Trap:** system `java` on PATH is a Java 8 JRE and `JAVA_HOME` is unset. The build pins
`org.gradle.java.home`. Unexplained "unsupported class file version" errors trace back to this.

## What this project is not

It is **not a curated modpack**. `docs/technical/mod-list.md` is a landscape survey, not a
dependency list. Ad Astra, Mekanism, Cataclysm and friends are reference points — we do not
depend on them. See ADR-0002.

## Architecture rules (ADR-0003) — these are hard

1. **No Tier-1 module may hard-depend on any third-party mod.** All foreign integration lives
   in optional Tier-2 `ascension-compat-*` jars.
2. **Mixins never appear in Tier-1.** Tier-2 only, with an ADR justifying it.
3. **Extension via provider registries**, never by requiring anyone to extend our classes.
4. `api` packages are a semver contract with third parties. `internal` is free to change.
5. Every Tier-1 module must load and be testable with only `core` present.

Modules: `core` → `atmosphere`, `worlds`, `gear`, `progression` → `compat/*`.
Map: [`docs/technical/architecture.md`](docs/technical/architecture.md).

## Performance rules (ADR-0007) — checked at review

- **No static or long-lived collection holds a `Level`, `Player`, `Entity`, or `BlockEntity`.**
  Store `ResourceKey` / `UUID` / `BlockPos` and resolve on demand. This is the leak class that
  kills Minecraft mods.
- Every listener, capability and cache has a defined teardown hooked to level unload.
- No per-tick full-world or full-chunk scans. Event-driven and dirty-flagged.
- Server-authoritative, delta-synced. No per-tick packets.
- Data-driven over hardcoded — adding a planet must not mean adding a class.

## Cadence (ADR-0008)

- **Nothing is done until it has been observed working in a running Minecraft client.**
  Compiling is not evidence. Unit tests are not evidence for anything touching world state.
- Anything touching world state or sync is verified on a **dedicated server**, not just
  single-player.
- Anything persistent is verified across **world unload/reload**.
- **Every third increment is a refactor pass** with no new features. Scheduled, not
  discretionary.
- Every milestone ends with a recorded measurement in `docs/technical/performance-log.md`.

## Current state (2026-09-05)

Design and decisions locked. **No implementation code exists yet.** JDK 21 installed and
verified. Next: [M0](plans/m0-toolchain-and-skeleton.md), then
[M1](plans/m1-atmosphere.md).

## Style

Sanchit's stated priorities, in order: compatibility (mods usable standalone by others, acting
as connectors between mods) → performance (no excess RAM, no leaks) → slow and steady with
frequent refactors → test in game at every step. Do not trade the first two for speed.
