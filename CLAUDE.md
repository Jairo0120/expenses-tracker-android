# Expenses Tracker — native Android

Personal budgeting app: expenses, incomes and savings grouped into monthly **cycles**, with budgets and
recurrent entries that the backend copies into each new cycle. This repo is a native rewrite of the Expo
app, feature-complete online. Offline support is being built (see "Offline plan" below).

Related repos (siblings in `~/projects`):
- `expenses-tracker` — FastAPI backend this app talks to (has its own CLAUDE.md).
- `expenses-tracker-app` — the original Expo/React Native app. Retired (no longer used); reference only for
  behaviour and Spanish copy. API changes don't need to stay compatible with it.

Follow-ups (backend quirks, UI ideas, open items) are tracked in a Claude Doc:
https://claude.ai/code/artifact/e725ea4c-d2ad-4546-96eb-20acb2935e62

## Build, run, test

- Android SDK in `~/Android/Sdk` (command-line tools, platform 37). The system JDK (26) runs Gradle fine.
- `local.properties` (git-ignored; see `local.properties.example`) holds `sdk.dir` and per-build config:
  `dev.*` for the debug build, `release.*` for release, shared `auth0.audience`.
- `./gradlew testDebugUnitTest` — JVM unit tests (MockWebServer API tests, PagedList, formatters…).
- `./gradlew installDebug` — installs `com.jldev.expensestracker.android.dev` "(DEV) Expenses Tracker" on the
  USB-connected phone.
- `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
  (`com.jldev.expensestracker.android`, prod API, R8 shrinking on), sideloaded with `adb install -r`. Signed
  with `~/.android/keystores/expenses-tracker-release.p12`; its passwords are the `expensesTracker.release.*`
  entries in `~/.gradle/gradle.properties` (both outside the repo, backed up by the user). Every update must
  use this same key. Without those properties the release build comes out unsigned. Test release builds
  against prod read-only unless the user says otherwise.
- Debug talks to the dev API on this machine, `http://192.168.0.218:8000` (cleartext allowed only in debug).
  The phone reaches it only because of a ufw rule for port 8000 on the LAN. The dev DB holds only test data:
  creating/deleting records while testing is fine. Never test against prod.
- Logs: `adb logcat | grep OkHttp` (full request/response bodies in debug, Authorization redacted);
  crashes: `adb logcat -b crash -d`; stored tokens:
  `adb shell run-as com.jldev.expensestracker.android.dev cat shared_prefs/com.auth0.authentication.storage.xml`.
- Driving the phone from a session: `adb shell input tap/swipe/text` + `adb exec-out screencap -p`. Screenshots
  are shown scaled (1080×2340 → ~923×2000): multiply coordinates by 1.17. The full keyboard shifts dialogs.
- Auth0 callback/logout URLs registered per tenant:
  `expensestracker://<auth0 domain>/android/<applicationId>/callback`.

## Stack and layout

Kotlin, Jetpack Compose (Material 3), Hilt, Auth0.Android v4, Retrofit 3 + OkHttp 5 + kotlinx.serialization.
AGP 9 with built-in Kotlin; versions in `gradle/libs.versions.toml`. No Navigation Compose: the shell switches
tabs by state and forms are dialogs.

`app/src/main/java/com/jldev/expensestracker/android/`
- `auth/AuthRepository` — Auth0 login/logout, `CredentialsManager` (auto refresh), `isSignedIn` flow.
- `data/remote/` — `ApiJson`, one Retrofit service per area (`api/`), models (`model/`), `AuthInterceptor`,
  `toUserMessage()` for Spanish error text. `data/CycleRepository` — shared cycles, selected cycle, status.
- `di/NetworkModule` — OkHttp/Retrofit/services.
- `ui/AppRoot` — login vs. shell; owns a session-scoped `ViewModelStore` cleared on logout.
- `ui/shell/` — drawer (Gastos/Ingresos/Ahorros), bottom tabs per section (`Destinations.kt`), top bar with
  the cycle summary, snackbar, and the add button slot.
- `ui/common/` — `PagedList` (skip/limit paging), `EntryListViewModel` (list + form dialog + submit),
  `CycleEntryListViewModel` (adds cycle selection and header refresh), `runReporting`/`report`.
- `ui/components/` — `EntryListScreen`, `PagedLazyColumn`, `EntryFormDialog`/`EntryForm` (money field),
  `EntryCard`, `BudgetChips`, `CyclePicker`, `AddButtonHost`, …
