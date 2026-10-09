# Ricky — Watch commands and Wear OS companion

## Scope and honest completion boundary

This implements Package 6's `sendWatchCommand`: command ID, action, session ID,
Start options → an acknowledgement and authoritative session state. It adds a
runnable `:wear` module, shared protocol, phone RPC receiver, and an explicitly
labelled demo gateway. Supabase/weather are Ricky's separate additional work.

The demo is executable code, not a design-only prototype. Its timer, distance and
steps are synthetic; Finish does **not** save a real activity. The production
receiver fails closed with `NOT_READY` until an authenticated phone activity
gateway is installed. No teammate's tracking, login, health or storage screen has
been replaced. Real GPS/background tracking/storage remain their existing owners'
implementations. Do not claim this branch has integrated the entire team app.

## Modules and data flow

```
Wear UI → WatchController → WearDataLayerBridge → phone RPC service
                                                → PhoneCommandDispatcher
                                                → PhoneSessionGateway
           ← correlated acknowledgement + phone state ←
```

- `watch-shared`: versioned Kotlin/JSON contract, validation, deduplication,
  account-generation binding, acknowledgement-driven UI controller, demo gateway.
- `wear`: round-watch scrollable Home, Start setup, Live, Paused and Complete
  screens. Start supports Walking/Running/Hiking and Open/15 min/1 km goals.
- `app/.../watch`: manifest RPC service and installable endpoint. No default
  production guest, fake login, hardcoded cloud credentials or automatic demo.
- `app/src/debug`: separate **Trailwise Bridge Demo** launcher, never included
  in release. The same dispatcher accepts phone demo UI and watch RPC commands.

Use `WatchBridge.sendWatchCommand(WatchCommand(...))` from a coroutine. The watch
waits for an application acknowledgement; a transport-send success alone is not
enough. `READ_STATE` is an extra handshake/refresh action. Duration uses seconds,
distance metres, and missing steps stay nullable.

## Reliability and account safety

1. Mutations use canonical UUID command/session IDs. Retry preserves the **same
   complete command**, not a fresh UUID. Concurrent duplicate commands execute
   once; reusing a command ID with a different payload is rejected.
2. Phone state is authoritative. Disconnect/timeouts retain the last confirmed
   state as stale, disable new mutations and offer retry/refresh. A timeout may
   mean the phone executed a command but its reply was lost.
3. `READ_STATE` supplies an opaque `accountToken`. This is **not** a Supabase
   credential or owner ID. Mutations require the current token. The host must
   call `PhoneWatchEndpoint.invalidateAccount()` on **every** auth transition,
   even logout/login of the same account. Signed-out replies disclose no session.
4. Process restart and a full 256-entry journal rotate the account generation.
   Outstanding old commands are rejected rather than replayed after cache loss.
   Refresh before issuing a new command. The real host must also retain stable
   session IDs and reject session reuse in its persisted records.
5. Finish in a non-demo gateway is accepted only if COMPLETE and `saved=true`.
   An unsaved COMPLETE state returns retryable `SAVE_FAILED`; its adapter must
   retry persistence using the original record ID, not start/finish another activity.
6. The client chooses a single reachable phone capability; it does not broadcast
   mutations or silently redirect an uncertain retry to a different phone.
7. Only the small session summary travels over Data Layer. No route coordinates,
   passwords, auth tokens, health notes or meal photos are sent or logged.
8. The visible watch refreshes a healthy live/paused session every 3 seconds;
   polling stops when the Activity is not started or the last response failed.
   No separate watch timer invents unconfirmed phone metrics.

## Run in Android Studio

Open this branch's repository root, Sync Gradle, install the requested SDK 37,
and use the project's Gradle JVM 25. Select the **wear** configuration and a
Wear OS emulator/device; select **app** only for the phone APK.

### Local demo (no phone pairing or API keys needed)

1. Run `wear`. If no configured phone is reachable, tap **Try local demo**.
2. Tap **Set up activity**, choose type/goal, then Start.
3. Pause → Resume → Finish. Complete is labelled **Not saved · demo only**.
4. Scroll to **Simulate disconnect**. Verify mutation controls are disabled and
   the state is stale. Reconnect and retry/refresh.
5. **Simulate sign out** clears session data; **Sign in demo** performs a fresh
   handshake. Demo account controls do not alter real Supabase accounts.

### Phone ↔ watch demo over real transport

