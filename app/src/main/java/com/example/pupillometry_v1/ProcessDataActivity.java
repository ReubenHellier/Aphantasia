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
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

        // Loop through every frame in the video
        int frameNumber = 0;
        for (long time = 0; time < videoDuration; time += frameInterval) {
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
//            String resultPath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/Frames/" + String.format(Locale.ENGLISH, "%06d", frameNumber) + "_res.jpeg";
//            String cppRes = processImageWithSave(mat.getNativeObjAddr(), resultPath);

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

    private void processDataDeprecated() {
        Log.d("frameExtraction", "Beginning frame extraction");
        String videoFilePath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/video.mp4";

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoFilePath);

        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assert duration != null;
        long videoDuration = Long.parseLong(duration);
        long frameInterval = 33;

        String csvFilePath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/data.csv";
        List<String[]> data = new ArrayList<>();
        data.add(new String[] {"frame", "center_x", "center_y", "width", "height", "angle",
                "confidence_value", "confidence_aspect_ratio", "confidence_angular_spread",
                "confidence_outline_contrast"});

        int frameNumber = 0;
        for (long time = 0; time < videoDuration; time += frameInterval) {
            Bitmap frame = retriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            int currFrameNumber = frameNumber;

            // Keep the user updated
            String frameCountText = "Frame: " + currFrameNumber;
            runOnUiThread(() -> frameCount.setText(frameCountText));

            String frameNumberFormatted = String.format(Locale.ENGLISH, "%06d", frameNumber);
            File file = new File(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/Frames", frameNumberFormatted + ".jpeg");

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                assert frame != null;
                frame.compress(Bitmap.CompressFormat.JPEG, 75, fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e) {
                Log.d("FileOutputStream", "IOException thrown");
                e.printStackTrace();
            }

            Imgcodecs imgcodecs = new Imgcodecs();
            String imagePath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/Frames/" + frameNumberFormatted + ".jpeg";
            Mat mat = imgcodecs.imread(imagePath);

            String resultPath = Environment.getExternalStorageDirectory() + "/Documents/Pupillometry/Frames/" + frameNumberFormatted + "_res.jpeg";
            String cppRes = processImageWithSave(mat.getNativeObjAddr(), resultPath);
            String res = frameNumberFormatted + "#" + cppRes;

            String[] row = res.split("#");
            data.add(row);

            frameNumber++;
        }

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

    public native String processImageWithSave(long image, String resultPath);
    public native String processImage(long image);
}