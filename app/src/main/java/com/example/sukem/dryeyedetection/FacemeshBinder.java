package com.example.sukem.dryeyedetection;

import android.os.Binder;
import android.util.Log;

import com.google.mediapipe.solutioncore.ResultListener;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

public class FacemeshBinder extends Binder {
    private final static String TAG = "FacemeshBinder";

    public FaceMesh facemesh;
    public ResultListener<FaceMeshResult> listenerInActivity;
    public ResultListener<FaceMeshResult> listenerInService;

    public void resultListener(FaceMeshResult result) {
        if (listenerInActivity != null) {
            listenerInActivity.run(result);
        }
        if (listenerInService != null) {
            listenerInService.run(result);
        }
//        if (listenerInActivity == null && listenerInService == null) {
//            Log.d(TAG, "No facemesh listener");
//        }
    }
}
