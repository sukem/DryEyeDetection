package com.example.sukem.dryeyedetection;

import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.ArrayDeque;
import java.util.List;

public class EyeAspectRatio {
    private static final String TAG = "EyeAspectRatio";

    public static final float DEFAULT_EAR_VALUE = 0.2f;
    public static final float MAX_EAR_VALUE = 0.5f;
    public static float earThreshold = DEFAULT_EAR_VALUE;
    private static float xyRatio = 0;

    static class EARData {
        public float left;
        public float right;
        public long time;
        EARData(float left, float right) {
            this.left = left;
            this.right = right;
            this.time = System.nanoTime();
        }
    }

    private final ArrayDeque<EARData> data;
    EyeAspectRatio() {
        data = new ArrayDeque<>(3000);
    }

    public EARData getCurrent() {
        return data.getLast();
    }

    private static float getDistance(float[] vec1, float[] vec2)
    {
        float x = vec1[0] - vec2[0];
        float y = vec1[1] - vec2[1];
        return (float) Math.sqrt(x*x + y*y);
    }

    private static float dotProduct(float[] vec1, float[] vec2)
    {
        return vec1[0]*vec2[0] + vec1[1]*vec2[1];
    }

    private static float calcEyesAspectRatio(
            FaceMeshResult faceMeshResult,
            ImmutableSet<FaceMeshConnections.Connection> connections)
    {
        // input frame のサイズを取得
        if (EyeAspectRatio.xyRatio == 0) {
            TextureFrame texture = faceMeshResult.acquireInputTextureFrame();
            EyeAspectRatio.xyRatio = (float) texture.getWidth() / texture.getHeight();
        }

        List<LandmarkProto.NormalizedLandmark> faceLandmarkList = faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList();
        List<FaceMeshConnections.Connection> list = connections.asList();
        LandmarkProto.NormalizedLandmark p1 = faceLandmarkList.get(list.get(0).start());
        LandmarkProto.NormalizedLandmark p2 = faceLandmarkList.get(list.get(3).start());
        LandmarkProto.NormalizedLandmark p3 = faceLandmarkList.get(list.get(4).end());
        LandmarkProto.NormalizedLandmark p4 = faceLandmarkList.get(list.get(7).end());
        LandmarkProto.NormalizedLandmark p5 = faceLandmarkList.get(list.get(12).end());
        LandmarkProto.NormalizedLandmark p6 = faceLandmarkList.get(list.get(11).start());

        float[] horizontalVec = {(p4.getX() - p1.getX()) * xyRatio, p4.getY() - p1.getY()};
        float[] orig = {0f, 0f};
        float hvd = getDistance(horizontalVec, orig);
        float[] verticalUnit = {horizontalVec[1] / hvd, -horizontalVec[0] / hvd};
        float[] verticalVec1 = {(p2.getX() - p6.getX()) * xyRatio, p2.getY() - p6.getY()};
        float[] verticalVec2 = {(p3.getX() - p5.getX()) * xyRatio, p3.getY() - p5.getY()};
        return Math.abs(dotProduct(verticalVec1, verticalUnit) + dotProduct(verticalVec2, verticalUnit)) / (2 * hvd);
    }

    public void updateEARs(FaceMeshResult result) {
        float leftEAR = 0f;
        float rightEAR = 0f;
        if (!result.multiFaceLandmarks().isEmpty()) {
            // 左右注意
            leftEAR = EyeAspectRatio.calcEyesAspectRatio(result,
                    FaceMeshConnections.FACEMESH_RIGHT_EYE);
            rightEAR = EyeAspectRatio.calcEyesAspectRatio(result,
                    FaceMeshConnections.FACEMESH_LEFT_EYE);
//                        Log.d(TAG, "FPS = " + String.valueOf(1000000000f / (System.nanoTime() - lastUpdate)));
        } else {
//                        Log.d(TAG, "EYES NOT FOUND");
        }
        data.add(new EARData(leftEAR, rightEAR));
    }
}
