# Package 4 Coordination Log

Owner: Huanyu Zhang (Henry)

This file records Package 4 decisions, dependencies and changes that may affect another contributor's area. Entries should be updated before integration work is merged.

## Phase 2 decisions

- Persist timestamps as UTC epoch milliseconds. Convert to local dates only when applying a report query's `timeZoneId`.
- Store duration in seconds, distance in metres and pace as a derived nullable value. Zero distance produces no pace.
- Use stable caller-generated record IDs so retrying a save can be idempotent.
- Accept owner-free drafts at the save boundary. The repository injects `ownerId` from `ActiveOwnerProvider`; UI and feature packages cannot choose an owner.
- Keep persistence and reporting behind `RecordRepository`; screens must not access Room DAOs directly.
- Use `Flow` for observable history and structured `StorageResult` failures for loading/error handling.

## Phase 3 decisions

- Use Room 2.8.5 with KSP 2.3.10, matching the project's Kotlin 2.2.10, AGP 9 and Gradle 9 toolchain.
- Use separate `activity_records` and `health_check_in_records` tables with indexed `ownerId` and `(ownerId, recordedAtEpochMillis)` columns.
- Use `@Upsert` with `(ownerId, id)` composite primary keys for account-scoped idempotent retries and collision safety.
- Require `ownerId` in every DAO read and history query. A record ID alone is never sufficient to read account data.
- Observe the active owner as a `Flow`; history immediately emits `SIGNED_OUT` instead of retaining the previous account's records after logout.
- Keep Room entities inside `data.local`; feature modules only receive domain models from `RecordRepository`.

## Changes to files originally introduced by another contributor

| File | Original area | Package 4 change | Reason | Risk |
|---|---|---|---|---|
| `gradle/libs.versions.toml` | Frontend scaffold (Yan Yu) | Add Kotlin Coroutines version and library alias | `RecordRepository.observeHistory` returns `Flow` | Low; additive dependency only |
| `app/build.gradle.kts` | Frontend scaffold (Yan Yu) | Add Coroutines Core dependency | Compile the repository contract | Low; no existing dependency changed |
| `build.gradle.kts` | Frontend scaffold (Yan Yu) | Register the KSP plugin without applying it globally | Generate Room implementations in `:app` | Low; additive plugin alias |
| `gradle/libs.versions.toml` | Frontend scaffold (Yan Yu) | Add Room 2.8.5 and KSP 2.3.10 aliases | Build the Package 4 database layer | Low; additive dependencies only |
| `app/build.gradle.kts` | Frontend scaffold (Yan Yu) | Apply KSP and add Room runtime, KTX, compiler and testing dependencies | Build and test Room persistence | Low; no existing dependency changed |

No existing screen, navigation, UI model or test file is modified in Phase 2.

## Cross-package dependencies and possible conflicts

### Package 1 — Zarif

- Package 4 defines `ActiveOwnerProvider` as its integration seam, with observable and one-shot owner access.
- Zarif's authentication implementation must supply the active account ID and a signed-out state.
- Possible conflict: Package 1 may introduce a different user/session abstraction. Resolve by adapting that abstraction to `ActiveOwnerProvider`, not by coupling Room to authentication-provider types.

### Package 2 — Iris

- Today and History UI will consume `ActivityReport` and `observeHistory` later.
- No Package 2 code is changed yet.
- Possible conflict: Package 2 may choose different loading/empty/error UI models. Keep those UI models outside the repository and map from `StorageResult`.

### Package 3 — Cassi

- `ActivityRecord` currently supports Walking, Running and Hiking, matching the backlog and scaffold.
- Cassi must provide a stable record ID, UTC completion timestamp, duration in seconds and distance in metres to Package 4.
- Possible conflict: the current Activity screen stores activity type and goals as display strings. An adapter will be required; Package 4 will not change the live-session state machine without coordination.

### Package 5 — Yan Yu

- The existing UI enum `WellbeingStatus` overlaps with the new domain enum `HealthCondition`.
- Package 4 deliberately does not import a UI-layer model into persistence. Package 5 should map `WellbeingStatus` to `HealthCondition` at the integration boundary.
- `Feeling` and `SymptomLevel` require confirmation from Package 5 before database schema freeze.
- Package 4 stores the condition supplied by Package 5; it does not calculate or diagnose it.

### Package 6 — Ricky

- Package 4 stores completed activity records only.
- Watch commands and live-session synchronization remain owned by Package 6 and Package 3.
- No Package 6 code is changed yet.

## Items requiring team confirmation before Room schema freeze

- Confirm the canonical authenticated user ID format with Zarif.
- Confirm whether activity goals need persistence with Cassi.
- Confirm the final feeling, breathing and fatigue value sets with Yan Yu.
- Confirm whether a single combined history or separate activity/health histories are preferred by Iris.
- Confirm whether deleted records are out of scope or require soft-delete metadata.

## Verification blockers

- The project currently requests Android SDK 37.0 in `app/build.gradle.kts`, while the available local SDK is 36.1. Gradle reaches project configuration but cannot run compilation or tests until SDK 37.0 is installed or the team intentionally changes the compile/target SDK. Package 4 does not change this shared SDK decision without team agreement.
