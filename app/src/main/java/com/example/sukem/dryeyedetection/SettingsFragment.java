package com.example.sukem.dryeyedetection;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment implements FaceMeshResultReceiverInterface{

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;
    private FaceMeshResultGlRenderer faceMeshResultGlRenderer;

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

        FaceMesh facemesh = ((MainActivity) getActivity()).facemesh;
        glSurfaceView = new SolutionGlSurfaceView<>(getContext(), facemesh.getGlContext(), facemesh.getGlMajorVersion());
        faceMeshResultGlRenderer = new FaceMeshResultGlRenderer();
        glSurfaceView.setSolutionResultRenderer(faceMeshResultGlRenderer);
        glSurfaceView.setRenderInputImage(true);

        FrameLayout frameLayout = view.findViewById(R.id.preview_display_layout);
        frameLayout.removeAllViewsInLayout();
        frameLayout.addView(glSurfaceView);
        glSurfaceView.setVisibility(View.VISIBLE);
        frameLayout.requestLayout();

        // メッシュ表示の切り替えボタン
        SwitchCompat eyesSwitch = view.findViewById(R.id.eyes_mesh_switch);
        eyesSwitch.setChecked(FaceMeshResultGlRenderer.renderEyesMesh);
        eyesSwitch.setOnCheckedChangeListener((compoundButton, b) -> FaceMeshResultGlRenderer.renderEyesMesh = b);
        SwitchCompat faceSwitch = view.findViewById(R.id.face_mesh_switch);
        faceSwitch.setChecked(FaceMeshResultGlRenderer.renderEyesMesh);
        faceSwitch.setOnCheckedChangeListener((compoundButton, b) -> FaceMeshResultGlRenderer.renderFaceMesh = b);

        return view;
    }

    public void changeMeshRenderState(CompoundButton compoundButton, boolean b) {
        if (faceMeshResultGlRenderer == null) {
            return;
        }
    }
}