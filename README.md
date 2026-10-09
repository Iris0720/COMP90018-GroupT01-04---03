# COMP90018-GroupT01-04---03

## Permission branch

- Location, activity recognition, camera and notifications share `PermissionController`.
- Notifications are requested explicitly from Profile, on Android 13+. Older devices
  read the system notification setting and link to Settings when disabled. This branch
  does not schedule or send notifications yet.
- Permission results release the active request before invoking the caller, so
  location -> optional step permission can continue. Dismissing the rationale also
  completes the request. Returning from Settings refreshes status; press Start/Take
  photo again to continue the original action.
- Camera approval opens the system camera using a scoped FileProvider URI. Denial,
  cancellation or missing camera apps leave Photo Picker and manual entry available.
  Photo Picker requires no broad media/storage permission.
- Photos enter an editable meal form with preview. Name/serving and optional calories
  are saved locally; records survive navigation and app restart. Picked images are
  copied to app-private storage. Automatic nutrition recognition is not connected.
- Meal storage is a small local fallback (`permission_meals` preferences and
  `permission-meal-photos/` files), to be replaced with the shared meal repository
  during integration. No account/cloud sync is implemented here.
- Activity still uses sample metrics. The explicit Demo button bypasses permissions;
  real GPS tracking/history persistence belong to the activity/data branches.

## Verification

Use Java 25 (the local Android Studio JBR is suitable), then run
`gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.
Avoid running two builds against the same output directory.

Regression tests cover chained permission requests, cancellation completing exactly
once, and preserving the active callback when another request arrives.

Device acceptance checklist:

1. Fresh install: no permission prompt before using a feature.
2. Profile: request notifications; deny/retry/allow and return from notification
   settings; confirm status refreshes. Also check notifications disabled on API 24-32.
3. Activity: allow location then deny optional steps; continue to the demo screen.
   Retry after a denial and choose Not now in the explanation; continuation must work.
4. Location denied/off: retry/settings remain available; Demo works without location.
5. Health: allow camera, capture, preview, edit and save a meal; cancel capture and retry.
6. Deny camera: choose a photo or enter manually; invalid numbers show a validation
   message. Save a meal, change tabs/restart the app and verify it remains listed.
7. No camera/picker handler: show a recovery message and allow manual entry.

Platform references: [notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
and [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker).
