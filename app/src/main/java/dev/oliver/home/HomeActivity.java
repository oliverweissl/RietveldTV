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
                if (label.getText().length() == 0 && f instanceof GlassBox) label.setText(((GlassBox) f).title.toUpperCase(Locale.getDefault()));
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
            c.setPivotY(((GlassBox) c).frontBottom());
            float sc = 1.10f - 0.22f * Math.min(a, 1.4f);
            c.setScaleX(sc);
            c.setScaleY(sc);
            c.setAlpha(Math.max(0.45f, 1f - 0.4f * a));
            ((GlassBox) c).setState(Math.max(0f, 1f - a * 1.6f), Math.max(-1f, Math.min(1f, d)));
            if (i == 0) scene.setFloor(sw / 2f, scroller.getTop() + c.getTop() + ((GlassBox) c).frontBottom());
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
        final GlassBox g = new GlassBox(this, icon, isBanner);
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
     * One app as a single Liquid-Glass style icon, built the way the usual shader recipes do it:
     * the icon is a rounded-rectangle distance field; near the rim the picture is bent along the surface
     * normal (refraction, with a slight R/G/B split), and the rim catches light from the slope of the bevel
     * (specular + fresnel). Everything is computed once per icon into two bitmaps.
     */
    private class GlassBox extends View {
        private final Drawable art;
        private final int fw = px(166), fh = px(97), depth = px(28), side = px(8), refl = px(32);
        String title = "";
        private float lit = 0f, dir = 0f;
        private Bitmap refr, spec;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        GlassBox(android.content.Context c, Drawable art, boolean banner) {
            super(c);
            this.art = art;
        }

        int totalW() { return fw + side * 2; }
        int totalH() { return depth + fh + refl; }
        float frontBottom() { return depth + fh; }

        void setState(float lit, float dir) {
            if (Math.abs(this.lit - lit) > 0.01f || Math.abs(this.dir - dir) > 0.01f) {
                this.lit = lit; this.dir = dir; invalidate();
            }
        }

        private int k(float a) { return Color.argb(Math.max(0, Math.min(255, Math.round(a))), 0, 0, 0); }
        private int w(float a) { return Color.argb(Math.max(0, Math.min(255, Math.round(a))), 255, 255, 255); }

        /** The flat artwork the glass sits on: dark base, app icon filling the rectangle, glyph floating on a soft shadow. */
        private Bitmap renderContent() {
            Bitmap b = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            RectF r = new RectF(0, 0, fw, fh);
            p.setShader(new android.graphics.LinearGradient(0, 0, 0, fh, Color.rgb(46, 48, 52), Color.rgb(16, 17, 19), Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            if (art instanceof android.graphics.drawable.AdaptiveIconDrawable) {
                android.graphics.drawable.AdaptiveIconDrawable ad = (android.graphics.drawable.AdaptiveIconDrawable) art;
                Drawable bg = ad.getBackground(), fg = ad.getForeground();
                float bs = Math.max(fw, fh) * 1.5f;
                if (bg != null) {
                    bg.setBounds((int) (r.centerX() - bs / 2), (int) (r.centerY() - bs / 2), (int) (r.centerX() + bs / 2), (int) (r.centerY() + bs / 2));
                    bg.setAlpha(255); bg.draw(c);
                }
                float gs = fh * 1.5f;
                android.graphics.Rect gb = new android.graphics.Rect((int) (r.centerX() - gs / 2), (int) (r.centerY() - gs / 2),
                        (int) (r.centerX() + gs / 2), (int) (r.centerY() + gs / 2));
                if (fg != null) {
                    fg.setBounds(gb.left, gb.top + px(3), gb.right, gb.bottom + px(3));
                    fg.setColorFilter(k(90), android.graphics.PorterDuff.Mode.SRC_IN);
                    fg.draw(c);
                    fg.clearColorFilter();
                    fg.setBounds(gb);
                    fg.draw(c);
                }
            } else if (art != null) {
                int iw = Math.max(1, art.getIntrinsicWidth()), ih = Math.max(1, art.getIntrinsicHeight());
                float sc = Math.max(fw / (float) iw, fh / (float) ih);
                float dw = iw * sc, dh = ih * sc;
                art.setBounds((int) (r.centerX() - dw / 2), (int) (r.centerY() - dh / 2), (int) (r.centerX() + dw / 2), (int) (r.centerY() + dh / 2));
                art.setAlpha(255); art.draw(c);
            } else {
                p.setColor(Color.rgb(236, 234, 230));
                p.setTextSize(px(40));
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText("\u2699", r.centerX(), r.centerY() + px(14), p);
            }
            return b;
        }

        private float chan(int[] a, float x, float y, int shift) {
            x = Math.max(0f, Math.min(fw - 1.001f, x));
            y = Math.max(0f, Math.min(fh - 1.001f, y));
            int x0 = (int) x, y0 = (int) y;
            float fx = x - x0, fy = y - y0;
            float v00 = (a[y0 * fw + x0] >> shift) & 255, v10 = (a[y0 * fw + x0 + 1] >> shift) & 255;
            float v01 = (a[(y0 + 1) * fw + x0] >> shift) & 255, v11 = (a[(y0 + 1) * fw + x0 + 1] >> shift) & 255;
            return (v00 * (1 - fx) + v10 * fx) * (1 - fy) + (v01 * (1 - fx) + v11 * fx) * fy;
        }

        private void build() {
            Bitmap content = renderContent();
            int[] src = new int[fw * fh];
            content.getPixels(src, 0, fw, 0, 0, fw, fh);
            int[] out = new int[fw * fh], hl = new int[fw * fh];
            float rad = fh * 0.22f, bw = fh * 0.10f;
            float cx = fw / 2f, cy = fh / 2f, hx = fw / 2f - rad, hy = fh / 2f - rad;
            float lx = -0.35f, ly = -0.94f;                       // light from the top, a touch from the left
            for (int y = 0; y < fh; y++) {
                for (int x = 0; x < fw; x++) {
                    float px_ = x + 0.5f - cx, py_ = y + 0.5f - cy;
                    float qx = Math.abs(px_) - hx, qy = Math.abs(py_) - hy;
                    float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
                    float len = (float) Math.sqrt(ox * ox + oy * oy);
                    float sdf = len + Math.min(Math.max(qx, qy), 0f) - rad;      // < 0 inside
                    float dIn = -sdf;
                    float cover = Math.max(0f, Math.min(1f, dIn + 0.5f));       // anti-aliased silhouette
                    if (cover <= 0f) continue;
                    // outward normal = gradient of the distance field
                    float nx, ny;
                    if (len > 1e-4f) { nx = ox / len * Math.signum(px_); ny = oy / len * Math.signum(py_); }
                    else if (qx > qy) { nx = Math.signum(px_); ny = 0f; }
                    else { nx = 0f; ny = Math.signum(py_); }
                    float t = Math.max(0f, Math.min(1f, 1f - dIn / bw));        // 0 in the flat middle, 1 on the rim
                    float shift = bw * 0.28f * t * t;                        // refraction: strongest at the rim
                    float rr = chan(src, x + 0.5f - nx * shift, y + 0.5f - ny * shift, 16);
                    float gg = chan(src, x + 0.5f - nx * shift, y + 0.5f - ny * shift, 8);
                    float bb = chan(src, x + 0.5f - nx * shift, y + 0.5f - ny * shift, 0);
                    out[y * fw + x] = Color.argb(Math.round(255 * cover), (int) rr, (int) gg, (int) bb);
                    // light on the bevel: main highlight facing the lamp, a weaker bounce on the opposite side, hairline fresnel rim
                    float nl = nx * lx + ny * ly;
                    float s1 = (float) Math.pow(Math.max(0f, nl), 3.0) * (float) Math.pow(t, 2.0) * 0.85f;
                    float s2 = (float) Math.pow(Math.max(0f, -nl), 4.0) * (float) Math.pow(t, 2.0) * 0.22f;
                    float fres = (float) Math.pow(t, 6) * 0.20f;
                    float a = Math.min(1f, s1 + s2 + fres) * cover;
                    hl[y * fw + x] = Color.argb(Math.round(255 * a), 255, 255, 255);
                }
            }
            refr = Bitmap.createBitmap(out, fw, fh, Bitmap.Config.ARGB_8888);
            spec = Bitmap.createBitmap(hl, fw, fh, Bitmap.Config.ARGB_8888);
        }

        @Override protected void onDraw(Canvas c) {
            if (refr == null) build();
            int W = getWidth();
            float x0 = (W - fw) / 2f, y0 = depth, x1 = x0 + fw, y1 = depth + fh;
            float L = lit;

            // soft contact shadow
            c.save();
            c.translate(W / 2f, y1);
            c.scale(1f, 0.08f);
            p.setColor(Color.WHITE);
            p.setShader(new RadialGradient(0, 0, fw * 0.55f, 0xC0000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, fw * 0.55f, p);
            c.restore();
            p.setShader(null);

            // faint reflection in the black floor
            int layer = c.saveLayer(0, y1, W, getHeight(), null);
            c.save();
            c.scale(1f, -1f, W / 2f, y1);
            drawIcon(c, x0, y0, 0.06f + 0.26f * L);
            c.restore();
            p.setColor(Color.WHITE);
            p.setShader(new android.graphics.LinearGradient(0, y1, 0, y1 + refl * 0.7f, 0x40000000, 0x00000000, Shader.TileMode.CLAMP));
            p.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
            c.drawRect(0, y1, W, getHeight(), p);
            p.setXfermode(null);
            p.setShader(null);
            c.restoreToCount(layer);

            drawIcon(c, x0, y0, 1f);
        }

        private void drawIcon(Canvas c, float x0, float y0, float st) {
            float L = lit;
            float rad = fh * 0.22f;
            RectF r = new RectF(x0, y0, x0 + fw, y0 + fh);
            Path shape = new Path();
            shape.addRoundRect(r, rad, rad, Path.Direction.CW);

            // refracted artwork
            p.setColor(Color.WHITE);
            p.setAlpha((int) (255 * st));
            c.drawBitmap(refr, x0, y0, p);
            p.setAlpha(255);

            int save = c.save();
            c.clipPath(shape);
            // outside the beam the icon sinks into the dark
            p.setColor(k((1f - L) * 150 * st));
            c.drawRect(r, p);
            // gentle top light on the surface
            p.setColor(Color.WHITE);
            p.setShader(new android.graphics.LinearGradient(0, y0, 0, y0 + fh * 0.55f, w((8 + L * 40) * st), w(0), Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            c.restoreToCount(save);

            // rim highlights from the bevel slope, brighter when the lamp is on it
            p.setColor(Color.WHITE);
            p.setAlpha((int) (255 * (0.35f + 0.65f * L) * st));
            c.drawBitmap(spec, x0, y0, p);
            p.setAlpha(255);
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
