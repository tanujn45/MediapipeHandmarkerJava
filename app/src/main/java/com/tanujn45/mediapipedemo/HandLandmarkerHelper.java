package com.tanujn45.mediapipedemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.Collections;
import java.util.List;

public class HandLandmarkerHelper {
    String MP_HAND_LANDMARKER_TASK = "hand_landmarker.task";
    float DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F;
    float DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F;
    float DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F;
    int DEFAULT_NUM_HANDS = 1;
    Float minHandDetectionConfidence = DEFAULT_HAND_DETECTION_CONFIDENCE;
    Float minHandTrackingConfidence = DEFAULT_HAND_TRACKING_CONFIDENCE;
    Float minHandPresenceConfidence = DEFAULT_HAND_PRESENCE_CONFIDENCE;
    int maxNumHands = DEFAULT_NUM_HANDS;
    BaseOptions baseOptions;
    HandLandmarker.HandLandmarkerOptions options;
    HandLandmarker handLandmarker;
    RunningMode runningMode;
    Context context;
    private LandmarkerListener handLandmarkerHelperListener;

    public HandLandmarkerHelper(Context context, LandmarkerListener handLandmarkerHelperListener) {
        this.context = context;
        this.handLandmarkerHelperListener = handLandmarkerHelperListener;
        this.runningMode = RunningMode.LIVE_STREAM;

        setupHandLandmarker();
    }

    public void clearHandLandmarker() {
        if (handLandmarker != null) {
            handLandmarker.close();
        }
        handLandmarker = null;
    }

    public boolean isClose() {
        return handLandmarker == null;
    }

    public void setupHandLandmarker() {
        if (runningMode == RunningMode.LIVE_STREAM) {
            if (handLandmarkerHelperListener == null) {
                throw new IllegalStateException("handLandmarkerHelperListener must be set when runningMode is LIVE_STREAM.");
            }
        }
        try {
            baseOptions = BaseOptions.builder().setDelegate(Delegate.CPU).setModelAssetPath(MP_HAND_LANDMARKER_TASK).build();

            options = HandLandmarker.HandLandmarkerOptions.builder().setBaseOptions(baseOptions).setMinHandDetectionConfidence(minHandDetectionConfidence).setMinTrackingConfidence(minHandTrackingConfidence).setMinHandPresenceConfidence(minHandPresenceConfidence).setNumHands(maxNumHands).setRunningMode(runningMode).setResultListener(this::returnLivestreamResult).setErrorListener(this::returnLivestreamError).build();

            handLandmarker = HandLandmarker.createFromOptions(context, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void detectLiveStream(ImageProxy imageProxy, boolean isFrontCamera) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            Log.e("HandLandMarker", "Running mode is not LIVE_STREAM");
            return;
        }
        long frameTime = SystemClock.uptimeMillis();
        Bitmap bitmapBuffer;
        if (imageProxy.getPlanes().length > 0) {
            bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            try {
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
            } finally {
                imageProxy.close();
            }
        } else {
            Log.e("HandLandMarker", "Invalid image buffer");
            imageProxy.close();
            return;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate((float) imageProxy.getImageInfo().getRotationDegrees());

        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, (float) imageProxy.getWidth(), (float) imageProxy.getHeight());
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);

        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();
        detectAsync(mpImage, frameTime);
    }

    public void detectAsync(MPImage image, long frameTime) {
        if (handLandmarker == null) {
            Log.e("HandLandMarker", "HandLandmarker is null");
            return;
        }
        handLandmarker.detectAsync(image, frameTime);
    }

    private void returnLivestreamResult(HandLandmarkerResult result, MPImage input) {
        long finishTimeMs = SystemClock.uptimeMillis();
        long inferenceTime = finishTimeMs - result.timestampMs();

        if (handLandmarkerHelperListener != null) {
            handLandmarkerHelperListener.onResults(new ResultBundle(Collections.singletonList(result), inferenceTime, input.getHeight(), input.getWidth()));
        }
    }

    private void returnLivestreamError(RuntimeException error) {
        if (handLandmarkerHelperListener != null) {
            handLandmarkerHelperListener.onError(error.getMessage() != null ? error.getMessage() : "An unknown error has occurred", 0);
        }
    }

    public static class ResultBundle {
        private final List<HandLandmarkerResult> results;
        private final long inferenceTime;
        private final int inputImageHeight;
        private final int inputImageWidth;

        public ResultBundle(List<HandLandmarkerResult> results, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.results = results;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }

        public List<HandLandmarkerResult> getResults() {
            return results;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public int getInputImageHeight() {
            return inputImageHeight;
        }

        public int getInputImageWidth() {
            return inputImageWidth;
        }
    }

    public interface LandmarkerListener {
        void onError(String error, int errorCode);

        void onResults(ResultBundle resultBundle);
    }
}
