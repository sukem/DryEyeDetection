package com.example.sukem.dryeyedetection;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

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
        if (leftEye != null) {
            if (leftEAR > EyeAspectRatioUtils.earThreshold) {
                leftEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
            } else {
                leftEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
            }
        }
        if (rightEye != null) {
            if (rightEAR > EyeAspectRatioUtils.earThreshold) {
                rightEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
            } else {
                rightEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
            }
        } else {
            Log.d(TAG, "NOOOOOOO");
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
        return view;
    }
}