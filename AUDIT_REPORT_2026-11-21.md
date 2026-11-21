# Orubus Island — Full-Scale Audit & Improvement Report

**Repository:** `orubus-dev/dynamic-island`  
**Audit date:** 2026-11-21  
**Reviewed branch:** `main`  
**Latest commit inspected:** `9af23a8ce4b3` (1.14.0 notification-stack update)  
**Audit type:** Static repository review, navigation architecture inspection, README/manifest/code-path review. No physical-device test or build was executed in this review.

## Executive summary

Island has already accumulated a substantial feature set: overlay/accessibility engine, notification reply and stacking, media and lyrics, timers/alarms, calls, payment detection, navigation extraction, system-event cards, gesture controls, liquid-glass rendering, self-recovery, diagnostics, update UI, and setup tooling. The code and README describe several fixes that should not be treated as unimplemented ideas.

The strongest next step is **verification, resilience and discoverability**, not simply adding more flashy cards. The settings/navigation surface is broad and its routing is hand-maintained; critical services have Android-version/vendor constraints; and the advertised service resurrection strategy needs testing against real device restrictions and lifecycle edge cases.

## Scope and confidence

### Reviewed directly
- README and English README, feature and roadmap claims
- Android manifest and declared permissions/services/receivers
- MainActivity navigation state and route dispatch
- NavScreen navigation extraction/parser module
- IslandBus state, event queue, deduplication and notification-stack handling
- IslandService visibility, overlay sizing/touch-window coordination, screen-event gating, screenshot flow, lifecycle cleanup (selected portions)
- Engine-related and settings/diagnostic/update components were located in the repository tree; not every source file could be fully traced in this pass.

### Not verified
- Compilation, unit/instrumentation tests, UI automation, or device behavior
- OEM-specific battery/autostart restrictions
- Every individual feature implementation and every screen callback
- Play Store policy compliance or formal security assessment
- Accessibility extraction behavior on every supported navigation app

Findings below are therefore a combination of code-confirmed observations, risk hypotheses, and test recommendations, clearly distinguished where possible.

---

## 1. Navigation and screen routing

### Code-confirmed positive findings
- Screen routes are represented by a typed enum, while the persisted back stack stores route names as strings.
- The stack uses `rememberSaveable`, so its route list survives configuration change and Android saved-state restoration.
- Unknown or obsolete route names are filtered during stack reconstruction; the renderer also has a fallback branch.
- Opening the route already at the top avoids duplicate stack entries.
- Back is handled only while the stack is non-empty.
- Dock navigation clears the detail stack and switches tabs.
- AnimatedContent uses direction state for forward/back transitions.
- Routes include a large number of screens (settings, permissions, diagnostics, appearance, notifications, effects, Bluetooth, update and more).

### Risks / improvement opportunities
1. **Route drift:** enum, dispatch `when`, screen imports and navigation callers are manually maintained. A screen can exist but remain unreachable, or callers can target a route with no renderer.
2. **Unknown-route fallback is visually blank:** this is safe from a crash but can strand the user on an empty screen if the saved stack contains an invalid route.
3. **Stack persistence is string-based:** rename/removal can invalidate persisted state. The current filtering protects against exceptions but silently drops invalid routes.
4. **Tab index is numeric:** magic indexes (`0`, `1`) are fragile as tab ordering evolves.
5. **Back behavior is narrow:** Android system back is covered by BackHandler for detail stack, but behavior from root tabs, modal/dialog state, keyboard/reply-like UI, and dock overlays should be tested systematically.
6. **Route visibility/accessibility:** no automated route reachability matrix or accessibility navigation checks were found in the portions inspected.

### Recommended navigation improvements
- Introduce a centralized route registry (route ID, title, parent/tab, renderer, required prerequisites, optional analytics/diagnostics ID).
- Replace tab integer values with a sealed/enum tab model.
- Add a graceful recovery screen for invalid persisted routes, with a reset-stack action rather than an empty layer.
- Make navigation transitions and back behavior deterministic when rapid taps occur or a screen is opened repeatedly.
- Add tests for: route round-trip, back-stack pop, duplicate pushes, tab reset, invalid saved route, process restoration, and every route's reachability.
- Add an internal navigation inspector showing current tab, stack, route source, and route transition history.

