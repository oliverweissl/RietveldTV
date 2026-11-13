package dev.oliver.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {

    private Config config = new Config();
    private final java.util.List<Widgets.Widget> widgets = new ArrayList<>();
    private FrameLayout rootView;
    private final java.util.List<View> widgetHosts = new ArrayList<>();
    private View termRow;
    private Widgets.Style style;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView label, termText;
    private LinearLayout indicator;
    private SceneView scene;
    private Typeface plexLight, plexRegular, plexMedium, plexMono;

    private void loadFonts() {
        plexLight = Typeface.createFromAsset(getAssets(), "fonts/IBMPlexSans-Light.ttf");
        plexRegular = Typeface.createFromAsset(getAssets(), "fonts/IBMPlexSans-Regular.ttf");
        plexMedium = Typeface.createFromAsset(getAssets(), "fonts/IBMPlexSans-Medium.ttf");
        plexMono = Typeface.createFromAsset(getAssets(), "fonts/IBMPlexMono-Regular.ttf");
    }
    private LinearLayout row;
    private HorizontalScrollView scroller;
    private float dp;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            for (Widgets.Widget w : widgets) w.update();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        loadFonts();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new BitmapDrawable(getResources(), loadWallpaper()));

        // Soft dark scrim so icons and text always stay readable on any wallpaper.
        View scrim = new View(this);
        scrim.setBackgroundColor(Color.TRANSPARENT);
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        style = new Widgets.Style(this, plexLight, plexRegular, plexMedium, plexMono);
        rootView = root;

        LinearLayout term = new LinearLayout(this);
        term.setOrientation(LinearLayout.HORIZONTAL);
        term.setGravity(Gravity.CENTER_VERTICAL);
        int[] acc = {Color.rgb(205, 30, 38), Color.rgb(30, 70, 200), Color.rgb(248, 200, 40)};
        for (int col : acc) {
            View sq = new View(this);
            sq.setBackgroundColor(col);
            LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(px(10), px(10));
            q.setMargins(0, 0, px(6), 0);
            term.addView(sq, q);
        }
        termText = text(14, Color.argb(200, 242, 240, 240), plexMono);
        termText.setLetterSpacing(0.08f);
        termText.setPadding(px(8), 0, 0, 0);
        term.addView(termText);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        tp.setMargins(px(56), px(46), 0, 0);
        root.addView(term, tp);
        termRow = term;

        // Rietveld-style structure: a thin rule with yellow end-cut caps under the header
        View rule = new View(this);
        rule.setBackgroundColor(0x40F2F0F0);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, px(1), Gravity.TOP);
        rp.setMargins(px(56), px(178), px(56), 0);
        root.addView(rule, rp);
        for (int side = 0; side < 2; side++) {
            View cap = new View(this);
            cap.setBackgroundColor(Color.rgb(248, 200, 40));
            FrameLayout.LayoutParams capLp = new FrameLayout.LayoutParams(px(22), px(5),
                    Gravity.TOP | (side == 0 ? Gravity.START : Gravity.END));
            capLp.setMargins(px(56), px(176), px(56), 0);
            root.addView(cap, capLp);
        }
        indicator = new LinearLayout(this);
        indicator.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ip.setMargins(0, 0, 0, px(22));
        root.addView(indicator, ip);

        scene = new SceneView(this);
        root.addView(scene, 1, new FrameLayout.LayoutParams(-1, -1));

        scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        scroller.setOnScrollChangeListener((v, x, y, ox, oy) -> dial());
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setPadding(px(349), px(30), px(349), px(6));
        scroller.addView(row, new ViewGroup.LayoutParams(-2, -2));
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        sp.setMargins(0, 0, 0, px(56));
        root.addView(scroller, sp);

        label = text(17, Color.rgb(242, 240, 240), plexMedium);
        label.setLetterSpacing(0.3f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.setMargins(0, 0, 0, px(44));
        label.setGravity(Gravity.CENTER);
        root.addView(label, lp);

        root.setAlpha(0f);
        setContentView(root);
        root.animate().alpha(1f).setDuration(600).start();
        tick.run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        config = Config.load(this);
        buildWidgets();
        populate();
        handler.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        // Home pressed while already home: go back to first tile.
        if (row.getChildCount() > 0) {
            scroller.smoothScrollTo(0, 0);
            row.getChildAt(0).requestFocus();
        }
    }

    /**
     * Places each widget at the anchor named in its "position": top-left, top-center, top-right (default),
     * bottom-left or bottom-right. Widgets sharing an anchor sit side by side in config order.
     */
    private void buildWidgets() {
        widgets.clear();
        for (View v : widgetHosts) rootView.removeView(v);
        widgetHosts.clear();
        java.util.Map<String, LinearLayout> hosts = new java.util.HashMap<>();
        boolean topLeft = false;
        for (org.json.JSONObject cfg : config.widgets) {
            String pos = cfg.optString("position", "top-right");
            int fg, align, mt = 0, mb = 0, ms = 0, me = 0;
            switch (pos) {
                case "top-left":     fg = Gravity.TOP | Gravity.START;              align = Gravity.START; mt = 36; ms = 56; topLeft = true; break;
                case "top-center":   fg = Gravity.TOP | Gravity.CENTER_HORIZONTAL;  align = Gravity.CENTER_HORIZONTAL; mt = 36; break;
                case "bottom-left":  fg = Gravity.BOTTOM | Gravity.START;           align = Gravity.START; mb = 24; ms = 56; break;
                case "bottom-right": fg = Gravity.BOTTOM | Gravity.END;             align = Gravity.END;   mb = 24; me = 56; break;
                default: pos = "top-right"; fg = Gravity.TOP | Gravity.END;         align = Gravity.END;   mt = 36; me = 56; break;
            }
            style.gravity = align;
            Widgets.Widget w = Widgets.create(style, cfg);
            if (w == null) continue;
            LinearLayout host = hosts.get(pos);
            if (host == null) {
                host = new LinearLayout(this);
                host.setOrientation(LinearLayout.HORIZONTAL);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, fg);
                lp.setMargins(px(ms), px(mt), px(me), px(mb));
                rootView.addView(host, lp);
                widgetHosts.add(host);
                hosts.put(pos, host);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (host.getChildCount() > 0) lp.setMarginStart(px(40));
            host.addView(w.view, lp);
            widgets.add(w);
        }
        // the typing line lives top-left: step aside when a widget is placed there, or when the config turns it off
        termRow.setVisibility(topLeft || !config.terminal ? View.GONE : View.VISIBLE);
    }

    // Home screen is the root: swallow Back.
    @Override public void onBackPressed() { }

    private void populate() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> found = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pinIndex(pkg) < 0 || seen.contains(pkg)) continue; // only apps listed in the config
                seen.add(pkg);
                found.add(ri);
            }
        }
        Collections.sort(found, (a, c) -> {
            int ia = pinIndex(a.activityInfo.packageName), ic = pinIndex(c.activityInfo.packageName);
            if (ia != ic) return Integer.compare(ia, ic);
            return a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString());
        });

        row.removeAllViews();
        boolean first = true;
        for (ResolveInfo ri : found) {
            final String pkg = ri.activityInfo.packageName;
            final String name = ri.loadLabel(pm).toString();
            Drawable banner = null;
            try { banner = ri.activityInfo.loadBanner(pm); } catch (Exception ignored) { }
            if (banner == null) try { banner = ri.activityInfo.applicationInfo.loadBanner(pm); } catch (Exception ignored) { }
            final boolean isBanner = false;
            Drawable icon = hiResIcon(pm, ri);
            View tile = tile(icon, isBanner, name, () -> launch(pkg));
            row.addView(tile);
            if (first) { tile.requestFocus(); first = false; }
        }
        if (config.showSettings) {
            View settings = tile(null, false, "Settings", () -> {
                try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
                catch (Exception ignored) { }
            });
            row.addView(settings);
        }
        indicator.removeAllViews();
        for (int i = 0; i < row.getChildCount(); i++) {
            View seg = new View(this);
            seg.setBackgroundColor(0x40F2F0F0);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(px(34), px(6));
            sl.setMargins(px(3), 0, px(3), 0);
            indicator.addView(seg, sl);
        }
        row.post(() -> {
            dial();
            if (indicator.getChildCount() > 0 && row.getChildCount() > 0) {
                View f = row.getFocusedChild() != null ? row.getFocusedChild() : row.getChildAt(0);
                int idx = row.indexOfChild(f), n = indicator.getChildCount();
                for (int k = 0; k < n; k++) {
                    indicator.getChildAt(k).setBackgroundColor(k == idx ? Color.rgb(205, 30, 38)
                            : (config.showSettings && k == n - 1) ? Color.rgb(30, 70, 200) : 0x40F2F0F0);
                }
                if (label.getText().length() == 0 && f instanceof IconTile) label.setText(((IconTile) f).title.toUpperCase(Locale.getDefault()));
            }
        });
        typeLine("> " + found.size() + " apps ready");
    }

    private void typeLine(final String full) {
        final int[] n = {0};
        handler.removeCallbacksAndMessages("type");
        Runnable r = new Runnable() {
            @Override public void run() {
                n[0]++;
                termText.setText(full.substring(0, Math.min(n[0], full.length())) + "\u258C");
                if (n[0] < full.length()) handler.postAtTime(this, "type", android.os.SystemClock.uptimeMillis() + 45);
            }
        };
        handler.postAtTime(r, "type", android.os.SystemClock.uptimeMillis() + 500);
    }

    /** Dial / cover-flow: tiles turn, shrink and fade with distance from the centre. */
    private void dial() {
        int sw = scroller.getWidth();
        if (sw == 0) return;
        float mid = scroller.getScrollX() + sw / 2f;
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            float d = (c.getLeft() + c.getWidth() / 2f - mid) / (c.getWidth() * 1.95f);
            d = Math.max(-1.6f, Math.min(1.6f, d));
            float a = Math.abs(d);
            c.setPivotX(c.getWidth() / 2f);
            c.setPivotY(((IconTile) c).frontBottom());
            float sc = 1.10f - 0.22f * Math.min(a, 1.4f);
            c.setScaleX(sc);
            c.setScaleY(sc);
            c.setAlpha(Math.max(0.45f, 1f - 0.4f * a));
            ((IconTile) c).setState(Math.max(0f, 1f - a * 1.6f), Math.max(-1f, Math.min(1f, d)));
            if (i == 0) scene.setFloor(sw / 2f, scroller.getTop() + c.getTop() + ((IconTile) c).frontBottom());
        }
    }

    /** Ask the app for its highest-density icon so it stays sharp at TV size (loadIcon gives the device-density one). */
    private Drawable hiResIcon(PackageManager pm, ResolveInfo ri) {
        try {
            int id = ri.getIconResource();
            if (id != 0) {
                android.content.res.Resources r = pm.getResourcesForApplication(ri.activityInfo.applicationInfo);
                Drawable d = r.getDrawableForDensity(id, android.util.DisplayMetrics.DENSITY_XXXHIGH, null);
                if (d != null) return d;
            }
        } catch (Exception ignored) { }
        return ri.loadIcon(pm);
    }

    private int pinIndex(String pkg) { return config.apps.indexOf(pkg); }

    private void launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
    }

    private View tile(Drawable icon, boolean isBanner, final String name, final Runnable action) {
        final IconTile g = new IconTile(this, icon, config.iconStyle, name.equals("Widgets") ? "+" : "\u2699");
        g.title = name;
        g.setFocusable(true);
        g.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(g.totalW(), g.totalH());
        lp.setMargins(px(44), 0, px(44), 0);
        g.setLayoutParams(lp);
        g.setOnFocusChangeListener((v, has) -> {
            if (has) {
                label.setText(name.toUpperCase(Locale.getDefault()));
                int target = v.getLeft() + v.getWidth() / 2 - scroller.getWidth() / 2;
                scroller.smoothScrollTo(Math.max(0, target), 0);
                dial();
                int idx = row.indexOfChild(v), n = indicator.getChildCount();
                for (int k = 0; k < n; k++) {
                    indicator.getChildAt(k).setBackgroundColor(k == idx ? Color.rgb(205, 30, 38)
                            : (config.showSettings && k == n - 1) ? Color.rgb(30, 70, 200) : 0x40F2F0F0);
                }
            }
        });
        g.setOnClickListener(v -> action.run());
        return g;
    }

    /** The room: pure black. Only a narrow soft cone of light and the pool it makes on the floor are visible. */
    private class SceneView extends View {
        private float cx, floorY = -1;
        private Bitmap light;
        private static final int DS = 2;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        SceneView(android.content.Context c) { super(c); }

        void setFloor(float cx, float floorY) {
            if (light == null || Math.abs(this.floorY - floorY) > 0.5f || Math.abs(this.cx - cx) > 0.5f) {
                this.cx = cx; this.floorY = floorY; build(); invalidate();
            }
        }

        private float sstep(float a, float b, float x) {
            float t = Math.max(0f, Math.min(1f, (x - a) / (b - a)));
            return t * t * (3 - 2 * t);
        }

        /** Per-pixel light so the falloff is perfectly smooth: no stacked layers, no ragged edges. */
        private void build() {
            int halfW = px(240);
            int wpx = halfW * 2 / DS;
            int hpx = (int) ((floorY + px(46)) / DS);
            int[] pix = new int[wpx * hpx];
            java.util.Random rnd = new java.util.Random(7);
            float cxl = wpx * DS / 2f;
            float topHalf = px(8), botHalf = px(120);
            float poolCy = floorY + px(4);
            for (int y = 0; y < hpx; y++) {
                float Y = y * DS;
                float t = Math.max(0f, Math.min(1f, Y / floorY));
                float hw = topHalf + (botHalf - topHalf) * t;
                for (int x = 0; x < wpx; x++) {
                    float dx = Math.abs(x * DS - cxl) / hw;
                    float cone = (Y <= floorY) ? 0.15f * (float) Math.exp(-dx * dx * 2.0f) * (1f - 0.35f * t) * sstep(0f, 0.06f, t)
                                               : 0f;
                    float px_ = (x * DS - cxl) / px(150);
                    float py_ = (Y - poolCy) / px(20);
                    float pool = 0.36f * (float) Math.exp(-(px_ * px_ + py_ * py_) * 2.4f);
                    float a = 1f - (1f - cone) * (1f - pool);
                    float v = a * 255f + (rnd.nextFloat() - 0.5f) * 1.6f;   // tiny dither against banding
                    int ai = Math.max(0, Math.min(255, Math.round(v)));
                    pix[y * wpx + x] = Color.argb(ai, 255, 250, 242);
                }
            }
            light = Bitmap.createBitmap(pix, wpx, hpx, Bitmap.Config.ARGB_8888);
        }

        @Override protected void onDraw(Canvas c) {
            if (light == null) return;
            android.graphics.RectF dst = new android.graphics.RectF(cx - px(240), 0,
                    cx + px(240), light.getHeight() * DS);
            c.drawBitmap(light, null, dst, p);
        }
    }

    /**
     * One app as a flat plane in the De Stijl / Rietveld spirit. By default ("glyph" style) only the app's logo is drawn,
     * without its background plate, straight onto the black room: modern apps ship the logo as a separate transparent
     * layer, and flat logo-on-colour icons (e.g. Netflix) have their background colour keyed out.
     * "mono" uses the app's themed single-colour icon (Android 13+) when it has one; "plate" draws the whole icon.
     * The lamp does the work: lit tiles show their colour, the rest sink into the dark, and the focused tile gets a small
     * yellow end-cut tab like the painted ends of the Red and Blue Chair.
     */
    private class IconTile extends View {
        private final Drawable art;
        private final String style, glyph;
        private final int fw = px(166), fh = px(97), depth = px(28), side = px(8), refl = px(32);
        String title = "";
        private float lit = 0f;
        private Bitmap content;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private static final int OFFWHITE = 0xFFF2F0F0;

        IconTile(android.content.Context c, Drawable art, String style, String glyph) {
            super(c);
            this.art = art;
            this.style = style;
            this.glyph = glyph;
        }

        int totalW() { return fw + side * 2; }
        int totalH() { return depth + fh + refl; }
        float frontBottom() { return depth + fh; }

        void setState(float lit, float dir) {
            if (Math.abs(this.lit - lit) > 0.01f) { this.lit = lit; invalidate(); }
        }

        /** The whole icon as a flat plane (the "plate" style). */
        private Bitmap renderPlate() {
            Bitmap b = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            RectF r = new RectF(0, 0, fw, fh);
            p.setColor(Color.rgb(24, 25, 27));
            c.drawRect(r, p);
            if (art instanceof android.graphics.drawable.AdaptiveIconDrawable) {
                android.graphics.drawable.AdaptiveIconDrawable ad = (android.graphics.drawable.AdaptiveIconDrawable) art;
                Drawable bg = ad.getBackground(), fg = ad.getForeground();
                float bs = Math.max(fw, fh) * 1.5f;
                if (bg != null) {
                    bg.setBounds((int) (r.centerX() - bs / 2), (int) (r.centerY() - bs / 2), (int) (r.centerX() + bs / 2), (int) (r.centerY() + bs / 2));
                    bg.setAlpha(255); bg.draw(c);
                }
                float gs = fh * 1.5f;
                if (fg != null) {
                    fg.setBounds((int) (r.centerX() - gs / 2), (int) (r.centerY() - gs / 2), (int) (r.centerX() + gs / 2), (int) (r.centerY() + gs / 2));
                    fg.draw(c);
                }
            } else if (art != null) {
                int iw = Math.max(1, art.getIntrinsicWidth()), ih = Math.max(1, art.getIntrinsicHeight());
                float sc = Math.max(fw / (float) iw, fh / (float) ih);
                float dw = iw * sc, dh = ih * sc;
                art.setBounds((int) (r.centerX() - dw / 2), (int) (r.centerY() - dh / 2), (int) (r.centerX() + dw / 2), (int) (r.centerY() + dh / 2));
                art.setAlpha(255); art.draw(c);
            }
            return b;
        }

        /** Just the logo: transparent background, cropped to the logo's bounds and fitted into the tile. */
        private Bitmap renderGlyph() {
            Bitmap out = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
            Canvas oc = new Canvas(out);
            if (art == null) {                                  // Settings / Widgets: a plain symbol
                p.setColor(OFFWHITE);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(fh * 0.78f);
                android.graphics.Rect tb = new android.graphics.Rect();
                p.getTextBounds(glyph, 0, glyph.length(), tb);
                oc.drawText(glyph, fw / 2f, fh / 2f - tb.exactCenterY(), p);
                return out;
            }
            final int N = 480;
            Bitmap src = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888);
            Canvas sc = new Canvas(src);
            boolean mono = false;
            if (art instanceof android.graphics.drawable.AdaptiveIconDrawable) {
                android.graphics.drawable.AdaptiveIconDrawable ad = (android.graphics.drawable.AdaptiveIconDrawable) art;
                Drawable layer = null;
                if (style.equals("mono") && android.os.Build.VERSION.SDK_INT >= 33) { layer = ad.getMonochrome(); mono = layer != null; }
                if (layer == null) layer = ad.getForeground();
                if (layer == null) return renderPlate();
                layer.setBounds(0, 0, N, N);
                layer.setAlpha(255);
                layer.draw(sc);
            } else {
                art.setBounds(0, 0, N, N);
                art.setAlpha(255);
                art.draw(sc);
                keyOutBackground(src);                          // flat logo-on-colour icon: drop the colour
            }
            int[] px_ = new int[N * N];
            src.getPixels(px_, 0, N, 0, 0, N, N);
            int minX = N, minY = N, maxX = -1, maxY = -1;
            double luma = 0, sat = 0; int cnt = 0;
            float[] hsv = new float[3];
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                int c = px_[y * N + x], a = c >>> 24;
                if (a < 24) continue;
                if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y;
                if (a > 200) {
                    Color.colorToHSV(c, hsv);
                    luma += (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)); sat += hsv[1]; cnt++;
                }
            }
            if (maxX < 0) return renderPlate();                  // nothing opaque: fall back to the whole icon
            boolean tint = mono || (cnt > 0 && luma / cnt < 70 && sat / cnt < 0.25);   // dark neutral logo would vanish on black
            int bw = maxX - minX + 1, bh = maxY - minY + 1;
            float scale = Math.min(fw * 0.70f / bw, fh * 0.72f / bh);
            float dw = bw * scale, dh = bh * scale;
            Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            if (tint) gp.setColorFilter(new android.graphics.PorterDuffColorFilter(OFFWHITE, android.graphics.PorterDuff.Mode.SRC_IN));
            oc.drawBitmap(src, new android.graphics.Rect(minX, minY, maxX + 1, maxY + 1),
                    new RectF(fw / 2f - dw / 2f, fh / 2f - dh / 2f, fw / 2f + dw / 2f, fh / 2f + dh / 2f), gp);
            return out;
        }

        /**
         * Makes the icon's background transparent. The background colour is read per row from the left/right borders,
         * so flat colours and vertical gradients both work; icons whose borders are not a plain backdrop are left alone.
         */
        private void keyOutBackground(Bitmap b) {
            int w = b.getWidth(), h = b.getHeight();
            int[] px_ = new int[w * h];
            b.getPixels(px_, 0, w, 0, 0, w, h);
            int[] rowBg = new int[h];
            int valid = 0;
            for (int y = 0; y < h; y++) {
                int l = px_[y * w + 2], r = px_[y * w + w - 3];
                if ((l >>> 24) >= 250 && (r >>> 24) >= 250 && dist(l, r) < 45f) {
                    rowBg[y] = Color.rgb((Color.red(l) + Color.red(r)) / 2, (Color.green(l) + Color.green(r)) / 2, (Color.blue(l) + Color.blue(r)) / 2);
                    valid++;
                } else rowBg[y] = 0;
            }
            if (valid < h * 0.9f) return;                          // not a plain backdrop
            int last = 0;
            for (int y = 0; y < h; y++) { if (rowBg[y] != 0) last = rowBg[y]; else rowBg[y] = last; }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = px_[y * w + x];
                    float a = Math.max(0f, Math.min(1f, (dist(c, rowBg[y]) - 22f) / 70f));
                    if (a < 1f) px_[y * w + x] = (Math.round(a * 255) << 24) | (c & 0x00FFFFFF);
                }
            }
            b.setPixels(px_, 0, w, 0, 0, w, h);
        }

        private float dist(int a, int b) {
            int dr = Color.red(a) - Color.red(b), dg = Color.green(a) - Color.green(b), db = Color.blue(a) - Color.blue(b);
            return (float) Math.sqrt(dr * dr + dg * dg + db * db);
        }

        @Override protected void onDraw(Canvas c) {
            if (content == null) content = style.equals("plate") && art != null ? renderPlate() : renderGlyph();
            float x0 = (getWidth() - fw) / 2f, y0 = depth, y1 = depth + fh;
            float L = lit;
            // outside the beam the tile sinks into the dark
            p.setColor(Color.WHITE);
            p.setShader(null);
            p.setAlpha(Math.round(255 * (0.28f + 0.72f * L)));
            c.drawBitmap(content, x0, y0, p);
            p.setAlpha(255);
            // the yellow end-cut tab, only on the lit tile
            p.setColor(Color.argb(Math.round(255 * L), 248, 200, 40));
            float tx = x0 + (fw - px(30)) / 2f;
            c.drawRect(tx, y1 + px(5), tx + px(30), y1 + px(9), p);
        }
    }

    private Drawable rounded(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(int sp, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setShadowLayer(8, 0, 2, Color.argb(160, 0, 0, 0));
        return t;
    }

    /** Wide soft ellipse of light, fully faded before the bitmap edges (no visible box). */
    private Bitmap glowBitmap() {
        int w = 960, h = 340;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = h * 0.48f;
        RadialGradient rg = new RadialGradient(0, 0, r,
                new int[]{0xCC46585F, 0x66303D43, 0x00000000}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        p.setShader(rg);
        c.save();
        c.translate(w / 2f, h / 2f);
        c.scale(w * 0.46f / r, 1f);
        c.drawCircle(0, 0, r, p);
        c.restore();
        return bmp;
    }

    private int px(int dpv) { return Math.round(dpv * dp); }

    /** Custom wallpaper if present (files/wallpaper.jpg), otherwise a dark procedural aurora. */
    private Bitmap loadWallpaper() {
        File f = new File(getFilesDir(), "wallpaper.jpg");
        if (f.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bmp != null) return bmp;
        }
        int w = 960, h = 540;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(Color.BLACK); // pure black: best contrast, lowest eye strain, colour only in accents
        return bmp;
    }

    private void blob(Canvas c, Paint p, float x, float y, float r, int color) {
        p.setColor(Color.WHITE); p.setShader(new RadialGradient(x, y, r, Color.argb(105, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, p);
    }
}
