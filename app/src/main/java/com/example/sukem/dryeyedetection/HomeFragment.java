package com.example.sukem.dryeyedetection;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

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
    private TextView blinkPerMinText;
    private TextView blinkRateText;

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
    public void setResult(EyeAspectRatio ear) {
        if (leftEye != null) {
            if (!ear.getCurrent().detected) {
                leftEye.setImageResource(R.drawable.ic_eye_no_svgrepo_com);
            } else if (ear.getCurrent().leftBlinked) {
                leftEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
            } else {
                leftEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
            }
        }
        if (rightEye != null) {
            if (!ear.getCurrent().detected) {
                rightEye.setImageResource(R.drawable.ic_eye_no_svgrepo_com);
            } else if (ear.getCurrent().rightBlinked) {
                rightEye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
            } else {
                rightEye.setImageResource(R.drawable.ic_eye_svgrepo_com);
            }
        }
        blinkPerMinText.post(() -> {
            setBlinkTexts(ear);
        });
    }

    private void setBlinkTexts(EyeAspectRatio ear) {
        switch (ear.getDataState()) {
            case FINE:
                if (blinkPerMinText != null) {
                    blinkPerMinText.setText(getString(R.string.blink_min_text_value, (int)ear.getLeftBlinkPerMin(), (int)ear.getRightBlinkPerMin()));
                }
                if (blinkRateText != null) {
                    blinkRateText.setText(getString(R.string.blink_rate_text_value, (int)(ear.getLeftBlinkRate() * 100), (int)(ear.getRightBlinkRate() * 100)));
                }
                break;
            case LACK_OF_DATA:
            case LACK_OF_DETECTED_DATA:
                if (blinkPerMinText != null) {
                    blinkPerMinText.setText(R.string.blink_text_measuring);
                }
                if (blinkRateText != null) {
                    blinkRateText.setText(R.string.blink_text_measuring);
                }
                break;
            case NO_EYES_DETECTED:
                if (blinkPerMinText != null) {
                    blinkPerMinText.setText(R.string.blink_text_not_detected);
                }
                if (blinkRateText != null) {
                    blinkRateText.setText(R.string.blink_text_not_detected);
                }
                break;
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

        SwitchCompat switchCompat = view.findViewById(R.id.floatingViewSwitch);
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.enableFloatingVeiw(ForegroundService.haveFloatingView);
        }
        switchCompat.setChecked(ForegroundService.haveFloatingView);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.enableFloatingVeiw(b);
                }
            }
        });

        blinkPerMinText = view.findViewById(R.id.blinkPerMinText);
        blinkRateText = view.findViewById(R.id.blinkRateText);

        return view;
    }
}