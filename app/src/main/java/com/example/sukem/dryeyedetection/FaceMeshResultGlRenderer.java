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

import android.opengl.GLES20;
import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.ResultGlRenderer;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/** A custom implementation of {@link ResultGlRenderer} to render {@link FaceMeshResult}. */
public class FaceMeshResultGlRenderer implements ResultGlRenderer<FaceMeshResult> {
    private static final String TAG = "FaceMeshResultGlR...";

    private static final float[] TESSELATION_COLOR = new float[] {0.75f, 0.75f, 0.75f, 0.5f};
    private static final int TESSELATION_THICKNESS = 5;
    private static final float[] RIGHT_EYE_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
    private static final int RIGHT_EYE_THICKNESS = 8;
    private static final float[] RIGHT_EYEBROW_COLOR = new float[] {1f, 0.2f, 0.2f, 1f};
    private static final int RIGHT_EYEBROW_THICKNESS = 8;
    private static final float[] LEFT_EYE_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
    private static final int LEFT_EYE_THICKNESS = 8;
    private static final float[] LEFT_EYEBROW_COLOR = new float[] {0.2f, 1f, 0.2f, 1f};
    private static final int LEFT_EYEBROW_THICKNESS = 8;
    private static final float[] FACE_OVAL_COLOR = new float[] {0.9f, 0.9f, 0.9f, 1f};
    private static final int FACE_OVAL_THICKNESS = 8;
    private static final float[] LIPS_COLOR = new float[] {0.9f, 0.9f, 0.9f, 1f};
    private static final int LIPS_THICKNESS = 8;
    private static final String VERTEX_SHADER =
            "uniform mat4 uProjectionMatrix;\n"
                    + "attribute vec4 vPosition;\n"
                    + "void main() {\n"
                    + "  gl_Position = uProjectionMatrix * vPosition;\n"
                    + "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform vec4 uColor;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = uColor;\n"
                    + "}";
    private int program;
    private int positionHandle;
    private int projectionMatrixHandle;
    private int colorHandle;

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void setupRendering() {
        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
    }

    @Override
    public void renderResult(FaceMeshResult result, float[] projectionMatrix) {
        if (result == null) {
            return;
        }
        GLES20.glUseProgram(program);
//        for (int i = 0; i < 4; i++) {
//            Log.d(TAG, String.format("%d: \t%.2f,\t%.2f,\t%.2f,\t%.2f", i, projectionMatrix[i*4], projectionMatrix[i*4+1], projectionMatrix[i*4+2], projectionMatrix[i*4+3]));
//        }
//        float[] m = {
//                1f,0f,0f,0f,
//                0f,1f,0f,0f,
//                0f,0f,1f,0f,
//                0f,0f,0f,1f};
//        float[] m = {
//                2f,     0f,     0f,     0f,
//                0f,     -3.23f, 0f,     0f,
//                0f,     0f,     -1f,    0f,
//                -1f,    1.61f,  0f,     1f};
//        Log.d(TAG, String.valueOf(projectionMatrix[5] / projectionMatrix[0]));
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);

        int numFaces = result.multiFaceLandmarks().size();
        for (int i = 0; i < numFaces; ++i) {
            // 顔全体
//            drawLandmarks(
//                    result.multiFaceLandmarks().get(i).getLandmarkList(),
//                    FaceMeshConnections.FACEMESH_TESSELATION,
//                    TESSELATION_COLOR,
//                    TESSELATION_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(i).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_RIGHT_EYE,
                    RIGHT_EYE_COLOR,
                    RIGHT_EYE_THICKNESS);
//            drawLandmarks(
//                    result.multiFaceLandmarks().get(i).getLandmarkList(),
//                    FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
//                    RIGHT_EYEBROW_COLOR,
//                    RIGHT_EYEBROW_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(i).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_LEFT_EYE,
                    LEFT_EYE_COLOR,
                    LEFT_EYE_THICKNESS);
