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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
// ContentResolver dependency
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutioncore.VideoInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.List;
import java.util.Objects;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private NavController navController;

    private FaceMesh facemesh;
    // Run the pipeline and the model inference on GPU or CPU.
    private static final boolean RUN_ON_GPU = true;

    private static float lastUpdate = System.nanoTime();

    private enum InputSource {
        UNKNOWN,
        IMAGE,
        VIDEO,
        CAMERA,
    }
    private InputSource inputSource = InputSource.UNKNOWN;
    private FaceMeshResultImageView imageView;
    // Live camera demo UI and camera components.
    private CameraInput cameraInput;

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert fragment != null;
        navController = fragment.getNavController();
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
        setupStreamingModePipeline(InputSource.CAMERA);
        startCamera();
    }

    private void setupStreamingModePipeline(InputSource inputSource) {
        this.inputSource = inputSource;
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        if (inputSource == InputSource.CAMERA) {
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
        }
        facemesh.setResultListener(
                faceMeshResult -> {
                    float leftEAR = 0f;
                    float rightEAR = 0f;
                    if (!faceMeshResult.multiFaceLandmarks().isEmpty()) {
                        leftEAR = calcEyesAspectRatio(faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList(),
                                FaceMeshConnections.FACEMESH_LEFT_EYE);
                        rightEAR = calcEyesAspectRatio(faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList(),
                                FaceMeshConnections.FACEMESH_RIGHT_EYE);
                        Log.d(TAG, "FPS = " + String.valueOf(1000000000f / (System.nanoTime() - lastUpdate)));
                        lastUpdate = System.nanoTime();
                    } else {
                        Log.d(TAG, "EYES NOT FOUND");
                    }

                    Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
                    assert navHostFragment != null;
                    ((FaceMeshResultReceiverInterface) navHostFragment.getChildFragmentManager().getFragments().get(0)).setResult(faceMeshResult, leftEAR, rightEAR, System.nanoTime());
                });
    }

    private float getDistance(float[] vec1, float[] vec2)
    {
        float x = vec1[0] - vec2[0];
        float y = vec1[1] - vec2[1];
        return (float) Math.sqrt(x*x + y*y);
    }

    private float dotProduct(float[] vec1, float[] vec2)
    {
        return vec1[0]*vec2[0] + vec1[1]*vec2[1];
    }

    private float calcEyesAspectRatio(
            List<NormalizedLandmark> faceLandmarkList,
            ImmutableSet<FaceMeshConnections.Connection> connections)
    {
        List<FaceMeshConnections.Connection> list = connections.asList();
        NormalizedLandmark p1 = faceLandmarkList.get(list.get(0).start());
        NormalizedLandmark p2 = faceLandmarkList.get(list.get(3).start());
        NormalizedLandmark p3 = faceLandmarkList.get(list.get(4).end());
        NormalizedLandmark p4 = faceLandmarkList.get(list.get(7).end());
        NormalizedLandmark p5 = faceLandmarkList.get(list.get(12).end());
        NormalizedLandmark p6 = faceLandmarkList.get(list.get(11).start());

        float xyRatio = (float) Math.sqrt(1.0/1.61);
        float[] horizontalVec = {(p4.getX() - p1.getX()) * xyRatio, p4.getY() - p1.getY()};
        float[] orig = {0f, 0f};
        float hvd = getDistance(horizontalVec, orig);
        float[] verticalUnit = {horizontalVec[1] / hvd, -horizontalVec[0] / hvd};
        float[] verticalVec1 = {(p2.getX() - p6.getX()) * xyRatio, p2.getY() - p6.getY()};
        float[] verticalVec2 = {(p3.getX() - p5.getX()) * xyRatio, p3.getY() - p5.getY()};
        float ear = (dotProduct(verticalVec1, verticalUnit) + dotProduct(verticalVec2, verticalUnit)) / (2 * hvd);

//        Log.d(TAG, "EAR = " + String.valueOf(ear));
        return ear;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
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

    private void startCamera() {
        cameraInput.start(
                this,
                facemesh.getGlContext(),
                CameraInput.CameraFacing.FRONT,
//                glSurfaceView.getWidth(),
//                glSurfaceView.getHeight()
                640,480
        );
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE);
        }
        if (facemesh != null) {
            facemesh.close();
        }
    }
}
