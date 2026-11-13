#!/usr/bin/env bash
# Install RietveldTV on an Android TV over Wi-Fi and make it the home screen.
# Usage: scripts/install.sh <tv-ip> [path/to/app-debug.apk]
set -euo pipefail

TV="${1:?usage: scripts/install.sh <tv-ip> [apk]}"
APK="${2:-app/build/outputs/apk/debug/app-debug.apk}"
case "$TV" in *:*) DEV="$TV" ;; *) DEV="$TV:5555" ;; esac   # accepts "ip" or "ip:port"

command -v adb >/dev/null || { echo "adb not found. Install android-tools-adb / platform-tools first."; exit 1; }
[ -f "$APK" ] || { echo "APK not found: $APK (build it with ./gradlew assembleDebug, or download one from the Releases page)"; exit 1; }

adb connect "$DEV"
adb -s "$DEV" get-state >/dev/null || { echo "TV not reachable or not authorised. Accept the 'Allow debugging?' prompt on the TV."; exit 1; }

adb -s "$DEV" install -r "$APK"
adb -s "$DEV" shell cmd package set-home-activity --user 0 dev.oliver.home/.HomeActivity
adb -s "$DEV" shell input keyevent KEYCODE_HOME
echo
echo "Installed. Press Home on the remote."
echo "If Home still opens the stock launcher, see 'Troubleshooting' in the README."
