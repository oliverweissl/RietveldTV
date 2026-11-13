#!/usr/bin/env bash
# Claude Code PostToolUse hook: after a Bash call that installed RietveldTV on a TV (adb install ... app-debug.apk /
# RietveldTV.apk / dev.oliver.home), refresh docs/screenshot-widgets.png so the README and the project page always
# show the current design. Does nothing for any other command. Does not commit; it tells Claude to.
input="$(cat)"
case "$input" in *"install"*) ;; *) exit 0 ;; esac
case "$input" in *app-debug.apk*|*RietveldTV.apk*|*dev.oliver.home*) ;; *) exit 0 ;; esac
case "$input" in *"scripts/update-screenshots.sh"*) exit 0 ;; esac

cd "$(dirname "$0")/../.." || exit 0
if scripts/update-screenshots.sh >/tmp/rietveldtv-screenshot.log 2>&1; then
  msg="Design iteration installed: docs/screenshot-widgets.png was refreshed from the TV (README and project page use it). Look at it, then commit and push it together with the change."
else
  msg="Could not refresh docs/screenshot-widgets.png (see /tmp/rietveldtv-screenshot.log). Run scripts/update-screenshots.sh manually before committing a design change."
fi
printf '{"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":"%s"}}\n' "$msg"
