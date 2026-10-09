# COMP90018-GroupT01-04---03

Android coursework project for COMP90018. The `feature/supabase-backend` branch
adds Supabase repositories, weather connectivity with persistent offline cache,
and automated build/test checks for team integration.

- [Supabase setup](supabase/TEAM_SETUP.md)
- [Backend API and authentication integration](supabase/INTEGRATION_GUIDE.md)
- [Weather API and frontend handoff](docs/WEATHER_HANDOFF.md)
- [Verification evidence and team message](docs/BACKEND_VERIFICATION.md)

Use the shared authenticated Supabase client when integrating repositories.
Copy the values from `local.properties.example` into an ignored
`local.properties` and supply the team's publishable key locally.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

GitHub Actions runs these checks and stores the reports/debug APK for seven
days. CI builds without Supabase credentials; configure the project locally to
use cloud features. Device and live-service tests are opt-in as documented in
the integration guide. Feature screens, Room sync scheduling and the final app
merge remain team integration work.
