package com.worldflight;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps3d.GoogleMap3D;
import com.google.android.gms.maps3d.Map3DView;
import com.google.android.gms.maps3d.OnMap3DViewReadyCallback;
import com.google.android.gms.maps3d.model.Camera;
import com.google.android.gms.maps3d.model.FlyToOptions;
import com.google.android.gms.maps3d.model.LatLngAltitude;
import com.google.android.gms.maps3d.model.Map3DMode;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map3DView map3DView;
    private GoogleMap3D map;
    private TextView status;
    private boolean flightStarted = false;

    private static final Waypoint[] NYC_ROUTE = new Waypoint[] {
            // Midtown / Times Square -> Central Park South. Low cinematic corridor flight.
            new Waypoint(40.75495, -73.98755, 18.0, 29.0, 83.0, -3.0, 52.0, 0),
            new Waypoint(40.75635, -73.98685, 20.0, 29.0, 84.0,  4.0, 48.0, 3200),
            new Waypoint(40.75805, -73.98595, 22.0, 28.0, 85.0, -5.0, 46.0, 3300),
            new Waypoint(40.75965, -73.98505, 24.0, 29.0, 84.0,  5.0, 48.0, 3300),
            new Waypoint(40.76130, -73.98410, 24.0, 30.0, 83.0, -4.0, 50.0, 3400),
            new Waypoint(40.76285, -73.98320, 26.0, 31.0, 82.0,  4.0, 54.0, 3400),
            new Waypoint(40.76430, -73.98230, 28.0, 32.0, 81.0, -2.0, 58.0, 3600),
            new Waypoint(40.76555, -73.98165, 34.0, 33.0, 79.0,  0.0, 66.0, 3800)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        map3DView = new Map3DView(this, Map3DMode.HYBRID);
        root.addView(map3DView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("NEW YORK · LOADING 3D");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12f);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(0x66000000);
        status.setPadding(22, 12, 22, 12);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = 36;
        root.addView(status, statusParams);

        setContentView(root);
        map3DView.onCreate(savedInstanceState);

        map3DView.getMap3DViewAsync(new OnMap3DViewReadyCallback() {
            @Override
            public void onMap3DViewReady(GoogleMap3D googleMap3D) {
                map = googleMap3D;
                status.setText("NEW YORK · PREPARING FLIGHT");
                Camera start = cameraFor(NYC_ROUTE[0]);
                map.setCamera(start);

                map.setOnMapSteadyListener(isSteady -> {
                    if (isSteady && !flightStarted) {
                        flightStarted = true;
                        map.setOnMapSteadyListener(null);
                        status.setText("NEW YORK · WORLD FLIGHT");
                        handler.postDelayed(() -> status.animate().alpha(0f).setDuration(900).start(), 1800);
                        handler.postDelayed(() -> flyLeg(1), 700);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                status.setAlpha(1f);
                status.setText("MAP ERROR · " + error.getMessage());
            }
        });
    }

    private Camera cameraFor(Waypoint p) {
        LatLngAltitude center = new LatLngAltitude(p.lat, p.lng, p.altitude);
        return new Camera(center, p.heading, p.tilt, p.roll, p.range);
    }

    private void flyLeg(int index) {
        if (map == null || index >= NYC_ROUTE.length) {
            handler.postDelayed(this::restartFlight, 1800);
            return;
        }

        Waypoint p = NYC_ROUTE[index];
        map.flyCameraTo(new FlyToOptions(cameraFor(p), p.durationMs));
        handler.postDelayed(() -> flyLeg(index + 1), p.durationMs);
    }

    private void restartFlight() {
        if (map == null) return;
        map.stopCameraAnimation();
        map.setCamera(cameraFor(NYC_ROUTE[0]));
        handler.postDelayed(() -> flyLeg(1), 1200);
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

    @Override
    protected void onStart() {
        super.onStart();
        if (map3DView != null) map3DView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (map3DView != null) map3DView.onResume();
    }

    @Override
    protected void onPause() {
        if (map3DView != null) map3DView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (map3DView != null) map3DView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (map3DView != null) map3DView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (map3DView != null) map3DView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (map3DView != null) map3DView.onSaveInstanceState(outState);
    }

    private static final class Waypoint {
        final double lat;
        final double lng;
        final double altitude;
        final double heading;
        final double tilt;
        final double roll;
        final double range;
        final long durationMs;

        Waypoint(double lat, double lng, double altitude, double heading,
                 double tilt, double roll, double range, long durationMs) {
            this.lat = lat;
            this.lng = lng;
            this.altitude = altitude;
            this.heading = heading;
            this.tilt = tilt;
            this.roll = roll;
            this.range = range;
            this.durationMs = durationMs;
        }
    }
}
