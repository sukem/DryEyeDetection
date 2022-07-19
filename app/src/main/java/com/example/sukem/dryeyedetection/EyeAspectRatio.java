package com.example.sukem.dryeyedetection;

import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

public class EyeAspectRatio {
    private static final String TAG = "EyeAspectRatio";

    public static final float DEFAULT_EAR_THRESHOLD = 0.18f;
    public static final float EAR_HYSTERESIS_VALUE = 0.02f;
    public static final float MAX_EAR_VALUE = 0.5f;
    public static final long DATA_DURATION_SEC = 40;
    public static final long TOTAL_DETECTED_THRESHOLD = 10;
    public static final long LAST_DETECTED_THRESHOLD = 5;

    public static float earThreshold = DEFAULT_EAR_THRESHOLD;
    public static int blinkPerMinThreshold = 30;
    public static float blinkRateThreshold = 0.03f;
    private static float xyRatio = 0;


    static class EARData {
        public float left;
        public float right;
        public boolean leftBlinked;
        public boolean rightBlinked;
        public long time;
        public boolean detected = false;
        EARData(float left, float right) {
            this.left = left;
            this.right = right;
            this.time = System.nanoTime();
            if (left != 0 && right != 0) {
                this.detected = true;
            }
        }

        public void setBlinkState(EARData previous) {
            if (previous.detected) {
                this.leftBlinked = (!previous.leftBlinked || !(this.left > (earThreshold + EAR_HYSTERESIS_VALUE)))
                        && (previous.leftBlinked || !(this.left > (earThreshold - EAR_HYSTERESIS_VALUE)));
                this.rightBlinked = (!previous.rightBlinked || !(this.right > (earThreshold + EAR_HYSTERESIS_VALUE)))
                        && (previous.rightBlinked || !(this.right > (earThreshold - EAR_HYSTERESIS_VALUE)));
            } else {
                this.leftBlinked = this.left > earThreshold;
                this.rightBlinked = this.right > earThreshold;
            }
        }
    }

    public enum EARDataState {
        LACK_OF_DATA,
        LACK_OF_DETECTED_DATA,
        NO_EYES_DETECTED,
        FINE
    }
    public FaceMeshResult currentResult;
    private EARDataState dataState;
    private final ArrayDeque<EARData> data;
    private float leftBlinkRate = 0;
    private float leftBlinkPerMin = 0;
    private float rightBlinkRate = 0;
    private float rightBlinkPerMin = 0;
    public EARDataState getDataState() {
        return dataState;
    }
    public EARData getCurrent() {
        return data.getLast();
    }
    public float getLeftBlinkRate() {
        return leftBlinkRate;
    }
    public float getLeftBlinkPerMin() {
        return leftBlinkPerMin;
    }
    public float getRightBlinkRate() {
        return rightBlinkRate;
    }
    public float getRightBlinkPerMin() {
        return rightBlinkPerMin;
    }

    EyeAspectRatio() {
        data = new ArrayDeque<>(3000);
        dataState = EARDataState.LACK_OF_DATA;
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

    private float calcEyesAspectRatio(ImmutableSet<FaceMeshConnections.Connection> connections)
    {
        // input frame のサイズを取得
        if (EyeAspectRatio.xyRatio == 0) {
            TextureFrame texture = currentResult.acquireInputTextureFrame();
            EyeAspectRatio.xyRatio = (float) texture.getWidth() / texture.getHeight();
        }

        List<LandmarkProto.NormalizedLandmark> faceLandmarkList = currentResult.multiFaceLandmarks().get(0).getLandmarkList();
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
        this.currentResult = result;

        float leftEAR = 0;
        float rightEAR = 0;
        if (!result.multiFaceLandmarks().isEmpty()) {
            // 左右注意
            leftEAR = calcEyesAspectRatio(FaceMeshConnections.FACEMESH_RIGHT_EYE);
            rightEAR = calcEyesAspectRatio(FaceMeshConnections.FACEMESH_LEFT_EYE);
//            Log.d(TAG, "FPS = " + String.valueOf(1000000000f / (System.nanoTime() - lastUpdate)));
        }
        EARData newData = new EARData(leftEAR, rightEAR);
        if (newData.detected && data.size() > 0) {
            newData.setBlinkState(data.getLast());
        }
        data.add(newData);

        // 古いデータを削除
        while (newData.time - data.getFirst().time > DATA_DURATION_SEC * 1000000000L) {
            data.removeFirst();
        }
    }

    public void calcBlinkRate() {
        int leftBlink = 0;
        float leftBlinkTime = 0;
        float leftOpenTime = 0;
        int rightBlink = 0;
        float rightBlinkTime = 0;
        float rightOpenTime = 0;
        int count = 0;
        Iterator<EARData> earDataIterator = data.descendingIterator();
        EARData lastEarData = earDataIterator.next();
        long latestTime = lastEarData.time;
        boolean noEyesDetected = true;
        if (latestTime - data.getFirst().time < 1000000000L * TOTAL_DETECTED_THRESHOLD) {
            dataState = EARDataState.LACK_OF_DATA;
            return;
        }
        while (earDataIterator.hasNext()) {
            EARData earData = earDataIterator.next();
            if (earData.detected && lastEarData.detected) {
                if (earData.leftBlinked) {
                    leftBlinkTime += (lastEarData.time - earData.time) / 1000000000f;
                    if (!lastEarData.leftBlinked) {
                        leftBlink++;
                    }
                } else {
                    leftOpenTime += (lastEarData.time - earData.time) / 1000000000f;
                }
                if (earData.rightBlinked) {
                    rightBlinkTime += (lastEarData.time - earData.time) / 1000000000f;
                    if (!lastEarData.rightBlinked) {
                        rightBlink++;
                    }
                } else {
                    rightOpenTime += (lastEarData.time - earData.time) / 1000000000f;
                }
                noEyesDetected = false;
                count++;
            } else {
                if (noEyesDetected && latestTime - earData.time > 1000000000L * LAST_DETECTED_THRESHOLD) {
                    dataState = EARDataState.NO_EYES_DETECTED;
                    return;
                }
            }
            lastEarData = earData;
        }
        float totalDetectedTime = leftBlinkTime + leftOpenTime;
        if (totalDetectedTime > TOTAL_DETECTED_THRESHOLD) {
            dataState = EARDataState.FINE;
        } else {
            dataState = EARDataState.LACK_OF_DETECTED_DATA;
        }
        if (totalDetectedTime > 0) {
            leftBlinkRate = leftBlinkTime / totalDetectedTime;
            rightBlinkRate = rightBlinkTime / totalDetectedTime;
        } else {
            leftBlinkRate = 0;
            rightBlinkRate = 0;
        }
        leftBlinkPerMin = leftBlink / totalDetectedTime * 60f;
        rightBlinkPerMin = rightBlink / totalDetectedTime * 60f;
//        Log.d(TAG, String.format("BLINK = %d,\t RATE = %.2f", leftBlink, leftBlinkRate));
    }
}
