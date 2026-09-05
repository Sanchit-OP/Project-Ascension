# ADR-0003: Modular architecture and compatibility policy

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Sanchit's stated first priority: the mods we build must be usable by other people as
standalone modules, and should act as connectors between other mods. A monolithic
`ascension.jar` that only makes sense inside this modpack fails that outright.

This is not only altruism. Modules with hard external dependencies are also the ones that
break on every upstream update, and the ones that cannot be tested in isolation. The
compatibility goal and the maintainability goal point the same direction.

## Decision

### Three tiers

| Tier | Module | Depends on | Purpose |
|---|---|---|---|
| 0 | `ascension-core` | NeoForge only | Thin cross-cutting infra: soft-dependency service loading, sync helpers, perf instrumentation. **No gameplay.** |
| 1 | `ascension-atmosphere` | core | Generic breathable-zone and oxygen API. The flagship reusable module. |
| 1 | `ascension-worlds` | core | Planet / orbit / gateway framework, data-driven. |
| 1 | `ascension-gear` | core, atmosphere | Suit modules, hazard protection. |
| 1 | `ascension-progression` | core | Team capability state and gating hooks. |
| 2 | `ascension-compat-sable` | atmosphere + Sable | Sub-levels carry pressurised zones: ships as habitats. |
| 2 | `ascension-compat-create` | varies | Connector. |
| 2 | `ascension-compat-jei`, `-ftbquests`, ... | varies | Connectors. |

Each module is a **separately publishable NeoForge mod jar with its own modid**. Someone can
take `ascension-atmosphere` alone and use it.

### Rules

1. **No Tier-1 module may hard-depend on any third-party mod.** Ever. Third-party integration
   lives exclusively in Tier-2 jars.
2. **Tier-2 modules are optional at runtime.** If the foreign mod is absent, the compat jar
   does nothing or is not installed. Tier-1 behaviour must not degrade.
3. **Every Tier-1 module has a stable public `api` package and a sealed `internal` package.**
   `api` follows semantic versioning and is treated as a contract with third parties.
   `internal` may change freely.
4. **Extension happens through provider registries, not inheritance or patching.** Any mod can
   register an `AtmosphereProvider` or an `OxygenSource`. We never require anyone to extend our
   classes.
5. **Mixins are a last resort** and never appear in Tier-1 modules. If a feature needs a mixin,
   it belongs in a Tier-2 jar and the ADR justifying it must say why no API path exists.
6. **Tier-1 modules must be loadable and testable with no other Ascension module present**,
   apart from `core`.

### Naming

Modid prefix `ascension` (confirmed 2026-09-05). Public-facing modules keep the prefix; the
API packages are the part third parties actually couple to.

## Consequences

- More build wiring up front: a Gradle multi-project rather than one module.
- `ascension-atmosphere` is deliberately designed to be adoptable by people who do not care
  about this modpack. It is the piece the ecosystem most visibly lacks.
- Integration work is additive. Adding Create support never touches Tier-1 code.
- Testing is genuinely isolable, which is what makes ADR-0008's cadence affordable.
- Some duplication between compat jars is accepted as the cost of decoupling.

## Alternatives rejected

- **Single monolithic mod** — simplest to build, fails the primary requirement.
- **Hard-depending on Sable/Create in Tier 1** — would make ships easier now and make every
  upstream update a crisis later. See ADR-0006.
