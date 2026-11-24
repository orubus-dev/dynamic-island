<p align="center">
<a href="README.md"><img src="docs/lang/ru-off.svg" height="46" alt="Русский"></a>&nbsp;&nbsp;<a href="README.en.md"><img src="docs/lang/en-on.svg" height="46" alt="English"></a>
</p>

<div align="center">

# 🏝️ Orubus Island — Dynamic Island for Android

**An iPhone-style Dynamic Island around the camera of any Android phone**

Notifications with reply · navigation · payments · music with lyrics · calls · timers · downloads · charging · flashlight · system events

[![Version 2.0.0](https://img.shields.io/badge/version-2.0.0-C6C3F7?style=for-the-badge&labelColor=1C1B22)](https://github.com/orubus-dev/dynamic-island/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/orubus-dev/dynamic-island/android.yml?style=for-the-badge&label=build&labelColor=1C1B22)](https://github.com/orubus-dev/dynamic-island/actions)
![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![No ads](https://img.shields.io/badge/ads-0-black?style=flat-square)

### Download · Orion Store · app.orubus.island

</div>

---

## What's new in 2.0.0

- 🌍 **Eight languages** — Español · Français · Deutsch · Português · العربية next to Russian and English, a language picker on first launch and a switcher in settings (Features → Language).
- 📡 **Self-heal via Wireless debugging (Android 11+)** — pair with the six-digit code inside the island: TLS 1.3 + SPAKE2, a 1:1 port of the `adb pair` protocol. One pairing and the island revives itself after any crash.
- 📶 **Ten new island states** — VPN, hotspot, Do not disturb, airplane mode, silent and vibrate, battery saver, low battery, roaming, storage low, no SIM — each with its own toggle under System alerts → States.
- 🪞 **Light Liquid Glass** — in light theme the glass turns milky with dark ink, caustics and a dark lens rim: refraction and blur in both themes.
- 📣 **Own-product promos only** — a "From Orubus" card (Relay, MANGACA, Payworth, Sol…) fed from the repo or your own Supabase project, hideable forever with one tap.
- 🩹 **Recovery that actually recovers** — the notification listener rebinds too; a successful heal shows an "Island revived" pill.

## What's new in 1.14.0

- Notification stack — browse all recent notifications in the island, dismiss individually
- Navigation extraction — live turn-by-turn from Google Maps, Waze shown in the island
- ADB Shield — protects the service from ADB kill commands
- Liquid Glass refraction — full specular highlight + blur refraction on the island surface
- Extended bank support — PayWorth, OPay, Kuda, GTBank payment detection
- Service resurrection engine — heartbeat monitor + bounded recovery with backoff policy
- Diagnostics screen — view engine state, recovery count, last failure reason

---

## Available on Orion Store

Package: app.orubus.island | Category: System Tools | Requires: Android 8.0+ + Accessibility

---

## Roadmap

| Feature | Status |
|---|---|
| AI notification summarization (Gene AI) | In progress |
| Widget integration | Planned |
| Bluetooth device island card | Planned |
| NFC tap detection | Planned |
| Custom island themes | Planned |
| Orion Store auto-updates | Planned |

---

## Installation

1. Download APK from Releases or install from Orion Store (app.orubus.island)
2. Grant Accessibility permission
3. Grant Notification Listener permission
4. Toggle island on from main screen

## Credits

Built on top of Akira9021/Dynamic-Island with new features, engine redesign, and Orubus branding by Velocity Labs.
