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

**Owner:** Huanyu Zhang (Henry)

### Required

- Define shared data models for activity records and health check-ins.
- Implement account-scoped Room storage. Every query and write must use the signed-in user's `ownerId`.
- Save and read activities and health check-ins after app restart.
- Build History with Week, Month, Year and All filters.
- Show activity totals, health-record counts, history, trends and previous-period comparisons.
- Handle duplicate saves, date boundaries, time zones, missing data and zero comparison baselines.
- Provide loading, empty, success and retryable error states to consuming UI.
- Add unit and database tests for storage, reporting and account isolation.

### Optional

- Meal storage.
- Cloud sync.
- Data export.

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
| 2 | `read / requestPermission` | Permission type | Granted, denied or unavailable | Iris |
| 3 | `startActivity` | Activity type, goal, demo/GPS source | Active session | Cassi |
| 3 | `pause / resume` | Session ID | Updated session | Cassi |
| 3 | `finishAndSave` | Session ID | Saved activity summary or retryable error | Cassi |
| 4 | `save / readRecord` | Record / record ID | Saved record / matching record | Huanyu Zhang (Henry) |
| 4 | `getReport` | Period, date, time zone | Activity totals, health counts, history and comparison | Huanyu Zhang (Henry) |
| 5 | `submitCheckIn` | Draft ID, feeling, breathing, fatigue | Saved check-in and condition | Yan Yu |
| 5 | `estimate / saveMeal` | Photo / edited meal fields | Editable estimate / saved meal | Yan Yu |
| 6 | `sendWatchCommand` | Command ID, action, session ID; activity options for Start | Acknowledgement and updated session state | Ricky |

### Package 4 contract

Package 4 owns persistence and reporting. It exposes repository interfaces to the other packages; UI code must not access Room DAOs directly.

#### `saveRecord`

- **Input:** A record with a stable `id`, record type, UTC timestamp and domain fields. The repository obtains `ownerId` from the active authenticated session rather than accepting an arbitrary owner from UI code.
- **Output:** The saved record, or a structured error containing an error code and whether retry is allowed.
- **Behaviour:** Saving the same `id` again updates or returns the same logical record and must not create a duplicate.

#### `readRecord`

- **Input:** Record ID.
- **Output:** The matching record for the active owner, `NotFound`, or a structured storage error.
- **Behaviour:** A record owned by another account must behave as unavailable and must never be returned.

#### `observeHistory`

- **Input:** Period (`Week`, `Month`, `Year` or `All`), reference date and time zone.
- **Output:** An observable list of the active owner's matching activity and health records, ordered newest first.
- **Behaviour:** Period boundaries are calculated in the requested time zone; stored timestamps remain UTC.

#### `getReport`

- **Input:** Period, reference date and time zone.
- **Output:** Activity count, total duration, total distance, health-record count, history and previous-period comparison.
- **Behaviour:** Missing previous data or a zero baseline returns `NoComparison`; it must not return an invalid percentage.

### Package 4 boundaries

- **Package 1 (Zarif):** supplies the authenticated user identity. Package 4 consumes the active `ownerId` and clears visible streams on logout; it does not implement login.
- **Package 2 (Iris):** consumes summaries for Today and History navigation. Package 4 supplies data and UI states; it does not own permission requests or the Today layout.
- **Package 3 (Cassi):** supplies the completed activity record and stable record ID to `saveRecord`. Package 4 persists it; it does not own GPS or live-session state.
- **Package 5 (Yan Yu):** supplies validated health check-ins to `saveRecord`. Package 4 persists them; it does not calculate the health condition or provide medical advice.
- **Package 6 (Ricky):** may read the current activity state through the activity owner. Package 4 stores completed records but does not own watch commands or synchronization.

### Package 4 acceptance criteria

- Activity and health records remain available after process restart.
- Two signed-in accounts cannot read, observe or report on each other's records.
- Retrying a save with the same record ID never creates a duplicate.
- Week, Month and Year filters include records at the correct local date boundaries for the supplied time zone.
- Reports use seconds for duration, metres for distance and UTC timestamps for storage.
- A zero-distance activity displays no pace rather than dividing by zero.
- A missing or zero previous-period baseline produces `NoComparison`.
- History exposes explicit loading, empty, success and retryable error states.
- Required DAO, repository and report tests pass before Package 4 is marked complete.

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
