# Watch branch merge and paired-test status — 11 October 2026

## Merge scope

Merged main `5e3a1e43f6fd8a24db57a4e21933082aab33fef6` into
`feature/watch-companion`, starting from watch head
`4a4703f42e8e9b49cd9ae692d289e811927084ab`. This updates the watch PR branch,
not main.

Resolved the content conflicts in README.md, app/build.gradle.kts and
gradle/libs.versions.toml by retaining both sets of features. Preserved main's
Supabase/Auth, Room/KSP, CameraX, location, permissions and meal configuration;
retained watch-shared, Wear/Data Layer, isolated watchQa package suffix and
Compose test dependencies. Consolidated identical lifecycle dependencies and
duplicate serialization plugin/catalog declarations, including the automatic
merge in the root build.gradle.kts.

Phone feature Kotlin sources, Room schemas and phone resources from main were
preserved unchanged. The phone source/resource diff relative to main contains
only the watch listener/endpoint and phone capability resource. No real activity
adapter, authentication bridge or database behavior was added in this merge.

## Merge verification

Executed `testDebugUnitTest assembleDebug lintDebug` on the merged tree:

| Check | Result |
|---|---|
| Phone JVM unit tests | 24 passed |
| Wear JVM unit tests | 24 passed |
| Shared phone command dispatcher JVM tests | 19 passed |
| Phone and Wear debug APK builds | Passed |
| Phone and Wear instrumentation test APK builds | Passed; device tests not rerun |
| Phone lint | 0 errors, 42 warnings |
| Wear and watch-shared lint | No issues found |

All 67 JVM tests had zero failures, errors or skips. Gradle reported BUILD
SUCCESSFUL. No lint suppressions or abortOnError changes were introduced.

Also executed `:app:assembleDebugAndroidTest :wear:assembleDebugAndroidTest`:
BUILD SUCCESSFUL. These test APKs were only built, not installed or executed;
the existing paired emulator apps/data were left unchanged during the merge.

Build outputs use a temporary init script and an external cache directory to
avoid the machine's Desktop/iCloud duplicate-output issue. No repository output
path setting was changed.

`git diff --name-only --diff-filter=U` was empty and `git diff --check origin/main`
passed. A full staged diff relative to the old watch HEAD also reported inherited
main whitespace/CRLF findings in teammate files; these files were preserved
rather than reformatted as part of this scoped merge.

## Actual paired-emulator result before this merge

Pixel 10 Pro XL API 37.1 and Trailwise Wear QA API 34 paired successfully.
Installed QA application IDs and signing certificates match. A real cross-device
READ_STATE request through MessageClient and the phone listener returned
ACCEPTED and the normal phone debug demo's LIVE state; this was a diagnostic
request to the unique connected node, not the shipped capability discovery path.

The default WearDataLayerBridge still returned DISCONNECTED before START. The
watch UI showed Phone companion / Phone not connected. Rebooting the watch
restored system pairing but did not fix capability discovery: the post-reboot
external run had two tests, one raw-read diagnostic pass and one default-path
lifecycle failure. Start/Pause/Resume/Finish over the default paired path remains
unverified. No device or application data was wiped.

Phone capability registration exists, but the watch's capability discovery/index
does not expose it. The phone and watch have different Play services versions;
that is an environmental factor to investigate, not a proven root cause.

## Remaining boundaries

- These paired results were obtained on pre-merge watch head 4a4703f, not on the
  merged phone APK. Paired runtime tests must be repeated after integration.
- Standalone Explore demo and phone Bridge Demo use synthetic sessions and do
  not save real activity records. Passing build/unit/lint checks does not change
  this limitation.
- Production phone Auth/tracking/persistence is still not wired into the watch
  endpoint; requests fail closed with NOT_READY until an adapter is installed.
- Physical-watch ergonomics, sensors, battery and actual team-app end-to-end
  behavior are not established by these checks.
- The PR stays draft. Resolving Git conflicts does not establish functional
  readiness, and no PR merge into main or assignment submission was performed.
