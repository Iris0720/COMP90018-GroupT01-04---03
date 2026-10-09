# Trailwise Android App

This branch contains the Health check-in and meal photo estimation feature.

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
