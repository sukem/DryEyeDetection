package com.example.sukem.dryeyedetection;

import android.os.Binder;

import com.google.mediapipe.solutioncore.ResultListener;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

public class FacemeshBinder extends Binder {
    private final static String TAG = "FacemeshBinder";

    public FaceMesh facemesh;

    public interface ResultListener<T1,T2> {
        void run(T1 result, T2 ear);
    }
    public ResultListener<FaceMeshResult, EyeAspectRatio> listenerInActivity;
    public ResultListener<FaceMeshResult, EyeAspectRatio> listenerInService;
    public ForegroundService service;
    private final EyeAspectRatio ear = new EyeAspectRatio();


    public void resultListener(FaceMeshResult result) {
        ear.updateEARs(result);

        if (listenerInActivity != null) {
            listenerInActivity.run(result, ear);
        }
        if (listenerInService != null) {
            listenerInService.run(result, ear);
        }
//        if (listenerInActivity == null && listenerInService == null) {
//            Log.d(TAG, "No facemesh listener");
//        }
    }
}
