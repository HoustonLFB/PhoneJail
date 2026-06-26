# PhoneJail 🔒

Lock your phone away, stay focused, and (soon) share your focus times Strava-style.

Tap your phone on an NFC tag to start a **focus session**. PhoneJail times how long
the phone sits untouched. Pick it up and the run ends — just like a Strava activity
stops when you stop moving. Every session is saved to a local history with streaks and
personal bests.

> This repo is **v1: Android app — timer + local history**. The Strava-like web app for
> sharing/comparing times with friends is the next milestone (see [Roadmap](#roadmap)).

## How it works

1. **Tap to start** — a tap on a PhoneJail NFC tag launches the app and starts the timer.
   No tag yet? Press **Start focus session** to run one manually.
2. **Put it down** — once the screen turns off, the session is *armed*.
3. **Don't touch it** — a foreground service keeps the timer alive and shows it in a
   notification.
4. **It ends when you pick it up** — unlocking the phone records the session (`broken`),
   or you can tap the tag again / press **Release** for a clean finish (`released`).
5. **History & stats** — every session is stored on-device (Room): today's total, current
   day streak, personal best, lifetime total.

The "good finish vs picked up" distinction is recorded but not penalised — the headline
number is simply *how long you lasted*.

## Architecture

```
MainActivity ──► SessionManager ──► FocusService (foreground)
 (NFC intents)    (single source     • live timer + notification
 (Compose UI)      of truth for       • ACTION_SCREEN_OFF  → arm
                   the live session)   • ACTION_USER_PRESENT → end (broken)
                        │
                        ▼
                 FocusRepository ──► Room (FocusSession history)
                        │
                        ▼
              HomeViewModel ──► HomeScreen (Compose)
```

| Layer | Files |
|-------|-------|
| NFC entry / UI host | `MainActivity.kt` |
| Session state machine | `session/SessionManager.kt`, `session/ActiveSession.kt` |
| Background timer + "touch" detection | `service/FocusService.kt` |
| Persistence | `data/` (`FocusSession`, DAO, `PhoneJailDatabase`, `FocusRepository`) |
| Stats (streak/best/today/total) | `data/FocusStats.kt` |
| UI | `ui/HomeScreen.kt`, `ui/HomeViewModel.kt`, `ui/theme/` |
| Tag programming helper | `nfc/NfcTag.kt` |

**Detection model.** "Don't touch it" is detected via `ACTION_SCREEN_OFF` (arm) followed by
`ACTION_USER_PRESENT` (unlock = touched). Arming only after the first screen-off means the
unlock you did *to start* the session via the app doesn't immediately end it. A short
debounce in `SessionManager` stops a re-tap right after a stop from spinning up a new run.

## Build & run

Requires the Android SDK (e.g. via Android Studio). Create `local.properties` pointing at
your SDK (Android Studio does this automatically when you open the project):

```
sdk.dir=/path/to/Android/sdk
```

Then:

```bash
./gradlew assembleDebug        # build the APK
./gradlew installDebug         # install on a connected device/emulator
```

- **minSdk 24**, **targetSdk 34**, Kotlin 2.0 + Jetpack Compose, Room, KSP.
- NFC auto-launch needs a **physical device** (emulators have no NFC). The manual
  **Start** button lets you exercise everything without a tag.

> Note: this scaffold was generated in an environment without the Android SDK, so it has
> not been compiled here. Open it in Android Studio (or run the Gradle commands above) for
> the first build.

## Programming a tag

Any cheap NTAG21x sticker works. PhoneJail auto-launches from an NDEF *external* record
(`phonejail.com:session`), matched by the `NDEF_DISCOVERED` filter in the manifest.

`nfc/NfcTag.program(tag)` writes that record onto a blank/formatable tag. Wiring a
"Program this tag" screen into the UI is a small TODO; until then you can write the record
with any NFC writer app using the external type `phonejail.com:session`. As a fallback the
app also registers `TECH_DISCOVERED`, so an unprogrammed tag still works if you pick
PhoneJail from the chooser.

## Roadmap

- [ ] In-app "Program this tag" onboarding flow (helper already in `NfcTag`).
- [ ] **Strava-like web app**: accounts, friends, a shared feed of focus sessions, weekly
      leaderboards. The on-device `FocusSession` model is the sync unit; add a sync
      endpoint + auth and push sessions up.
- [ ] Optional per-session goals ("lock for 25 min") with success/fail.
- [ ] Widget / quick tile to start without the tag.
