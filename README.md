# COMP90018-GroupT01-04---03

Trailwise is the COMP90018 group mobile activity and wellbeing project.

## Watch companion (Ricky / Package 6)

The `wear` module implements the runnable Wear OS command UI; `watch-shared`
contains its protocol, acknowledgement handling and testable phone bridge.
Start with the clearly labelled local demo, or connect the phone's debug-only
**Trailwise Bridge Demo** through Wear Data Layer. The real phone activity
gateway remains an explicit team integration seam, not a second tracking engine.

- [Run and integrate the watch](docs/WATCH_HANDOFF.md)
- [Executed tests and remaining verification](docs/WATCH_VERIFICATION.md)

Build/check all modules with `./gradlew testDebugUnitTest assembleDebug lintDebug`.
This branch requires no cloud/API keys. Do not confuse demo completion with a
persisted real activity or a completed Canvas submission.
