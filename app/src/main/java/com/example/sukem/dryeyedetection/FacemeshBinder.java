package com.example.sukem.dryeyedetection;

import android.os.Binder;
import android.util.Log;

import com.google.mediapipe.solutioncore.ResultListener;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

public class FacemeshBinder extends Binder {
    private final static String TAG = "FacemeshBinder";

    public FaceMesh facemesh;

    public interface EARResultListener {
        void run(EyeAspectRatio ear);
    }
    public EARResultListener listenerInActivity;
    public EARResultListener listenerInService;
    public ForegroundService service;
    private final EyeAspectRatio ear = new EyeAspectRatio();

    public void resultListener(FaceMeshResult result) {
//        long start = System.nanoTime();
        ear.updateEARs(result);
        ear.calcBlinkRate();
//        Log.d(TAG, String.format("TIME TO CALC BLINK RATE = %f", (System.nanoTime() - start) / 1000000000f));

        if (listenerInActivity != null) {
            listenerInActivity.run(ear);
        }
        if (listenerInService != null) {
            listenerInService.run(ear);
        }
//        if (listenerInActivity == null && listenerInService == null) {
//            Log.d(TAG, "No facemesh listener");
//        }
    }
}
