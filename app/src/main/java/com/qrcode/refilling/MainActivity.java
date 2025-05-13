package com.qrcode.refilling;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity{

    public static final String FILE_DATE = "file_date";
    public static final String FILE_COUNTER = "file_counter";

    private static final String SERVER_TYPE = "server_type";
    private static final String HOST_ADDRESS = "ftp_host";
    private static final String FTP_PORT = "ftp_portNumber";
    private static final String SHARED_FOLDER = "shared_folder";
    private static final String FTP_USERNAME = "ftp_username";
    private static final String FTP_PASSWORD = "ftp_password";
    private static final String IS_MANUAL = "is_manual";
    private static final String SCANNED_NUMBER = "scanned_number";

    private String mUserName = "";

    private TextView txtScannedNumber;
    private TextView txtCurrentDate, txtCurrentTime;

    private TextInputLayout txtCtNrField1, txtPartNrField1, txtQuantityField1;
    private TextInputLayout txtCtNrField2, txtPartNrField2, txtQuantityField2;
    private TextInputLayout txtCtNrField3, txtPartNrField3, txtQuantityField3;

    private TextInputEditText txtCtNr1, txtPartNr1, txtDNr1, txtQuantity1;
    private TextInputEditText txtCtNr2, txtPartNr2, txtDNr2, txtQuantity2;
    private TextInputEditText txtCtNr3, txtPartNr3, txtDNr3, txtQuantity3;

    private RecyclerView listSmallLabels, listBigLabels;
    private AppCompatButton btnPlus1, btnPlus2;
    private AppCompatButton btnUpload;

    LabelsAdapter smallListAdapter = new LabelsAdapter(new ArrayList<>());
    LabelsAdapter bigListAdapter = new LabelsAdapter(new ArrayList<>());

    private ActivityResultLauncher<String> storagePermissionLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;

    private ColorStateList greenColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.GREEN,
                    Color.GREEN,
                    Color.GREEN
            }
    );

    private ColorStateList redColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.RED,
                    Color.RED,
                    Color.RED
            }
    );

    private final Handler clockHandler = new Handler();
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("MM.dd.yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            String currentDate = sdfDate.format(new Date());
            String currentTime = sdfTime.format(new Date());
            txtCurrentDate.setText(currentDate);
            txtCurrentTime.setText(currentTime);
            clockHandler.postDelayed(this, 30000); // update every 1 second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mUserName = getIntent().getStringExtra("UserName");
        TextView txtUserName = findViewById(R.id.txtUserName);
        txtUserName.setText(mUserName);

        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        clockHandler.post(updateTime);

        TextView txtVersion = findViewById(R.id.txtVersion);
        String version = "Unknown";
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        txtVersion.setText(String.format(Locale.getDefault(), "Version : %s", version));

        txtScannedNumber = findViewById(R.id.txtScannedNumber);

        // Carton Label
        txtCtNrField1 = findViewById(R.id.txtCtNrField1);
        txtPartNrField1 = findViewById(R.id.txtPartNrField1);
        txtQuantityField1 = findViewById(R.id.txtQuantityField1);
        txtCtNr1 = findViewById(R.id.txtCtNr1);
        txtPartNr1 = findViewById(R.id.txtPartNr1);
        txtDNr1 = findViewById(R.id.txtDNr1);
        txtQuantity1 = findViewById(R.id.txtQuantity1);

        // Minus Label
        txtCtNrField2 = findViewById(R.id.txtCtNrField2);
        txtPartNrField2 = findViewById(R.id.txtPartNrField2);
        txtQuantityField2 = findViewById(R.id.txtQuantityField2);
        txtCtNr2 = findViewById(R.id.txtCtNr2);
        txtPartNr2 = findViewById(R.id.txtPartNr2);
        txtDNr2 = findViewById(R.id.txtDNr2);
        txtQuantity2 = findViewById(R.id.txtQuantity2);

        // Good Label
        txtCtNrField3 = findViewById(R.id.txtCtNrField3);
        txtPartNrField3 = findViewById(R.id.txtPartNrField3);
        txtQuantityField3 = findViewById(R.id.txtQuantityField3);
        txtCtNr3 = findViewById(R.id.txtCtNr3);
        txtPartNr3 = findViewById(R.id.txtPartNr3);
        txtDNr3 = findViewById(R.id.txtDNr3);
        txtQuantity3 = findViewById(R.id.txtQuantity3);

        listSmallLabels = findViewById(R.id.smallLabelListView);
        listBigLabels = findViewById(R.id.bigLabelListView);
        btnPlus1 = findViewById(R.id.btnPlus1);
        btnPlus2 = findViewById(R.id.btnPlus2);

        smallListAdapter = new LabelsAdapter(new ArrayList<>());
        bigListAdapter = new LabelsAdapter(new ArrayList<>());

        listSmallLabels.setLayoutManager(new LinearLayoutManager(this));
        listSmallLabels.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listSmallLabels.setAdapter(smallListAdapter);

        listBigLabels.setLayoutManager(new LinearLayoutManager(this));
        listBigLabels.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listBigLabels.setAdapter(bigListAdapter);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int scannedNumber = sharedPreferences.getInt(SCANNED_NUMBER, 0);
        txtScannedNumber.setText(String.valueOf(scannedNumber));

        AppCompatButton btnViewData = findViewById(R.id.btnViewData);
        btnUpload = findViewById(R.id.btnUpload);
        checkUploadAvailable();

        AppCompatButton btnNew = findViewById(R.id.btnNew);
        AppCompatButton btnReset = findViewById(R.id.btnReset);

        btnPlus1.setOnClickListener(view -> {
            showAddLabelDialog(true);
        });

        btnPlus2.setOnClickListener(view -> {
            showAddLabelDialog(false);
        });

        btnViewData.setOnClickListener(view -> {
            Intent intent = new Intent(this, ViewDataActivity.class);
            startActivity(intent);
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });

        btnNew.setOnClickListener(view -> onNew());

        btnReset.setOnClickListener(view -> reset());

        findViewById(R.id.btnSettings).setOnClickListener(view -> showSettingsDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            finish();
        });

        checkManual();
        initLeftScan();
        initRightScan();

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Storage Permission Allowed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                });

        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "Storage Permission Allowed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Manage Storage Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        checkPermissions();

        // prevent going to login screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            }