- Feature screens: `ui/expenses`, `ui/budgets` (recurrent budgets = "Presupuestos" tab), `ui/incomes`,
  `ui/savings` (Resumen = grouped savings, Ahorros, Recurrentes).

## Conventions and gotchas

- UI text is Spanish, matching the Expo app. Money is COP in whole pesos (`formatMoney`, es-CO).
- Backend datetimes are naive but UTC → modelled as `Instant` (`InstantSerializer` reads naive as UTC, writes
  `Z`). Cycle dates are `LocalDate`. Always send `date_*` on create.
- `ApiJson`: snake_case naming, `explicitNulls = false` (Kotlin nulls are omitted). To send a real JSON null,
  use a `JsonElement` field — see `ExpenseUpdate.budgetId` / `budgetChange()` (clearing an expense's budget).
- A null `cycleId` means the backend's active cycle. Budget id `0` is the synthetic "Sin ppto." entry: a
  list filter only, never offered in the form.
- A new list screen: subclass `EntryListViewModel` (or `CycleEntryListViewModel`) and call `refresh()`
  (or `start()`) **from the subclass init**, never the base init (fetching would hit unset subclass fields).
  Mutations go through `submit(...)`; override `onChanged()` for extra refreshes.
- Launching `async` requests from `viewModelScope.launch` must be wrapped in `coroutineScope {}` — a failure
  otherwise escapes the try/catch and crashes the app.
- `PagedLazyColumn` only emits keyed items; an always-present footer would become the scroll anchor and jump
  the list to the end on first load.
- Screens publish their add button with `PublishAddButton`; it lives in the shell Scaffold so snackbars stack
  above it.
- A saving's "description" is its **saving type**. Editing it renames the whole type for every saving — this
  is intended (user decision); the app only sends the description when it actually changed.
- Icons are Material Symbols vector drawables in `res/drawable` (downloaded from google/material-design-icons,
  `android:tint` attribute removed). No material-icons-extended.
- `AuthRepository.credentials()` throws `IOException` when a refresh fails for lack of network (session kept)
  and marks the user signed out otherwise; `AppRoot` then shows login.

## Offline plan (decided 2026-09-26, in progress)

- First launch asks: log in (online + local copy, synced) or use fully offline. Build the online path first.
- Local-first: Room is the source of truth for screens; writes go to Room plus a pending-changes queue;
  WorkManager syncs when connected (push queue, pull changes, merge). DataStore for mode and sync state.
- Backend "option B": every synced record has a client-generated UUID, a server `sync_version`, soft deletes
  (`deleted_at`); `GET /sync` (pull since a cursor) and `POST /sync` (batch push). Conflicts: last write wins,
  delete wins. Cycles are identified by (user, month); recurrent copies link to their source recurrent entry
  so copies made on the phone and on the server merge instead of duplicating.
- Other writers: the email script (not running yet) will write to the API too, so the server stays a real
  sync peer. The Expo app is retired.
- Offline-only data must be uploadable later when the user creates an account (a normal sync push).
- Keep the last 6 cycles of expenses/incomes/budgets locally (to be reviewed); older cycles load on demand
  when online. Keep **all** savings locally: "Totalizado" sums every saving ever recorded.
- Order: (1) backend Alembic + sync columns/data migration, (2) backend `/sync` — both done 2026-09-26, see
  the backend CLAUDE.md for the protocol — (3) Room + repositories, (4) queue + WorkManager sync + 6-month
  window, (5) offline-only mode + upload on account creation.
- Pull pages are in version order, not dependency order: a record may reference one that arrives in a later
  page, so Room tables should reference by uuid without enforced foreign keys (or resolve after a full pull).
- Summaries (cycle status, budget totals, "Sin ppto.", grouped savings) must be computed locally, matching
  the backend queries.

## History (see `git log` for details)

2026-09-25/26: scaffold + Auth0 login → networking layer → app shell → Expenses → Incomes and Savings →
UI polish (compact header, pinned dialogs, cycle menu limited to the last 24 cycles with "Ver más") →
signed, R8-shrunk release APK (1.0.0) for sideloading.
Backend fixes made along the way live in `expenses-tracker`.

Open: saving withdrawals UI (`POST /savings/saving-outcome` exists in `SavingsApi`);
tapping a Resumen card to see its movements; a 401 from the API shows an error instead of sending the user
to login; internationalization (user wants it later: UI strings are hard-coded
Spanish in Kotlin, and money/dates are fixed to COP/es-CO in `ui/format/Formatters.kt`).