//            drawLandmarks(
//                    result.multiFaceLandmarks().get(i).getLandmarkList(),
//                    FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
//                    LEFT_EYEBROW_COLOR,
//                    LEFT_EYEBROW_THICKNESS);
//            drawLandmarks(
//                    result.multiFaceLandmarks().get(i).getLandmarkList(),
//                    FaceMeshConnections.FACEMESH_FACE_OVAL,
//                    FACE_OVAL_COLOR,
//                    FACE_OVAL_THICKNESS);
//            drawLandmarks(
//                    result.multiFaceLandmarks().get(i).getLandmarkList(),
//                    FaceMeshConnections.FACEMESH_LIPS,
//                    LIPS_COLOR,
//                    LIPS_THICKNESS);

            // 目の四角
//            if (result.multiFaceLandmarks().get(i).getLandmarkCount()
//                    == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES) {
//                drawLandmarks(
//                        result.multiFaceLandmarks().get(i).getLandmarkList(),
//                        FaceMeshConnections.FACEMESH_RIGHT_IRIS,
//                        RIGHT_EYE_COLOR,
//                        RIGHT_EYE_THICKNESS);
//                drawLandmarks(
//                        result.multiFaceLandmarks().get(i).getLandmarkList(),
//                        FaceMeshConnections.FACEMESH_LEFT_IRIS,
//                        LEFT_EYE_COLOR,
//                        LEFT_EYE_THICKNESS);
//            }
        }
    }

    /**
     * Deletes the shader program.
     *
     * <p>This is only necessary if one wants to release the program while keeping the context around.
     */
    public void release() {
        GLES20.glDeleteProgram(program);
    }

    private void drawLandmarks(
            List<NormalizedLandmark> faceLandmarkList,
            ImmutableSet<FaceMeshConnections.Connection> connections,
            float[] colorArray,
            int thickness) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
        GLES20.glLineWidth(thickness);
        int count = 0;
        float[] ptsX = new float[16*2];
        float[] ptsY = new float[16*2];
        for (FaceMeshConnections.Connection c : connections) {
            NormalizedLandmark start = faceLandmarkList.get(c.start());
            NormalizedLandmark end = faceLandmarkList.get(c.end());
            ptsX[count] = start.getX();
            ptsX[count+16] = end.getX();
            ptsY[count] = start.getY();
            ptsY[count+16] = end.getY();
            float[] vertex = {start.getX(), start.getY(), end.getX(), end.getY()};
            FloatBuffer vertexBuffer =
                    ByteBuffer.allocateDirect(vertex.length * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                            .put(vertex);
            vertexBuffer.position(0);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
            count++;
        }
//        Log.d(TAG, String.valueOf(count));
        if (connections.equals(FaceMeshConnections.FACEMESH_LEFT_EYE)) {
//            Log.d(TAG, String.format("%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f",
//                    ptsX[0], ptsX[1], ptsX[2], ptsX[3], ptsX[4], ptsX[5], ptsX[6], ptsX[7],
//                    ptsX[8], ptsX[9], ptsX[10], ptsX[11], ptsX[12], ptsX[13], ptsX[14], ptsX[15]));
//            Log.d(TAG, String.format("%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f",
//                    ptsX[0+16], ptsX[1+16], ptsX[2+16], ptsX[3+16], ptsX[4+16], ptsX[5+16], ptsX[6+16], ptsX[7+16],
//                    ptsX[8+16], ptsX[9+16], ptsX[10+16], ptsX[11+16], ptsX[12+16], ptsX[13+16], ptsX[14+16], ptsX[15+16]));
//            Log.d(TAG, String.format("%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f",
//                    ptsY[0], ptsY[1], ptsY[2], ptsY[3], ptsY[4], ptsY[5], ptsY[6], ptsY[7],
//                    ptsY[8], ptsY[9], ptsY[10], ptsY[11], ptsY[12], ptsY[13], ptsY[14], ptsY[15]));
//            Log.d(TAG, String.format("%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f",
//                    ptsY[0+16], ptsY[1+16], ptsY[2+16], ptsY[3+16], ptsY[4+16], ptsY[5+16], ptsY[6+16], ptsY[7+16],
//                    ptsY[8+16], ptsY[9+16], ptsY[10+16], ptsY[11+16], ptsY[12+16], ptsY[13+16], ptsY[14+16], ptsY[15+16]));
        }
    }
}