//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (!android.os.Environment.isExternalStorageManager()) {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
//                manageStorageLauncher.launch(intent);
//            }
        } else {
            Dexter.withContext(getApplicationContext())
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    private void showSettingsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView)
                .setCancelable(true);
        AlertDialog dialog = builder.create();

        MaterialButtonToggleGroup toggleServerType = dialogView.findViewById(R.id.toggleServerType);
        MaterialButton btnFtpServer = dialogView.findViewById(R.id.btnFtpServer);
        MaterialButton btnSMBServer = dialogView.findViewById(R.id.btnSMBServer);

        TextInputLayout fieldHostAddress = dialogView.findViewById(R.id.fieldHostAddress);
        TextInputLayout fieldPort = dialogView.findViewById(R.id.fieldPort);
        TextInputLayout fieldUsername = dialogView.findViewById(R.id.fieldUserName);
        TextInputLayout fieldPassword = dialogView.findViewById(R.id.fieldPassword);

        TextInputEditText txtHost = dialogView.findViewById(R.id.txtHostAddress);
        TextInputEditText txtPortNumber = dialogView.findViewById(R.id.txtPortNumber);
        TextInputEditText txtUserName = dialogView.findViewById(R.id.txtUserName);
        TextInputEditText txtPassword = dialogView.findViewById(R.id.txtPassword);
        CheckBox checkboxManual = dialogView.findViewById(R.id.checkboxManual);

        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnTestConnection = dialogView.findViewById(R.id.btnTestConnection);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if ("ftp".equals(sharedPreferences.getString(SERVER_TYPE, "ftp"))) {
            toggleServerType.check(R.id.btnFtpServer);
            fieldHostAddress.setHint(getString(R.string.host_address));
            fieldPort.setHint(getString(R.string.port));
            txtPortNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
            fieldUsername.setHint(getString(R.string.username));
            fieldPassword.setHint(getString(R.string.password));

            txtHost.setText(sharedPreferences.getString(HOST_ADDRESS, ""));
            txtPortNumber.setText(sharedPreferences.getString(FTP_PORT, ""));
            txtUserName.setText(sharedPreferences.getString(FTP_USERNAME, ""));
            txtPassword.setText(sharedPreferences.getString(FTP_PASSWORD, ""));
        } else if ("smb".equals(sharedPreferences.getString(SERVER_TYPE, "ftp"))) {
            toggleServerType.check(R.id.btnSMBServer);
            fieldHostAddress.setHint(getString(R.string.smb_address));
            fieldPort.setHint(getString(R.string.shared_folder));
            txtPortNumber.setInputType(InputType.TYPE_CLASS_TEXT);
            fieldUsername.setHint(getString(R.string.username));
            fieldPassword.setHint(getString(R.string.password));

            txtHost.setText(sharedPreferences.getString(HOST_ADDRESS, ""));
            txtPortNumber.setText(sharedPreferences.getString(SHARED_FOLDER, ""));
            txtUserName.setText(sharedPreferences.getString(FTP_USERNAME, ""));
            txtPassword.setText(sharedPreferences.getString(FTP_PASSWORD, ""));
        }

        checkboxManual.setChecked(sharedPreferences.getBoolean(IS_MANUAL, false));

        toggleServerType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFtpServer) {
                    fieldHostAddress.setHint(getString(R.string.host_address));
                    fieldPort.setHint(getString(R.string.port));
                    txtPortNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                    fieldUsername.setHint(getString(R.string.username));
                    fieldPassword.setHint(getString(R.string.password));

                    if ("smb".equals(sharedPreferences.getString(SERVER_TYPE, "ftp"))) {
                        txtHost.setText("");
                        txtPortNumber.setText("");
                        txtUserName.setText("");
                        txtPassword.setText("");
                    } else {
                        txtHost.setText(sharedPreferences.getString(HOST_ADDRESS, ""));
                        txtPortNumber.setText(sharedPreferences.getString(FTP_PORT, ""));
                        txtUserName.setText(sharedPreferences.getString(FTP_USERNAME, ""));
                        txtPassword.setText(sharedPreferences.getString(FTP_PASSWORD, ""));
                    }
                } else if (checkedId == R.id.btnSMBServer) {
                    fieldHostAddress.setHint(getString(R.string.smb_address));
                    fieldPort.setHint(getString(R.string.shared_folder));
                    txtPortNumber.setInputType(InputType.TYPE_CLASS_TEXT);
                    fieldUsername.setHint(getString(R.string.username));
                    fieldPassword.setHint(getString(R.string.password));

                    if ("ftp".equals(sharedPreferences.getString(SERVER_TYPE, "ftp"))) {
                        txtHost.setText("");
                        txtPortNumber.setText("");
                        txtUserName.setText("");
                        txtPassword.setText("");
                    } else {
                        txtHost.setText(sharedPreferences.getString(HOST_ADDRESS, ""));
                        txtPortNumber.setText(sharedPreferences.getString(SHARED_FOLDER, ""));
                        txtUserName.setText(sharedPreferences.getString(FTP_USERNAME, ""));
                        txtPassword.setText(sharedPreferences.getString(FTP_PASSWORD, ""));
                    }
                }
            }
        });

        btnSave.setOnClickListener(view -> {

            String hostAddress = txtHost.getText().toString();
            String portNumber = txtPortNumber.getText().toString();
            String username = txtUserName.getText().toString();
            String password = txtPassword.getText().toString();
            boolean isManual = checkboxManual.isChecked();

            SharedPreferences sharedPreferences1 = getSharedPreferences(getPackageName(), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences1.edit();
            if (toggleServerType.getCheckedButtonId() == R.id.btnFtpServer) {
                editor.putString(SERVER_TYPE, "ftp");
                editor.putString(FTP_PORT, portNumber);
            } else if (toggleServerType.getCheckedButtonId() == R.id.btnSMBServer) {
                editor.putString(SERVER_TYPE, "smb");
                editor.putString(SHARED_FOLDER, portNumber);
            }
            editor.putString(HOST_ADDRESS, hostAddress);
            editor.putString(FTP_USERNAME, username);
            editor.putString(FTP_PASSWORD, password);
            editor.putBoolean(IS_MANUAL, isManual);
            editor.apply();

            checkManual();

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnTestConnection.setOnClickListener(view -> {
            String hostAddress = txtHost.getText().toString();
            String portNumber = txtPortNumber.getText().toString();
            String username = txtUserName.getText().toString();
            String password = txtPassword.getText().toString();

            if (toggleServerType.getCheckedButtonId() == R.id.btnFtpServer) {
                if (!portNumber.isEmpty()) {
                    int port = Integer.parseInt(portNumber);
                    testFtpConnection(hostAddress, port, username, password);
                }
            } else if (toggleServerType.getCheckedButtonId() == R.id.btnSMBServer) {
                testSmbConnection(hostAddress, portNumber, username, password);
            }
        });

        dialog.show();
    }

    private void checkManual() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);
        if (isManual) {
            txtCtNr2.setShowSoftInputOnFocus(true);
            txtCtNr2.setEnabled(true);
            txtPartNr2.setEnabled(true);
            txtDNr2.setEnabled(true);
            txtQuantity2.setEnabled(true);

            txtCtNr3.setEnabled(true);
            txtPartNr3.setEnabled(true);
            txtDNr3.setEnabled(true);
            txtQuantity3.setEnabled(true);
        } else {
            txtCtNr2.setShowSoftInputOnFocus(false);
            txtCtNr2.setEnabled(true);
            txtPartNr2.setEnabled(false);
            txtDNr2.setEnabled(false);
            txtQuantity2.setEnabled(false);

            txtCtNr3.setShowSoftInputOnFocus(false);
            txtCtNr3.setEnabled(true);
            txtPartNr3.setEnabled(false);
            txtDNr3.setEnabled(false);
            txtQuantity3.setEnabled(false);
        }
    }

    private void checkUploadAvailable() {
        String fileName = getFileName();

        File dir = Utils.getDocumentsDirectory(this);
        File file = new File(dir, fileName);

        if (file.exists()) {
            btnUpload.setEnabled(true);
        } else {
            btnUpload.setEnabled(false);
        }
    }

    private void initLeftScan() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);

        txtCtNr2.requestFocus();
        if (!isManual) {
            txtCtNr2.setShowSoftInputOnFocus(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        txtCtNr2.post(() -> txtCtNr2.setSelection(txtCtNr2.getText().length()));

        txtCtNr2.setOnKeyListener((view, keyCode, event) -> {

            if (keyCode==KeyEvent.KEYCODE_ENTER)
            {
                // Just ignore the [Enter] key
                return true;
            }
            // Handle all other keys in the default way
            return (keyCode == KeyEvent.KEYCODE_ENTER);
        });
        txtCtNr2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String strCtNr = txtCtNr2.getText().toString();
                int count = strCtNr.split(";").length;

                if(count == 4) {
                    txtPartNr2.setText(strCtNr.split(";")[1]);
                    txtDNr2.setText(strCtNr.split(";")[2]);
                    txtQuantity2.setText(strCtNr.split(";")[3]);
                    txtCtNr2.setText(strCtNr.split(";")[0]);
                    txtCtNr3.requestFocus();
                }

                if (!txtCtNr2.getText().toString().isEmpty()) {
                    isCartonExisting();
                    compare();
                } else {
                    txtCtNrField2.setErrorEnabled(false);
                }
            }
        });

        txtPartNr2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!(txtPartNr3.getText().toString().isEmpty())) {
                    compare();
                }
            }
        });

        txtQuantity2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!(txtQuantity3.getText().toString().isEmpty())) {
                    compare();
                    txtCtNr3.requestFocus();
                }
            }
        });
    }

    private void initRightScan() {
        txtCtNr3.setFocusable(true);

        txtCtNr3.post(() -> txtCtNr3.setSelection(txtCtNr3.getText().length()));

        txtCtNr3.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {

                if (keyCode==KeyEvent.KEYCODE_ENTER)
                {
                    // Just ignore the [Enter] key
                    return true;
                }
                // Handle all other keys in the default way
                return (keyCode == KeyEvent.KEYCODE_ENTER);
            }
        });
        txtCtNr3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String strCName = txtCtNr3.getText().toString();
                int count = strCName.split(";").length;

                if(count == 4) {
                    txtPartNr3.setText(strCName.split(";")[1]);
                    txtDNr3.setText(strCName.split(";")[2]);
                    txtQuantity3.setText(strCName.split(";")[3]);
                    txtCtNr3.setText(strCName.split(";")[0]);
                }

                if (!txtCtNr3.getText().toString().isEmpty()) {
                    compare();
                }
            }
        });

        txtPartNr3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!(txtPartNr3.getText().toString().isEmpty())) {
                    compare();
                }
            }
        });

        txtQuantity3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!(txtQuantity3.getText().toString().isEmpty())) {
                    compare();
                }
            }
        });
    }

    private boolean isCartonExisting() {
        if (txtCtNr2.getText().toString().isEmpty()) {
            txtCtNrField2.setErrorEnabled(false);
            txtCtNrField3.setErrorEnabled(false);
            return false;
        }

        String strCtNr = String.format(Locale.getDefault(), "; %-12s ;", txtCtNr2.getText().toString());

        String fileName = getFileName();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = Utils.getDocumentsDirectory(this);
            File file = new File(dir, fileName);
            if (!file.exists()) {
                txtCtNrField2.setErrorEnabled(false);
                txtCtNrField3.setErrorEnabled(false);
                return false;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(strCtNr)) {
                        txtCtNrField2.setError("DOUBLE CT-Nr");
                        btnPlus1.setEnabled(false);
                        return true;
                    } else {
                        txtCtNrField2.setErrorEnabled(false);
                    }

                    txtCtNrField3.setErrorEnabled(false);

                }
                return false;
            } catch (IOException e) {
                Log.e("ReadFile", "Error reading file", e);
                return false;
            }
        }
        return false;
    }

    private boolean isDialogCartonExisting(String cartonNr, boolean isLeft) {
        String oldCartonNr = "";
        if (isLeft) {
            oldCartonNr = txtCtNr2.getText().toString();
        } else {
            oldCartonNr = txtCtNr3.getText().toString();
        }

        if (cartonNr.equals(oldCartonNr)) {
            return true;
        }

        String strCartonNr = String.format(Locale.getDefault(), "; %-12s ;", cartonNr);

        String fileName = getFileName();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = Utils.getDocumentsDirectory(this);
            File file = new File(dir, fileName);
            if (!file.exists()) {
                return false;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(strCartonNr) && !strCartonNr.isEmpty()) {
                        return true;
                    }
                }
                return false;
            } catch (IOException e) {
                Log.e("ReadFile", "Error reading file", e);
                return false;
            }
        }
        return false;
    }

    private void compare() {
        String strPartNr1 = txtPartNr2.getText().toString();
        String strPartNr2 = txtPartNr3.getText().toString();
        String strQtty1 = txtQuantity2.getText().toString();
        String strQtty2 = txtQuantity3.getText().toString();

        int quantity1 = 0, quantity2 = 0;
        if (!strQtty1.isEmpty()) {
            quantity1 = Integer.parseInt(strQtty1);
        }
        if (!strQtty2.isEmpty()) {
            quantity2 = Integer.parseInt(strQtty2);
        }

        int result = 0;
        if (!strPartNr1.isEmpty() && strPartNr1.equals(strPartNr2)) {
            result += 1;
            txtPartNr2.setBackgroundTintList(greenColors);
            txtPartNr3.setBackgroundTintList(greenColors);

            if (smallListAdapter.getItemCount() > 0) {
                StringBuilder quantityHelperString = new StringBuilder();
                List<HashMap<String, String>> arrSmallLabels = smallListAdapter.getItems();
                for (HashMap<String, String> smallLabelData: arrSmallLabels) {
                    String partNr = smallLabelData.getOrDefault(Utils.PART_NR, "");
                    if (strPartNr1.equals(partNr)) {
                        String strQtty = smallLabelData.getOrDefault(Utils.QUANTITY, "0");
                        int quantity = Integer.parseInt(strQtty);
                        quantity1 += quantity;
                        quantityHelperString.append(strQtty).append(" + ");
                    }
                }
                if (quantityHelperString.toString().isEmpty()) {
                    txtQuantityField2.setHelperText("");
                } else {
                    quantityHelperString.append(strQtty1.isEmpty() ? "0" : strQtty1);
                    quantityHelperString.append(" = ").append(quantity1);
                    txtQuantityField2.setHelperText(quantityHelperString.toString());
                }
            } else {
                txtQuantityField2.setHelperText("");
            }

            if (bigListAdapter.getItemCount() > 0) {
                StringBuilder quantityHelperString = new StringBuilder();
                List<HashMap<String, String>> arrBigLabels = bigListAdapter.getItems();
                for (HashMap<String, String> bigLabelData: arrBigLabels) {
                    String partNr = bigLabelData.getOrDefault(Utils.PART_NR, "");
                    if (strPartNr2.equals(partNr)) {
                        String strQtty = bigLabelData.getOrDefault(Utils.QUANTITY, "0");
                        int quantity = Integer.parseInt(strQtty);
                        quantity2 += quantity;
                        quantityHelperString.append(strQtty).append(" + ");
                    }
                }
                if (quantityHelperString.toString().isEmpty()) {
                    txtQuantityField3.setHelperText("");
                } else {
                    quantityHelperString.append(strQtty1.isEmpty() ? "0" : strQtty1);
                    quantityHelperString.append(" = ").append(quantity2);
                    txtQuantityField3.setHelperText(quantityHelperString.toString());
                }


            } else {
                txtQuantityField3.setHelperText("");
            }
        } else {
            txtPartNr2.setBackgroundTintList(redColors);
            txtPartNr3.setBackgroundTintList(redColors);
        }

        if (quantity1 != 0 && (quantity1 == quantity2)) {
            result += 1;
            txtQuantity2.setBackgroundTintList(greenColors);
            txtQuantity3.setBackgroundTintList(greenColors);
        } else {
            txtQuantity2.setBackgroundTintList(redColors);
            txtQuantity3.setBackgroundTintList(redColors);
        }

        if (result == 2) {
            if (!isCartonExisting()) {
                saveAndNext();
            }

            btnPlus1.setEnabled(false);
            btnPlus2.setEnabled(false);
        } else {

            if (!txtCtNr2.getText().toString().isEmpty()) {
                btnPlus1.setEnabled(true);
            }
            if (!txtCtNr3.getText().toString().isEmpty()) {
                btnPlus2.setEnabled(true);
            }
        }
    }

    // For add label dialog
    TextInputLayout dlgCartonNumberField;
    TextInputLayout dlgPartNrField;
    TextInputLayout dlgQuantityField;

    TextInputEditText txtDialogCartonNumber;
    TextInputEditText txtDialogPartNr;
    TextInputEditText txtDialogDNr;
    TextInputEditText txtDialogQuantity;
    TextInputEditText txtDialogOrderNr;
    MaterialButton btnDialogAdd;

    private void showAddLabelDialog(boolean isLeft) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_label, null);
        builder.setView(dialogView)
                .setCancelable(true);
        AlertDialog dialog = builder.create();

        TextView txtDlgTitle = dialogView.findViewById(R.id.txtDlgTitle);

        dlgCartonNumberField = dialogView.findViewById(R.id.txtCartonNumberField);
        dlgPartNrField = dialogView.findViewById(R.id.txtPartNrField);
        TextInputLayout dlgDNrField = dialogView.findViewById(R.id.txtDNrField);
        dlgQuantityField = dialogView.findViewById(R.id.txtQuantityField);

        txtDialogCartonNumber = dialogView.findViewById(R.id.txtCartonNumber);
        txtDialogPartNr = dialogView.findViewById(R.id.txtPartNr);
        txtDialogDNr = dialogView.findViewById(R.id.txtDNr);
        txtDialogQuantity = dialogView.findViewById(R.id.txtQuantity);
        txtDialogOrderNr = dialogView.findViewById(R.id.txtOrderNr);

        btnDialogAdd = dialogView.findViewById(R.id.btnAdd);
        MaterialButton btnClear = dialogView.findViewById(R.id.btnClear);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (isLeft) {
            txtDlgTitle.setText("Add Minus Label");
            dlgCartonNumberField.setHint(getString(R.string.ct_nr));
            dlgPartNrField.setHint(getString(R.string.part_nr));
            dlgDNrField.setHint(getString(R.string.d_nr));
            dlgQuantityField.setHint(R.string.qtty);
        } else {
            txtDlgTitle.setText("Add Good Label");
            dlgCartonNumberField.setHint(getString(R.string.c_name));
            dlgPartNrField.setHint(getString(R.string.part_nr));
            dlgDNrField.setHint(getString(R.string.cust_n));
            dlgQuantityField.setHint(R.string.qtty);
        }

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);

        txtDialogCartonNumber.requestFocus();
        if (!isManual) {
            txtDialogCartonNumber.setShowSoftInputOnFocus(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            txtDialogPartNr.setEnabled(false);
            txtDialogDNr.setEnabled(false);
            txtDialogQuantity.setEnabled(false);
            txtDialogOrderNr.setEnabled(false);
        } else {
            txtDialogPartNr.setEnabled(true);
            txtDialogDNr.setEnabled(true);
            txtDialogQuantity.setEnabled(true);
            txtDialogOrderNr.setEnabled(true);
        }
        txtDialogCartonNumber.post(() -> txtDialogCartonNumber.setSelection(txtDialogCartonNumber.getText().length()));

        txtDialogCartonNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String strCartonNumber = txtDialogCartonNumber.getText().toString();
                int count = strCartonNumber.split(";").length;

                if (count == 4) {
                    txtDialogPartNr.setText(strCartonNumber.split(";")[1]);
                    txtDialogDNr.setText(strCartonNumber.split(";")[2]);
                    txtDialogQuantity.setText(strCartonNumber.split(";")[3]);
                    txtDialogCartonNumber.setText(strCartonNumber.split(";")[0]);
                }
                if (count == 5) {
                    txtDialogPartNr.setText(strCartonNumber.split(";")[1]);
                    txtDialogDNr.setText(strCartonNumber.split(";")[2]);
                    txtDialogQuantity.setText(strCartonNumber.split(";")[3]);
                    txtDialogOrderNr.setText(strCartonNumber.split(";")[4]);
                    txtDialogCartonNumber.setText(strCartonNumber.split(";")[0]);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkAddLabelValidation(isLeft);
            }
        });
        txtDialogPartNr.addTextChangedListener(new AddDialogTextWatcher(isLeft));
        txtDialogQuantity.addTextChangedListener(new AddDialogTextWatcher(isLeft));

        btnDialogAdd.setOnClickListener(view -> {

            String cartonName = txtDialogCartonNumber.getText().toString();
            String partNr = txtDialogPartNr.getText().toString();
            String dNr = txtDialogDNr.getText().toString();
            String quantity = txtDialogQuantity.getText().toString();
            String orderNr = isLeft ? "" : txtDialogOrderNr.getText().toString();

            if (isLeft) {
                HashMap<String, String> smallLabelData = new HashMap<>();
                smallLabelData.put(Utils.CARTON_NR, txtCtNr2.getText().toString());
                smallLabelData.put(Utils.PART_NR, txtPartNr2.getText().toString());
                smallLabelData.put(Utils.D_NR, txtDNr2.getText().toString());
                smallLabelData.put(Utils.QUANTITY, txtQuantity2.getText().toString());
                smallListAdapter.addItem(smallLabelData);

                txtCtNr2.setText(cartonName);
                txtPartNr2.setText(partNr);
                txtDNr2.setText(dNr);
                txtQuantity2.setText(quantity);
            } else {
                HashMap<String, String> bigLabelData = new HashMap<>();
                bigLabelData.put(Utils.CARTON_NR, txtCtNr3.getText().toString());
                bigLabelData.put(Utils.PART_NR, txtPartNr3.getText().toString());
                bigLabelData.put(Utils.CUST_N, txtDNr3.getText().toString());
                bigLabelData.put(Utils.QUANTITY, txtQuantity3.getText().toString());
                bigListAdapter.addItem(bigLabelData);

                txtCtNr3.setText(cartonName);
                txtPartNr3.setText(partNr);
                txtDNr3.setText(dNr);
                txtQuantity3.setText(quantity);
            }

            dialog.dismiss();
        });

        btnClear.setOnClickListener(view -> {
            txtDialogCartonNumber.setText("");
            txtDialogPartNr.setText("");
            txtDialogDNr.setText("");
            txtDialogQuantity.setText("");
            if (!isLeft) {
                txtDialogOrderNr.setText("");
            }
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void checkAddLabelValidation(boolean isLeft) {
        int correctResults = 0;

        if (txtDialogCartonNumber.getText().toString().isEmpty()) {
            if (isLeft) {
                dlgCartonNumberField.setError("Empty Ct-Nr");
            } else {
                dlgCartonNumberField.setError("Empty C-Name");
            }

            txtDialogCartonNumber.setBackgroundTintList(redColors);
        } else {
            if (isLeft && isDialogCartonExisting(txtDialogCartonNumber.getText().toString(), isLeft)) {
                dlgCartonNumberField.setError("DOUBLE Ct-Nr");
                txtDialogCartonNumber.setBackgroundTintList(redColors);
            } else {
                correctResults += 1;
                dlgCartonNumberField.setErrorEnabled(false);
                txtDialogCartonNumber.setBackgroundTintList(greenColors);
            }
        }

        String strOldPartNr = txtPartNr1.getText().toString();
        if (!isLeft) {
            strOldPartNr = txtPartNr2.getText().toString();
        }

        if (!txtDialogPartNr.getText().toString().equals(strOldPartNr)) {
            dlgPartNrField.setError("Invalid Part Number");
            txtDialogPartNr.setBackgroundTintList(redColors);
        } else {
            dlgPartNrField.setErrorEnabled(false);
            txtDialogPartNr.setBackgroundTintList(greenColors);
            correctResults += 1;
        }

        if (txtDialogQuantity.getText().toString().isEmpty()) {
            dlgQuantityField.setError("Empty Quantity");
        } else {
            dlgQuantityField.setErrorEnabled(false);
            correctResults += 1;
        }

        if (correctResults == 3) {
            btnDialogAdd.setEnabled(true);
        } else {
            btnDialogAdd.setEnabled(false);
        }
    }

    private class AddDialogTextWatcher implements TextWatcher {

        private boolean isLeft = false;

        public AddDialogTextWatcher(boolean isLeft) {
            super();
            this.isLeft = isLeft;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            checkAddLabelValidation(isLeft);
        }
    }

    private void onNew() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("New")
                .setMessage("Are you sure to create new one?")
                .setNegativeButton("Yes", (dialogInterface, i) -> {
                    upload();

                    dialogInterface.dismiss();
                })
                .setPositiveButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void saveAndNext() {

        HashMap<String, String> smallLabelData = new HashMap<>();
        smallLabelData.put(Utils.CARTON_NR, txtCtNr2.getText().toString());
        smallLabelData.put(Utils.PART_NR, txtPartNr2.getText().toString());
        smallLabelData.put(Utils.D_NR, txtDNr2.getText().toString());
        smallLabelData.put(Utils.QUANTITY, txtQuantity2.getText().toString());
        smallListAdapter.addItem(smallLabelData);

        HashMap<String, String> bigLabelData = new HashMap<>();
        bigLabelData.put(Utils.CARTON_NR, txtCtNr3.getText().toString());
        bigLabelData.put(Utils.PART_NR, txtPartNr3.getText().toString());
        bigLabelData.put(Utils.CUST_N, txtDNr3.getText().toString());
        bigLabelData.put(Utils.QUANTITY, txtQuantity3.getText().toString());
        bigListAdapter.addItem(bigLabelData);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int scannedNumber = sharedPreferences.getInt(SCANNED_NUMBER, 0);
        saveData(generateStringToSave(scannedNumber));

        // increase Scanned Number
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SCANNED_NUMBER, scannedNumber + 1);
        editor.apply();
        txtScannedNumber.setText(String.valueOf(scannedNumber + 1));

        reset();
        checkUploadAvailable();
    }

    private String generateStringToSave(int scannedNumber) {
        StringBuilder result = new StringBuilder();

        HashMap<String, String> smallLabel = smallListAdapter.getItem(0);
        String ctNr = smallLabel.getOrDefault(Utils.CARTON_NR, "");
        String partNr1 = smallLabel.getOrDefault(Utils.PART_NR, "");
        String dNr = smallLabel.getOrDefault(Utils.D_NR, "");
        String qtty1 = smallLabel.getOrDefault(Utils.QUANTITY, "");
        String strSmallLabel = String.format(Locale.getDefault(),
                "%-11s ; SCAN%03d ; %-12s ; %-14s ; %-14s ; %-12s;\n",
                "SmallLabel", scannedNumber + 1, ctNr, partNr1, dNr, qtty1);
        result.append(strSmallLabel);

        HashMap<String, String> bigLabel = bigListAdapter.getItem(0);
        String cName = bigLabel.getOrDefault(Utils.CARTON_NR, "");
        String partNr2 = bigLabel.getOrDefault(Utils.PART_NR, "");
        String custN = bigLabel.getOrDefault(Utils.CUST_N, "");
        String qtty2 = bigLabel.getOrDefault(Utils.QUANTITY, "");
        String orderNr = bigLabel.getOrDefault(Utils.ORDER_NR, "");
        String strBigLabel = String.format(Locale.getDefault(),
                "%-11s ; SCAN%03d ; %-12s ; %-14s ; %-14s ; %-12s; %-20s;\n",
                "BigLabel", scannedNumber + 1, cName, partNr2, custN, qtty2, orderNr);
        result.append(strBigLabel);

        for (int i = 1; i < smallListAdapter.getItemCount(); i++) {
            smallLabel = smallListAdapter.getItem(i);

            ctNr = smallLabel.getOrDefault(Utils.CARTON_NR, "");
            partNr1 = smallLabel.getOrDefault(Utils.PART_NR, "");
            dNr = smallLabel.getOrDefault(Utils.D_NR, "");
            qtty1 = smallLabel.getOrDefault(Utils.QUANTITY, "");

            strSmallLabel = String.format(Locale.getDefault(),
                    "%-11s ; SCAN%03d ; %-12s ; %-14s ; %-14s ; %-12s;\n",
                    "SmallLabel+", scannedNumber + 1, ctNr, partNr1, dNr, qtty1);

            result.append(strSmallLabel);
        }

        for (int i = 1; i < bigListAdapter.getItemCount(); i++) {
            bigLabel = bigListAdapter.getItem(i);
            cName = bigLabel.getOrDefault(Utils.CARTON_NR, "");
            partNr2 = bigLabel.getOrDefault(Utils.PART_NR, "");
            custN = bigLabel.getOrDefault(Utils.CUST_N, "");
            qtty2 = bigLabel.getOrDefault(Utils.QUANTITY, "");
            orderNr = bigLabel.getOrDefault(Utils.ORDER_NR, "");

            strBigLabel = String.format(Locale.getDefault(),
                    "%-11s ; SCAN%03d ; %-12s ; %-14s ; %-14s ; %-12s; %-20s;\n",
                    "BigLabel+", scannedNumber + 1, cName, partNr2, custN, qtty2, orderNr);

            result.append(strBigLabel);
        }
        return result.toString();
    }

    private void saveData(String strData) {

        String fileName = getFileName();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = Utils.getDocumentsDirectory(this);
            File file = new File(dir, fileName);
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.append(strData);
                System.out.println("File saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("External storage not available.");
        }

    }

    private void reset() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Code to run after delay
                txtCtNr2.setText("");
                txtPartNr2.setText("");
                txtDNr2.setText("");
                txtQuantity2.setText("");
                txtQuantityField2.setHelperText("");

                txtCtNr3.setText("");
                txtPartNr3.setText("");
                txtDNr3.setText("");
                txtQuantity3.setText("");
                txtQuantityField3.setHelperText("");

                smallListAdapter.clear();
                bigListAdapter.clear();

                txtPartNr2.setBackgroundTintList(txtCtNr2.getBackgroundTintList());
                txtPartNr3.setBackgroundTintList(txtCtNr2.getBackgroundTintList());
                txtQuantity2.setBackgroundTintList(txtCtNr2.getBackgroundTintList());
                txtQuantity3.setBackgroundTintList(txtCtNr2.getBackgroundTintList());

                btnPlus1.setEnabled(false);
                btnPlus2.setEnabled(false);

                txtCtNr2.requestFocus();
            }
        }, 1000);


    }

    private void upload() {

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String serverType = sharedPreferences.getString(SERVER_TYPE, "ftp");
        String host = sharedPreferences.getString(HOST_ADDRESS, "");
        String username = sharedPreferences.getString(FTP_USERNAME, "");
        String password = sharedPreferences.getString(FTP_PASSWORD, "");

        if (host.isEmpty() || !isValidUrl(host)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Error")
                    .setMessage("Please set valid server address in Settings. Do you want to create new one without uploading?")
                    .setNegativeButton("Yes", (dialogInterface, i) -> {
                        // set scanned number to 0
                        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
                        editor.putInt(SCANNED_NUMBER, 0);
                        editor.apply();
                        txtScannedNumber.setText(String.valueOf(0));
                        reset();

                        removeCurrentFile();
                        dialogInterface.dismiss();
                    })
                    .setPositiveButton("No", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .show();
        } else {

            if (serverType.equals("ftp")) {
                String portString = sharedPreferences.getString(FTP_PORT, "");
                if (portString.isEmpty()) {
                    uploadFileUsingFTP(host, username, password);
                } else {
                    int ftpPort = 0;
                    if (!portString.isEmpty()) {
                        ftpPort = Integer.parseInt(portString);
                    }
                    uploadFileUsingSFTP(host, ftpPort, username, password);
                }
            } else if (serverType.equals("smb")) {
                String sharedFolder = sharedPreferences.getString(SHARED_FOLDER, "");
                uploadFileToSMB(host, sharedFolder, "", username, password);
            }

            // set scanned number to 0
            SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
            editor.putInt(SCANNED_NUMBER, 0);
            editor.apply();
            txtScannedNumber.setText(String.valueOf(0));
            reset();
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && Patterns.WEB_URL.matcher(url).matches();
    }

    private void uploadFileUsingFTP(final String ftpServer, final String ftpUsername, final String ftpPassword) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            FTPClient ftpClient = new FTPClient();
            String errorMessage = null;
            boolean success = false;

            try {
                String fileName = getFileName();

                File dir = Utils.getDocumentsDirectory(this);
                File file = new File(dir, fileName);

                if (!file.exists()) {
                    errorMessage = "There is no file to upload!";
                } else {
                    ftpClient.connect(ftpServer);
                    ftpClient.login(ftpUsername, ftpPassword);
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                    FileInputStream inputStream = new FileInputStream(file);
                    success = ftpClient.storeFile(file.getName(), inputStream);
                    inputStream.close();
                }
            } catch (Exception e) {
                errorMessage = "Error uploading file: " + e.getMessage();
                Log.e("FTP", errorMessage);
            } finally {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (Exception e) {
                    Log.e("FTP", "Error disconnecting: " + e.getMessage());
                }
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;

            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                    removeCurrentFile();
                } else {
                    Toast.makeText(this, "Upload failed: " + (finalErrorMessage != null ? finalErrorMessage : "Unknown error occurred."), Toast.LENGTH_LONG).show();
                }
            });
        });
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

        String fileName = String.format(Locale.getDefault(), "SCAN%s-%02d.txt", strDate, fileCounter);

        return fileName;
    }

    private void uploadFileUsingSFTP(final String host, final int port, final String username, final String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String errorMessage = null;
            boolean success = false;

            try {
                String fileName = getFileName();

                File dir = Utils.getDocumentsDirectory(this);
                File file = new File(dir, fileName);

                if (!file.exists()) {
                    errorMessage = "There is no file to upload!";
                } else {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(username, host, port);
                    session.setPassword(password);

                    java.util.Properties config = new java.util.Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();

                    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                    channel.connect();

                    FileInputStream inputStream = new FileInputStream(file);
                    channel.put(inputStream, file.getName());
                    inputStream.close();

                    channel.disconnect();
                    session.disconnect();
                    success = true;
                }
            } catch (Exception e) {
                errorMessage = "Error uploading file: " + e.getMessage();
                Log.e("SFTP", errorMessage, e);
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;

            handler.post(() -> {
                if (finalSuccess) {
                    Log.d("========", "File uploaded successfully.");
                    Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                    removeCurrentFile();
                } else {
                    Log.d("========", "Upload Failed: " + (finalErrorMessage != null ? finalErrorMessage : "Unknown error occurred."));
                    Toast.makeText(this, "Upload failed: " + (finalErrorMessage != null ? finalErrorMessage : "Unknown error occurred."), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public void uploadFileToSMB(String hostname, String shareName, String domain,
                                String username, String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // SMB upload logic here
                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(hostname)) {
                    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                    com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
                    DiskShare share = (DiskShare) session.connectShare(shareName);

                    String fileName = getFileName();
                    File dir = Utils.getDocumentsDirectory(this);
                    File file = new File(dir, fileName);

                    FileInputStream fis = new FileInputStream(file);
                    OutputStream os = share.openFile(fileName,
                            EnumSet.of(AccessMask.GENERIC_WRITE),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            null).getOutputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }

                    os.close();
                    fis.close();
                    share.close();
                    session.close();
                }

                // Notify success on main thread
                mainHandler.post(() -> {
                    Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    removeCurrentFile();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void testFtpConnection(String host, int port, String username, String password) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;

            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(host, port);
                boolean login = ftpClient.login(username, password);

                if (login) {
                    // Optional: try to list files to confirm access
                    ftpClient.enterLocalPassiveMode(); // avoid firewall issues
                    ftpClient.listFiles(); // just to trigger access
                    ftpClient.logout();
                    success = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                if (finalSuccess) {
                    showInformationDialog("Test Success", "Your FTP Server is available!");
                } else {
                    showInformationDialog("Test Failed", "Your FTP Server is unavailable!");
                }
            });
        });
    }

    private void testSmbConnection(String serverIp, String shareName, String username, String password) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // SMB upload logic here
                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(serverIp)) {
                    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                    com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
                    DiskShare share = (DiskShare) session.connectShare(shareName);
                    share.close();
                    session.close();
                }

                // Notify success on main thread
                mainHandler.post(() -> {
                    showInformationDialog("Test Success", "Your SMB Server is available!");
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showInformationDialog("Test Failed", "Your SMB Server is unavailable!");
                });
            }
        });
    }

    private void removeCurrentFile() {
        File dir = Utils.getDocumentsDirectory(this);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("SCAN") && file.getName().endsWith(".txt")) {
                        boolean deleted = file.delete();
                        Log.d("FileDelete", file.getName() + (deleted ? " deleted." : " failed to delete."));
                    }
                }
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
        String strDate = format.format(new Date());

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int fileCounter = sharedPreferences.getInt(FILE_COUNTER, 1);
        fileCounter += 1;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FILE_DATE, strDate);
        editor.putInt(FILE_COUNTER, fileCounter);
        editor.putInt(SCANNED_NUMBER, 0);
        editor.apply();

    }

    private void showInformationDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
    }
}