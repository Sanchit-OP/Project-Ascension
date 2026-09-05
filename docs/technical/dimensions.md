# Dimensions

> **Updated 2026-09-05.** Settled by
> [ADR-0004](../decisions/0004-fully-custom-dimensions.md): we author **fully custom
> dimensions**, no Ad Astra dependency, and planets are **data-driven JSON** rather than
> code-per-planet. `ascension-worlds` owns this.
>
> Per [ADR-0005](../decisions/0005-v1-scope-vertical-slice.md), v1 builds **Earth + Moon +
> Planet 3** only.
>
> **Resolved 2026-09-05 by [ADR-0010](../decisions/0010-orbit-is-one-shared-space-dimension.md):
> orbit is neither.** It is a single shared interplanetary space dimension, with planets as
> destinations within it. Four dimensions for v1 instead of six, eight instead of fourteen at
> seven worlds — and the only option where travel between planets is a journey rather than a
> transition, which is the pillar ADR-0004 rejected Ad Astra to protect.
>
> The assumptions below are written against orbit-per-planet and are **stale**. The API design
> is [`worlds-api.md`](worlds-api.md).

## Purpose

Defines the dimension model for planets, orbit layers, and transit spaces.

## Current Assumptions

- Each major planet has a surface dimension.
- Each major planet has an orbit dimension.
- Players arrive in orbit first and descend manually.
- Rift Gates connect orbital spaces after discovery.
- Optional orbital content can include stations, wrecks, or meteor events.
- Earth is a controlled starting dimension with reduced default progression routes.

## Questions To Resolve

- Is interplanetary space a navigable dimension, a set of routed orbit layers, or both?
- Where do ancient gateways physically exist?
- How are failed landings handled?
- How do orbital side destinations spawn, persist, or reset?

## Open Questions

- Which dimensions are mandatory for version one?
- What technical limits exist around ship behavior in orbit?
- How expensive should dimension travel be after mastery?
