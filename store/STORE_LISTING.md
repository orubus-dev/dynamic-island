# Orubus Island — Orion Store listing

Everything needed to submit Orubus Island to the Orion Store. All files in this folder
(`store/`) are committed, so their permanent URLs are:

| Asset | URL |
| --- | --- |
| App icon 512×512 | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/icon.png` |
| Feature graphic 1024×500 | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/feature.png` |
| Screenshot 1 — notifications + stack | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-1.png` |
| Screenshot 2 — music player | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-2.png` |
| Screenshot 3 — charging | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-3.png` |
| Screenshot 4 — phone payment | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-4.png` |
| Screenshot 5 — idle expanded | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-5.png` |
| Screenshot 6 — states + language | `https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/screenshots/shot-6.png` |
| APK (signed release) | `https://github.com/orubus-dev/dynamic-island/releases/latest` — `OrubusIsland.apk` |

## Basic

- **Name:** Orubus Island — Dynamic Island
- **Package:** `app.orubus.island`
- **Version:** 2.0.0 (versionCode 22)
- **Category:** Personalization
- **Tags:** dynamic island, notifications, liquid glass, self-heal, wireless debugging
- **Min Android:** 8.0 (API 26). Best on 11+ (wireless-debugging self-heal).
- **Price:** Free, no ads network — promo cards for Orubus products only, hideable.
- **Languages:** English, Русский, Español, Français, Deutsch, Português, العربية

## Short description (≤80 chars)

The real Dynamic Island for Android — notifications, payments, self-heal, liquid glass.

## Full description

Orubus Island puts a true Dynamic Island over your camera cutout — not a picture of one, a working one.

**Everything lands on the island.** Music with album art, lyrics and seek bar. Messages from any
messenger with Reply and Mark-as-read right in the capsule. Incoming calls with answer and decline.
Timers, stopwatch and countdowns. Download progress. Navigation from Yandex Navigator, 2GIS and
Google Maps — distance, street and turn arrow without leaving your game. Phone payments: the island
shows "Hold near reader" the moment a payment screen opens and confirms "Paid" with the amount the
instant it happens — Mir Pay, T-Pay, Google Wallet, Samsung Wallet and any NFC bank app.

**Ten phone states** — VPN, hotspot, Do not disturb, airplane mode, silent and vibrate, battery
saver, low battery, roaming, low storage and missing SIM — each with its own toggle, shown only
when nothing more important is on the island.

**Liquid Glass, both themes.** Translucent glass with caustics, a travelling sheen, chromatic rim
dispersion and highlights that shift as you tilt the phone. Dark glass by default; in light theme
the glass turns milky with dark ink — refraction and blur in both.

**It heals itself.** A watchdog engine watches the island's pulse and revives it after crashes,
reboots and app updates. Pair once with Wireless debugging (six-digit code, right in the app —
a 1:1 port of the adb pair protocol over TLS 1.3 + SPAKE2) and the island gets
WRITE_SECURE_SETTINGS: it revives itself after every crash, no computer needed. Classic adb still
works for emulators and older phones.

**Eight languages** with a language picker on first launch and a switcher in settings: English,
Русский, Español, Français, Deutsch, Português, العربية — dates and units follow along.

**Gestures, tuned like iOS.** Tap, double tap, long press and partitioned long press; swipe to
change tracks; swipe up to fold a notification into a badge with a counter; flip through the whole
stack sideways. Expanded island with quick tiles: timer, stopwatch, flashlight, screenshot,
camera, lock.

**Private by design.** Nothing is uploaded. Nothing is tracked. The only network calls are the
update check against GitHub and the promo card listing Orubus's own apps (hideable with one tap).

**It doesn't get killed.** A foreground service keeps the process alive, battery whitelisting is
one tap, quick-settings tile turns the island off for your bank and back on after.

Root is never required. Open-source code, signed release APKs on GitHub Releases with in-app
updates that keep your settings.

## What's new in 2.0.0

Eight languages with a startup picker · self-heal via Wireless debugging (Android 11+) · ten new
island states · light-mode Liquid Glass with refraction and blur · own-product promo card · the X
gone from switches · notification listener rebind after crashes, reboots and updates.

## Permissions (justification for the store review)

| Permission | Why |
| --- | --- |
| Accessibility service | Draws the island over the status bar; reads the foreground app name to hide the player in its own app, navigation screens for the turn distance, Clock screens for timers, payment screens for "Hold near reader"/"Paid" |
| Notification listener | Music, progress, timers and message cards in the island; replies are sent through the notification's own action buttons |
| READ_PHONE_STATE / READ_CALL_LOG / READ_CONTACTS | Incoming-call card with the contact's name and the answer/decline buttons |
| POST_NOTIFICATIONS | "The island stopped" alerts and the timer ring when the island is dead |
| BLUETOOTH_CONNECT | Headphone name and battery in the player |
| WRITE_SECURE_SETTINGS (granted once by the user via adb/wireless pairing) | Self-heal: re-enables the island's own service after a crash |
| RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE | Restarts and keeps the island alive after reboot |
| REQUEST_INSTALL_PACKAGES | In-app updates from GitHub Releases |
| INTERNET | Update check and the promo card feed only |
