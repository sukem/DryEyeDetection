package com.example.sukem.dryeyedetection;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements FaceMeshResultReceiverInterface {
    private static final String TAG = "HomeFragment";
    private ImageView leftEye;
    private ImageView rightEye;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String param1, String param2) {
        return new HomeFragment();
    }

    @Override
    public void setResult(FaceMeshResult faceMeshResult, float leftEAR, float rightEAR, long currentTime) {
        float threshold = 0.2f;
        if (leftEAR > threshold) {
            leftEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
        } else {
            leftEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
        }
        if (rightEAR > threshold) {
            rightEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
        } else {
            rightEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        leftEye = view.findViewById(R.id.leftEye);
        rightEye = view.findViewById(R.id.rightEye);

        Button buttonStart = view.findViewById(R.id.button1);
        Button buttonStop = view.findViewById(R.id.button2);
        buttonStart.setOnClickListener(v -> {
            checkOverlayPermission();
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                Log.d(TAG, "START button pushed");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(Settings.canDrawOverlays(getContext())) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent floatingButtonServiceIntent = new Intent(mainActivity, ForegroundService.class);
                            mainActivity.startForegroundService(floatingButtonServiceIntent);
                        } else {
                            mainActivity.startService(new Intent(mainActivity, ForegroundService.class));
                        }
                    }
                } else {
                    mainActivity.startService(new Intent(mainActivity, ForegroundService.class));
                }
            } else {
                Toast.makeText(getContext(), "NO activity", Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    public void checkOverlayPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getContext())) {
                // send user to the device settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            }
        }
    }
}