1. Pair phone/watch using Android Studio Device Manager/Wear companion setup.
2. Install both debug APKs from this checkout (matching application ID and debug
   signing certificate). Launch **Trailwise Bridge Demo** on the phone.
3. Leave watch in Phone companion mode; Refresh → Start/Pause/Resume/Finish.
   The phone debug screen should show the same demo session and phase.
4. Kill the phone process: subsequent mutations must NOT pretend to succeed.
   Relaunch the debug phone activity, Refresh and obtain a fresh generation.

The QA build flag `-PwatchQa=true` adds `.watchqa` to **both** application IDs,
so device verification does not replace the team's existing installed app.
Do not mix QA and normal APKs when pairing. Gradle connected tests ordinarily
uninstall their test applications after execution.

## Team integration: required seams, not completed merges

The production host installs one `PhoneSessionGateway` for the **same** activity
controller used by the phone UI:

```kotlin
val installedDispatcher = PhoneWatchEndpoint.install(realPhoneSessionGateway)
// On each authentication transition, before accepting any more commands:
PhoneWatchEndpoint.invalidateAccount()
// If tearing down/replacing that host:
PhoneWatchEndpoint.uninstall(installedDispatcher)
```

Do not create a second `TrackingViewModel` for the watch. Supply the actual
session-owning controller; synchronize phone UI and watch operations together.
The service calls the gateway on its worker. Android UI mutations must be
marshalled to Main by the adapter; database operations must remain off Main.
The gateway's `execute(command, expectedOwnerId)` must atomically verify the
trusted owner against current authentication before mutating its session. Auth
invalidation is non-blocking, so an auth event on Main cannot deadlock against
a command worker waiting for Main.

| Collaborator | Adapter requirement |
|---|---|
| Zarif | `currentOwnerId()` from the real signed-in session; invalidation on logout/account change/expiry; reject commands until Auth restoration finishes. No demo owner in production. |
| Cassi | Map Start options to the actual type/goal, Start with the caller's stable session UUID; map LIVE/PAUSED/COMPLETE and real metrics; provide Pause/Resume/Finish against that same session. Current branch generates its own UUID, so this needs an explicit extension. |
| Henry + Cassi | Finish retains a stable draft until `saveRecord` succeeds. `saved=true` only after persistence. Retrying an unsaved COMPLETE session retries its draft save. |
| Integration owner | Install the gateway from an app/session-level owner and keep it available while a real activity runs. Restore/validate persisted state after process restart; optional FGS still belongs to phone tracking. |

The initial repository assignment says demo bridge first and real synchronization
after integration. This deliverable completes the runnable demo/command layer;
the adapter above is necessary before claiming control of Cassi's real session.

## Verification commands

```sh
# JVM tests, both APKs and static analysis; no API keys:
./gradlew testDebugUnitTest assembleDebug lintDebug

# Phone endpoint codec/dispatcher on a phone (isolated package):
ANDROID_SERIAL=<phone-serial> ./gradlew :app:connectedDebugAndroidTest \
  -PwatchQa=true \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.comp90018.watch.PhoneWatchEndpointTest

# Actual Wear UI flow/error-state tests on a Wear emulator/device:
ANDROID_SERIAL=<watch-serial> ./gradlew :wear:connectedDebugAndroidTest -PwatchQa=true
```

Reports are under each module's `build/reports/`; debug APKs under
`app/build/outputs/apk/debug/` and `wear/build/outputs/apk/debug/`.
See [verification record](WATCH_VERIFICATION.md) for executed results and limits.

## Viva explanation

- Why no optimistic updates? A disconnected watch cannot prove a phone action ran.
- Why command ID and session ID? One identifies a retryable request, the other
  prevents a command from controlling a different activity.
- Why an account generation? An old retry must not affect a newly signed-in user.
- Why separate gateway? The watch can be tested independently without taking
  over Cassi/Henry's state machine or persistence.
- Why `saved`? Ending a timer is not proof that a record is safely stored.

Development used AI assistance for implementation and tests. Retain the team's
AI acknowledgement and explain the above code and limitations in the viva.

## Primary technical references

- [Android Data Layer overview: matching package/signature](https://developer.android.com/training/wearables/data/overview)
- [MessageClient request/reply API](https://developers.google.com/android/reference/com/google/android/gms/wearable/MessageClient)
- [Wear event listener service](https://developer.android.com/training/wearables/data/events)
- [Wear Compose setup](https://developer.android.com/training/wearables/compose)
- [Wear application packaging](https://developer.android.com/training/wearables/packaging)