### Route test matrix
| Scenario | Expected behavior | Priority |
|---|---|---|
| Detail → Back | Returns to previous screen | P0 |
| Detail → detail → Back twice | Each screen pops exactly once | P0 |
| Re-open current top route | No duplicate stack entry | P0 |
| Detail → dock tab | Stack clears; correct tab opens | P0 |
| Process death/restoration | Valid stack restored; invalid names safely recovered | P0 |
| Rapid route taps | No race/blank frame/stuck route | P1 |
| Back from modal/dialog | Modal closes before route pops | P1 |
| Root tab → Back | Android/app expected behavior documented and consistent | P1 |
| Landscape/resize while detail screen open | Route and scroll/state remain stable | P1 |
| Screen with long content | Scroll state and top-bar back remain usable | P1 |

---

## 2. Island engine, overlay and lifecycle

### Code-confirmed architecture
- Accessibility service renders the island and maintains separate draw and touch windows.
- Touch-window bounds are updated from the island and bubble bounds; the touch window is temporarily non-touchable when not usable.
- Window resizing is delayed until motion settles to reduce animation jitter.
- Island visibility is calculated from settings, landscape, lock-screen state, foreground app, game mode, and schedule.
- Hidden island state collapses the overlay and suspends Compose activity.
- Screen events are dynamically enabled when the clock/pay readers need them.
- Service cleanup removes callbacks, listeners, readers, windows, sensor activity and state.
- Screenshot-without-island temporarily fades the overlay, then restores it.
- The repository advertises heartbeat/self-healing and boot/update resurrection mechanisms.

### Risks to validate
- Recovery loops and simultaneous recovery attempts can race service startup or produce repeated alarms/notifications.
- OEMs may suppress foreground services, accessibility services, exact alarms, or boot broadcasts differently.
- Temporary overlay fade for screenshots can fail to restore alpha if the process/service is interrupted during the sequence.
- Window touch forwarding and bounds settling are timing-sensitive and require stress testing on refresh-rate variations and screen density/cutout layouts.
- Lifecycle cleanup and re-registration need testing across repeated service disable/enable, reboot, force-stop, update, battery saver, and permission revocation.

### Improvements
- Build an explicit engine state machine: `OFF → STARTING → RUNNING → DEGRADED → RECOVERING → OFF`, with reason codes and bounded recovery attempts.
- Use a recovery backoff policy and a recovery lock to prevent duplicate wake/restart attempts.
- Add a watchdog event ledger (last heartbeat, recovery count, last failure, current degraded capabilities).
- Ensure screenshot restoration is state-safe with a single cleanup path and timeout fallback.
- Add window geometry telemetry (density, bounds, cutout, orientation, touch bounds) to diagnostics.
- Define a vendor-specific troubleshooting guide and let users export a sanitized diagnostic bundle.

---

## 3. Notification pipeline and stack

### Code-confirmed positive findings
- Notification banner queue has bounded capacity and drops oldest events under overload.
- Deduplication has both a longer content signature window and a short burst gate.
- The dedup signature includes source package, title, content/state and style.
- Message burst logic allows distinct text through even when the notification key is shared.
- The notification stack has a bounded size, browse direction state, removal paths, and reply-state handling.
- Stack updates are synchronized with notification dismissal/removal from shade.
- Quiet-hours logic is applied at posting time.

### Potential risks
- Queue saturation can drop older notices by design; priority classes could better preserve calls, timers and payment safety alerts.
- Notification content is sensitive; lock-screen/privacy behavior and screen capture/screenshot flows deserve explicit tests.
- Dedup rules could suppress legitimate repeating system events or allow duplicates across slightly different payloads.
- Notification keys may change across app updates, device restarts, or OEM implementations.
- Stack persistence currently appears to be process-local state in the inspected bus; assess whether lost stack on process death is intentional.

