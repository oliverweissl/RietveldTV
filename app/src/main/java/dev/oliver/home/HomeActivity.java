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
    private LinearLayout widgetRow;
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
        widgetRow = new LinearLayout(this);
        widgetRow.setOrientation(LinearLayout.HORIZONTAL);
        widgetRow.setGravity(Gravity.TOP | Gravity.END);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, px(36), px(56), 0);
        root.addView(widgetRow, cp);

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
        row.setPadding(px(346), px(38), px(346), px(6));
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

    private void buildWidgets() {
        widgets.clear();
        widgetRow.removeAllViews();
        for (org.json.JSONObject cfg : config.widgets) {
            Widgets.Widget w = Widgets.create(style, cfg);
            if (w == null) continue;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (widgetRow.getChildCount() > 0) lp.setMarginStart(px(40));
            widgetRow.addView(w.view, lp);
            widgets.add(w);
        }
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
            float d = (c.getLeft() + c.getWidth() / 2f - mid) / (c.getWidth() * 1.55f);
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
        lp.setMargins(px(26), 0, px(26), 0);
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

    /** One app as a single Liquid-Glass style icon: the art fills the glass, the glyph floats inside it, light lives on the rim. */
    private class GlassBox extends View {
        private final Drawable art;
        private final int fw = px(216), fh = px(126), depth = px(34), side = px(8), refl = px(36);
        String title = "";
        private float lit = 0f, dir = 0f;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

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

        private int w(float a) { return Color.argb(Math.max(0, Math.min(255, Math.round(a))), 255, 255, 255); }
        private int k(float a) { return Color.argb(Math.max(0, Math.min(255, Math.round(a))), 0, 0, 0); }

        @Override protected void onDraw(Canvas c) {
            int W = getWidth();
            float x0 = (W - fw) / 2f, y0 = depth, x1 = x0 + fw, y1 = depth + fh;
            float L = lit;

            // contact shadow
            c.save();
            c.translate(W / 2f, y1);
            c.scale(1f, 0.08f);
            p.setColor(Color.WHITE); p.setShader(new RadialGradient(0, 0, fw * 0.55f, 0xC0000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, fw * 0.55f, p);
            c.restore();
            p.setShader(null);

            // faint gloss reflection in the black floor
            int layer = c.saveLayer(0, y1, W, getHeight(), null);
            c.save();
            c.scale(1f, -1f, W / 2f, y1);
            drawIcon(c, x0, y0, x1, y1, 0.06f + 0.26f * L);
            c.restore();
            p.setColor(Color.WHITE); p.setShader(new android.graphics.LinearGradient(0, y1, 0, y1 + refl * 0.7f, 0x40000000, 0x00000000, Shader.TileMode.CLAMP));
            p.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
            c.drawRect(0, y1, W, getHeight(), p);
            p.setXfermode(null);
            p.setShader(null);
            c.restoreToCount(layer);

            drawIcon(c, x0, y0, x1, y1, 1f);
        }

        private void drawIcon(Canvas c, float x0, float y0, float x1, float y1, float st) {
            float L = lit;
            RectF r = new RectF(x0, y0, x1, y1);
            float rad = fh * 0.2f;                        // soft rounded rectangle, not a square
            Path shape = new Path();
            shape.addRoundRect(r, rad, rad, Path.Direction.CW);

            int save = c.save();
            c.clipPath(shape);
            p.setColor(Color.WHITE); p.setShader(new android.graphics.LinearGradient(0, y0, 0, y1,
                    Color.rgb(46, 48, 52), Color.rgb(16, 17, 19), Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);

            int alpha = (int) (255 * st);
            float rw = x1 - x0, rh = y1 - y0;
            if (art instanceof android.graphics.drawable.AdaptiveIconDrawable) {
                android.graphics.drawable.AdaptiveIconDrawable ad = (android.graphics.drawable.AdaptiveIconDrawable) art;
                Drawable bg = ad.getBackground(), fg = ad.getForeground();
                // background layer fills the whole rectangle
                float bs = Math.max(rw, rh) * 1.5f;
                android.graphics.Rect bb = new android.graphics.Rect((int) (r.centerX() - bs / 2), (int) (r.centerY() - bs / 2),
                        (int) (r.centerX() + bs / 2), (int) (r.centerY() + bs / 2));
                if (bg != null) { bg.setBounds(bb); bg.setAlpha((int) (alpha * (0.30f + 0.22f * L))); bg.draw(c); }
                // glyph layer: a square sized to the rectangle's height, floating on a soft shadow
                float gs = rh * 1.5f;
                android.graphics.Rect gb = new android.graphics.Rect((int) (r.centerX() - gs / 2), (int) (r.centerY() - gs / 2),
                        (int) (r.centerX() + gs / 2), (int) (r.centerY() + gs / 2));
                if (fg != null) {
                    fg.setBounds(gb.left, gb.top + px(3), gb.right, gb.bottom + px(3));
                    fg.setColorFilter(k(90 * st), android.graphics.PorterDuff.Mode.SRC_IN);
                    fg.setAlpha(alpha);
                    fg.draw(c);
                    fg.clearColorFilter();
                    fg.setBounds(gb);
                    fg.setAlpha(alpha);
                    fg.draw(c);
                }
            } else if (art != null) {
                // plain bitmap icon: scale to fill the rectangle (crop the overflow), keeps the logo centred and big
                int iw = Math.max(1, art.getIntrinsicWidth()), ih = Math.max(1, art.getIntrinsicHeight());
                float sc = Math.max(rw / iw, rh / ih);
                float dw = iw * sc, dh = ih * sc;
                art.setBounds((int) (r.centerX() - dw / 2), (int) (r.centerY() - dh / 2),
                        (int) (r.centerX() + dw / 2), (int) (r.centerY() + dh / 2));
                art.setAlpha(alpha);
                art.draw(c);
            } else {
                p.setColor(Color.argb(alpha, 236, 234, 230));
                p.setTextSize(px(56));
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText("\u2699", r.centerX(), r.centerY() + px(19), p);
            }

            // outside the beam the icon sinks into the dark
            p.setColor(k((1f - L) * 150 * st));
            c.drawRect(r, p);

            // glass body: soft top gloss from the lamp
            p.setColor(Color.WHITE); p.setShader(new android.graphics.LinearGradient(0, y0, 0, y0 + fh * 0.5f,
                    w((18 + L * 70) * st), w(0), Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            // liquid gloss: a curved sheet of light across the upper third
            c.save();
            c.translate(r.centerX(), y0 - fh * 0.55f);
            c.scale(1f, 0.62f);
            p.setColor(Color.WHITE); p.setShader(new RadialGradient(0, 0, fw * 0.95f,
                    new int[]{w(0), w(0), w((10 + L * 46) * st), w(0)}, new float[]{0f, 0.78f, 0.9f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, fw * 0.95f, p);
            c.restore();
            p.setShader(null);
            // edge refraction: the glass thickens toward the rim, darkening it slightly
            p.setColor(Color.WHITE); p.setShader(new RadialGradient(r.centerX(), r.centerY(), fw * 0.75f,
                    new int[]{0x00000000, 0x00000000, k(70 * st)}, new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            c.restoreToCount(save);

            // rim light: a thin specular edge, bright on top where the lamp hits, gone at the bottom
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.6f * dp);
            p.setColor(Color.WHITE); p.setShader(new android.graphics.LinearGradient(0, y0, 0, y1,
                    new int[]{w((60 + L * 195) * st), w((14 + L * 40) * st), w(4 * st)}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            RectF rim = new RectF(r.left + 0.8f * dp, r.top + 0.8f * dp, r.right - 0.8f * dp, r.bottom - 0.8f * dp);
            c.drawRoundRect(rim, rad, rad, p);
            // second, inner rim: the far wall of the glass seen through the surface
            p.setStrokeWidth(1f * dp);
            p.setColor(Color.WHITE); p.setShader(new android.graphics.LinearGradient(0, y0, 0, y1,
                    new int[]{w(4 * st), w((10 + L * 30) * st), w((20 + L * 70) * st)}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            RectF inner = new RectF(r.left + 3.5f * dp, r.top + 3.5f * dp, r.right - 3.5f * dp, r.bottom - 3.5f * dp);
            c.drawRoundRect(inner, rad - 3 * dp, rad - 3 * dp, p);
            p.setStyle(Paint.Style.FILL);
            p.setShader(null);
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
