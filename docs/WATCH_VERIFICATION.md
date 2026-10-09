# Watch verification record — 9 October 2026

Branch: `feature/watch-companion`, based on main `143b442`.
Scope: Ricky's Package 6 Watch command component. This is not a verification of
the whole team application, a Canvas submission, or a predicted grade.

## Executed locally

| Check | Result | Evidence |
|---|---|---|
| Shared phone command dispatcher | 19 unit tests passed, no skips | `watch-shared/build/test-results/testDebugUnitTest/TEST-com.example.comp90018.watch.PhoneCommandDispatcherTest.xml` |
| Watch acknowledgement/UI controller | 7 unit tests passed, no skips | `wear/build/test-results/testDebugUnitTest/TEST-com.example.comp90018.wear.WatchControllerTest.xml` |
| Existing phone template unit test | 1 passed | `app/build/test-results/testDebugUnitTest/` |
| Phone RPC codec/endpoint and template instrumentation | 3 passed on Pixel 10 Pro XL emulator, Android 17 | `app/build/reports/androidTests/connected/debug/index.html` |
| Wear UI instrumentation | 2 passed on Wear OS 5 emulator, API 34 | `wear/build/reports/androidTests/connected/debug/index.html` |
| Phone and Watch debug APK build | Passed | Each module's `build/outputs/apk/debug/` |
| Android lint | Passed, no errors; existing phone dependency-version warnings remain | Each module's `build/reports/lint-results-debug.html` |
| Git whitespace check | Passed | `git diff --check` |

Unit coverage includes valid transitions, stable retry IDs, concurrent duplicate
Start executing once, reused IDs, wrong sessions, malformed commands, sign-out,
same-owner re-login, process restart, journal rotation, auth changes during an
operation, non-blocking auth invalidation, Finish persistence failures, lost
acknowledgements, mismatched acknowledgements and coroutine cancellation.

The two Wear device tests render real Compose screens and exercise Home → setup
→ Start → Live → Pause → Resume → Finish → Complete, plus stale/disconnected
controls. The phone tests invoke the RPC handler used by the listener service,
including its asynchronous task, JSON codec and dispatcher. **They do not pair
two devices or traverse the actual Google Play services network.**

Device tests used `-PwatchQa=true` to isolate the installed package. The existing
Android Studio template asserted a fixed package name; it was adjusted to accept
the normal package or this explicit QA suffix. An initial Wear UI test targeted
the outer rotary-scroll container rather than the inner LazyColumn; the test now
targets the actual scroll action. Generated duplicate `* 2.dex` files appeared
in local build outputs; Gradle clean removed those outputs and the rerun passed.
Their filesystem origin was not established. None are source files or tracked.

## Not yet verified / not part of this component

- Real paired phone/watch Data Layer request/reply, disconnection and process
  restart: run the manual pairing procedure in `WATCH_HANDOFF.md`.
- Physical-watch usability, battery behaviour and Bluetooth-only connectivity.
- Integration with Zarif's Supabase Auth, Cassi's actual tracking controller and
  Henry's persisted activity records. No teammate branch was merged or rewritten.
- Real GPS/step metrics and successful database save driven from the watch.
  The default production endpoint returns `NOT_READY`; demo completion always
  states **Not saved · demo only**.
- CI is configured in `.github/workflows/watch.yml`; local results alone do not
  prove that a GitHub Actions run has succeeded.

## Reproduce

Use the repository Gradle wrapper, SDK 37 and Gradle JVM 25:

```sh
./gradlew clean testDebugUnitTest assembleDebug lintDebug
ANDROID_SERIAL=<phone> ./gradlew :app:connectedDebugAndroidTest -PwatchQa=true
ANDROID_SERIAL=<watch> ./gradlew :wear:connectedDebugAndroidTest -PwatchQa=true
```

`ANDROID_HOME` must point to the local Android SDK if Android Studio has not
created `local.properties`. Do not commit that machine-specific file. Reports and
APKs are generated artifacts, intentionally ignored by Git; rerun to regenerate.
The QA emulator `Trailwise_Wear_QA` can be opened from Android Studio Device
Manager for the local demo. Keep normal/QA phone and watch application IDs and
signing certificates matched when testing real transport.
