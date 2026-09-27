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
- `./gradlew testDebugUnitTest` — JVM unit tests (Room DAOs and sync via Robolectric, MockWebServer, formatters…).
  They run on a JDK 21 toolchain that Gradle downloads (Robolectric doesn't support newer JDKs).
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

Kotlin, Jetpack Compose (Material 3), Hilt, Auth0.Android v4, Room 2.8, DataStore, Retrofit 3 + OkHttp 5 +
kotlinx.serialization.
AGP 9 with built-in Kotlin; versions in `gradle/libs.versions.toml`. No Navigation Compose: the shell switches
tabs by state and forms are dialogs.

`app/src/main/java/com/jldev/expensestracker/android/`
- `auth/AuthRepository` — Auth0 login/logout, `CredentialsManager` (auto refresh), `isSignedIn` flow.
- `data/local/` — Room: `Entities.kt` (one class per synced table, doubling as the /sync JSON record), `Daos.kt`
  (including the summary queries that mirror the backend SQL), `AppDatabase`. Schemas exported to
  `app/schemas/` (bump the version and add a migration when entities change).
- `data/model/` — what screens show: `ExpenseItem`, `BudgetTotal` (+ `UNBUDGETED`), `CycleStatus`, `GroupedSaving`…
- `data/sync/` — `SyncApi` (GET/POST /sync, GET /sync/cycles/{uuid}), `SyncTables` (wire name ↔ DAO per table;
  `save`/`delete` build a `LocalChange`), `SyncStore` (cursor in DataStore), `SyncRepository` (`save` = apply
  locally + queue; `sync` = push the queue in order, then pull within the window; `loadCycle`; `clear`),
  `SyncWorker` + `WorkManagerSyncScheduler` (sync soon after changes and when the connection returns,
  every 30 min while signed in; stopped on logout).
- `data/local/PendingChange.kt` — the offline queue (`pending_changes`): every change carries the whole record
  and they're never merged, so pushing in order is always safe.
- `data/*Repository` — per area: read Flows from Room, and turn user actions into sync pushes, including the
  side effects the old REST endpoints did server-side (e.g. a recurrent budget also creates/edits/deletes the
  active cycle's copy; saving types are found or created by capitalized name). `CycleRepository` — cycles,
  selected/current cycle (null selection = the active one) and its status.
- `data/remote/` — `ApiJson`, `AuthInterceptor`, `toUserMessage()` for Spanish error text.
- `di/` — `NetworkModule` (OkHttp/Retrofit/SyncApi), `DatabaseModule`.
- `ui/AppRoot` — login vs. shell; owns a session-scoped `ViewModelStore` cleared on logout.
- `ui/shell/` — drawer (Gastos/Ingresos/Ahorros), bottom tabs per section (`Destinations.kt`), top bar with
  the cycle summary, snackbar, and the add button slot.
- `ui/common/` — `EntryListViewModel` (Room-backed list + form dialog + submit; refresh = sync),
  `CycleEntryListViewModel` (items of the current cycle), `runReporting`/`report`.
- `ui/components/` — `EntryListScreen`, `EntryLazyColumn`, `EntryFormDialog`/`EntryForm` (money field),
  `EntryCard`, `BudgetChips`, `CyclePicker`, `AddButtonHost`, …
- Feature screens: `ui/expenses`, `ui/budgets` (recurrent budgets = "Presupuestos" tab), `ui/incomes`,
  `ui/savings` (Resumen = grouped savings, Ahorros, Recurrentes).

## Conventions and gotchas

- UI text is Spanish, matching the Expo app. Money is COP in whole pesos (`formatMoney`, es-CO).
- Backend datetimes are naive but UTC → modelled as `Instant` (`InstantSerializer` reads naive as UTC, writes
  `Z`). Cycle dates are `LocalDate`. Always send `date_*` on create.
- `ApiJson`: snake_case naming, `explicitNulls = false` (Kotlin nulls are omitted). To send a real JSON null,
  use a `JsonElement` field — see `ExpenseUpdate.budgetId` / `budgetChange()` (clearing an expense's budget).
- Screens read only from Room; everything is keyed by uuid (no server integer ids on the phone). Writes go
  through `SyncRepository.save`: applied to Room at once (works offline) and queued; WorkManager pushes the
  queue when there's a connection. A pull never overwrites a record with queued changes. Push results:
  merged → local references (and queued changes) are repointed to the server's uuid; rejected → the local
  record is restored to the server's copy, or removed if the server has none, and the shell shows a message.
  Logout asks first when changes are still queued (logging out clears the database, queue and cursor).
- Sync window: pulls keep the last 6 monthly cycles of budgets/expenses/incomes (`SyncRepository.windowStart`);
  older ones are pruned on the first sync of each process and fetched on demand when picked
  (`CycleEntryListViewModel.selectCycle` → `loadCycle`). Savings and cycles are always complete.
- Room schema changes: bump the version and add a migration (`AutoMigration` when Room can infer it; 1→2
  added `pending_changes`). Released versions have user data on the phone.
- `BudgetTotal.UNBUDGETED` is the synthetic "Sin ppto." entry: a list filter only, never offered in the form.
- A new list screen: subclass `EntryListViewModel` (or `CycleEntryListViewModel`) and implement `items()`;
  `uiState` is lazy because subclasses aren't constructed yet during the base init. Mutations go through
  `submit(...)`.
- Launching `async` requests from `viewModelScope.launch` must be wrapped in `coroutineScope {}` — a failure
  otherwise escapes the try/catch and crashes the app.
- Lazy lists/rows anchor on the first visible item's key: an always-present footer, or a chip added in
  front later (e.g. "Sin ppto." once unbudgeted expenses sync in), would scroll the view unexpectedly.
  `EntryLazyColumn` only emits keyed items; `BudgetChips` re-scrolls when its first chip changes.
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
- Order: (1) backend Alembic + sync columns/data migration, (2) backend `/sync`, (3) Room + repositories, screens
  reading from Room, writes pushed online — all done 2026-09-26 — (4) offline write queue + WorkManager sync +
  6-month window with older cycles on demand — done 2026-09-27 — (5) offline-only mode + upload on account
  creation.
- Pull pages are in version order, not dependency order: a record may reference one that arrives in a later
  page, so Room tables should reference by uuid without enforced foreign keys (or resolve after a full pull).
- Summaries (cycle status, budget totals, "Sin ppto.", grouped savings) must be computed locally, matching
  the backend queries.

## History (see `git log` for details)

2026-09-25/26: scaffold + Auth0 login → networking layer → app shell → Expenses → Incomes and Savings →
UI polish (compact header, pinned dialogs, cycle menu limited to the last 24 cycles with "Ver más") →
signed, R8-shrunk release APK (1.0.0) for sideloading → offline steps 1–3 (Room + /sync), released as 1.1.0
(versionCode 2) on 2026-09-27 after the backend migration reached prod.
Offline step 4 (write queue + WorkManager sync + 6-month window) released as 1.2.0 (versionCode 3) on
2026-09-27 after the backend reached prod.
Backend fixes made along the way live in `expenses-tracker`.

Open: saving withdrawals UI (backend has `POST /savings/saving-outcome`; the app would now write them via sync);
tapping a Resumen card to see its movements; a 401 from the API shows an error instead of sending the user
to login; internationalization (user wants it later: UI strings are hard-coded
Spanish in Kotlin, and money/dates are fixed to COP/es-CO in `ui/format/Formatters.kt`).
