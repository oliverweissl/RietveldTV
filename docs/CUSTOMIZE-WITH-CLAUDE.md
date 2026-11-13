# Customise RietveldTV with Claude

You do not need to know Android. Describe the change, let [Claude Code](https://claude.com/claude-code) edit the project, and let it look at your TV to check the result.

## 0. Set up once

```bash
git clone https://github.com/oliverweissl/RietveldTV.git
cd RietveldTV
claude            # starts Claude Code in the project
```

Keep your TV connected over adb (`adb connect <tv-ip>:5555`, see the README) so Claude can install builds and take screenshots.
Claude reads [`CLAUDE.md`](../CLAUDE.md) in the repo root, which explains the project layout and the build/screenshot loop.

## 1. Change *what* is shown (config only, no code)

Everything in [`config.json`](config.example.json) is safe to change by asking:

> Replace Spotify with Disney+ and put Netflix last.

> Add a second clock for New York and one for Sydney next to the main clock.

> Add a weather widget for Munich in °C with a 4-day forecast.

Claude edits `config.json` and runs `scripts/push-config.sh <tv-ip> config.json`. To find a package name:

> What is the Android package name of the Disney+ app on my TV? Check with adb.

## 2. Change *how it looks* (a few lines of Java)

All drawing is in `app/src/main/java/dev/oliver/home/HomeActivity.java`. These are the knobs, so you can name them in a prompt:

| What | Where | Example prompt |
| --- | --- | --- |
| Icon shape and size | `GlassBox` (`fw`, `fh`, corner `rad`) | "Make the icons wider and less rounded." |
| Glass look | `GlassBox.drawIcon` (top gloss, rim light, edge darkening) | "Make the glass clearer and the rim light stronger." |
| Spotlight | `SceneView.build` (`topHalf`, `botHalf`, intensity `0.15f`, pool `0.36f`) | "Make the beam narrower and warmer." |
| Spacing and zoom | `dial()` and `tile()` margins | "Space the apps further apart, shrink the neighbours more." |
| Colours | `Color.rgb(...)` accents: red `205,30,38`, blue `30,70,200`, yellow `248,200,40` | "Replace the yellow accents with orange." |
| Fonts | `app/src/main/assets/fonts` + `loadFonts()` | "Use Inter instead of IBM Plex. Download the OFL fonts." |
| Wallpaper | `loadWallpaper()` (pure black by default) | "Use a very dark blue gradient instead of black." |

**Always include in your prompt:** "Build, install on my TV, take a screenshot, and look at it before you tell me it is done." That is the loop that keeps Claude honest:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell 'am force-stop dev.oliver.home; input keyevent KEYCODE_HOME'
adb exec-out screencap -p > shot.png      # Claude reads this image
```

Press keys with `adb shell input keyevent DPAD_RIGHT` to check other focus states (first app, last app, Settings).

### Tips for good results

- Give a reference ("like Apple's Liquid Glass", "like this website: <url>") and a feeling ("calmer", "more modernist"), not pixel values.
- Change one thing at a time and commit when you like it, so you can go back.
- If Claude cannot see a reference image on the web, save it to a file and give it the path.

## Add a widget

Widgets live in [`Widgets.java`](../app/src/main/java/dev/oliver/home/Widgets.java). Each is a small text block (mono label, big light value, quiet sub line). Prompt:

> Add a widget type "countdown" with options `label` and `date` that shows the days left until that date. Follow the style of ClockWidget. Register it in `Widgets.create`, document it in the README, add it to `docs/config.example.json`, then build, install and screenshot.

What Claude does:

1. Adds a `CountdownWidget extends Widget` (build the views in the constructor, fill them in `update()`, which runs every ~15 s).
2. Adds one line in `Widgets.create`: `case "countdown": return new CountdownWidget(st, cfg);`.
3. Documents it and adds an example.

Ideas: calendar next event, public transport departures, a Home Assistant sensor, now-playing, CPU/ping of your server. Anything with network access needs the `INTERNET` permission (already in the manifest) and should say so in the README.

## Other things worth asking Claude for

> Make the launcher react to the app in the foreground: dim my smart lights when Netflix starts. (It will suggest Home Assistant and its Android TV integration.)

> Add a screensaver: after 5 minutes without a key press, fade everything to black and show only the clock.

> Remember the last focused app when I press Home again.

## If something breaks

- Tell Claude what you see, and give it a screenshot (`shot.png`).
- `git diff` shows exactly what changed; `git checkout .` throws all uncommitted changes away.
- `scripts/restore.sh <tv-ip>` brings back the stock launcher.