### Improvements
- Introduce priority lanes (critical, interactive, ordinary, low-priority) with fair scheduling and bounded per-lane queues.
- Add notification privacy modes: hide sender, hide message text, hide all content, per-app override, locked-screen override.
- Add per-app notification routing controls: never show, always show, show only when screen on, allow replies, allow stack.
- Add a notification queue/stack inspector to diagnostics with privacy-safe payload redaction.
- Make stack state transitions formally modeled and unit-tested.

---

## 4. Navigation/Maps live extraction

### Code-confirmed
- The app has a dedicated `NavScreen` module that extracts navigation text and imagery from selected navigator screens.
- It merges extracted screen data with notification/live activity data.
- The parser handles distance units, time/ETA, street text and turn imagery.
- It synthesizes a live activity when a navigator provides no suitable notification.
- The reader is tied to foreground navigation app behavior rather than always reading the screen.

### Risks
- Accessibility layouts vary dramatically by app version, locale, screen orientation, theme, and ads/menus.
- Parser heuristics may confuse total distance, turn distance, arrival time, street names or nearby UI labels.
- Screen reading should be limited, transparent, and disabled when not in a supported navigator; validate privacy expectations and consent copy.
- Images/turn icons may not match expected classifications.

### Improvements
- Add a parser fixture corpus for supported apps and locales, including anonymized representative node layouts.
- Track parser confidence and expose a diagnostic status instead of silently presenting questionable data.
- Add app-specific adapters with version-independent fallback heuristics.
- Add an explicit supported navigator list and opt-in screen extraction switch.
- Test at least Google Maps, Waze, Yandex Navigator, 2GIS and unsupported-app fallback if available.

---

## 5. Permissions, privacy and safety

The manifest declares sensitive permissions and services including phone state/call log/contacts, notification listener, accessibility, Bluetooth connection, foreground service, exact alarms, wake/screen flags, install packages, and secure-settings permission via ADB. These are understandable for the feature set but increase the need for precise explanation and controls.

### Recommended improvements
- Permission dashboard with purpose, data read, feature affected, last use, and revoke shortcut.
- Least-privilege review: verify each permission is used and avoid requesting it before the feature is enabled.
- Privacy-first lock screen mode, including hiding notification text and reply controls.
- Add a data-flow/privacy page explaining what is processed locally, what is transient, and whether any network request occurs.
- Verify update download/install flow and FileProvider path scope.
- Add a permission-revocation test suite: revoke each permission while running and ensure no crash, stuck state or misleading UI.
- Review accessibility-service description and onboarding copy for clarity and consent.

---

## 6. Settings UX and discoverability

The settings surface is extensive and feature-rich, but the number of screens risks making options hard to locate.

### Improvements
- Settings search with indexed synonyms and bilingual labels.
- "Quick setup" wizard that checks accessibility, notification access, battery restrictions, call access, and Bluetooth only when needed.
- Settings home reorganized around: Island behavior, notifications, appearance, system events, permissions/engine, advanced.
- Consistent breadcrumb/title/back patterns, empty states, descriptions, reset defaults, and preview-before-apply.
- Presets: Minimal, Balanced, Full-featured, Battery saver, Privacy-first.
- A per-feature "Test now" action and clear status indicator.
- Contextual help links directly to the relevant OS settings.

---

## 7. Performance, battery and rendering

### Risks / test areas
- Live blur, refraction, tilt sensors, equalizer, animations, and large expanded windows may be costly on older devices.
- Sensor registration, animation jobs and observers must be cancelled when hidden or disabled.
- Refresh-rate changes, screen-off, battery saver and thermal throttling can expose timing issues.

### Improvements
- Performance profile: Battery / Balanced / Fluid, with measurable caps on blur, sensor update rate, animation and live polling.
- Use frame-time and memory benchmarks for collapsed, expanded, notification-stack, lyrics, and navigation states.
- Ensure sensor rate is reduced or disabled when screen-off/hidden.
- Add thermal/battery saver degradation strategy and make it visible to users.
- Cache expensive artwork/color extraction and avoid unnecessary recompositions.

