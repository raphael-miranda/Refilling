package com.qrcode.refilling;

import static com.qrcode.refilling.MainActivity.FILE_COUNTER;
import static com.qrcode.refilling.MainActivity.FILE_DATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewDataActivity extends AppCompatActivity {

    private TextView txtData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_data);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtData = findViewById(R.id.txtData);
        String text = readTextFile();
        txtData.setText(text);
    }

    private String readTextFile() {
        StringBuilder text = new StringBuilder();

        String fileName = getFileName();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = Utils.getDocumentsDirectory(this);
            File file = new File(dir, fileName);
            if (!file.exists()) {
                return "File not found!";
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line).append("\n");
                }
                return text.toString();
            } catch (IOException e) {
                Log.e("ReadFile", "Error reading file", e);
                return "Error reading file";
            }
        } else {
            text.append("External storage not available.");
        }
        return text.toString();
    }


    private String getFileName() {
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
        String strDate = format.format(new Date());

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String fileDate = sharedPreferences.getString(FILE_DATE, "");
        int fileCounter = sharedPreferences.getInt(FILE_COUNTER, 1);

        if (fileDate.isEmpty() || !strDate.equals(fileDate)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FILE_DATE, strDate);
            editor.putInt(FILE_COUNTER, 1);
            editor.apply();
        }

        String fileName = String.format(Locale.getDefault(), "refillScan%s-%02d.txt", strDate, fileCounter);

        return fileName;
    }
}