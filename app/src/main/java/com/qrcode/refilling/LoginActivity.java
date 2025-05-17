package com.qrcode.refilling;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    private EditText txtUserName;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtUserName = findViewById(R.id.txtUserName);
        txtPassword = findViewById(R.id.txtPassword);

        txtUserName.setFocusable(true);
        txtUserName.requestFocus();

        txtUserName.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String finalText = s.toString();

                if (!finalText.isEmpty()) {

                    String[] credentials = finalText.split(";");
                    if (credentials.length >= 2) {
                        String username = credentials[0];  // Get the username
                        String password = credentials[1];  // Get the password

                        txtUserName.setText(username);
                        txtPassword.setText(password);
                    }
                }
            }
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            if (txtUserName.getText().toString().isEmpty()) {
                Toast.makeText(this, "please enter username.", Toast.LENGTH_SHORT).show();
            } else if (txtPassword.getText().toString().isEmpty()) {
                Toast.makeText(this, "please enter password.", Toast.LENGTH_SHORT).show();
            } else {
                login(txtUserName.getText().toString(), txtPassword.getText().toString());
            }
        });


        if (!checkPermission()) {
            List<String> permissionsNeeded = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Request manage external storage permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        if (checkPermission() &&
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R ||
                        Environment.isExternalStorageManager())) {
            // All permissions are granted
            new Thread(this::checkLoggerFolder).start();
        } else {
            Toast.makeText(getApplication(),"You didn't provided all the permissions", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean checkPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public void checkLoggerFolder(){

        File loggerFile = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

        if(!loggerFile.exists()){
            boolean isLoggerCreated = loggerFile.mkdir();
        }else{
            File exlFile2 = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

            File loginExcel = new File(loggerFile, "/login.xls");
            if(!loginExcel.exists()){
                copyAssets();
            }
        }
    }

    private void copyAssets() {

        AssetManager assetManager = getApplication().getAssets();
        String[] files = null;
        try {
            files = assetManager.list(Constants.FolderName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fPath = Utils.getMainFilePath(getApplicationContext());

        if (files != null) {
            for (String filename : files) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(Constants.FolderName + "/"+filename);
                    File outFile = new File(fPath+"/" + Constants.FolderName + "/", filename);
                    out = Files.newOutputStream(outFile.toPath());
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            }
        }else{
            Log.e(getClass().getName(), "LOGIN No ASSETS FILES");
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void login(String userName, String password) {
        try {
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/login.xls" ;

            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new HSSFWorkbook(fs);

            Sheet sh = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sh.iterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                String currentUser = row.getCell(0).getStringCellValue();
                Cell passwordCell = row.getCell(1);
                String currentPass = "";
                if (passwordCell != null) {
                    switch (passwordCell.getCellType()) {
                        case STRING:
                            currentPass = passwordCell.getStringCellValue();
                            break;
                        case NUMERIC:
                            currentPass = String.valueOf((int) passwordCell.getNumericCellValue()); // Convert number to String
                            break;
                        case BOOLEAN:
                            currentPass = String.valueOf(passwordCell.getBooleanCellValue());
                            break;
                        case BLANK:
                            currentPass = ""; // Handle blank cells
                            break;
                        default:
                            currentPass = "Unknown Type"; // Handle unexpected cases
                    }
                }

                if(currentUser.equals(userName) && currentPass.equals(password)){

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("UserName", userName);
                    startActivity(intent);
                    break;
                }
            }

            if (!rowIterator.hasNext()) {
                Toast.makeText(this, "Username and password didn't match.", Toast.LENGTH_SHORT).show();
            }

        }catch(Exception exp){
            Log.e("LoginScreen", "Error reading file", exp);
            Toast.makeText(this, "move the files in " + Utils.getMainFilePath(getApplicationContext())+"/logger: " + exp.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}