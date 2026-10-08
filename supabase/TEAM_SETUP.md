# Team Supabase setup

The shared database is ready in the `COMP90018 Group T01-04-03` Supabase project.

## 1. Get the backend files

Until this branch is merged, fetch and base work on:

```bash
git fetch origin
git checkout feature/supabase-backend
```

The database migrations have already been applied to the shared project. Do not rerun rollback scripts against the shared database.

## 2. Configure Android locally

Ask the project owner for the existing **publishable key** through the team's private channel. Never request or use the `service_role` or secret key in the Android app.

Add these values to the untracked root `local.properties` file:

```properties
SUPABASE_URL=https://zuhcgdlfomrdlsjcshrw.supabase.co
SUPABASE_PUBLISHABLE_KEY=replace-with-the-shared-publishable-key
```

Do not commit `local.properties`. The repository already ignores it.
`local.properties.example` contains the exact property names and a safe
placeholder that can be copied without exposing a real key.

## 3. Repository boundary

- UI and ViewModels call repository interfaces.
- Only data/remote implementations use `SupabaseClientProvider.client`.
- The authenticated session supplies `owner_id`; the UI must never allow a user to choose it.
- Use the same client-generated UUID in Room and Supabase so retries can upsert without duplicates.
- Room remains the immediate local source for offline use; network failures must not block the core activity or health flow.
- Use `BackendRepository` from `data/remote`; do not call PostgREST directly
  from a screen or ViewModel.

See `INTEGRATION_GUIDE.md` for concrete activity, health and meal examples.

## 4. Feature mapping

| Feature | Supabase table |
|---|---|
| Account credentials/session | Managed `auth.users` |
| Profile, goal and units | `public.profiles` |
| Activity summary | `public.activity_sessions` |
| GPS route segments | `public.route_points` |
| Health check-in | `public.health_check_ins` |
| Meal nutrition | `public.meal_entries` |

Weather stays in its external API plus a local cache. History and Today summaries are derived from records rather than stored in extra tables. Meal image bytes stay local until the team explicitly adds a private Storage bucket and policies.

## 5. Required test before integration

1. Sign up User A and User B.
2. Confirm that each signup creates one matching `profiles` row.
3. Insert one record for User A through the app.
4. Confirm User B cannot select, update or delete User A's record.
5. Turn off the network and confirm Room-backed workflows still work.
