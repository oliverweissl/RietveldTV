package dev.oliver.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Widgets shown top-right. Each one is a small text block in the launcher's style:
 * a mono label, a big light value and a quiet sub line. No panels, no icons.
 *
 * To add a new widget type: subclass {@link Widget}, build its views in the constructor,
 * implement {@link #update()}, and register the type name in {@link #create}.
 */
final class Widgets {
    private Widgets() { }

    /** Shared look: fonts, density, sizes. */
    static final class Style {
        final Context ctx;
        final float dp;
        final Typeface light, regular, medium, mono;
        /** Horizontal alignment for the widget being built: Gravity.START, CENTER_HORIZONTAL or END. */
        int gravity = Gravity.END;

        Style(Context ctx, Typeface light, Typeface regular, Typeface medium, Typeface mono) {
            this.ctx = ctx;
            this.dp = ctx.getResources().getDisplayMetrics().density;
            this.light = light; this.regular = regular; this.medium = medium; this.mono = mono;
        }

        int px(float v) { return Math.round(v * dp); }

        TextView text(float sp, int color, Typeface tf, float tracking) {
            TextView t = new TextView(ctx);
            t.setTextSize(sp);
            t.setTextColor(color);
            t.setTypeface(tf);
            t.setLetterSpacing(tracking);
            t.setIncludeFontPadding(false);
            t.setGravity(gravity);
            return t;
        }
    }

    abstract static class Widget {
        final Style st;
        final LinearLayout view;
        final JSONObject cfg;

        Widget(Style st, JSONObject cfg) {
            this.st = st;
            this.cfg = cfg;
            view = new LinearLayout(st.ctx);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setGravity(st.gravity);
        }

        /** Called on the UI thread, every ~15 s while the home screen is visible. */
        abstract void update();
    }

    static Widget create(Style st, JSONObject cfg) {
        String type = cfg.optString("type", "");
        switch (type) {
            case "clock":   return new ClockWidget(st, cfg);
            case "weather": return new WeatherWidget(st, cfg);
            default:
                Log.w("RietveldTV", "unknown widget type: " + type);
                return null;
        }
    }

    // ---------------------------------------------------------------- clock

    /**
     * {"type":"clock"}                                              local time + date (large)
     * {"type":"clock","label":"Tokyo","timezone":"Asia/Tokyo","size":"m"}   second clock
     * Options: label, timezone (IANA id), size ("l" or "m"), hour24 (default true), date (default: true for local clock)
     */
    static final class ClockWidget extends Widget {
        private final TextView label, time, sub;
        private final SimpleDateFormat fmt, dateFmt;
        private final TimeZone tz;
        private final boolean showDate;

        ClockWidget(Style st, JSONObject cfg) {
            super(st, cfg);
            boolean big = !"m".equals(cfg.optString("size", cfg.has("timezone") ? "m" : "l"));
            String tzId = cfg.optString("timezone", "");
            tz = tzId.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(tzId);
            showDate = cfg.optBoolean("date", tzId.isEmpty());
            fmt = new SimpleDateFormat(cfg.optBoolean("hour24", true) ? "HH:mm" : "h:mm a", Locale.getDefault());
            fmt.setTimeZone(tz);
            dateFmt = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
            dateFmt.setTimeZone(tz);

            label = st.text(12, Color.argb(170, 242, 240, 240), st.mono, 0.14f);
            time = st.text(big ? 78 : 44, Color.rgb(242, 240, 240), st.light, 0f);
            sub = st.text(big ? 16 : 13, Color.argb(190, 235, 235, 245), st.regular, 0.2f);
            String l = cfg.optString("label", "");
            if (!l.isEmpty()) { label.setText(l.toUpperCase(Locale.getDefault())); view.addView(label); }
            view.addView(time);
            view.addView(sub);
        }

        @Override void update() {
            Date now = new Date();
            time.setText(fmt.format(now));
            String s;
            if (showDate) {
                s = dateFmt.format(now).toUpperCase(Locale.getDefault());
            } else {
                int diffH = (tz.getOffset(now.getTime()) - TimeZone.getDefault().getOffset(now.getTime())) / 3600000;
                s = diffH == 0 ? "SAME TIME" : String.format(Locale.US, "%+dH", diffH);
            }
            sub.setText(s);
        }
    }

    // -------------------------------------------------------------- weather

    /**
     * {"type":"weather","city":"Berlin","units":"celsius","days":3}                    (city is looked up once)
     * {"type":"weather","name":"Home","latitude":52.52,"longitude":13.41}                  (or exact coordinates)
     * Needs the INTERNET permission (already in the manifest). Data: Open-Meteo (no key, no account).
     * Only the city name (geocoding, once) or the coordinates you put in the config are sent, nothing else.
     */
    static final class WeatherWidget extends Widget {
        private static final long REFRESH_MS = 30 * 60 * 1000L;
        private final TextView label, temp, cond, hilo, days;
        private final Handler ui = new Handler(Looper.getMainLooper());
        private final SharedPreferences prefs;
        private final String key;
        private long lastFetch = 0;
        private volatile boolean fetching = false;

        WeatherWidget(Style st, JSONObject cfg) {
            super(st, cfg);
            prefs = st.ctx.getSharedPreferences("weather", Context.MODE_PRIVATE);
            key = cfg.optString("city", "") + "|" + cfg.optDouble("latitude") + "," + cfg.optDouble("longitude") + "," + cfg.optString("units", "celsius");
            label = st.text(12, Color.argb(170, 242, 240, 240), st.mono, 0.14f);
            temp = st.text(52, Color.rgb(242, 240, 240), st.light, 0f);
            cond = st.text(13, Color.argb(190, 235, 235, 245), st.regular, 0.2f);
            hilo = st.text(13, Color.argb(150, 235, 235, 245), st.mono, 0.08f);
            days = st.text(12, Color.argb(190, 235, 235, 245), st.mono, 0.06f);
            label.setText(cfg.optString("name", cfg.optString("city", "WEATHER")).toUpperCase(Locale.getDefault()));
            temp.setText("--°");
            view.addView(label);
            view.addView(temp);
            view.addView(cond);
            if (cfg.optInt("days", 3) > 0) { days.setPadding(0, st.px(4), 0, 0); view.addView(days); }
            show(prefs.getString(key, null));      // instant, from the last successful fetch
        }

        @Override void update() {
            long now = System.currentTimeMillis();
            if (fetching || now - lastFetch < REFRESH_MS) return;
            lastFetch = now;
            fetching = true;
            new Thread(() -> {
                try {
                    String units = cfg.optString("units", "celsius");
                    double[] ll = locate();
                    String u = "https://api.open-meteo.com/v1/forecast?latitude=" + ll[0]
                            + "&longitude=" + ll[1]
                            + "&current=temperature_2m,weather_code"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                            + "&forecast_days=" + (Math.max(1, cfg.optInt("days", 3)) + 1)
                            + "&timezone=auto&temperature_unit=" + units;
                    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                    }
                    String body = sb.toString();
                    prefs.edit().putString(key, body).apply();
                    ui.post(() -> show(body));
                } catch (Exception e) {
                    Log.w("RietveldTV", "weather fetch failed: " + e);
                    lastFetch = 0;      // try again on the next tick
                } finally {
                    fetching = false;
                }
            }, "weather").start();
        }

        /** Coordinates from the config, or from a one-time geocoding lookup of "city" (cached on the device). */
        private double[] locate() throws Exception {
            if (cfg.has("latitude") && cfg.has("longitude")) return new double[]{cfg.getDouble("latitude"), cfg.getDouble("longitude")};
            String city = cfg.getString("city");
            String cached = prefs.getString("geo:" + city, null);
            if (cached != null) { String[] a = cached.split(","); return new double[]{Double.parseDouble(a[0]), Double.parseDouble(a[1])}; }
            String u = "https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + java.net.URLEncoder.encode(city, "UTF-8");
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject hit = new JSONObject(sb.toString()).getJSONArray("results").getJSONObject(0);
            prefs.edit().putString("geo:" + city, hit.getDouble("latitude") + "," + hit.getDouble("longitude")).apply();
            return new double[]{hit.getDouble("latitude"), hit.getDouble("longitude")};
        }

        private void show(String body) {
            if (body == null) return;
            try {
                JSONObject o = new JSONObject(body);
                JSONObject cur = o.getJSONObject("current");
                JSONObject d = o.getJSONObject("daily");
                temp.setText(Math.round(cur.getDouble("temperature_2m")) + "°");
                JSONArray hi = d.getJSONArray("temperature_2m_max"), lo = d.getJSONArray("temperature_2m_min");
                JSONArray dates = d.getJSONArray("time");
                cond.setText(describe(cur.getInt("weather_code")) + "  " + Math.round(hi.getDouble(0)) + "°/" + Math.round(lo.getDouble(0)) + "°");
                StringBuilder sb = new StringBuilder();
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat out = new SimpleDateFormat("EEE", Locale.getDefault());
                for (int i = 1; i < dates.length(); i++) {
                    if (sb.length() > 0) sb.append("   ");
                    sb.append(out.format(in.parse(dates.getString(i))).toUpperCase(Locale.getDefault()))
                      .append(' ').append(Math.round(hi.getDouble(i))).append('/').append(Math.round(lo.getDouble(i)));
                }
                days.setText(sb);
            } catch (Exception e) {
                Log.w("RietveldTV", "weather parse failed: " + e);
            }
        }

        /** WMO weather interpretation codes, as used by Open-Meteo. */
        static String describe(int code) {
            if (code == 0) return "CLEAR";
            if (code <= 2) return "PARTLY CLOUDY";
            if (code == 3) return "OVERCAST";
            if (code == 45 || code == 48) return "FOG";
            if (code >= 51 && code <= 57) return "DRIZZLE";
            if (code >= 61 && code <= 67) return "RAIN";
            if (code >= 71 && code <= 77) return "SNOW";
            if (code >= 80 && code <= 82) return "SHOWERS";
            if (code == 85 || code == 86) return "SNOW SHOWERS";
            if (code >= 95) return "THUNDERSTORM";
            return "—";
        }
    }
}
