# Roadmap

Target: **Minecraft 1.21.1 / NeoForge / Java 21** ([ADR-0001](docs/decisions/0001-target-minecraft-version-and-loader.md))

v1 scope: **Earth + Moon + Planet 3, finished** ([ADR-0005](docs/decisions/0005-v1-scope-vertical-slice.md))

---

## Pre-production — complete (2026-09-05)

- [x] Lock vision pillars
- [x] Lock technical responsibility map — custom mods, not a pack (ADR-0002)
- [x] Lock target version and loader (ADR-0001)
- [x] Lock module architecture and compatibility policy (ADR-0003)
- [x] Lock planet framework approach — fully custom dimensions (ADR-0004)
- [x] Lock v1 scope (ADR-0005)
- [x] Lock performance contract (ADR-0007) and build/test cadence (ADR-0008)
- [ ] Lock the mandatory planet order and identities
- [ ] Lock combat and boss unlock philosophy in implementable terms

## Foundation

- [ ] **[M0 — Toolchain and skeleton](plans/m0-toolchain-and-skeleton.md)**
      Build → launch → test loop proven end to end, with a recorded performance baseline.
- [ ] **[M1 — `ascension-atmosphere` v0.1](plans/m1-atmosphere.md)**
      Oxygen that is genuinely felt, testable entirely in the vanilla Overworld.

## Systems

- [ ] M2 — `ascension-worlds`: planet framework, orbit model, first custom dimension
- [ ] M3 — `ascension-progression`: team capability state and gating
- [ ] M4 — `ascension-gear`: suit modules, hazard protection
- [ ] M5 — `ascension-compat-sable`: sub-levels as pressurised habitats

Ordering after M1 is provisional and will be revisited once M1 exposes what the API actually
needs.

## Vertical slice

- [ ] Constrained Earth: primitive survival through first-flight readiness
- [ ] One full orbit-to-surface loop
- [ ] One authored boss unlock
- [ ] One restored gateway / repeat-travel shortcut
- [ ] Moon and Planet 3 built out
- [ ] Multiplayer progression testing

## Production

- [ ] Expand to the remaining critical-path planets
- [ ] Optional side locations
- [ ] Full content balance passes
