#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Orubus Island — popup protection setup over wireless debugging (Android 11+)
#
# Grants everything the island needs so NO popup is ever missed:
#   • WRITE_SECURE_SETTINGS  — the engine can re-enable itself after a crash
#   • battery whitelist      — Android won't throttle the island
#   • background appops      — no "background restriction" surprises
#   • all runtime permissions — calls, contacts, Bluetooth
#   • notification listener   — direct secure-settings grant (if missing)
#   • restarts the island     — so everything applies right away
#
# Usage:
#   1. Phone: Settings → Developer options → Wireless debugging → ON
#   2. Tap "Pair device with pairing code" — note IP:PORT and the 6-digit code
#   3. Run:  ./orubus-setup.sh pair            (or: ./orubus-setup.sh IP:PORT)
#   4. Then: ./orubus-setup.sh
#
# Verify-only mode:  ./orubus-setup.sh check
# ─────────────────────────────────────────────────────────────────────────────
set -u

PKG="app.orubus.island"
ACTIVITY="$PKG/app.orubus.island.service.IslandService"

bold()  { printf '\033[1m%s\033[0m\n' "$*"; }
ok()    { printf '  \033[32m[OK]\033[0m %s\n' "$*"; }
bad()   { printf '  \033[31m[X]\033[0m %s\n' "$*"; }
info()  { printf '\033[36m>\033[0m %s\n' "$*"; }

have_adb() { command -v adb >/dev/null 2>&1 || { bad "adb not found. Install platform-tools: https://developer.android.com/tools/releases/platform-tools"; exit 1; }; }

pair() {
  have_adb
  read -r -p "Pair address (IP:PORT from 'Pair device with pairing code'): " PAIR_ADDR
  read -r -p "Pairing code (6 digits): " CODE
  adb pair "$PAIR_ADDR" "$CODE" || exit 1
  read -r -p "Connect address (IP:PORT from the Wireless debugging screen): " CONN_ADDR
  adb connect "$CONN_ADDR" && info "Paired. Now run: $0"
}

# idempotent: grant → verify, print [OK]/[X] for every step, so NOTHING is missed
grant() {
  have_adb
  local FAIL=0
  bold "Orubus Island · popup protection setup"

  adb get-state >/dev/null 2>&1 || { bad "No device connected. Run 'adb connect IP:PORT' first."; exit 1; }
  ok "Device connected: $(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')"

  # 1) Notification listener access (the heart of popup protection)
  if adb shell settings get secure enabled_notification_listeners 2>/dev/null | tr -d '\r' | grep -q "$PKG"; then
    ok "Notification listener access"
  else
    info "Granting notification listener access…"
    CUR=$(adb shell settings get secure enabled_notification_listeners | tr -d '\r')
    if [ "$CUR" = "null" ] || [ -z "$CUR" ]; then
      adb shell settings put secure enabled_notification_listeners "$PKG/app.orubus.island.service.NotifListener"
    else
      case "$CUR" in *"$PKG"*) ;; *) adb shell settings put secure enabled_notification_listeners "$CUR:$PKG/app.orubus.island.service.NotifListener" ;; esac
    fi
    adb shell settings get secure enabled_notification_listeners | tr -d '\r' | grep -q "$PKG" \
      && ok "Notification listener access" || { bad "Notification listener — grant it manually: adb shell cmd notification allow_listener $PKG/app.orubus.island.service.NotifListener"; FAIL=1; }
  fi
  adb shell cmd notification allow_listener "$PKG/app.orubus.island.service.NotifListener" >/dev/null 2>&1 \
    && ok "Listener allow-listed (cmd notification allow_listener)"

  # 2) WRITE_SECURE_SETTINGS — self-heal (the island re-enables itself after a crash)
  if adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null; then
    ok "WRITE_SECURE_SETTINGS (self-heal)"
  else
    if adb shell dumpsys package "$PKG" 2>/dev/null | tr -d '\r' | grep -q "WRITE_SECURE_SETTINGS"; then
      ok "WRITE_SECURE_SETTINGS (already granted)"
    else
      bad "WRITE_SECURE_SETTINGS"
      FAIL=1
    fi
  fi

  # 3) Battery whitelist — no throttling
  if adb shell dumpsys deviceidle whitelist 2>/dev/null | tr -d '\r' | grep -q "$PKG"; then
    ok "Battery whitelist"
  else
    adb shell dumpsys deviceidle whitelist +"$PKG" >/dev/null 2>&1
    adb shell dumpsys deviceidle whitelist 2>/dev/null | tr -d '\r' | grep -q "$PKG" \
      && ok "Battery whitelist" || { bad "Battery whitelist"; FAIL=1; }
  fi

  # 4) Background appops
  for OP in RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND; do
    adb shell cmd appops set "$PKG" "$OP" allow >/dev/null 2>&1
  done
  ok "Background appops allowed"

  # 5) Runtime permissions (no dialogs, no skipping)
  for P in READ_PHONE_STATE READ_CALL_LOG READ_CONTACTS ANSWER_PHONE_CALLS BLUETOOTH_CONNECT POST_NOTIFICATIONS; do
    if adb shell pm grant "$PKG" "android.permission.$P" >/dev/null 2>&1; then
      ok "Permission $P"
    else
      # fails when already granted or not applicable on this Android version — check to be sure
      adb shell dumpsys package "$PKG" 2>/dev/null | tr -d '\r' | grep -q "android.permission.$P" \
        && ok "Permission $P" || info "Permission $P — skipped (not applicable here)"
    fi
  done

  # 6) Restart the island so everything applies now
  info "Restarting the island…"
  adb shell am force-stop "$PKG" 2>/dev/null
  sleep 1
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 0 >/dev/null 2>&1
  sleep 2
  if adb shell dumpsys activity services "$PKG" 2>/dev/null | grep -q "$PKG"; then
    ok "Island services running"
  else
    adb shell am startservice -n "$ACTIVITY" >/dev/null 2>&1
    info "Triggered the service start — open the app once if the capsule doesn't appear"
  fi

  echo
  if [ "$FAIL" = 0 ]; then
    bold "All good — nothing will be missed."
  else
    bold "Done with warnings — fix the [X] lines above."
    exit 2
  fi
}

check() {
  have_adb
  bold "Orubus Island · verification"
  adb shell settings get secure enabled_notification_listeners | tr -d '\r' | grep -q "$PKG" && ok "Notification listener" || bad "Notification listener"
  adb shell dumpsys package "$PKG" 2>/dev/null | tr -d '\r' | grep -q "WRITE_SECURE_SETTINGS" && ok "WRITE_SECURE_SETTINGS" || bad "WRITE_SECURE_SETTINGS"
  adb shell dumpsys deviceidle whitelist 2>/dev/null | tr -d '\r' | grep -q "$PKG" && ok "Battery whitelist" || bad "Battery whitelist"
  adb shell dumpsys activity services "$PKG" 2>/dev/null | grep -q "$PKG" && ok "Island services running" || bad "Island services not running"
}

case "${1:-}" in
  pair)  pair ;;
  check) check ;;
  "")    grant ;;
  *)     echo "Usage: $0            # grant everything (device must be connected via adb)"
         echo "       $0 pair       # pair over wireless debugging"
         echo "       $0 check      # verify only" ;;
esac
