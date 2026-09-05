# Project-Ascension

A handcrafted Minecraft progression campaign — primitive survival, authored tech eras, physical
spaceflight, and planet-by-planet advancement — built on **custom mods we write ourselves**.

**Target:** Minecraft 1.21.1 / NeoForge / Java 21

---

## Start here

**Resuming work, or picking this up in a new session? Read [`docs/decisions/`](docs/decisions/)
first.** It is the decision register: every locked choice with the reasoning that produced it.
It is the fastest path to full project context, and it is what stops settled questions being
relitigated.

Then:

| | |
|---|---|
| [`docs/vision.md`](docs/vision.md) | Pillars and non-negotiable rules |
| [`docs/technical/architecture.md`](docs/technical/architecture.md) | Module map |
| [`docs/technical/dev-environment.md`](docs/technical/dev-environment.md) | Toolchain and commands |
| [`plans/`](plans/) | Current milestone plans |
| [`todo.md`](todo.md) | What is still open |

## What this is

Not a curated modpack. We author the Java, because the design requires three things existing
mods cannot do: ships that are real play spaces, an oxygen system with zones and logistics, and
bosses that resist one-note strategies ([ADR-0002](docs/decisions/0002-custom-mods-not-curated-modpack.md)).

Everything is built as **separately publishable, independently usable modules**. No module
hard-depends on a third-party mod; all foreign integration lives in optional connector jars
([ADR-0003](docs/decisions/0003-modular-architecture-and-compatibility-policy.md)).

## Repository structure

```
docs/decisions/     Decision register (ADRs) — read this first
docs/               Game design documentation
plans/              Milestone plans (M0, M1, ...)
design/             Diagrams and supporting design artifacts
modules/            Mod source (from M0 onward)
roadmap.md          Milestone overview
todo.md             Open questions
```

## How we work

Four rules, from [ADR-0007](docs/decisions/0007-performance-contract.md) and
[ADR-0008](docs/decisions/0008-build-and-test-cadence.md):

1. **Compatibility first** — modules must be usable standalone by other people.
2. **Performance is a hard requirement**, enforced at review time, not a later pass.
3. **Every third increment is a refactor pass**, scheduled — not done when it feels needed.
4. **Nothing is done until it has been seen working in a running Minecraft client.** Compiling
   is not evidence.

## Current status

M0 (toolchain and skeleton) not yet started. No implementation code exists yet.
JDK 21 is installed and verified.
