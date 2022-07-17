package com.example.sukem.dryeyedetection;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment implements FaceMeshResultReceiverInterface{

    private static final String TAG = "SettingsFragment";
    private static final int MAX_SEEK_VALUE = 50;

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;
    private LineChart lineChart;

    private long startTime = 0;
    private long currentTime = 0;
    private final Object lockObj = new Object();
    private float leftEAR;
    private float rightEAR;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void setResult(FaceMeshResult faceMeshResult, float leftEAR, float rightEAR, long currentTime) {
        if (glSurfaceView != null) {
            glSurfaceView.setRenderData(faceMeshResult);
            glSurfaceView.requestRender();
        }

        synchronized (lockObj) {
            this.leftEAR = leftEAR;
            this.rightEAR = rightEAR;
            this.currentTime = currentTime;
        }
        if (lineChart != null) {
            lineChart.post(this::updateChart);
        }
    }

    private void updateChart() {
        if (lineChart != null) {
            final float width = 5.0001f;
            if (lineChart.getData() != null && lineChart.getData().getDataSetCount() > 0) {
                // データ追加
                ILineDataSet dataSetL = lineChart.getData().getDataSetByIndex(0);
                ILineDataSet dataSetR = lineChart.getData().getDataSetByIndex(1);
                ILineDataSet dataSetT = lineChart.getData().getDataSetByIndex(2);
                float x = 0f;
                synchronized (lockObj) {
                    x = (currentTime - startTime) / 1000000000f;
                    dataSetL.addEntry(new Entry(x, leftEAR));
                    dataSetR.addEntry(new Entry(x, rightEAR));
                }
                Entry entry0 =  dataSetT.getEntryForIndex(0);
                Entry entry1 =  dataSetT.getEntryForIndex(1);
                entry0.setY(EyeAspectRatioUtils.earThreshold);
                entry1.setX(Math.max(x, width));
                entry1.setY(EyeAspectRatioUtils.earThreshold);

                lineChart.getData().notifyDataChanged();
                lineChart.notifyDataSetChanged();
                lineChart.invalidate();
                lineChart.setVisibleXRange(0, width);
                lineChart.moveViewToX(x);
            } else {
                // 初期化
                LinkedList<Entry> valuesL = new LinkedList<>();
                LinkedList<Entry> valuesR = new LinkedList<>();
                synchronized (lockObj) {
                    startTime = currentTime;
                    valuesL.add(new Entry(0, leftEAR));
                    valuesR.add(new Entry(0, rightEAR));
                }
                // グラフ設定 (EAR)
                LineDataSet dataSetL = new LineDataSet(valuesL, "Left EAR");
                LineDataSet dataSetR = new LineDataSet(valuesR, "Right EAR");
                dataSetL.setDrawIcons(false);
                dataSetL.setColor(Color.RED);
                dataSetL.setCircleColor(Color.RED);
                dataSetL.setDrawValues(false);
                dataSetR.setDrawIcons(false);
                dataSetR.setColor(Color.GREEN);
                dataSetR.setCircleColor(Color.GREEN);
                dataSetR.setDrawValues(false);

                // グラフ設定 (threshold)
                ArrayList<Entry> valuesT = new ArrayList<>();
                valuesT.add(new Entry(0, EyeAspectRatioUtils.earThreshold));
                valuesT.add(new Entry(width, EyeAspectRatioUtils.earThreshold));
                LineDataSet dataSetThreshold = new LineDataSet(valuesT, "Threshold");
                dataSetThreshold.setDrawIcons(false);
                dataSetThreshold.setColor(Color.BLUE);
                dataSetThreshold.setDrawCircles(false);
                dataSetThreshold.setDrawValues(false);

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSetL);
                dataSets.add(dataSetR);
                dataSets.add(dataSetThreshold);
                LineData lineData = new LineData(dataSets);
                lineChart.setData(lineData);
                lineChart.setTouchEnabled(false);
                lineChart.getDescription().setEnabled(false);
                lineChart.setDrawGridBackground(true);
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setGranularity(1);
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Facemesh 描画設定
        MainActivity mainActivity = (MainActivity) getActivity();
        FaceMesh facemesh = null;
        if (mainActivity != null) {
            facemesh = mainActivity.getFacemesh();
        }
        if (facemesh != null) {
            glSurfaceView = new SolutionGlSurfaceView<>(getContext(), facemesh.getGlContext(), facemesh.getGlMajorVersion());
            glSurfaceView.setSolutionResultRenderer(new FaceMeshResultGlRenderer());
            glSurfaceView.setRenderInputImage(true);

            FrameLayout frameLayout = view.findViewById(R.id.preview_display_layout);
            frameLayout.removeAllViewsInLayout();
            frameLayout.addView(glSurfaceView);
            glSurfaceView.setVisibility(View.VISIBLE);
            frameLayout.requestLayout();
        }


        // メッシュ表示の切り替えボタン
        SwitchCompat eyesSwitch = view.findViewById(R.id.eyes_mesh_switch);
        eyesSwitch.setChecked(FaceMeshResultGlRenderer.renderEyesMesh);
        eyesSwitch.setOnCheckedChangeListener((compoundButton, b) -> FaceMeshResultGlRenderer.renderEyesMesh = b);
        SwitchCompat faceSwitch = view.findViewById(R.id.face_mesh_switch);
        faceSwitch.setChecked(FaceMeshResultGlRenderer.renderEyesMesh);
        faceSwitch.setOnCheckedChangeListener((compoundButton, b) -> FaceMeshResultGlRenderer.renderFaceMesh = b);

        // グラフ表示
        lineChart = view.findViewById(R.id.line_chart);

        // EAR閾値変更シークバー設定
        SeekBar thresholdSeekBar = view.findViewById(R.id.thresholdSeekBar);
        TextView thresholdTextView = view.findViewById(R.id.thresholdTextView);
        thresholdSeekBar.setProgress((int) (MAX_SEEK_VALUE * EyeAspectRatioUtils.DEFAULT_EAR_VALUE / EyeAspectRatioUtils.MAX_EAR_VALUE));
        thresholdSeekBar.setMax(MAX_SEEK_VALUE);
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        EyeAspectRatioUtils.earThreshold = (float)i / MAX_SEEK_VALUE * EyeAspectRatioUtils.MAX_EAR_VALUE;
                        thresholdTextView.setText("value = " + String.valueOf(EyeAspectRatioUtils.earThreshold));


                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                }
        );

        return view;
    }

//    private String getThresholdText(int barValue) {
////        return
//    }
}