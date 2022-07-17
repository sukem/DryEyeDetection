package com.example.sukem.dryeyedetection;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SelfcheckFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SelfcheckFragment extends Fragment implements FaceMeshResultReceiverInterface {

    private static final int MAX_PROGRESS_VALUE = 256;
    private static final int COUNTDWON_INTERVAL = 100;
    private static final long DRYEYE_THRESHOLD = 12000;

    private ProgressBar progressBar;
    private TextView progressText;
    private ImageView eye;
    private CountDownTimer countDownTimer;
    private Button startCheckButton;
    private TextView resultText;

    public SelfcheckFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SelfcheckFragment.
     */
    public static SelfcheckFragment newInstance(String param1, String param2) {
        return new SelfcheckFragment();
    }

    @Override
    public void setResult(FaceMeshResult faceMeshResult, EyeAspectRatio.EARData current) {
        if (eye != null) {
            if ((current.left + current.right) / 2 > EyeAspectRatio.earThreshold) {
                eye.setImageResource(R.drawable.ic_eye_svgrepo_com);
            } else {
                if (current.detected) {
                    eye.setImageResource(R.drawable.ic_eye_closed_svgrepo_com);
                } else {
                    eye.setImageResource(R.drawable.ic_eye_no_svgrepo_com);
                }
                if (countDownTimer != null) {
                    // セルフチェック失敗
                    countDownTimer.cancel();
                    countDownTimer = null;
                    startCheckButton.post(() -> {
                        startCheckButton.setEnabled(true);
                        resultText.setText("Failure");
                    });
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_selfcheck, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        progressText = view.findViewById(R.id.progressText);
        eye = view.findViewById(R.id.checkEye);
        progressBar.setMax(MAX_PROGRESS_VALUE);
        startCheckButton = view.findViewById(R.id.startCheckButton);
        startCheckButton.setOnClickListener(view1 -> {
            countDownTimer = new CountDownTimer(DRYEYE_THRESHOLD, COUNTDWON_INTERVAL) {
                @Override
                public void onTick(long l) {
                    setProgressState(l);
                }

                @Override
                public void onFinish() {
                    // セルフチェック成功
                    setProgressState(0);
                    countDownTimer = null;
                    startCheckButton.setEnabled(true);
                    resultText.setText("OK");
                }
            };
            countDownTimer.start();
            startCheckButton.setEnabled(false);
        });
        setProgressState(DRYEYE_THRESHOLD);
        resultText = view.findViewById(R.id.resultText);

        return view;
    }

    private void setProgressState(long l) {
        progressText.setText(String.format("%2.1f s", l / 1000f));
        progressBar.setProgress((int) ((float) MAX_PROGRESS_VALUE * l / DRYEYE_THRESHOLD));
    }
}