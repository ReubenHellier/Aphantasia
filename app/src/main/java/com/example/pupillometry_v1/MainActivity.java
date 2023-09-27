package com.example.pupillometry_v1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static {
       System.loadLibrary("pupillometry_v1");
       System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Loaded Successfully");
        } else {
            Log.d("OpenCV", "Failed to Load");
        }
    }

    public void launchTutorial(View view) {
        Intent intent = new Intent(this, TutorialActivity.class);
        startActivity(intent);
    }

    public void launchAlignCamera(View view) {
        Intent intent = new Intent(this, AlignCameraActivity.class);
        startActivity(intent);
    }

    public void launchRunTest(View view) {
        Intent intent = new Intent(this, RunTestActivity.class);
        startActivity(intent);
    }

    public void launchProcessData(View view) {
        Intent intent = new Intent(this, ProcessDataActivity.class);
        startActivity(intent);
    }

    public void launchCleanupData(View view) {
        Intent intent = new Intent(this, CleanupDataActivity.class);
        startActivity(intent);
    }
}