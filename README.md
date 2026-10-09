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
- Camera and gallery images feed the existing meal estimation/edit/save flow. Camera
  access is requested before opening the external camera; gallery and manual entry
  remain available if camera access is denied.
- LogMeal recognition uses the optional `LOGMEAL_API_TOKEN` in `local.properties`.
  Without a token, photo estimation reports a recoverable error and manual entry stays
  available. The current meal service is an in-memory demo implementation; persistent
  repository integration is still required before claiming records survive restart.
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
5. Health: allow camera, capture, estimate (or token fallback), edit and save a meal;
   cancel capture and retry.
6. Deny camera: choose a photo or enter manually; invalid nutrition values are rejected.
   Save a meal and check it appears for the current app session.
7. No camera/picker handler: show a recovery message and allow manual entry.

Platform references: [notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
and [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker).

# Meal check-in and photo estimation

## Implemented features

- Health check-in for feeling, breathing, and fatigue
- Condition calculation with validation
- Food photo capture using the Android camera
- Food photo selection from the device gallery
- Real food recognition and nutrition estimation through LogMeal
- Editable meal name, serving, calories, protein, carbohydrates, and fat
- Meal validation and save action
- Automatic image resizing and compression before upload

## Configure the real LogMeal API

The app does not include an API token in Git. Each developer who wants to test real food recognition should create a LogMeal account and use their own APIUser token. Do not send tokens in chat, screenshots, commits, or pull requests.

### 1. Create a LogMeal account

1. Open the [LogMeal API signup page](https://logmeal.com/api/).
2. Create an account and verify the email address.
3. Sign in to the LogMeal dashboard.
4. Activate the available trial or choose a plan that provides food recognition and nutritional information.

### 2. Get an APIUser token

LogMeal has different token types. Food recognition must use an **APIUser token**, not an APICompany token.

1. Open the Users section in the LogMeal dashboard.
2. Use the testing APIUser created during signup, or create a new APIUser.
3. Copy that APIUser's access token.

See the official [LogMeal access-token guide](https://docs.logmeal.com/docs/guides-essential-concepts-users-and-access-tokens) if the token types are unclear.

### 3. Add the token locally

Open the project-level `local.properties` file. Keep the existing Android SDK line and add:

```properties
LOGMEAL_API_TOKEN=YOUR_APIUSER_TOKEN
```

For example, the file should have this structure:

```properties
sdk.dir=/path/to/your/Android/sdk
LOGMEAL_API_TOKEN=YOUR_APIUSER_TOKEN
```

`local.properties` is ignored by Git and must remain untracked. Never place the real token in Kotlin source code or commit it to the repository.

### 4. Sync and run

1. Open the project in Android Studio.
2. Click **Sync Now** if Android Studio reports that Gradle files have changed.
3. Start an Android emulator or connect an Android phone with USB debugging enabled.
4. Select the `app` run configuration.
5. Click **Run**.
6. Open the **Health** tab.
7. Under **Meal log**, select **Take photo** or **Choose photo**.
8. Review the returned nutrition estimate, edit it if necessary, and select **Save meal**.

The emulator or phone must have internet access. Clear, well-lit food photos with the whole plate visible provide better results.

## API flow

The app performs two authenticated requests:

1. Uploads a compressed meal image to LogMeal's complete segmentation endpoint.
2. Uses the returned image ID to request nutritional information.

Photos are resized and compressed below LogMeal's upload limit before being sent. Estimates are approximate and should be reviewed before saving.

## Build from the terminal

On macOS or Linux:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## Troubleshooting

- **Run button is disabled:** complete Gradle sync and make sure a device is selected.
- **Token is not configured:** add `LOGMEAL_API_TOKEN` to `local.properties`, then sync and rebuild.
- **401 response:** the token is invalid, expired, or was copied incorrectly.
- **403 response:** the account or token does not have access to the requested feature.
- **429 response:** the account has reached a request or plan limit.
- **Recognition fails:** confirm internet access and retry with a clear food photo.

For endpoint details, see the [LogMeal quickstart](https://docs.logmeal.com/docs/guides-getting-started-quickstart).
