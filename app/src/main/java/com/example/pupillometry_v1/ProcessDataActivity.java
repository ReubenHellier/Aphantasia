package com.example.pupillometry_v1;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVWriter;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ProcessDataActivity extends AppCompatActivity {

    ImageView imageView;
    TextView frameCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_data);
        imageView = findViewById(R.id.image_view);
        frameCount = findViewById(R.id.frame_count_text_view);


        /*
        Create directory if not already present
         */
        File directory = new File(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/Frames");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        /*
        Run the frame extraction in a new thread
         */
        new Thread(this::processData).start();


    }

    private void processData() {
        Log.d("frameExtraction", "Beginning frame extraction");
        String videoFilePath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/video.mp4";

        // Initialise the MetadataRetriever and other variables
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoFilePath);

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long videoDuration = Long.parseLong(duration);
        int frameInterval = 33;

        // Initialise objects to contain data for the csv
        String csvFilePath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/data.csv";
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"frame", "center_x", "center_y", "width", "height", "angle",
                "confidence_value", "confidence_aspect_ratio", "confidence_angular_spread",
                "confidence_outline_contrast"});

        // Get the time stamps
        File file = new File(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/timestamps.txt");
        byte[] timeStampsBytes = new byte[(int) file.length()];
        try {
            FileInputStream stream = new FileInputStream(file);
            stream.read(timeStampsBytes);
        } catch (Exception e) {
            Log.d("TimeStampsRead", "Exception thrown at FileInputStream");
            e.printStackTrace();
        }
        String[] timeStampsList = new String(timeStampsBytes).split("\n");

        // Put the time stamps into a hashmap for easier access
        HashMap<String, Long> timeStamps = new HashMap<>();
        for (String timeStamp : timeStampsList) {
            String[] keyValue = timeStamp.split("=");
            timeStamps.put(keyValue[0], Long.valueOf(keyValue[1]));
        }

        // Get video offset from clock time
        Long offset = timeStamps.get("recording_start");

        // Loop through every frame in the video
        int frameNumber = 0;
        for (long time = 0; time < videoDuration; time += frameInterval) {

//            if (time > timeStamps.get("test1_imagery")-offset && time < timeStamps.get("test1_wait")-offset) {
//                // In test 1
//                if (frameNumber < 0) {
//                    frameNumber = 0;
//                }
//
//            } else if (time > timeStamps.get("test2_imagery")-offset && time < timeStamps.get("test2_wait")-offset) {
//                // In test 2
//                if (frameNumber < 100000) {
//                    frameNumber = 100000;
//                }
//
//            } else if (time > timeStamps.get("test3_imagery")-offset && time < timeStamps.get("test3_wait")-offset) {
//                // In test 3
//                if (frameNumber < 200000) {
//                    frameNumber = 200000;
//                }
//
//            } else if (time > timeStamps.get("test4_imagery")-offset && time < timeStamps.get("test4_wait")-offset) {
//                // In test 4
//                if (frameNumber < 300000) {
//                    frameNumber = 300000;
//                }
//
//            } else {
//                // Not in a test
//                continue;
//            }

            // Retrieve a bitmap of the frame
            Bitmap bmp = retriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST);

            // Convert this to an openCV matrix
            Mat mat = new Mat();
            Utils.bitmapToMat(bmp, mat);

            // Keep the user updated
            int currFrameNumber = frameNumber;
            String frameCountText = "Frame: " + currFrameNumber;
            runOnUiThread(() -> frameCount.setText(frameCountText));

            // Call to PURE algorithm
            String cppRes = processImage(mat.getNativeObjAddr());

            // Add data to array to later save as CSV
            String res = String.format(Locale.ENGLISH, "%06d", frameNumber) + "#" + cppRes;
            String[] row = res.split("#");
            data.add(row);

            frameNumber++;
        }

        // Save the data to a csv
        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilePath));
            csvWriter.writeAll(data);
            csvWriter.close();
        } catch (IOException e) {
            Log.d("CSVWriter", "IOException thrown at new FileWriter()");
            e.printStackTrace();
        }

        runOnUiThread(() -> frameCount.setText(R.string.completed_processing));
        Log.d("frameExtraction", "Frame extraction completed");
    }
    public native String processImage(long image);
}