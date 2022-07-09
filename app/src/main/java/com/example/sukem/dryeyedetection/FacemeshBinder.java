package com.example.sukem.dryeyedetection;

import android.os.Binder;

import com.google.mediapipe.solutions.facemesh.FaceMesh;

public class FacemeshBinder extends Binder {
    public FaceMesh facemesh;
}
