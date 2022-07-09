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
import android.os.Binder;
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
import androidx.lifecycle.LifecycleService;

import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;

public class ForegroundService extends LifecycleService {
    private static final String TAG = "ForegroundService";
    private WindowManager windowManager;
    private ImageView floatingButton;
    private WindowManager.LayoutParams params;
    private FacemeshBinder facemeshBinder = new FacemeshBinder();
    enum InputSource {
        UNKNOWN,
        CAMERA
    }
    private InputSource inputSource = InputSource.UNKNOWN;
    private CameraInputForService cameraInput;

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return facemeshBinder;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

//        setupFloatingView();
        setupStreamingModePipeline();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startFloatingButtonForeground();
        } else {
            startForeground(1, new Notification());
        }
    }

    private void setupStreamingModePipeline() {
        this.inputSource = InputSource.CAMERA;
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
        facemeshBinder.facemesh.setResultListener(facemesh -> {
//            Log.d(TAG, "DATA RECEIVED");
        });

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
                .setContentText("Displaying over other apps")

                // this is important, otherwise the notification will show the way
                // you want i.e. it will show some default notification
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(openIntent)
                .addAction(R.drawable.ic_launcher_foreground, "停止する", sendPendingIntent)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCurrentPipeline();
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
