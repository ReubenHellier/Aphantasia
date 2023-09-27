package com.example.pupillometry_v1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.util.Objects;

public class CleanupDataActivity extends AppCompatActivity {

    TextView infoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cleanup_data);

        infoText = findViewById(R.id.info_text);
        infoText.setText("Beginning data clean up");

        File file = new File(Environment.getExternalStorageDirectory() + "/Documents/Pupillometry");
        new Thread() {
            @Override
            public void run() {
                deleteRecursive(file);
                runOnUiThread(() -> infoText.setText("Finished data clean up"));
            }
        }.start();
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }
}