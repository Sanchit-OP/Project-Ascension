# Architecture

Living map of what we build. The *reasoning* behind this shape is
[ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md); this file
tracks the current state.

## Shape

Every module is a **separately publishable NeoForge mod jar with its own modid**. Someone can
take one module and use it without the rest.

```
Tier 0   ascension-core            NeoForge only. Infra, no gameplay.
           |
Tier 1   ascension-atmosphere      Breathable zones + oxygen.  <- flagship, reusable
         ascension-worlds          Planets, orbit, gateways.   (data-driven)
         ascension-gear            Suit modules, hazard protection.
         ascension-progression     Team capability state, gating.
           |
Tier 2   ascension-compat-sable    Sub-levels carry pressurised zones -> ships as habitats
         ascension-compat-create   Connector
         ascension-compat-jei      Connector
         ascension-compat-ftbquests Connector
```

## Dependency rules

| Rule | |
|---|---|
| Tier 1 -> third-party mod | **Never.** All foreign integration lives in Tier 2. |
| Tier 2 optional at runtime | Yes. Absent compat jar must not degrade Tier 1. |
| Mixins in Tier 1 | **Never.** Tier 2 only, with an ADR justifying it. |
| Extension mechanism | Provider registries. Never require extending our classes. |
| Tier 1 testable alone | Yes, with only `core` present. |

## Package convention

```
com.ascension.<module>.api.*        stable, semver, third-party contract
com.ascension.<module>.internal.*   free to change, no external guarantees
```

If a third party has to import from `internal`, the `api` is wrong.

## Build

Gradle multi-project, NeoForge ModDevGradle, versions centralised in
`gradle/libs.versions.toml`, shared config in a `buildSrc` convention plugin so per-module
build files stay near-empty.

```
settings.gradle.kts
gradle/libs.versions.toml
buildSrc/                     convention plugin
modules/
  core/
  atmosphere/
  worlds/
  gear/
  progression/
  compat/
    sable/
    create/
run/                          dev client/server, gitignored
```

## Current status

| Module | Status |
|---|---|
| `ascension-core` | Not started — M0 |
| `ascension-atmosphere` | Not started — M1 |
| everything else | Not started |

## Constraints inherited from the register

- [ADR-0001](../decisions/0001-target-minecraft-version-and-loader.md) — MC 1.21.1, NeoForge, Java 21
- [ADR-0006](../decisions/0006-sable-integration-posture.md) — atmosphere zones must be able to
  ride a moving region, or the Sable ship bridge becomes impossible later
- [ADR-0007](../decisions/0007-performance-contract.md) — binding performance rules on all code
