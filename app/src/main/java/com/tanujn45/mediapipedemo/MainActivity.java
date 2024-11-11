package com.tanujn45.mediapipedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements HandLandmarkerHelper.LandmarkerListener {
    HandLandmarkerHelper handLandmarkerHelper;
    ImageAnalysis imageAnalyzer;
    Camera camera;
    ProcessCameraProvider cameraProvider;
    Preview preview;
    int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    ExecutorService backgroundExecutor;
    private String[] PERMISSIONS_REQUIRED = {Manifest.permission.CAMERA};
    PreviewView viewFinder;
    OverlayView overlayView;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            backgroundExecutor.execute(() -> {
                if (handLandmarkerHelper.isClose()) {
                    handLandmarkerHelper.setupHandLandmarker();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handLandmarkerHelper != null) {
            backgroundExecutor.execute(() -> handLandmarkerHelper.clearHandLandmarker());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            backgroundExecutor.shutdownNow();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overlayView = findViewById(R.id.overlay);
        viewFinder = findViewById(R.id.view_finder);


        backgroundExecutor = Executors.newSingleThreadExecutor();

        viewFinder.post(this::setUpCamera);

        backgroundExecutor.execute(() -> {
            handLandmarkerHelper = new HandLandmarkerHelper(this, this);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e("CameraError", "CameraProvider is null");
            return;
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectHand);

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

            if (preview != null) {
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            }

        } catch (Exception e) {
            Log.e("CameraError", "Use case binding failed: " + e.getMessage());
        }
    }


    private void detectHand(ImageProxy imageProxy) {
        boolean isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT;
        handLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera);
    }

    public boolean hasPermissions(Context context) {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onError(String error, int errorCode) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResults(HandLandmarkerHelper.ResultBundle resultBundle) {
        System.out.println("Results: " + resultBundle.getResults().get(0).landmarks());
        runOnUiThread(() -> {
            overlayView.setResults(resultBundle.getResults().get(0), resultBundle.getInputImageHeight(), resultBundle.getInputImageWidth(), RunningMode.LIVE_STREAM);
            overlayView.invalidate();
        });
    }
}

