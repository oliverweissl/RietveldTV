#!/usr/bin/env bash
# Send a config.json to the TV and reload the launcher.
# Usage: scripts/push-config.sh <tv-ip> [config.json]
set -euo pipefail
TV="${1:?usage: scripts/push-config.sh <tv-ip> [config.json]}"
CFG="${2:-config.json}"
case "$TV" in *:*) DEV="$TV" ;; *) DEV="$TV:5555" ;; esac   # accepts "ip" or "ip:port"
python3 -m json.tool "$CFG" >/dev/null || { echo "$CFG is not valid JSON"; exit 1; }
adb connect "$DEV"
adb -s "$DEV" shell mkdir -p /sdcard/Android/data/dev.oliver.home/files
adb -s "$DEV" push "$CFG" /sdcard/Android/data/dev.oliver.home/files/config.json
adb -s "$DEV" shell am force-stop dev.oliver.home
adb -s "$DEV" shell input keyevent KEYCODE_HOME
echo "Config pushed."
