package dev.oliver.home;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Launcher configuration.
 *
 * Read from {@code /sdcard/Android/data/dev.oliver.home/files/config.json} if present (push it with
 * {@code adb push config.json /sdcard/Android/data/dev.oliver.home/files/}), otherwise from the bundled
 * {@code assets/config.default.json}. Reloaded every time the home screen comes to the front.
 */
final class Config {
    private static final String TAG = "RietveldTV";

    /** Package names of the apps to show, in order. */
    final List<String> apps = new ArrayList<>();
    /** Widget definitions, shown left to right in the top-right corner. Each has at least a "type". */
    final List<JSONObject> widgets = new ArrayList<>();
    boolean showSettings = true;
    /** The small typing line "> 5 apps ready" in the top-left corner. Hidden automatically if a widget sits there. */
    boolean terminal = true;
    /** "glyph" (default): logo only, no background plate; "mono": themed single-colour icon if the app has one; "plate": whole icon. */
    String iconStyle = "glyph";

    static Config load(Context c) {
        String json = null;
        File ext = c.getExternalFilesDir(null);
        if (ext != null) {
            File f = new File(ext, "config.json");
            if (f.exists()) {
                try { json = read(new FileInputStream(f)); }
                catch (Exception e) { Log.w(TAG, "could not read " + f, e); }
            }
        }
        Config cfg = parse(json);
        if (cfg == null) {
            try { cfg = parse(read(c.getAssets().open("config.default.json"))); }
            catch (Exception e) { Log.e(TAG, "bundled default config is broken", e); }
        }
        return cfg != null ? cfg : new Config();
    }

    private static Config parse(String json) {
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            Config cfg = new Config();
            JSONArray apps = o.optJSONArray("apps");
            if (apps != null) for (int i = 0; i < apps.length(); i++) cfg.apps.add(apps.getString(i));
            JSONArray widgets = o.optJSONArray("widgets");
            if (widgets != null) for (int i = 0; i < widgets.length(); i++) cfg.widgets.add(widgets.getJSONObject(i));
            cfg.showSettings = o.optBoolean("showSettings", true);
            cfg.terminal = o.optBoolean("terminal", true);
            cfg.iconStyle = o.optString("iconStyle", "glyph");
            return cfg;
        } catch (Exception e) {
            Log.w(TAG, "config.json is not valid JSON, falling back to the default", e);
            return null;
        }
    }

    private static String read(InputStream in) throws java.io.IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
