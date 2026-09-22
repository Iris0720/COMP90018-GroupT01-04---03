Outdoor Activity & Health App — Backlog

Six work packages, one per contributor. **Everyone writes production code and tests their own features.**

Login is required. Optional features are included now and can be removed later.

- **Required:** complete these first.
- **Optional:** implement if time allows.
- Check a task only after its code is merged and tested.

## 1. Login and Profile

- Build registration and login screens using a real authentication provider.
- Restore login sessions and implement logout.
- Build Profile: name, activity goal, units and optional health note.
- Keep each account's profile and records separate.
- Test invalid login, logout and account switching.
- Password reset, email verification and social login.

## 2. Navigation, Today and Permissions

- Set up the Android project, theme and reusable UI components.
- Connect Login, Today, Activity, Health and Profile screens.
- Build Today: daily activity summary, goal progress and health status.
- Handle permissions with clear retry/settings actions.
- Test navigation, empty states and denied permissions.
- Step counter, step goals and weather card.

## 3. Activity Tracking

- Build activity setup: Walking, Running or Hiking, plus a goal.
- Implement Start, Pause, Resume and Finish.
- Display duration, distance and pace using a labelled demo route first.
- Save the activity once and show its summary; allow retry after save failure.
- Preserve the session during navigation/rotation; test pause and zero distance.
- Real GPS, live map, background tracking and route export.

## 4. Storage, History and Analysis

- Define shared data models and account-scoped Room storage.
- Save/read activities and health check-ins after app restart.
- Build History with Week, Month, Year and All filters.
- Show activity totals, health-record counts, trends and period comparisons.
- Test date boundaries, duplicate saves and missing/zero comparison data.
- Meal storage, cloud sync and data export.

## 5. Health and Meals

- Build health check-in: feeling, breathing difficulty and fatigue.
- Implement Good / Caution / Unknown; missing information must not show Good.
- Build Health Hub and save check-ins through Package 4.
- Test health rules, missing data and save failures.
- Camera/gallery input and editable preset meal estimates.
- Manual meal entry, meal history and daily nutrition totals.
- External food recognition and personalised non-diagnostic tips.

## 6. Watch Companion

- Build a runnable Wear OS module and phone-bridge interface.
- Code Home, Start, Live, Paused and Complete screens.
- Implement commands against a clearly labelled demo bridge first.
- Test duplicate commands, disconnects and signed-out states.
- Real phone/watch synchronization controlling the same activity session.
- Reminders, notification settings and watch sensor display.

The watch remains removable from the overall scope. If removed, transfer another production feature into Package 6 so all six contributors still own coding work. A design-only prototype does not count as its coding deliverable.

## API Agreement

These are internal app interfaces, not custom HTTP endpoints. UI code calls shared services/repositories. Authentication uses a provider adapter; activities and health records stay local unless optional cloud sync is added.

| Owner | Interface | Input | Output | Assignee |
|---|---|---|---|---|
| 1 | `register / login` | Email, password; name for registration | Signed-in user or authentication error | Zarif |
| 1 | `observeAuth / logout` | None | Login state / completion | Zarif |
| 1 | `updateProfile` | Name, goal, units, health note | Saved profile or validation error | Zarif |
| 2 | `read / requestPermission` | Permission type | Granted, denied or unavailable | Iris
| 3 | `startActivity` | Activity type, goal, demo/GPS source | Active session | Cassi
| 3 | `pause / resume` | Session ID | Updated session | Cassi
| 3 | `finishAndSave` | Session ID | Saved activity summary or retryable error | Cassi
| 4 | `save / readRecord` | Record / record ID | Saved record / matching record | Henry
| 4 | `getReport` | Period, date, time zone | Activity totals, health counts, history and comparison | Henry
| 5 | `submitCheckIn` | Draft ID, feeling, breathing, fatigue | Saved check-in and condition | Yan Yu
| 5 | `estimate / saveMeal` | Photo / edited meal fields | Editable estimate / saved meal | Yan Yu
| 6 | `sendWatchCommand` | Command ID, action, session ID; activity options for Start | Acknowledgement and updated session state | Ricky

### Shared rules

- Records include `id` and `ownerId`. Repositories get the owner from the signed-in session; users cannot choose another account's owner ID.
- Logout clears visible account data and subscriptions. Resolve an active activity before normal logout; forced expiry locks its account-bound draft.
- Use seconds for duration, metres for distance, seconds/km for pace and UTC timestamps for storage.
- Pause stops time/distance accumulation. Zero distance displays `—` for pace.
- Retrying Finish or Save uses the same record ID and never creates duplicates.
- No previous data or a zero baseline shows “No comparison”, not an invalid percentage.
- API failures return an error code and whether retry is possible. UI handles loading, empty and error states.
- Never store raw passwords in app storage. Never log tokens, health notes or precise routes.
- Demo routes and meal estimates must be labelled. Health messages must not diagnose.

