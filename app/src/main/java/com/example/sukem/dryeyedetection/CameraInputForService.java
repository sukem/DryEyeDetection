/*
 * This source is a modified version of the mediapipe code released under the Apache License 2.0.
 * https://github.com/google/mediapipe
 *
 * */

package com.example.sukem.dryeyedetection;


import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;

import androidx.lifecycle.LifecycleOwner;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.framework.MediaPipeException;

import javax.microedition.khronos.egl.EGLContext;

public class CameraInputForService {
    private static final String TAG = "CameraInput";
    private final CameraXPreviewHelperForService cameraHelper = new CameraXPreviewHelperForService();
    private CameraHelper.OnCameraStartedListener customOnCameraStartedListener;
    private TextureFrameConsumer newFrameListener;
    private SurfaceTexture frameTexture;
    private ExternalTextureConverter converter;

    public CameraInputForService() {
//        PermissionHelper.checkAndRequestCameraPermissions(activity);
    }

    public void setNewFrameListener(TextureFrameConsumer listener) {
        this.newFrameListener = listener;
    }

    public void setOnCameraStartedListener(CameraHelper.OnCameraStartedListener listener) {
        this.customOnCameraStartedListener = listener;
    }

    public void start(Context context, LifecycleOwner lifecycleOwner, EGLContext eglContext, com.google.mediapipe.solutioncore.CameraInput.CameraFacing cameraFacing, int width, int height) {
//        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            if (this.converter == null) {
                this.converter = new ExternalTextureConverter(eglContext, 2);
            }

            if (this.newFrameListener == null) {
                throw new MediaPipeException(MediaPipeException.StatusCode.FAILED_PRECONDITION.ordinal(), "newFrameListener is not set.");
            } else {
                this.frameTexture = this.converter.getSurfaceTexture();
                this.converter.setConsumer(this.newFrameListener);
                this.cameraHelper.setOnCameraStartedListener((surfaceTexture) -> {
                    if (width != 0 && height != 0) {
                        this.updateOutputSize(width, height);
                    }

                    if (this.customOnCameraStartedListener != null) {
                        this.customOnCameraStartedListener.onCameraStarted(surfaceTexture);
                    }

                });
//                this.cameraHelper.startCamera(activity, cameraFacing == com.google.mediapipe.solutioncore.CameraInput.CameraFacing.FRONT ? com.google.mediapipe.components.CameraHelper.CameraFacing.FRONT : com.google.mediapipe.components.CameraHelper.CameraFacing.BACK, this.frameTexture, width != 0 && height != 0 ? new Size(width, height) : null);
//                this.cameraHelper.startCamera(context,
//                        lifecycleOwner,
//                        cameraFacing == com.google.mediapipe.solutioncore.CameraInput.CameraFacing.FRONT ? com.google.mediapipe.components.CameraHelper.CameraFacing.FRONT : com.google.mediapipe.components.CameraHelper.CameraFacing.BACK,
////                        this.frameTexture,
//                        width != 0 && height != 0 ? new Size(width, height) : null);
                this.cameraHelper.startCamera(context,
                        lifecycleOwner,
                        cameraFacing == com.google.mediapipe.solutioncore.CameraInput.CameraFacing.FRONT ? com.google.mediapipe.components.CameraHelper.CameraFacing.FRONT : com.google.mediapipe.components.CameraHelper.CameraFacing.BACK,
                        this.frameTexture,
                        width != 0 && height != 0 ? new Size(width, height) : null);
            }
//        }
    }

    public void updateOutputSize(int width, int height) {
        Size displaySize = this.cameraHelper.computeDisplaySizeFromViewSize(new Size(width, height));
        boolean isCameraRotated = this.cameraHelper.isCameraRotated();
        Log.i("CameraInput", "Set camera output texture frame size to width=" + displaySize.getWidth() + " , height=" + displaySize.getHeight());
        this.converter.setDestinationSize(isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(), isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    public void close() {
        if (this.converter != null) {
            this.converter.close();
        }

    }

    public boolean isCameraRotated() {
        return this.cameraHelper.isCameraRotated();
    }

    public static enum CameraFacing {
        FRONT,
        BACK;

        private CameraFacing() {
        }
    }
}
