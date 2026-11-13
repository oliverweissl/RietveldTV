#!/usr/bin/env bash
# Refresh the README / project-page screenshot (docs/screenshot-widgets.png) from a connected TV.
# It temporarily shows docs/screenshot-config.json (clock, second time zone, weather) with the third app focused,
# then puts your own config back.
# Usage: scripts/update-screenshots.sh [tv-ip[:port]]     (default: the device adb already knows)
set -euo pipefail
cd "$(dirname "$0")/.."

ADB=(adb)
if [ -n "${1:-}" ]; then
  case "$1" in *:*) D="$1" ;; *) D="$1:5555" ;; esac
  adb connect "$D" >/dev/null
  ADB=(adb -s "$D")
fi
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "no TV connected over adb" >&2; exit 1; }

DIR=/sdcard/Android/data/dev.oliver.home/files
TMP="$(mktemp)"
HAD=0
if "${ADB[@]}" shell "[ -f $DIR/config.json ]" >/dev/null 2>&1; then "${ADB[@]}" pull "$DIR/config.json" "$TMP" >/dev/null && HAD=1; fi

"${ADB[@]}" shell "mkdir -p $DIR"
"${ADB[@]}" push docs/screenshot-config.json "$DIR/config.json" >/dev/null
"${ADB[@]}" shell 'am force-stop dev.oliver.home; input keyevent KEYCODE_HOME; sleep 10; input keyevent DPAD_RIGHT; input keyevent DPAD_RIGHT; sleep 2'
"${ADB[@]}" exec-out screencap -p > docs/screenshot-widgets.png

if [ "$HAD" = 1 ]; then "${ADB[@]}" push "$TMP" "$DIR/config.json" >/dev/null; else "${ADB[@]}" shell "rm -f $DIR/config.json"; fi
rm -f "$TMP"
"${ADB[@]}" shell 'am force-stop dev.oliver.home; input keyevent KEYCODE_HOME'
echo "updated docs/screenshot-widgets.png"
