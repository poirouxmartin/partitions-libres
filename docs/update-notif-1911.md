# Update notification (2026-11-19)

Branch `dev/poiroux/update-notif-1911`, from `dev/poiroux/english-1911` (e4d7913).
Code commit: `98c5509` (feat(app): notify when an app update is available).
Mirrors Certo (`sure-weather`, 8ed479c), adapted to this app.

## What was added

The app already had an in-app updater (`UpdateChecker`: GitHub releases of the
public repo, banner on Home, checked at start and on return to foreground). The
new part is the background check and the notification:

- `UpdateCheckWorker` (WorkManager `CoroutineWorker`): unique periodic work
  `update-check`, every 6 h (1 h flex), network required, scheduled with
  `KEEP` from `PartitionsApp.onCreate`. WorkManager keeps it across reboots.
  It calls the same `UpdateChecker.check()` as the banner, so it shares its
  cache and rate limit (network at most every 6 h, 30 min after a failure)
  and its APK origin check.
- `UpdateNotifier`: posts "Update available: vX.Y.Z" / "Tap to install it." on
  channel `app_updates` ("App updates" / "Mises à jour de l'appli").
- Notify once: last notified tag in SharedPreferences `update_notif`. A release
  is notified only if newer than the installed version and the last notified
  one; the tag is stored only after the notification was really posted.
  A leftover notification is cleared at app start once that version is installed.
- `UpdateNotifyPolicy`: pure rules, unit-tested in `UpdateNotifyPolicyTest` (6 tests).
- Tap: opens `MainActivity` with action `com.musique.partitions.OPEN_UPDATE`;
  the stack goes back to Home, the "Later" snooze is cleared, and the resume
  check shows the update banner.
- POST_NOTIFICATIONS: declared, asked once at first launch on Android 13+
  (`MainActivity.askNotificationsOnce`). The worker never prompts; without
  the permission it stays silent and the version is notified later if allowed.
- Strings in `values/` (EN) and a new `values-fr/` (FR, only these 3 strings);
  status icon `ic_stat_update`. New dependency: `androidx.work:work-runtime-ktx:2.10.2`.

Why WorkManager (Certo uses JobScheduler): requested in the brief, and it
handles reboot and constraints without extra receivers.

## How to test manually

1. Install a debug APK built with an older version than the latest GitHub
   release (e.g. `versionName = "0.1.0"`), open it once, allow notifications.
2. Force the job: `adb shell cmd jobscheduler run -f com.musique.partitions <jobId>`
   (job id from `adb shell dumpsys jobscheduler | findstr partitions`), or wait.
   If the app already checked in the last 6 h, the cached response is used.
3. Expect "Update available: vX.Y.Z"; tap it: the app opens on Home with the
   update banner.
4. Run the job again: no second notification for the same version.
5. Reset: `adb shell pm clear com.musique.partitions`.

## Verification

- `gradlew :catalog:test :render:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug -PvrvAbis=x86_64`:
  exit 0; app unit tests 24 passed, 0 failed (6 new).
- Not run on an emulator or phone in this session.
- `values-fr` only translates the new strings; the rest of the UI is still
  hard-coded French in Kotlin (unchanged).

## Disabled in 0.3.1

Play Protect blocked Certo for the same pattern (periodic background job that
queries GitHub and prompts to install an APK); the same build without it got a
clean verdict. 0.3.1 no longer schedules `UpdateCheckWorker`, cancels the job
0.3.0 persisted (`UpdateCheckWorker.cancel` at app start), no-ops a run that
fires before that (`ENABLED = false`), drops `POST_NOTIFICATIONS` and the
permission prompt. The in-app check (start and return to foreground) is unchanged.
Code kept for a rework.