---

## 8. Build, testing and release quality

### Recommendations
- CI should run lint, compile, unit tests, instrumentation smoke tests, and dependency/license checks.
- Add a UI test that traverses every route and verifies screen title/content presence.
- Add screenshot tests for key island states across density/size/cutout and light/dark settings.
- Add test devices/API levels: Android 8/9 baseline, 11, 12, 13, 14, 15, 16; prioritize representative OEMs.
- Add dependency update automation and vulnerability scanning.
- Validate shrinker/ProGuard release builds and verify release signing/fingerprint.
- Add release checklist including permission behavior, update installation, and migration.

---

## 9. Feature additions beyond the current roadmap

Prioritize these rather than duplicating existing features:

### High-value additions
1. **Context-aware notification privacy**: lock-screen masking, per-app privacy, private reply mode.
2. **Island profiles/presets**: switch behavior instantly for work, gaming, driving, sleep, focus.
3. **Adaptive battery/performance mode**: automatic effect throttling and polling control.
4. **Navigation reliability mode**: confidence indicator, supported-app adapter, parser diagnostics.
5. **Settings search**: fast, indexed, bilingual search with deep links to settings.
6. **Interactive diagnostic report**: engine, permissions, battery restrictions, notification listener, overlay geometry, recent recovery.
7. **Recovery timeline**: explain why the engine stopped and what was attempted.
8. **Per-app event filters**: whitelist/blacklist, quiet behavior, and notification priority.
9. **Notification action safety**: ensure reply and dismiss actions are guarded against stale notification objects.
10. **Accessibility audit mode**: screen reader labels, keyboard/navigation focus, high-contrast and reduce-motion behavior.

### Longer-term ideas
- Island extension SDK/API with permission-scoped content, rate limits and source attribution.
- Plugin/extension sandbox and signed extension registry, only after API threat modeling.
- Calendar/weather/context capsules with explicit permission and privacy settings.
- App-specific live-activity adapters for ride, delivery, sports and download progress.
- Exportable preset backup and restore with migration validation.
- A/B animation performance experiments behind opt-in flags.
- Haptics composer with system intensity and quiet mode.
- Adaptive layout for foldables, tablets, landscape and split-screen.
- Multi-island/activity arbitration UI with user-controlled priority and focus.
- Accessibility-aware gesture configuration with alternative actions.

---

## 10. Priority roadmap

### P0 — Reliability and navigation
- [ ] Automated route reachability and stack tests
- [ ] Invalid route recovery UI
- [ ] Typed tab model and centralized route registry
- [ ] Recovery state machine/backoff and watchdog telemetry
- [ ] Screenshot fade restoration safety
- [ ] Permission revoke/restore tests

### P1 — Privacy, battery and diagnostics
- [ ] Notification privacy profiles and per-app privacy
- [ ] Permission-purpose dashboard
- [ ] Settings search
- [ ] Engine/overlay diagnostic export
- [ ] Performance/battery profiles
- [ ] Queue priority and notification-state tests

### P2 — Feature expansion
- [ ] Island presets/profiles
- [ ] Navigator adapter fixtures and confidence display
- [ ] Accessibility/screen-reader audit
- [ ] Context capsule extensions
- [ ] Extension SDK threat model and prototype

## Conclusion

The README and inspected source show that many formerly planned features are already present, including navigation extraction, notification stacking/reply, diagnostics, Liquid Glass, service recovery, and a broad settings UI. The most valuable audit follow-up is to convert these capabilities into **observable, testable guarantees**: route coverage, service lifecycle state, parser confidence, permission-revocation resilience, privacy controls, and battery/performance budgets.

This report is a static audit, not a claim that every listed risk is an actual defect. Prioritize the P0 items, verify them on-device and in CI, and then use the roadmap to add features without destabilizing the existing island.
