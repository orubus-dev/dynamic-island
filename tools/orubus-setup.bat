@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM Orubus Island - popup protection setup over wireless debugging (Windows)
REM
REM Grants everything the island needs so NO popup is ever missed:
REM   - WRITE_SECURE_SETTINGS (self-heal)   - battery whitelist
REM   - background appops                   - runtime permissions
REM   - notification listener access        - restarts the island
REM
REM Usage:
REM   1. Phone: Settings - Developer options - Wireless debugging - ON
REM   2. Tap "Pair device with pairing code", note IP:PORT + code
REM   3. orubus-setup.bat pair
REM   4. orubus-setup.bat
REM ─────────────────────────────────────────────────────────────────────────────
setlocal enabledelayedexpansion
set PKG=app.orubus.island
set LISTENER=%PKG%/app.orubus.island.service.NotifListener

where adb >nul 2>nul || (
    echo [X] adb not found. Install platform-tools and add it to PATH.
    exit /b 1
)

if "%1"=="pair" (
    set /p PAIR_ADDR=Pair address (IP:PORT from "Pair device with pairing code"):
    set /p CODE=Pairing code (6 digits):
    adb pair %PAIR_ADDR% %CODE% || exit /b 1
    set /p CONN_ADDR=Connect address (IP:PORT from the Wireless debugging screen):
    adb connect %CONN_ADDR%
    echo Paired. Now run: orubus-setup.bat
    exit /b 0
)

adb get-state >nul 2>nul || (
    echo [X] No device connected. Run "adb connect IP:PORT" first.
    exit /b 1
)
echo Orubus Island - popup protection setup

REM 1) Notification listener
adb shell settings get secure enabled_notification_listeners | findstr /C:"%PKG%" >nul
if errorlevel 1 (
    echo [>] Granting notification listener access...
    adb shell cmd notification allow_listener %LISTENER%
    adb shell settings get secure enabled_notification_listeners | findstr /C:"%PKG%" >nul
    if errorlevel 1 (
        echo [X] Notification listener - open the app and grant it manually.
    ) else (
        echo   [OK] Notification listener access
    )
) else (
    echo   [OK] Notification listener access
)

REM 2) WRITE_SECURE_SETTINGS
adb shell pm grant %PKG% android.permission.WRITE_SECURE_SETTINGS >nul 2>nul
if errorlevel 1 (
    adb shell dumpsys package %PKG% | findstr /C:"WRITE_SECURE_SETTINGS" >nul
    if errorlevel 1 (
        echo [X] WRITE_SECURE_SETTINGS
    ) else (
        echo   [OK] WRITE_SECURE_SETTINGS (already granted)
    )
) else (
    echo   [OK] WRITE_SECURE_SETTINGS (self-heal)
)

REM 3) Battery whitelist
adb shell dumpsys deviceidle whitelist | findstr /C:"%PKG%" >nul
if errorlevel 1 (
    adb shell dumpsys deviceidle whitelist +%PKG% >nul 2>nul
    adb shell dumpsys deviceidle whitelist | findstr /C:"%PKG%" >nul
    if errorlevel 1 ( echo [X] Battery whitelist ) else ( echo   [OK] Battery whitelist )
) else (
    echo   [OK] Battery whitelist
)

REM 4) Background appops
adb shell cmd appops set %PKG% RUN_IN_BACKGROUND allow >nul 2>nul
adb shell cmd appops set %PKG% RUN_ANY_IN_BACKGROUND allow >nul 2>nul
echo   [OK] Background appops allowed

REM 5) Runtime permissions
for %%P in (READ_PHONE_STATE READ_CALL_LOG READ_CONTACTS ANSWER_PHONE_CALLS BLUETOOTH_CONNECT POST_NOTIFICATIONS) do (
    adb shell pm grant %PKG% android.permission.%%P >nul 2>nul
    if errorlevel 1 (
        echo   [..] Permission %%P - skipped or already granted
    ) else (
        echo   [OK] Permission %%P
    )
)

REM 6) Restart the island
echo [>] Restarting the island...
adb shell am force-stop %PKG% >nul 2>nul
timeout /t 1 /nobreak >nul
adb shell monkey -p %PKG% -c android.intent.category.LAUNCHER 0 >nul 2>nul
echo.
echo All good - nothing will be missed.
endlocal
