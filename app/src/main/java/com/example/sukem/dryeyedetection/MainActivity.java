// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.sukem.dryeyedetection;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
// ContentResolver dependency
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.List;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private NavHostFragment navHostFragment;

    public FaceMesh facemesh;
    private static float lastUpdate = System.nanoTime();
    private View viewForCamera;

    private enum InputSource {
        UNKNOWN,
        CAMERA
    }
    private InputSource inputSource = InputSource.UNKNOWN;
    private CameraInput cameraInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {

//                Log.d(TAG, String.format("ID = %d", navDestination.getId()));
//                Log.d(TAG, String.format("home = %d", R.id.navigation_home));
//                Log.d(TAG, String.format("self = %d", R.id.navigation_selfcheck));
//                Log.d(TAG, String.format("setting = %d", R.id.navigation_settings));
            }
        });
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        stopCurrentPipeline();
//        setupStreamingModePipeline();
        viewForCamera = findViewById(R.id.fragmentContainerView);

        // TODO permission check
        // TODO versions
        Intent floatingButtonServiceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(floatingButtonServiceIntent);
        }
    }

    private void setupStreamingModePipeline() {
        this.inputSource = InputSource.CAMERA;
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(true)
                                .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        cameraInput = new CameraInput(this);
        cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));

        facemesh.setResultListener(this::faceMeshResultReceive);
    }

    void faceMeshResultReceive(FaceMeshResult faceMeshResult) {
        // input frame のサイズを取得
        if (EyeAspectRatioUtils.xyRatio == 0) {
            TextureFrame texture = faceMeshResult.acquireInputTextureFrame();
            EyeAspectRatioUtils.xyRatio = (float) texture.getWidth() / texture.getHeight();
        }

        float leftEAR = 0f;
        float rightEAR = 0f;
        if (!faceMeshResult.multiFaceLandmarks().isEmpty()) {
            // 左右注意
            leftEAR = EyeAspectRatioUtils.calcEyesAspectRatio(faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_RIGHT_EYE);
            rightEAR = EyeAspectRatioUtils.calcEyesAspectRatio(faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_LEFT_EYE);
//                        Log.d(TAG, "FPS = " + String.valueOf(1000000000f / (System.nanoTime() - lastUpdate)));
            lastUpdate = System.nanoTime();
        } else {
//                        Log.d(TAG, "EYES NOT FOUND");
        }

        if (navHostFragment == null) {
            navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        }
        assert navHostFragment != null;
        ((FaceMeshResultReceiverInterface) navHostFragment.getChildFragmentManager().getFragments().get(0)).setResult(faceMeshResult, leftEAR, rightEAR, System.nanoTime());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
            viewForCamera.post(this::startCamera);
//            glSurfaceView.post(this::startCamera);
//            glSurfaceView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inputSource == InputSource.CAMERA) {
//            glSurfaceView.setVisibility(View.GONE);
            cameraInput.close();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopCurrentPipeline();
        Log.d(TAG, "onStop called");
    }

    private void startCamera() {
        cameraInput.start(
                this,
                facemesh.getGlContext(),
                CameraInput.CameraFacing.FRONT,
                viewForCamera.getWidth(),
                viewForCamera.getHeight()
//                glSurfaceView.getWidth(),
//                glSurfaceView.getHeight()
//                144,176
        );
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (facemesh != null) {
            facemesh.close();
        }
    }
}
