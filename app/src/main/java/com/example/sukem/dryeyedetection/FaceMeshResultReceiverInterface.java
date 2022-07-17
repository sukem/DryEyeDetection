package com.example.sukem.dryeyedetection;

import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

public interface FaceMeshResultReceiverInterface {
    public abstract void setResult(FaceMeshResult faceMeshResult, EyeAspectRatio.EARData current);
}
