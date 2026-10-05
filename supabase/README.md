# Supabase backend contract

## Shared backend scope

Supabase provides authentication plus a shared remote database for profiles, activity sessions, route points, health check-ins and meals. Room remains the offline cache and immediate UI data source, so the app can continue working when the network is unavailable.

Shared development project:

- Project name: `COMP90018 Group T01-04-03`
- Project ref: `zuhcgdlfomrdlsjcshrw`
- Region: Northeast Asia (Tokyo), `ap-northeast-1`
- Project URL: `https://zuhcgdlfomrdlsjcshrw.supabase.co`

Every remote record belongs to one authenticated user. Row Level Security prevents users from reading or changing another user's records. “Shared backend” means all feature teams use the same schema and repository contract; it does not make personal records public to other users.

## Data ownership

| Data | Owner | Storage |
|---|---|---|
| Email, password hash, session | Supabase Auth | Managed `auth` schema |
| Display name, daily step target, unit system | Profile repository | `public.profiles` |
| Optional health note | Local profile/settings repository | DataStore or Room |
| Activity and route data | Record/sync repository | Room plus Supabase |
| Health check-ins | Record/sync repository | Room plus Supabase |
| Meal nutrition records | Health/meal repository | Room plus Supabase |
| Meal photo bytes | Health/meal repository | App-private files until Storage policies are approved |

The Android app must never store raw passwords or use a Supabase `service_role`/secret key. A publishable project key is expected in a mobile client; Row Level Security is the security boundary.

## Apply the migration

1. Open the shared `COMP90018 Group T01-04-03` Supabase project.
2. Open **SQL Editor** in the Supabase Dashboard.
3. Run `migrations/202610050001_create_profiles.sql` once.
4. Run `migrations/202610050002_create_app_records.sql` once.
5. Run `verification/verify_schema.sql` and confirm that all five rows have `rls_enabled = true`. `profiles` must have three policies and each record table must have four.
6. In **Authentication**, create two test users or sign up through the app.
7. Confirm that each signup creates one row in `public.profiles`.
8. Confirm through the app that User A cannot read or update User B's profile or records.

Both migrations and the schema verification query were run successfully on 5 October 2026. This confirms schema and policy presence, but the two-user isolation test still needs the Android authentication flow.

Use the rollback scripts only in a disposable development project. Run `202610050002_drop_app_records.sql` before `202610050001_drop_profiles.sql`. They delete application data but do not delete Auth users.

## Android contract

Zarif's auth implementation should expose the existing backlog interfaces rather than leaking Supabase types into the UI:

```kotlin
interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun register(email: String, password: String, displayName: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun logout()
}

interface ProfileRepository {
    suspend fun getCurrentProfile(): UserProfile
    suspend fun updateProfile(profile: UserProfile): UserProfile
}
```

Recommended wire model:

```kotlin
@Serializable
data class SupabaseProfile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("daily_step_target") val dailyStepTarget: Int,
    @SerialName("unit_system") val unitSystem: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
```

Registration should send `display_name` as signup metadata so the database trigger can populate the profile row. The UI should not insert an arbitrary profile ID; it must use the authenticated session's user ID.

## Android build notes

- The current project uses `minSdk = 24`.
- The current Supabase Kotlin documentation states that the client requires Android 26 unless core library desugaring is enabled.
- Decide as a team whether to raise `minSdk` to 26 or keep 24 and add desugaring before adding the dependency.
- Add the `auth-kt` and `postgrest-kt` modules plus a compatible Ktor Android engine.
- Put the project URL and publishable key into ignored `local.properties`, then expose them through generated `BuildConfig` fields.
- Commit a placeholder/example configuration only. Never commit real secret or service-role keys.

Do not pin library versions until the auth branch is available: the versions must be compatible with its Kotlin, Ktor and serialization configuration.

## Acceptance checks

- Signup creates exactly one Auth user and one matching profile.
- Login restores a valid session after process restart.
- Logout clears user-specific UI state and subscriptions.
- A user can read and update only their own profile.
- Unauthenticated requests cannot read profiles.
- Invalid unit systems and step targets outside `0..100000` fail at the database boundary.
- Deleting an Auth user cascades to the matching profile.
- Guest mode remains usable offline without requiring Supabase.
- Network failure shows a retryable state and does not block local activity, health or meal workflows.
- Activity, health and meal UUIDs match between Room and Supabase after an upsert.
- A route point cannot reference an activity session owned by another user.
- Sync runs off the main thread and retries without creating duplicate records.

## Deliberately excluded from the first integration

- Meal-photo upload to Supabase Storage.
- Realtime subscriptions.
- Admin APIs in the Android application.
- Cross-device conflict resolution.

The first sync implementation should use idempotent UUID upserts and a simple server `updated_at` timestamp. Do not add Realtime or complex conflict resolution before the basic upload/download path passes device tests.
