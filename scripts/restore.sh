#!/usr/bin/env bash
# Undo RietveldTV: re-enable the stock launchers and remove this app.
# Usage: scripts/restore.sh <tv-ip> [package ...]    (extra packages are re-enabled too)
set -euo pipefail
TV="${1:?usage: scripts/restore.sh <tv-ip> [package ...]}"; shift || true
case "$TV" in *:*) DEV="$TV" ;; *) DEV="$TV:5555" ;; esac   # accepts "ip" or "ip:port"
adb connect "$DEV"
for p in com.google.android.apps.tv.launcherx com.mitv.tvhome.atv com.google.android.tungsten.setupwraith "$@"; do
  adb -s "$DEV" shell pm enable "$p" 2>/dev/null && echo "enabled $p" || true
done
adb -s "$DEV" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity 2>/dev/null || true
adb -s "$DEV" uninstall dev.oliver.home || true
