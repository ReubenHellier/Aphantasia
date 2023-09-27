package com.example.pupillometry_v1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AlignCameraActivity extends AppCompatActivity {

    CameraManager cameraManager;
    TextureView textureView;
    CaptureRequest.Builder captureRequestBuilder;
    Handler handler;
    HandlerThread handlerThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_align_camera);
        getCameraPermission();

        textureView = findViewById(R.id.texture_view);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        };
        textureView.setSurfaceTextureListener(surfaceTextureListener);


    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        // Get a list of cameras available on the device, for Google Pixel 4, cameraIdList[2] corresponds to NIR
        String[] cameraIdList;
        try {
            cameraIdList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d("Camera2", "Camera Access Exception at CameraManager.getCameraIdList()");
            throw new RuntimeException(e);
        }

        // Create the Callback for opening the camera with next
        CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                Log.d("Camera2", "Attempting to open camera");
                try {
                    captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    Surface surface = new Surface(textureView.getSurfaceTexture());
                    captureRequestBuilder.addTarget(surface);
                    List<Surface> surfaceList = new ArrayList<>();
                    surfaceList.add(surface);
                    CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Log.d("Camera2", "CameraAccessException at CameraCaptureSession.setRepeatingRequest()");
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    };
                    cameraDevice.createCaptureSession(surfaceList, cameraCaptureSessionStateCallback, handler);
                } catch (CameraAccessException e) {
                    Log.d("Camera2", "Camera Access Exception at CameraDevice.StateCallback.onOpened()");
                    throw new RuntimeException(e);
                }
                Log.d("Camera2", "Camera opened!");
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {

            }
        };

        // Attempt to open the camera, change the specific camera required here (cameraIdList[2] for NIR)
        try {
            cameraManager.openCamera(cameraIdList[2], cameraDeviceStateCallback, handler);
        } catch (CameraAccessException e) {
            Log.d("Camera2", "Runtime exception at CameraManager.openCamera()");
            throw new RuntimeException(e);
        }
    }

    /*
    Camera Permissions
     */

    private void getCameraPermission() {
        boolean cameraGranted = checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        String[] permissionAsList = {android.Manifest.permission.CAMERA};

        if (!cameraGranted) {
            requestPermissions(permissionAsList, 101);
        } else {
            Log.d("Permissions", "Permission(s) already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                getCameraPermission();
            }
        }
        Log.d("Permissions", "Permission(s) granted");
    }
}