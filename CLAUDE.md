# RietveldTV: notes for Claude

Android TV launcher. Plain Android Views + Canvas, no AndroidX, no dependencies. Java, `minSdk 26`.

## Layout
- `app/src/main/java/dev/oliver/home/HomeActivity.java`: the screen. `SceneView` (spotlight/floor light, per-pixel bitmap), `GlassBox` (one glass icon), `dial()` (focus/scale/dim by distance from centre), `populate()` (apps from config).
- `Widgets.java`: widget types (`clock`, `weather`). Add new ones by subclassing `Widget` and registering in `create`. Placement (`position`) is handled by `HomeActivity.buildWidgets`; `Style.gravity` sets text alignment per anchor.
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

## Design rules
- Pure black background. Colour only as small accents: red `205,30,38`, blue `30,70,200`, yellow `248,200,40`.
- Fonts: IBM Plex Sans (light/regular/medium) and Plex Mono. No round, friendly fonts.
- Icons are wide rounded rectangles, not squares. No outlines or frames. The glass is subtle: thin bevel, tiny refraction, a thin rim light. If it looks busy, tone it down.
- Light should be smooth: compute gradients per pixel instead of stacking translucent layers.
- Nothing may use the network except the `weather` widget.

## Gotchas
- The TV runs with a density override (1080p at 320 dpi, so 1 dp = 2 px); use `px()`.
- Reusing one `Paint`: call `p.setColor(Color.WHITE)` before `p.setShader(...)` or a previous alpha leaks into the shader.
- Scaled children get clipped by the scroller: keep top padding in `row.setPadding` large enough for the zoomed tile.
- Disabling system packages on a TV can reset adb debugging. Prefer `set-home-activity`; disable only when needed, reversibly (`pm disable-user`).
- Commit and push every change.
