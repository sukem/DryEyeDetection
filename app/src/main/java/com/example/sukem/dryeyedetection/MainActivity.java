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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.solutions.facemesh.FaceMesh;

import java.util.List;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "MainActivity";
    private NavHostFragment navHostFragment;
    private FacemeshBinder facemeshBinder;


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected");
//        Toast.makeText(getApplicationContext(), "サービスに接続しました", Toast.LENGTH_SHORT).show();
        facemeshBinder = (FacemeshBinder) iBinder;
        facemeshBinder.listenerInActivity = this::faceMeshResultReceive;
    }

    private void cleanBinderFromActivity() {
//        Toast.makeText(getApplicationContext(), "サービスから切断しました", Toast.LENGTH_SHORT).show();
        if (facemeshBinder != null) {
            facemeshBinder.listenerInActivity = null;
            facemeshBinder = null;
        }
    }

    public void enableFloatingVeiw(boolean b) {
        if (facemeshBinder != null && facemeshBinder.service != null) {
            if (b) {
                facemeshBinder.service.setupFloatingView();
            } else {
                facemeshBinder.service.removeFloatingView();
            }
            ForegroundService.haveFloatingView = b;
        }
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
            startForegroundService();
        } else {
            PermissionHelper.checkAndRequestCameraPermissions(this);
        }
    }

    private void startForegroundService() {
        Intent floatingButtonServiceIntent = new Intent(this, ForegroundService.class);
        startForegroundService(floatingButtonServiceIntent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (grantResults.length <= 0) {
            return;
        }
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startForegroundService();
            } else {
                Toast.makeText(this, "No Camera permission", Toast.LENGTH_LONG).show();
                finish();
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

    void faceMeshResultReceive(EyeAspectRatio ear) {
        // DO NOT CALL FROM SERVICE
        if (navHostFragment == null) {
            navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        }
        assert navHostFragment != null;
        List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
        if (fragments.size() > 0) {
            ((FaceMeshResultReceiverInterface) fragments.get(0)).setResult(ear);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}
