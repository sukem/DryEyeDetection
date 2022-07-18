package com.example.sukem.dryeyedetection;

import static android.content.Intent.ACTION_SEND;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleService;
import androidx.navigation.fragment.NavHostFragment;

import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;

import java.text.AttributedCharacterIterator;

public class ForegroundService extends LifecycleService {
    private static final String TAG = "ForegroundService";
    private WindowManager windowManager;
    private ImageView floatingButton;
    public static boolean haveFloatingView = false;
    private WindowManager.LayoutParams params;
    private final FacemeshBinder facemeshBinder = new FacemeshBinder();
    private CameraInputForService cameraInput;
    private long lastNotificationTime = 0;

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        facemeshBinder.service = this;
        return facemeshBinder;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

        setupStreamingModePipeline();

        startFloatingButtonForeground();

        facemeshBinder.listenerInService = this::faceMeshResultReceive;
    }

    void faceMeshResultReceive(EyeAspectRatio ear) {
        if (floatingButton != null) {
            if ((ear.getCurrent().left + ear.getCurrent().right) / 2 > EyeAspectRatio.earThreshold) {
                floatingButton.post(() -> {
                    if (floatingButton != null) {
                        floatingButton.setImageResource(R.drawable.ic_eye_svgrepo_com);
                    }
                });
            } else {
                if (ear.getCurrent().detected) {
                    floatingButton.post(() -> {
                        if (floatingButton != null) {
                            floatingButton.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
                        }
                    });
                } else {
                    floatingButton.post(() -> {
                        if (floatingButton != null) {
                            floatingButton.setImageResource(R.drawable.ic_eye_no_svgrepo_com);
                        }
                    });
                }
            }
        }

        float blinkPerMin = (ear.getLeftBlinkPerMin() + ear.getRightBlinkPerMin()) / 2;
        float blinkRate = (ear.getLeftBlinkRate() + ear.getRightBlinkRate()) / 2;
        if (ear.getDataState() == EyeAspectRatio.EARDataState.FINE
                && blinkPerMin < 30
                && blinkRate < 0.3
                && (System.nanoTime() - lastNotificationTime > 60 * 1000000000L)) {
            alertByNotification();
            lastNotificationTime = System.nanoTime();
        }
    }

    private void alertByNotification() {
        // 通知
        String channelId = "com.example.sukem.dryeyedetection.alert";
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Dry Eye Alert",
                NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        // タップしたときにMainActivityを起動するためのIntent
        Intent openMainIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(getApplicationContext(), 0, openMainIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Dry eye alert")
                .setContentText("Your eyes may be dry!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(openIntent)
                .build();
//        notification.flags &= ~Notification.FLAG_NO_CLEAR;
//        notification.flags &= ~Notification.FLAG_FOREGROUND_SERVICE;

        Log.d(TAG, String.valueOf(notification.flags));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        // 通知
        notificationManagerCompat.notify(2, notification);
    }

    private void setupStreamingModePipeline() {
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemeshBinder.facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(true)
                                .build());
        facemeshBinder.facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        cameraInput = new CameraInputForService();
        cameraInput.setNewFrameListener(textureFrame -> facemeshBinder.facemesh.send(textureFrame));
        facemeshBinder.facemesh.setResultListener(facemeshBinder::resultListener);

        startCamera();
    }

    private void startCamera() {
        cameraInput.start(
                getApplicationContext(),
                this,
                facemeshBinder.facemesh.getGlContext(),
                CameraInput.CameraFacing.FRONT,
                480,
                640
        );
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (facemeshBinder.facemesh != null) {
            facemeshBinder.facemesh.close();
        }
    }

    public void setupFloatingView() {
        if (floatingButton != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingButton = new ImageView(this);
        floatingButton.setImageResource(R.drawable.ic_eye_svgrepo_com);
        floatingButton.setBackgroundResource(R.drawable.floating_background);

        int LAYOUT_FLAG;
        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params = new WindowManager.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics()),
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

    public void removeFloatingView() {
        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
            floatingButton = null;
        }
        haveFloatingView = false;
    }

    private void startFloatingButtonForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "com.example.sukem.dryeyedetection.service";
        String channelName = "Foreground Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        // タップしたときにMainActivityを起動するためのIntent
        Intent openMainIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(getApplicationContext(), 0, openMainIntent, PendingIntent.FLAG_IMMUTABLE);

        // 停止ボタン
        Intent sendIntent = new Intent(getApplicationContext(), ServiceBroadcastReceiver.class);
        sendIntent.setAction(ACTION_SEND);
        PendingIntent sendPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, sendIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Service running")
                .setContentText("Facemesh running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(openIntent)
                .addAction(R.drawable.ic_eye_svgrepo_com, "stop service", sendPendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCurrentPipeline();
        removeFloatingView();
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
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            removeFloatingView();
            // mainActivityに繋がってなければ終了
            if (facemeshBinder.listenerInActivity == null) {
                stopSelf();
            }
            return true;
        }
    }
}
