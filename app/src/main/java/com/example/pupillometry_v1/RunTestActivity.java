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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RunTestActivity extends AppCompatActivity {

    /*
    Camera global variables
     */
    CameraManager cameraManager;
    CameraDevice cameraDevice;
    TextureView textureView;
    CaptureRequest.Builder captureRequestBuilder;
    Handler cameraHandler;
    HandlerThread cameraHandlerThread;
    MediaRecorder mediaRecorder;

    /*
    Other global variables
     */
    Timer timer;
    int testSecondsCounter = -5;
    String timestamps = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_test);

        // Get permissions to run the camera in case they are not yet granted
        getCameraPermission();

        // TextureView still required even if it can't be seen, the MediaRecorder gets its image from here
        // Also contains the basic setup required to start the camera, including handlers
        textureView = findViewById(R.id.texture_view);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraHandlerThread = new HandlerThread("videoThread");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
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

        // Swap overlay image every x seconds
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> testUI());
            }
        }, 0, 1000);

    }

    @Override
    protected void onDestroy() {
        timestamps += "recording_end=" + System.currentTimeMillis() + "\n";
        mediaRecorder.stop();
        mediaRecorder.reset();
        writeTimestampsFile(timestamps);
        timer.cancel();
        super.onDestroy();
    }

    /*
        Test UI
         */

    private void testUI() {
        Log.d("startTestUI", "Seconds: " + testSecondsCounter);
        // Declare UI elements we will be modifying
        TextView centerText = findViewById(R.id.center_text);
        ImageView blackBackground = findViewById(R.id.black_background);
        ImageView oneTriangleBlack = findViewById(R.id.one_triangle_black);
        ImageView fourTrianglesBlack = findViewById(R.id.four_triangles_black);
        ImageView oneTriangleWhite = findViewById(R.id.one_triangle_white);
        ImageView fourTrianglesWhite = findViewById(R.id.four_triangles_white);

        // Switch statement based off the testSecondsCounter variable, since method is called every
        // every second, the variable denotes the number of seconds since the activity was opened
        if (testSecondsCounter == -5) {
            centerText.setText(R.string.tests_begin_shortly_text);
        }
        if (testSecondsCounter == -2) {
            // Begin recording video
            mediaRecorder = new MediaRecorder();
            setupMediaRecorder();
            startRecording();
            // Record the exact time start of video
            timestamps += "recording_start=" + System.currentTimeMillis() + "\n";
        }

        switch (testSecondsCounter) {

            // Test 1
            case 0:
                centerText.setText("+");
                timestamps += "test1_baseline=" + System.currentTimeMillis() + "\n";
                break;
            case 1:
                oneTriangleBlack.setVisibility(View.VISIBLE);
                timestamps += "test1_perception=" + System.currentTimeMillis() + "\n";
                break;
            case 6:
                oneTriangleBlack.setVisibility(View.GONE);
                blackBackground.setVisibility(View.VISIBLE);
                timestamps += "test1_rest=" + System.currentTimeMillis() + "\n";
                break;
            case 14:
                blackBackground.setVisibility(View.GONE);
                timestamps += "test1_imagery=" + System.currentTimeMillis() + "\n";
                break;
            case 20:
                centerText.setText(R.string.between_test1_and_test2);
                timestamps += "test1_wait=" + System.currentTimeMillis() + "\n";
                break;

            // Test 2
            case 30:
                centerText.setText("+");
                timestamps += "test2_baseline=" + System.currentTimeMillis() + "\n";
                break;
            case 31:
                oneTriangleWhite.setVisibility(View.VISIBLE);
                timestamps += "test2_perception=" + System.currentTimeMillis() + "\n";
                break;
            case 36:
                oneTriangleWhite.setVisibility(View.GONE);
                blackBackground.setVisibility(View.VISIBLE);
                timestamps += "test2_rest=" + System.currentTimeMillis() + "\n";
                break;
            case 44:
                blackBackground.setVisibility(View.GONE);
                timestamps += "test2_imagery=" + System.currentTimeMillis() + "\n";
                break;
            case 50:
                centerText.setText(R.string.between_test2_and_test3);
                timestamps += "test2_wait=" + System.currentTimeMillis() + "\n";
                break;

            // Test 3
            case 60:
                centerText.setText("+");
                timestamps += "test3_baseline=" + System.currentTimeMillis() + "\n";
                break;
            case 61:
                fourTrianglesBlack.setVisibility(View.VISIBLE);
                timestamps += "test3_perception=" + System.currentTimeMillis() + "\n";
                break;
            case 66:
                fourTrianglesBlack.setVisibility(View.GONE);
                blackBackground.setVisibility(View.VISIBLE);
                timestamps += "test3_rest=" + System.currentTimeMillis() + "\n";
                break;
            case 74:
                blackBackground.setVisibility(View.GONE);
                timestamps += "test3_imagery=" + System.currentTimeMillis() + "\n";
                break;
            case 80:
                centerText.setText(R.string.between_test3_and_test4);
                timestamps += "test3_wait=" + System.currentTimeMillis() + "\n";
                break;

            // Test 4
            case 90:
                centerText.setText("+");
                timestamps += "test4_baseline=" + System.currentTimeMillis() + "\n";
                break;
            case 91:
                fourTrianglesWhite.setVisibility(View.VISIBLE);
                timestamps += "test4_perception=" + System.currentTimeMillis() + "\n";
                break;
            case 96:
                fourTrianglesWhite.setVisibility(View.GONE);
                blackBackground.setVisibility(View.VISIBLE);
                timestamps += "test4_rest=" + System.currentTimeMillis() + "\n";
                break;
            case 104:
                blackBackground.setVisibility(View.GONE);
                timestamps += "test4_imagery=" + System.currentTimeMillis() + "\n";
                break;
            case 110:
                centerText.setText(R.string.tests_finished);
                timestamps += "test4_wait=" + System.currentTimeMillis() + "\n";
                break;
            case 120:
                finish();
                break;
        }
        testSecondsCounter++;
    }

    /*
    Alignment functions
     */
    public void writeTimestampsFile(String text) {
        File file = new File(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry", "timestamps.txt");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(text);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Log.d("startTime", "FileNotFoundException thrown when creating start.txt");
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.d("startTime", "IOException thrown when writing text to start.txt");
            throw new RuntimeException(e);
        }
    }

    /*
    Camera workings
     */

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
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d("Camera2", "Attempting to open camera");
                cameraDevice = camera;
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
                    cameraDevice.createCaptureSession(surfaceList, cameraCaptureSessionStateCallback, cameraHandler);
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
            cameraManager.openCamera(cameraIdList[2], cameraDeviceStateCallback, cameraHandler);
        } catch (CameraAccessException e) {
            Log.d("Camera2", "Runtime exception at CameraManager.openCamera()");
            throw new RuntimeException(e);
        }
    }

    private void setupMediaRecorder() {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(640, 480);
        mediaRecorder.setVideoFrameRate(30);
        try {
            Files.createDirectories(Paths.get(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry"));
        } catch (IOException e) {
            Log.d("Files.createDirectories", "IOException thrown at Files.createDirectories()");
            throw new RuntimeException(e);
        }
        mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/video.mp4");
        mediaRecorder.setVideoEncodingBitRate(10000000);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.d("setupMediaRecorder", "IOException when calling MediaRecorder.prepare()");
            throw new RuntimeException(e);
        }
    }

    private void startRecording() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        assert surfaceTexture != null;
        surfaceTexture.setDefaultBufferSize(640, 480);
        Surface previewSurface = new Surface(surfaceTexture);
        Surface recordingSurface = mediaRecorder.getSurface();
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            Log.d("startRecording" ,"CameraAccessException at CameraDevice.createCaptureRequest()");
            throw new RuntimeException(e);
        }
        captureRequestBuilder.addTarget(previewSurface);
        captureRequestBuilder.addTarget(recordingSurface);

        List<Surface> surfaces = new ArrayList<>();
        surfaces.add(previewSurface);
        surfaces.add(recordingSurface);
        CameraCaptureSession.StateCallback captureStateVideoCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                try {
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler);
                    mediaRecorder.start();
                } catch (CameraAccessException e) {
                    Log.d("CameraCaptureSession.StateCallback", "CameraAccessException at onConfigured");
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.d("CameraCaptureSession.StateCallback", "onConfigureFailed");
            }
        };
        try {
            cameraDevice.createCaptureSession(surfaces, captureStateVideoCallback, cameraHandler);
        } catch (CameraAccessException e) {
            Log.d("startRecording", "CameraAccessException at CameraDevice.createCaptureSession()");
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