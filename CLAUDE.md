# RietveldTV: notes for Claude

Android TV launcher. Plain Android Views + Canvas, no AndroidX, no dependencies. Java, `minSdk 26`.

## Layout
- `app/src/main/java/dev/oliver/home/HomeActivity.java`: the screen. `SceneView` (spotlight/floor light, per-pixel bitmap), `IconTile` (one flat icon), `dial()` (focus/scale/dim by distance from centre), `populate()` (apps from config).
- `Widgets.java`: widget types (`clock`, `weather`). Add new ones by subclassing `Widget` and registering in `create`. Placement (`position`) is handled by `HomeActivity.buildWidgets`; `Style.gravity` sets text alignment per anchor.
- On-TV widget editor: `openEditor`/`editorMain`/`editorDetail` in `HomeActivity.java` (overlay of focusable rows; every change calls `persist()` which saves `config.json` and rebuilds the widgets). Keep it in sync with `docs/demo.html`, which mimics it.
- `IconTile.renderAbstract`: the optional Rietveld-style logo stand-ins (planes on a 100x60 grid per package; Mondrian fallback).
- `Config.java`: loads `config.json` from `/sdcard/Android/data/dev.oliver.home/files/`, falling back to `assets/config.default.json`.
- `scripts/`: `install.sh`, `push-config.sh`, `restore.sh`. `docs/`: README assets, GitHub Pages site, guides.

## Build and check (always do this after a visual change)
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell 'am force-stop dev.oliver.home; input keyevent KEYCODE_HOME'
adb exec-out screencap -p > shot.png     # then look at the image
adb shell input keyevent DPAD_RIGHT      # check other focus states
```
Look at the screenshot before saying a visual change works. Check the first app, a middle app and the last tile.

**Screenshot hook:** `.claude/settings.json` runs `scripts/hooks/refresh-screenshot.sh` after any Bash call that installs the APK. It refreshes `docs/screenshot-widgets.png` (used by the README and the project page) via `scripts/update-screenshots.sh`. Look at the new image, then commit and push it with the change. Run the script by hand if the hook did not fire.

## Design rules
- Pure black background. Colour only as small accents: red `205,30,38`, blue `30,70,200`, yellow `248,200,40`.
- Fonts: IBM Plex Sans (light/regular/medium) and Plex Mono. No round, friendly fonts.
- Icons are logo-only by default (`iconStyle: "glyph"`): no background plate, no gloss, no glass. Adaptive icons use their foreground layer, flat logo-on-colour icons have the backdrop keyed out. The lamp and one yellow end-cut tab mark focus.
- Light should be smooth: compute gradients per pixel instead of stacking translucent layers.
- Nothing may use the network except the `weather` widget.

## Gotchas
- The TV runs with a density override (1080p at 320 dpi, so 1 dp = 2 px); use `px()`.
- Reusing one `Paint`: call `p.setColor(Color.WHITE)` before `p.setShader(...)` or a previous alpha leaks into the shader.
- Scaled children get clipped by the scroller: keep top padding in `row.setPadding` large enough for the zoomed tile.
- Disabling system packages on a TV can reset adb debugging. Prefer `set-home-activity`; disable only when needed, reversibly (`pm disable-user`).
- Commit and push every change.
