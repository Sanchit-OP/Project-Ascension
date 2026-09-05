# ADR-0008: Build-and-test-in-game cadence

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Sanchit's requirement: build something, test it in game, gain confidence in the code, and
debug at early steps rather than discovering problems during a large integration. Plus:
refactor every few steps so technical debt does not accumulate.

Minecraft modding punishes the alternative unusually hard. Code that compiles cleanly can
still fail on dedicated servers, desync in multiplayer, leak on dimension unload, or crash
only on world reload. None of that is visible without actually running it.

## Decision

### Increment rule

**No increment is complete until it has been observed working in a running Minecraft client.**
Compiling is not evidence. Unit tests are not evidence for anything touching world state.

Every increment defines its in-game verification *before* implementation starts. If a change
cannot be observed in game, it needs a temporary debug affordance (a command, an overlay, a
test block) so that it can be.

### Dedicated server check

Anything touching world state, sync, or persistence is verified on a **dedicated server**, not
only in single-player. Single-player runs an integrated server and hides an entire class of
side-only bugs.

### Reload check

Every increment touching persistent or level-scoped state is verified across a
**world unload and reload**, and across a **dimension change**. This catches leaks and
persistence bugs at the moment they are introduced, and feeds ADR-0007 rule 12.

### Refactor cadence

**Every third increment is a refactor pass** with no new features: naming, duplication,
module boundaries, dead code, and API surface review. It is a scheduled step in the milestone
plan, not something done when it feels needed — because it never feels needed at the time.

### Definition of done

An increment is done when all of the following hold:

- [ ] Builds clean, no new warnings
- [ ] Verified in a running client
- [ ] Verified on a dedicated server, if it touches world state or sync
- [ ] Survives world unload / reload, if it touches persistent state
- [ ] No new heap retained after reload (ADR-0007 rule 12)
- [ ] Public API surface reviewed if `api` packages changed
- [ ] Committed with the verification noted in the commit message

## Consequences

- Visibly slower per increment. This is the point.
- Debug affordances become part of the codebase and need their own hygiene — they live behind a
  dev flag and never ship enabled.
- The dedicated-server requirement means the dev environment must support launching one easily.
  This is part of M0.
