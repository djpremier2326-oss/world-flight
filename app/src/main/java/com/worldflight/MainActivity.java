package com.worldflight;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.content.Context;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUi();
        setContentView(new FlightView(this));
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    static class FlightView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private long startMs = SystemClock.elapsedRealtime();
        private long pauseStartedMs = 0L;
        private long pausedTotalMs = 0L;
        private boolean paused = false;

        FlightView(Context context) {
            super(context);
            paint.setStrokeCap(Paint.Cap.ROUND);
            setBackgroundColor(Color.BLACK);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                long now = SystemClock.elapsedRealtime();
                if (!paused) {
                    paused = true;
                    pauseStartedMs = now;
                } else {
                    paused = false;
                    pausedTotalMs += now - pauseStartedMs;
                }
                invalidate();
                return true;
            }
            return true;
        }

        private float timeSeconds() {
            long now = paused ? pauseStartedMs : SystemClock.elapsedRealtime();
            return (now - startMs - pausedTotalMs) / 1000f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            float t = timeSeconds();
            float horizon = h * 0.34f;

            paint.setShader(new LinearGradient(0, 0, 0, horizon * 1.2f,
                    Color.rgb(136, 190, 193), Color.rgb(215, 224, 190), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, horizon * 1.25f, paint);
            paint.setShader(null);

            canvas.save();
            float bank = (float)Math.sin(t * 0.78f) * 4.2f + (float)Math.sin(t * 0.21f) * 2.0f;
            float bob = (float)Math.sin(t * 1.7f) * 8f;
            canvas.rotate(bank, w * 0.5f, h * 0.56f);
            canvas.translate(0, bob);

            drawFarMountains(canvas, w, h, horizon, t);
            drawCanyon(canvas, w, h, horizon, t);
            drawSpeedDetails(canvas, w, h, horizon, t);
            canvas.restore();

            drawOverlay(canvas, w, h, t);

            if (!paused) postInvalidateOnAnimation();
        }

        private void drawFarMountains(Canvas c, float w, float h, float horizon, float t) {
            float shift = (float)Math.sin(t * 0.12f) * w * 0.04f;
            paint.setColor(Color.rgb(79, 108, 79));
            path.reset();
            path.moveTo(-w * 0.15f + shift, horizon + h * 0.03f);
            path.lineTo(w * 0.06f + shift, horizon - h * 0.10f);
            path.lineTo(w * 0.18f + shift, horizon - h * 0.02f);
            path.lineTo(w * 0.33f + shift, horizon - h * 0.16f);
            path.lineTo(w * 0.49f + shift, horizon - h * 0.03f);
            path.lineTo(w * 0.64f + shift, horizon - h * 0.13f);
            path.lineTo(w * 0.83f + shift, horizon - h * 0.01f);
            path.lineTo(w * 1.12f + shift, horizon - h * 0.11f);
            path.lineTo(w * 1.18f, horizon + h * 0.08f);
            path.close();
            c.drawPath(path, paint);

            paint.setColor(Color.rgb(57, 86, 59));
            path.reset();
            path.moveTo(-w * 0.1f - shift, horizon + h * 0.05f);
            path.lineTo(w * 0.10f - shift, horizon - h * 0.03f);
            path.lineTo(w * 0.25f - shift, horizon + h * 0.01f);
            path.lineTo(w * 0.42f - shift, horizon - h * 0.08f);
            path.lineTo(w * 0.57f - shift, horizon + h * 0.02f);
            path.lineTo(w * 0.78f - shift, horizon - h * 0.07f);
            path.lineTo(w * 1.1f - shift, horizon + h * 0.05f);
            path.close();
            c.drawPath(path, paint);
        }

        private void drawCanyon(Canvas c, float w, float h, float horizon, float t) {
            int slices = 34;
            float phase = (t * 0.62f) % 1f;
            float prevY = horizon;
            float prevCenter = w * 0.5f;
            float prevHalf = w * 0.055f;

            for (int i = 1; i <= slices; i++) {
                float depth = i / (float)slices;
                float curve = (float)Math.sin(t * 0.54f + depth * 2.8f) * w * (0.015f + depth * 0.075f)
                        + (float)Math.sin(t * 0.19f + depth * 5.2f) * w * 0.018f;
                float y = horizon + (float)Math.pow(depth, 1.72) * (h - horizon + h * 0.08f);
                float half = w * (0.055f + (float)Math.pow(depth, 1.24) * 0.36f);
                float center = w * 0.5f + curve;

                int stripe = (int)Math.floor(depth * 18f + t * 3.0f);
                paint.setColor((stripe & 1) == 0 ? Color.rgb(66, 92, 61) : Color.rgb(58, 82, 55));
                path.reset();
                path.moveTo(prevCenter - prevHalf, prevY);
                path.lineTo(prevCenter + prevHalf, prevY);
                path.lineTo(center + half, y);
                path.lineTo(center - half, y);
                path.close();
                c.drawPath(path, paint);

                int leftShade = 54 + (int)(42 * depth);
                paint.setColor(Color.rgb(34, leftShade, 40));
                path.reset();
                path.moveTo(0, prevY - h * 0.12f * depth);
                path.lineTo(prevCenter - prevHalf, prevY);
                path.lineTo(center - half, y);
                path.lineTo(0, y + h * 0.08f);
                path.close();
                c.drawPath(path, paint);

                int rightShade = 62 + (int)(45 * depth);
                paint.setColor(Color.rgb(38, rightShade, 44));
                path.reset();
                path.moveTo(w, prevY - h * 0.10f * depth);
                path.lineTo(prevCenter + prevHalf, prevY);
                path.lineTo(center + half, y);
                path.lineTo(w, y + h * 0.07f);
                path.close();
                c.drawPath(path, paint);

                if (i % 5 == 0 && depth > 0.18f) {
                    float riverHalf = half * 0.24f;
                    paint.setColor(Color.rgb(67, 112, 111));
                    c.drawRect(center - riverHalf, y - Math.max(2f, depth * 10f), center + riverHalf, y + Math.max(2f, depth * 10f), paint);
                }

                prevY = y;
                prevCenter = center;
                prevHalf = half;
            }

            paint.setColor(Color.argb(55, 230, 235, 216));
            c.drawRect(0, horizon - h * 0.025f, w, horizon + h * 0.065f, paint);
        }

        private void drawSpeedDetails(Canvas c, float w, float h, float horizon, float t) {
            int count = 24;
            for (int i = 0; i < count; i++) {
                float p = ((i / (float)count) + (t * 0.18f)) % 1f;
                float depth = p;
                float y = horizon + (float)Math.pow(depth, 1.65) * (h - horizon);
                float center = w * 0.5f + (float)Math.sin(t * 0.54f + depth * 2.8f) * w * (0.02f + depth * 0.08f);
                float spread = w * (0.08f + depth * 0.48f);
                float side = (i % 2 == 0) ? -1f : 1f;
                float x = center + side * spread;
                float size = 3f + depth * depth * 34f;

                paint.setColor(Color.rgb(24, 63 + (int)(45 * depth), 30));
                c.drawCircle(x, y, size * 0.62f, paint);
                c.drawCircle(x + side * size * 0.3f, y - size * 0.35f, size * 0.45f, paint);

                if (depth > 0.72f) {
                    paint.setColor(Color.argb((int)(90 * depth), 225, 235, 210));
                    paint.setStrokeWidth(1.5f + depth * 2f);
                    c.drawLine(x, y - size, x - side * size * 0.7f, y - size * 2.4f, paint);
                }
            }
        }

        private void drawOverlay(Canvas c, float w, float h, float t) {
            paint.setTextAlign(Paint.Align.CENTER);
            if (t < 3.2f) {
                float alpha = t < 2.2f ? 1f : Math.max(0f, 1f - (t - 2.2f));
                paint.setColor(Color.argb((int)(230 * alpha), 255, 255, 255));
                paint.setTextSize(w * 0.075f);
                paint.setFakeBoldText(true);
                c.drawText("WORLD FLIGHT", w * 0.5f, h * 0.16f, paint);
                paint.setTextSize(w * 0.035f);
                paint.setFakeBoldText(false);
                paint.setColor(Color.argb((int)(200 * alpha), 255, 255, 255));
                c.drawText("JUNGLE RUN · PROTOTYPE 01", w * 0.5f, h * 0.205f, paint);
                paint.setTextSize(w * 0.031f);
                paint.setColor(Color.argb((int)(170 * alpha), 255, 255, 255));
                c.drawText("tap anywhere to pause", w * 0.5f, h * 0.89f, paint);
            }

            if (paused) {
                paint.setColor(Color.argb(120, 0, 0, 0));
                c.drawRect(0, 0, w, h, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(w * 0.075f);
                paint.setFakeBoldText(true);
                c.drawText("PAUSED", w * 0.5f, h * 0.49f, paint);
                paint.setTextSize(w * 0.032f);
                paint.setFakeBoldText(false);
                c.drawText("tap to continue", w * 0.5f, h * 0.54f, paint);
            }
            paint.setFakeBoldText(false);
        }
    }
}
