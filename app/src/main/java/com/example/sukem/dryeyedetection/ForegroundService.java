package com.example.sukem.dryeyedetection;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    private WindowManager windowManager;
    private ImageView floatingButton;

    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

        setupFloatingView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startFloatingButtonForeground();
        } else {
            startForeground(1, new Notification());
        }
    }

    private void setupFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingButton = new ImageView(this);
        floatingButton.setImageResource(R.mipmap.ic_launcher_round);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // 左右の警告は無視
        params.gravity = Gravity.TOP | Gravity.LEFT;
        SharedPreferences coordinates = PreferenceManager.getDefaultSharedPreferences(ForegroundService.this);
        int savedX = coordinates.getInt("x", 100);
        int savedY = coordinates.getInt("y", 100);
        params.x = savedX;
        params.y = savedY;

        final GestureDetector gestureDetector = new GestureDetector(this, new MyGestureDetector());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        };

        floatingButton.setOnTouchListener(gestureListener);

        windowManager.addView(floatingButton, params);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startFloatingButtonForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "com.example.sukem.dryeyedetection";
        String channelName = "Overlay Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Service running")
                .setContentText("Displaying over other apps")

                // this is important, otherwise the notification will show the way
                // you want i.e. it will show some default notification
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != floatingButton) {
            windowManager.removeView(floatingButton);
        }
    }

    class MyGestureDetector extends android.view.GestureDetector.SimpleOnGestureListener {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;

        @Override
        public boolean onDown(MotionEvent e) {
            initialX = params.x;
            initialY = params.y;
            initialTouchX = e.getRawX();
            initialTouchY = e.getRawY();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // TODO orientation support
            params.x = initialX + (int) (e2.getRawX() - initialTouchX);
            params.y = initialY + (int) (e2.getRawY() - initialTouchY);
            windowManager.updateViewLayout(floatingButton, params);

            PreferenceManager.getDefaultSharedPreferences(ForegroundService.this).edit().putInt("x", params.x).apply();
            PreferenceManager.getDefaultSharedPreferences(ForegroundService.this).edit().putInt("y", params.y).apply();

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            startActivity(intent);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            stopSelf();
            return true;
        }
    }
}
