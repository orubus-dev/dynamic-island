<div align="center">

# 🏝️ Orubus Island

**iPhone-style Dynamic Island for any Android phone — hardened, liquid-glassed, self-healing.**

A fork of the excellent [Akira9021/Dynamic-Island-](https://github.com/Akira9021/Dynamic-Island-) (upstream README in [README.upstream.md](README.upstream.md)), rebuilt as `app.orubus.island`.

**Notifications with reply · navigation · payments · music with lyrics · calls · timers · downloads · charging · flashlight · system events**

![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Package](https://img.shields.io/badge/package-app.orubus.island-34C759?style=flat-square)

</div>

---

## ✨ What's new in 1.7.0 (the Orubus fork)

### 🫀 Island Engine — the island can no longer silently die
Upstream issue #13 ("when the app crashes, it doesn't try to start up again") is fixed with a three-layer resurrection engine:

1. **Heartbeat** — the overlay service beats every 10 s; the keep-alive foreground service watches the pulse.
2. **Self-healing** — grant `WRITE_SECURE_SETTINGS` once over wireless debugging and the engine flips its own accessibility service off→on when it dies (no more "turn it off and on again" by hand). Without the grant it honestly reports *Island crashed* with a tap-to-fix notification.
3. **Crash alarm** — uncaught exceptions are intercepted and a 1.5-second exact alarm wakes the process back up. Boot and app-update broadcasts revive it too.

Status is visible in **Keep the island running → Island engine**: `Running` / `Reviving` / `Down`, plus a toggle and the one-line adb recipe.

### 🚫 No more double pop-ups
A dedup gate in the island event bus: an identical banner (title + text + style) re-posted within 4.5 s is swallowed — messenger apps love re-posting the same notification under a new key (read receipts, reactions, summary + child).

### 🧊 Liquid Glass, now with real blur on state change
Upstream's painted glass (specular highlights, tilt-reactive lens rim, chromatic dispersion) now also **blurs what's behind the island while it's expanded** (window blur-behind, Android 12+), and fades back to sharp when collapsed. Graceful no-op on devices without hardware blur.

### 📡 Wireless debugging workflow
See [Wireless debugging](#-wireless-debugging) — pair once, then install builds and grant the self-healing permission straight from your computer.

## 📡 Wireless debugging

```bash
# one-time pairing (Android 11+): Developer options → Wireless debugging → Pair device
adb pair <PHONE_IP>:<PAIR_PORT>        # enter the 6-digit code
adb connect <PHONE_IP>:<DEBUG_PORT>

# install a build
adb install -r app-orubus-island.apk

# let the island heal itself after crashes/reboots (recommended)
adb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS
```

`scripts/orubus-debug.sh` wraps pair/connect/install/logcat for convenience.

## 🔨 Building

```bash
./gradlew assembleDebug        # debug APK, signed with the debug key
./gradlew assembleRelease      # unsigned; CI signs release builds with its own key
```

## 🗺️ Roadmap ideas (the "crazy features" board)

Pulled from the feature-ideation pass — candidates for future versions:

- **LiveActivities bridge**: mirror web/app live events into the island (orders, streams, builds)
- **Context pills**: weather on unlock, next calendar event, step count — rotating idle capsule
- **Beat-synced glass**: FFT-driven refraction shimmer synced to the playing track
- **Island API**: tiny content-provider so any Orubus app can post into the island
- **Gesture palette**: double-knock (back-tap) to expand, flip-to-silence with proximity
- **Theme packs**: seasonal glass tints, Material You-driven dispersion colors

## 📄 License & credits

Everything here builds on the original work of [Akira9021](https://github.com/Akira9021) and contributors — see the upstream README and license. Fork changes: Orubus.
