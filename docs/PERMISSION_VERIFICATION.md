# Permission completion verification

Verified on 2026-10-09, branch `feature/requestPermission`.

## Automated checks

- `:app:assembleDebug`: passed, using Java 25 and an isolated build directory.
- `:app:testDebugUnitTest`: 4 tests passed, 0 failures/errors.
- Three new regression tests cover chained requests retaining their callbacks,
  cancellation completing once, and concurrent requests not overwriting a callback.
- `git diff --check`: passed (line-ending normalization notice only).

## Pixel 3 XL API 34 emulator

- APK installation and MainActivity launch: passed.
- Profile displays Notifications and opens the system notification permission dialog: passed.
- Allow notifications -> Profile updates to Allowed: passed.
- Camera permission dialog from Take photo: passed before the check-in/meal branch
  merge. Re-run on the merged version to verify the authorization callback opens the
  camera and leaves gallery/manual entry available after denial.

## Still requiring device acceptance

- Camera Done -> photo preview and record save; capture cancellation.
- Photo Picker selection/cancellation and persisted photo reopening.
- Meal record visibility after force-stop/relaunch (the merged demo service is
  in-memory and does not promise persistence).
- Notification denial/settings return and API 24-32 behavior.
- Physical step-sensor permission and location/step sequencing on a device with
  a step counter (this emulator reports no step-counter sensor).
- Permissions changed in Settings, device rotation during permission dialogs,
  and missing camera/picker handlers.

The emulator briefly showed a System UI ANR before testing; choosing Wait recovered
the device. That was not an application crash. These checks were run against the
permission-only version before meal-branch integration; re-run the listed merged
device checks after building the current version.

## Integration boundaries

Activity metrics remain explicitly labelled demo values. Notifications are permission
only; no notices are scheduled or sent. Meal recognition is optional and requires a
local API token. The merged demo meal service is in-memory; connect the shared data
repository before promising storage across app restarts.
