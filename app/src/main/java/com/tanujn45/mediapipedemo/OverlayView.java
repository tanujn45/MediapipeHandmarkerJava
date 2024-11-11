package com.tanujn45.mediapipedemo;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

public class OverlayView extends View {

    private HandLandmarkerResult results;
    private Paint linePaint;
    private Paint pointPaint;

    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public void clear() {
        results = null;
        linePaint.reset();
        pointPaint.reset();
        invalidate();
        initPaints();
    }

    private void initPaints() {
        linePaint = new Paint();
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results != null) {
            List<List<NormalizedLandmark>> landmarks;
            landmarks = results.landmarks();
            for (List<NormalizedLandmark> landmark : landmarks) {
                for (NormalizedLandmark normalizedLandmark : landmark) {
                    canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                    );
                }

                for (Connection connection : HandLandmarker.HAND_CONNECTIONS) {
                    int startIdx = connection.start();
                    int endIdx = connection.end();
                    canvas.drawLine(
                            landmark.get(startIdx).x() * imageWidth * scaleFactor,
                            landmark.get(startIdx).y() * imageHeight * scaleFactor,
                            landmark.get(endIdx).x() * imageWidth * scaleFactor,
                            landmark.get(endIdx).y() * imageHeight * scaleFactor,
                            linePaint
                    );
                }
            }
        }
    }

    public void setResults(HandLandmarkerResult handLandmarkerResults, int imageHeight, int imageWidth, RunningMode runningMode) {
        this.results = handLandmarkerResults;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;

        scaleFactor = calculateScaleFactor(runningMode);
        invalidate();
    }

    private float calculateScaleFactor(RunningMode runningMode) {
        switch (runningMode) {
            case IMAGE:
            case VIDEO:
                return min(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
            case LIVE_STREAM:
                return max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
            default:
                return 1f;
        }
    }

    private static final float LANDMARK_STROKE_WIDTH = 8F;
}
