# Wear UI and command verification — 11 October 2026

## Scope

Verified in the `feature/watch-companion` checkout, based on `a1bcb6e`, after
transferring only the watch UI/controller/transport changes from the local
integration checkout. No teammate phone screen, production activity adapter,
Supabase migration or repository build setting was changed in this update.

Changes include the forest/mint Wear UI, metrics card, expanded type/goal setup,
Finish confirmation, collapsed demo tools, safe demo exit, and stricter reply
validation. Invalid or uncertain replies do not create optimistic session state.

## Executed checks

| Check | Result |
|---|---|
| Shared phone command dispatcher JVM tests | 19 passed |
| Watch controller JVM tests | 7 passed |
| Reply validation and mode reset JVM tests | 13 passed |
| Setup options and time formatting JVM tests | 4 passed |
| Wear UI instrumentation, Android 14/API 34 emulator | 10 passed |
| Wear debug APK build (`watchQa` variant) | Passed |
| Wear debug lint | No issues found |

All 43 JVM tests and 10 instrumentation tests had zero failures/errors/skips.
The instrumentation suite includes setup/goal selection, activity lifecycle,
Finish cancellation/confirmation, disconnection/retry, sign-out, demo tools and
guarded mode exit, and real-unsaved completion labelling.

Portable commands (from the repository root):

```sh
./gradlew :watch-shared:testDebugUnitTest :wear:testDebugUnitTest \
  :wear:assembleDebug :wear:lintDebug -PwatchQa=true
ANDROID_SERIAL=<wear-serial> ./gradlew :wear:connectedDebugAndroidTest -PwatchQa=true
```

For this machine's Desktop/iCloud duplicate-output workaround, verification used
a temporary, uncommitted Gradle init script to redirect build outputs to
`/Users/dell/Library/Caches/TrailwiseWatchBranchQA/2026-10-11`. The repository's
build settings are unchanged. Device: `Trailwise_Wear_QA`, `emulator-5554`.

## What these results do not prove

- The executable local demo is synthetic: no GPS, no real account and no database
  save. A completed demo is visibly labelled **Not saved · demo only**.
- Paired phone/watch Data Layer delivery was not exercised in this run. The
  branch retains the independent debug phone bridge demo for a future pairing
  test; this is not the team's login/activity screen.
- The real phone Auth, tracking and persistence gateway is not installed. The
  production receiver fails closed with `NOT_READY` until that adapter exists.
- Watch sensors, physical-watch ergonomics, battery use and accessibility on
  real hardware were not verified. Watch metrics are supplied by the gateway.
- Deduplication is bounded and in-memory, not durable/global exactly-once.
- These branch-local checks are not proof of a merged `main`, a fully integrated
  team app, final APK packaging or assignment submission.

Historical 9 October results remain in [WATCH_VERIFICATION.md](WATCH_VERIFICATION.md).
Team adapter requirements and demo instructions remain in [WATCH_HANDOFF.md](WATCH_HANDOFF.md).
