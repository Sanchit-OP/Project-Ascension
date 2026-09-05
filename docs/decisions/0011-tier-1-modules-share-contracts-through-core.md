# ADR-0011: Tier 1 modules share contracts through `core`

- **Status:** Accepted
- **Date:** 2026-09-05
- **Amends:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md), which does not
  cover Tier 1 → Tier 1 dependencies

## Context

ADR-0003 has rules for Tier 1 → third-party (never), Tier 1 → Tier 2 (optional at runtime), and
Tier 1 standalone (must load with only `core` present). It says nothing about **one Tier 1
module depending on another**, because when it was written only one Tier 1 module existed.

`ascension-worlds` is the first case and it is not an edge case. A planet has an atmosphere.
`worlds` knows a planet is airless; `atmosphere` is the module that makes airless mean something.
They have to agree about it somehow, and ADR-0003 rule 6 forbids the obvious answer — `worlds`
cannot hard-depend on `atmosphere`, because it must work with only `core` present.

This will recur immediately: `gear` → `atmosphere` for suit modules, `progression` → `worlds`
for dimension gating. Whatever is decided here is the pattern for all of them, so it is worth
deciding once rather than three times.

## Decision

**Tier 1 modules do not depend on each other. They share contracts defined in `ascension-core`.**

`core` defines the vocabulary — interfaces, records, registry keys, codecs — that more than one
Tier 1 module needs to agree about. `worlds` writes into it; `atmosphere` reads out of it;
neither knows the other exists.

### What `core` may contain

`architecture.md` said *"NeoForge only. Infra, no gameplay."* That is now too narrow, so the
line moves to a place that can actually be defended:

| `core` may hold | `core` may not hold |
|---|---|
| Interfaces and records describing shared concepts | Any behaviour that implements a concept |
| Registry keys and codecs for shared data | Blocks, items, entities, commands |
| Attachment types more than one module reads | Event handlers that make something happen |

The test is: **does removing every Tier 1 module leave `core` doing nothing observable?** If
`core` alone changes the game, gameplay has leaked into Tier 0.

`core` still depends on nothing but NeoForge.

## Why this over a soft dependency

A soft dependency — `worlds` compiling against `atmosphere`'s `api` as `compileOnly` and
registering a provider only when it is loaded — also satisfies rule 6, needs no new policy, and
reuses the Tier 2 pattern pointed inward. It was the cheaper option and it was not chosen.

**Because `core` serves the project's first priority better.** The stated order is compatibility
first: modules usable standalone by other people, acting as connectors between mods. A contract
in `core` means a **third party** can describe a world's environment without depending on
`ascension-worlds` at all — someone else's dimension mod can implement it and our atmosphere
module will honour it. A soft dependency on `atmosphere`'s api only helps *our* modules
cooperate; a contract in `core` makes the contract itself the product.

It also keeps the dependency graph acyclic and shallow by construction. With soft dependencies,
`worlds` → `atmosphere`, `gear` → `atmosphere`, `progression` → `worlds` accumulates into a web
where "can this module be built alone" needs checking each time. With `core`, every Tier 1
module has exactly one dependency, forever.

## Consequences

- **`core` stops being an empty stub.** It has been a placeholder since M0; this is the first
  thing that genuinely belongs in it, and it now has a real API surface with the semver
  obligations that implies.
- **ADR-0003's dependency table gains a row**, and `architecture.md`'s description of Tier 0
  changes with it.
- **A contract in `core` is more expensive to change than one in a module**, because everything
  depends on it. That friction is deliberate and it is the same argument that froze
  `atmosphere`'s api before implementation started (M1.1). Contracts go in `core` when two
  modules genuinely need them — not in advance, on the guess that they might.
- **`core` needs its own tests and its own review**, because "it is just infrastructure" is no
  longer true.
- Any concept only one module cares about stays in that module. `core` is not a dumping ground
  for anything that feels shared.

## Alternatives rejected

**Soft dependency between Tier 1 modules.** Cheaper, needs no new policy, and reuses a pattern
we already trust. Rejected for the compatibility reason above: it makes our modules cooperate
without making the contract available to anyone else, and the whole point of this architecture
is that other people can use the pieces.

**A Tier 2 bridge between our own modules.** Fully consistent with ADR-0003 as written and
requires no amendment at all. Rejected as ceremony: a third jar to build, version and ship for
something both modules already agree about, and it would leave the pattern question unanswered
for `gear` and `progression`.
