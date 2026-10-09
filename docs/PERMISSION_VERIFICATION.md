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
- Camera permission dialog from Take photo: passed.
- Deny camera -> manual entry remains available: passed.
- Manual meal name/serving -> Save meal -> saved message and record in list: passed.
  A test record named `PermissionQA1-bowl` remains on this test emulator.
- Camera retry -> rationale -> Continue -> system permission -> Allow opens
  `com.android.camera2/com.android.camera.CaptureActivity`: passed.
- Shutter opens the camera confirmation screen: passed.

## Still requiring device acceptance

- Camera Done -> photo preview and record save; capture cancellation.
- Photo Picker selection/cancellation and persisted photo reopening.
- Meal record visibility after force-stop/relaunch.
- Notification denial/settings return and API 24-32 behavior.
- Physical step-sensor permission and location/step sequencing on a device with
  a step counter (this emulator reports no step-counter sensor).
- Permissions changed in Settings, device rotation during permission dialogs,
  and missing camera/picker handlers.

The emulator briefly showed a System UI ANR before testing; choosing Wait recovered
the device. That was not an application crash. Device checks above are smoke tests,
not a claim that every acceptance scenario has passed.

## Integration boundaries

Activity metrics remain explicitly labelled demo values. No real tracking, scheduled
notifications, automatic meal recognition or shared database integration was added.
The local meal fallback should be adapted to the team's meal/data repository when
those branches merge; do not keep two independent meal stores in the integrated app.
