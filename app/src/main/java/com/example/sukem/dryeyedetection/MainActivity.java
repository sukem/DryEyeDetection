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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
// ContentResolver dependency
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.List;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "MainActivity";

    private NavHostFragment navHostFragment;

    private FacemeshBinder facemeshBinder;
//    private boolean bounded = false;


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected");
//        Toast.makeText(getApplicationContext(), "サービスに接続しました", Toast.LENGTH_SHORT).show();
        facemeshBinder = (FacemeshBinder) iBinder;
        facemeshBinder.listenerInActivity = this::faceMeshResultReceive;
//        bounded = true;
    }

    private void cleanBinderFromActivity() {
//        Toast.makeText(getApplicationContext(), "サービスから切断しました", Toast.LENGTH_SHORT).show();
        facemeshBinder.listenerInActivity = null;
        facemeshBinder = null;
//        bounded = false;
    }

    // onServiceDisconnected は不正終了などのときしか呼ばれない
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        cleanBinderFromActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNavigationView, navController);


        // サービス開始
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            checkOverlayPermission();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(Settings.canDrawOverlays(getApplicationContext())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent floatingButtonServiceIntent = new Intent(this, ForegroundService.class);
                        startForegroundService(floatingButtonServiceIntent);
                    } else {
                        startService(new Intent(this, ForegroundService.class));
                    }
                }
            } else {
                startService(new Intent(this, ForegroundService.class));
            }
        }
    }

    public FaceMesh getFacemesh() {
        if (facemeshBinder != null && facemeshBinder.facemesh != null) {
            return facemeshBinder.facemesh;
        } else {
            return null;
        }
    }


    public void checkOverlayPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                // send user to the device settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            }
        }
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
        } else {
//                        Log.d(TAG, "EYES NOT FOUND");
        }

        if (navHostFragment == null) {
            navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        }
        // TODO support service
        assert navHostFragment != null;
        ((FaceMeshResultReceiverInterface) navHostFragment.getChildFragmentManager().getFragments().get(0)).setResult(faceMeshResult, leftEAR, rightEAR, System.nanoTime());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(this, ForegroundService.class);
        bindService(bindIntent, this, Context.BIND_IMPORTANT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cleanBinderFromActivity();
        unbindService(this);
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }
}
