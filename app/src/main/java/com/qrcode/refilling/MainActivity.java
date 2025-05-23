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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private TextView txtCartonCount, txtMinusCount, txtGoodCount;

    private TextInputLayout txtCtNrField1, txtPartNrField1, txtQuantityField1;
    private TextInputLayout txtCtNrField2, txtPartNrField2, txtQuantityField2;
    private TextInputLayout txtCtNrField3, txtPartNrField3, txtQuantityField3;

    private TextInputEditText txtCtNr1, txtPartNr1, txtDNr1, txtQuantity1;
    private TextInputEditText txtCtNr2, txtPartNr2, txtDNr2, txtQuantity2;
    private TextInputEditText txtCtNr3, txtPartNr3, txtDNr3, txtQuantity3;

    private TextView txtTotalQuantity2, txtTotalQuantity3;

    private RecyclerView listSmallLabels, listBigLabels;
    private AppCompatButton btnPlus1, btnPlus2;
    private AppCompatButton btnUpload, btnNext;

    private boolean isResetting = false;

    private int cartonCount = 0, minusCount = 0, goodCount = 0;

    LabelsAdapter smallListAdapter = new LabelsAdapter(new ArrayList<>());
    LabelsAdapter bigListAdapter = new LabelsAdapter(new ArrayList<>());

    private ActivityResultLauncher<String> storagePermissionLauncher;

    private final ColorStateList greenColors = new ColorStateList(
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

    private final ColorStateList redColors = new ColorStateList(
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

    private final ColorStateList yellowColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.YELLOW,
                    Color.YELLOW,
                    Color.YELLOW
            }
    );

    private final ColorStateList transparentColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
            }
    );
    private ColorStateList normalColors;

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

        txtCartonCount = findViewById(R.id.txtCartonCount);
        txtMinusCount = findViewById(R.id.txtMinusCount);
        txtGoodCount = findViewById(R.id.txtGoodCount);

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

        normalColors = txtDNr1.getBackgroundTintList();

        // Minus Label
        txtCtNrField2 = findViewById(R.id.txtCtNrField2);
        txtPartNrField2 = findViewById(R.id.txtPartNrField2);
        txtQuantityField2 = findViewById(R.id.txtQuantityField2);
        txtCtNr2 = findViewById(R.id.txtCtNr2);
        txtPartNr2 = findViewById(R.id.txtPartNr2);
        txtDNr2 = findViewById(R.id.txtDNr2);
        txtQuantity2 = findViewById(R.id.txtQuantity2);
        txtTotalQuantity2 = findViewById(R.id.txtTotalQuantity2);

        // Good Label
        txtCtNrField3 = findViewById(R.id.txtCtNrField3);
        txtPartNrField3 = findViewById(R.id.txtPartNrField3);
        txtQuantityField3 = findViewById(R.id.txtQuantityField3);
        txtCtNr3 = findViewById(R.id.txtCtNr3);
        txtPartNr3 = findViewById(R.id.txtPartNr3);
        txtDNr3 = findViewById(R.id.txtDNr3);
        txtQuantity3 = findViewById(R.id.txtQuantity3);
        txtTotalQuantity3 = findViewById(R.id.txtTotalQuantity3);

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

        btnNext = findViewById(R.id.btnNext);

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

        btnNext.setOnClickListener(view -> saveAndNext());

        findViewById(R.id.btnClear).setOnClickListener(view -> reset());

        findViewById(R.id.btnSettings).setOnClickListener(view -> showSettingsDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            logout();
        });

        checkManual();
        initFirstScan();
        initSecondScan();
        initThirdScan();

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Storage Permission Allowed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
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


    private void logout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure to log out?")
                .setNegativeButton("Yes", (dialogInterface, i) -> {
                    finish();
                    dialogInterface.dismiss();
                })
                .setPositiveButton("No", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
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
            txtCtNr1.setShowSoftInputOnFocus(true);
            txtCtNr1.setEnabled(true);
            txtPartNr1.setEnabled(true);
            txtDNr1.setEnabled(true);
            txtQuantity1.setEnabled(true);

            txtCtNr2.setEnabled(true);
            txtPartNr2.setEnabled(true);
            txtDNr2.setEnabled(true);
            txtQuantity2.setEnabled(true);

            txtCtNr3.setEnabled(true);
            txtPartNr3.setEnabled(true);
            txtDNr3.setEnabled(true);
            txtQuantity3.setEnabled(true);
        } else {
            txtCtNr1.setShowSoftInputOnFocus(false);
            txtCtNr1.setEnabled(true);
            txtPartNr1.setEnabled(false);
            txtDNr1.setEnabled(false);
            txtQuantity1.setEnabled(false);

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

        File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);

        if (file.exists()) {
            btnUpload.setEnabled(true);
        } else {
            btnUpload.setEnabled(false);
        }
    }

    private void initFirstScan() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);

        txtCtNr1.requestFocus();
        if (!isManual) {
            txtCtNr1.setShowSoftInputOnFocus(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        txtCtNr1.post(() -> txtCtNr1.setSelection(txtCtNr1.getText().length()));

        txtCtNr1.setOnKeyListener((view, keyCode, event) -> {

            if (keyCode==KeyEvent.KEYCODE_ENTER)
            {
                // Just ignore the [Enter] key
                return true;
            }
            // Handle all other keys in the default way
            return (keyCode == KeyEvent.KEYCODE_ENTER);
        });
        txtCtNr1.addTextChangedListener(new SimpleTextWatcher() {

            @Override
            public void afterTextChanged() {
                String strCtNr = txtCtNr1.getText().toString();
                int count = strCtNr.split(";").length;

                if(count == 4) {
                    String partNr = strCtNr.split(";")[1];
                    txtPartNr1.setText(partNr);
                    txtPartNr1.setSelection(partNr.length());

                    txtDNr1.setText(strCtNr.split(";")[2]);
                    txtQuantity1.setText(strCtNr.split(";")[3]);
                    txtCtNr1.setText(strCtNr.split(";")[0]);
                    txtCtNr2.requestFocus();
                }

                if (!txtCtNr1.getText().toString().isEmpty()) {
                    compare(false);
                } else {
                    txtCtNrField1.setErrorEnabled(false);
                }
            }
        });

        txtPartNr1.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                if (!(txtPartNr2.getText().toString().isEmpty())) {
                    compare(false);
                }
            }
        });

        txtDNr1.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                compare(false);
            }
        });

        txtQuantity1.addTextChangedListener(new SimpleTextWatcher() {

            @Override
            public void afterTextChanged() {
                if (!(txtQuantity2.getText().toString().isEmpty())) {
                    compare(false);
                    txtCtNr2.requestFocus();
                }
            }
        });
    }

    private void initSecondScan() {
        txtTotalQuantity2.setText("");
        txtTotalQuantity2.setVisibility(View.GONE);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);

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
        txtCtNr2.addTextChangedListener(new SimpleTextWatcher() {

            @Override
            public void afterTextChanged() {
                String strCtNr = txtCtNr2.getText().toString();
                int count = strCtNr.split(";").length;

                if(count == 4) {
                    String partNr = strCtNr.split(";")[1];
                    txtPartNr2.setText(partNr);
                    txtPartNr2.setSelection(partNr.length());
                    txtDNr2.setText(strCtNr.split(";")[2]);
                    txtQuantity2.setText(strCtNr.split(";")[3]);
                    txtCtNr2.setText(strCtNr.split(";")[0]);
                    txtCtNr3.requestFocus();
                }

                if (!txtCtNr2.getText().toString().isEmpty()) {
                    compare(false);
                } else {
                    txtCtNrField2.setErrorEnabled(false);
                }
            }
        });

        txtPartNr2.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                compare(false);
            }
        });

        txtDNr2.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                compare(false);
            }
        });

        txtQuantity2.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                if (!(txtQuantity3.getText().toString().isEmpty())) {
                    compare(true);
                    txtCtNr3.requestFocus();
                }
            }
        });
    }

    private void initThirdScan() {
        txtTotalQuantity3.setText("");
        txtTotalQuantity3.setVisibility(View.GONE);

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
        txtCtNr3.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                String strCName = txtCtNr3.getText().toString();
                int count = strCName.split(";").length;

                if(count == 4) {
                    String partNr = strCName.split(";")[1];
                    txtPartNr3.setText(partNr);
                    txtPartNr3.setSelection(partNr.length());
                    txtDNr3.setText(strCName.split(";")[2]);
                    txtQuantity3.setText(strCName.split(";")[3]);
                    txtCtNr3.setText(strCName.split(";")[0]);
                }

                if (!txtCtNr3.getText().toString().isEmpty()) {
                    compare(false);
                }
            }
        });

        txtPartNr3.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                if (!(txtPartNr3.getText().toString().isEmpty())) {
                    compare(false);
                }
            }
        });

        txtDNr3.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                compare(false);
            }
        });

        txtQuantity3.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                if (!(txtQuantity3.getText().toString().isEmpty())) {
                    compare(true);
                }
            }
        });
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override public void afterTextChanged(Editable s) {
            afterTextChanged();
        }
        public abstract void afterTextChanged();
    }

    private void compare(boolean isQttyChanged) {
        if (isResetting) return;

        String strCtNr1 = txtCtNr1.getText().toString();
        String strCtNr2 = txtCtNr2.getText().toString();
        String strCtNr3 = txtCtNr3.getText().toString();

        String strDNr1 = txtDNr1.getText().toString();
        String strDNr2 = txtDNr2.getText().toString();
        String strDNr3 = txtDNr3.getText().toString();

        if (!strCtNr1.isEmpty()) {
            txtCartonCount.setText("1");
        }
        if (!strCtNr2.isEmpty()) {
            txtMinusCount.setText(String.valueOf(smallListAdapter.getItemCount() + 1));
        }
        if (!strCtNr3.isEmpty()) {
            txtGoodCount.setText(String.valueOf(bigListAdapter.getItemCount() + 1));
        }

        if (strCtNr1.equals(strCtNr2) && strCtNr1.equals(strCtNr3)) {
            txtCtNr1.setBackgroundTintList(normalColors);
            txtCtNr2.setBackgroundTintList(normalColors);
            txtCtNr3.setBackgroundTintList(normalColors);
        } else {
            txtCtNr1.setBackgroundTintList(yellowColors);
            txtCtNr2.setBackgroundTintList(yellowColors);
            txtCtNr3.setBackgroundTintList(yellowColors);
        }

        if (strDNr1.equals(strDNr2) && strDNr1.equals(strDNr3)) {
            txtDNr1.setBackgroundTintList(normalColors);
            txtDNr2.setBackgroundTintList(normalColors);
            txtDNr3.setBackgroundTintList(normalColors);
        } else {
            txtDNr1.setBackgroundTintList(yellowColors);
            txtDNr2.setBackgroundTintList(yellowColors);
            txtDNr3.setBackgroundTintList(yellowColors);
        }

        String strPartNr1 = txtPartNr1.getText().toString();
        String strPartNr2 = txtPartNr2.getText().toString();
        String strPartNr3 = txtPartNr3.getText().toString();
        String strQuantity2 = txtQuantity2.getText().toString();
        String strQuantity3 = txtQuantity3.getText().toString();

        int quantity2 = 0, quantity3 = 0;
        if (!strQuantity2.isEmpty()) {
            quantity2 = Integer.parseInt(strQuantity2);
        }
        if (!strQuantity3.isEmpty()) {
            quantity3 = Integer.parseInt(strQuantity3);
        }

        int result = 0;
        if (!strPartNr1.isEmpty() && strPartNr1.equals(strPartNr2) && strPartNr1.equals(strPartNr3)) {
            result += 1;
            txtPartNr1.setBackgroundTintList(greenColors);
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
                        quantity2 += quantity;
                        quantityHelperString.append(strQtty).append(" + ");
                    }
                }
                if (quantityHelperString.toString().isEmpty()) {
                    txtQuantityField2.setHelperText("");
                } else {
                    quantityHelperString.append(strQuantity2.isEmpty() ? "0" : strQuantity2);
                    quantityHelperString.append(" = ").append(quantity2);
                    txtQuantityField2.setHelperText(quantityHelperString.toString());

                    txtTotalQuantity2.setVisibility(View.VISIBLE);
                    txtTotalQuantity2.setText(String.valueOf(quantity2));
                }
            } else {
                txtQuantityField2.setHelperText("");
                txtTotalQuantity2.setVisibility(View.GONE);
            }

            if (bigListAdapter.getItemCount() > 0) {
                StringBuilder quantityHelperString = new StringBuilder();
                List<HashMap<String, String>> arrBigLabels = bigListAdapter.getItems();
                for (HashMap<String, String> bigLabelData: arrBigLabels) {
                    String partNr = bigLabelData.getOrDefault(Utils.PART_NR, "");
                    if (strPartNr3.equals(partNr)) {
                        String strQtty = bigLabelData.getOrDefault(Utils.QUANTITY, "0");
                        int quantity = Integer.parseInt(strQtty);
                        quantity3 += quantity;
                        quantityHelperString.append(strQtty).append(" + ");
                    }
                }
                if (quantityHelperString.toString().isEmpty()) {
                    txtQuantityField3.setHelperText("");
                } else {
                    quantityHelperString.append(strQuantity3.isEmpty() ? "0" : strQuantity3);
                    quantityHelperString.append(" = ").append(quantity3);
                    txtQuantityField3.setHelperText(quantityHelperString.toString());

                    txtTotalQuantity3.setVisibility(View.VISIBLE);
                    txtTotalQuantity3.setText(String.valueOf(quantity3));
                }

            } else {
                txtQuantityField3.setHelperText("");
                txtTotalQuantity3.setVisibility(View.GONE);
            }
        } else {
            txtPartNr1.setBackgroundTintList(redColors);
            txtPartNr2.setBackgroundTintList(redColors);
            txtPartNr3.setBackgroundTintList(redColors);
        }

        if (quantity2 > 0 && quantity3 > 0) {
            if (quantity2 == quantity3) {
                result += 1;
                txtQuantity2.setBackgroundTintList(greenColors);
                txtQuantity3.setBackgroundTintList(greenColors);

                txtTotalQuantity2.setBackgroundColor(Color.GREEN);
                txtTotalQuantity3.setBackgroundColor(Color.GREEN);

            } else if (quantity2 > quantity3){
                result += 1;
                txtQuantity2.setBackgroundTintList(yellowColors);
                txtQuantity3.setBackgroundTintList(yellowColors);
                txtTotalQuantity2.setBackgroundColor(Color.YELLOW);
                txtTotalQuantity3.setBackgroundColor(Color.YELLOW);

                if (isQttyChanged) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Qtty are not same")
                            .setMessage("Carton will remain “minus”?")
                            .setNegativeButton("Ok", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                            })
                            .show();
                }

            } else {
                txtQuantity2.setBackgroundTintList(redColors);
                txtQuantity3.setBackgroundTintList(redColors);
                txtTotalQuantity2.setBackgroundColor(Color.RED);
                txtTotalQuantity3.setBackgroundColor(Color.RED);

                if (isQttyChanged) {
                    showInformationDialog("Qtty are not same", "Good Qtty cannot be more than Minus Qtty. Please add Minus-Qtty.");
                }
            }
        } else {
            txtQuantity2.setBackgroundTintList(redColors);
            txtQuantity3.setBackgroundTintList(redColors);
            txtTotalQuantity2.setBackgroundColor(Color.RED);
            txtTotalQuantity3.setBackgroundColor(Color.RED);

        }


        if (result == 2) {
            btnNext.setEnabled(true);
        } else {
            btnNext.setEnabled(false);
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

        btnDialogAdd = dialogView.findViewById(R.id.btnAdd);
        MaterialButton btnDialogClear = dialogView.findViewById(R.id.btnClear);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (isLeft) {
            txtDlgTitle.setText("Add Minus Label");
        } else {
            txtDlgTitle.setText("Add Good Label");
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
        } else {
            txtDialogPartNr.setEnabled(true);
            txtDialogDNr.setEnabled(true);
            txtDialogQuantity.setEnabled(true);
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

            if (isLeft) {
                HashMap<String, String> smallLabelData = new HashMap<>();
                smallLabelData.put(Utils.CARTON_NR, txtCtNr2.getText().toString());
                smallLabelData.put(Utils.PART_NR, txtPartNr2.getText().toString());
                smallLabelData.put(Utils.D_NR, txtDNr2.getText().toString());
                smallLabelData.put(Utils.QUANTITY, txtQuantity2.getText().toString());
                smallListAdapter.addItem(smallLabelData);

                txtQuantity2.setText(quantity);
                txtCtNr2.setText(cartonName);
                txtPartNr2.setText(partNr);
                txtDNr2.setText(dNr);
            } else {
                HashMap<String, String> bigLabelData = new HashMap<>();
                bigLabelData.put(Utils.CARTON_NR, txtCtNr3.getText().toString());
                bigLabelData.put(Utils.PART_NR, txtPartNr3.getText().toString());
                bigLabelData.put(Utils.D_NR, txtDNr3.getText().toString());
                bigLabelData.put(Utils.QUANTITY, txtQuantity3.getText().toString());
                bigListAdapter.addItem(bigLabelData);

                txtCtNr3.setText(cartonName);
                txtPartNr3.setText(partNr);
                txtDNr3.setText(dNr);
                txtQuantity3.setText(quantity);
            }

            dialog.dismiss();
        });

        btnDialogClear.setOnClickListener(view -> {
            txtDialogCartonNumber.setText("");
            txtDialogPartNr.setText("");
            txtDialogDNr.setText("");
            txtDialogQuantity.setText("");
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void checkAddLabelValidation(boolean isLeft) {
        int correctResults = 0;

        if (txtDialogCartonNumber.getText().toString().isEmpty()) {
            dlgCartonNumberField.setError("Empty Ct-Nr");
            txtDialogCartonNumber.setBackgroundTintList(redColors);
        } else {
            correctResults += 1;
            dlgCartonNumberField.setErrorEnabled(false);
            txtDialogCartonNumber.setBackgroundTintList(greenColors);
        }

        String strOldPartNr = txtPartNr2.getText().toString();
        if (!isLeft) {
            strOldPartNr = txtPartNr3.getText().toString();
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
        bigLabelData.put(Utils.D_NR, txtDNr3.getText().toString());
        bigLabelData.put(Utils.QUANTITY, txtQuantity3.getText().toString());
        bigListAdapter.addItem(bigLabelData);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int scannedNumber = sharedPreferences.getInt(SCANNED_NUMBER, 0);
        saveData(scannedNumber);

        // increase Scanned Number
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SCANNED_NUMBER, scannedNumber + 1);
        editor.apply();
        txtScannedNumber.setText(String.valueOf(scannedNumber + 1));

        reset();
        checkUploadAvailable();
    }

    private void saveData(int scannedNumber) {

        ArrayList<List<String>> result = new ArrayList<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = simpleDateFormat.format(new Date());

        String scanLabel = String.format(Locale.getDefault(), "Scan%03d", scannedNumber + 1);


        String ctNr1 = txtCtNr1.getText().toString();
        String partNr1 = txtPartNr1.getText().toString();
        String dNr1 = txtDNr1.getText().toString();
        String qtty1 = txtQuantity1.getText().toString();
        List<String> rowCartonData = Arrays.asList(mUserName, currentTime, "CartonLabel", scanLabel, ctNr1, partNr1, dNr1, qtty1);
        result.add(rowCartonData);

        HashMap<String, String> smallLabel = smallListAdapter.getItem(0);
        String ctNr2 = smallLabel.getOrDefault(Utils.CARTON_NR, "");
        String partNr2 = smallLabel.getOrDefault(Utils.PART_NR, "");
        String dNr2 = smallLabel.getOrDefault(Utils.D_NR, "");
        String qtty2 = smallLabel.getOrDefault(Utils.QUANTITY, "");

        List<String> rowMinusData = Arrays.asList(mUserName, currentTime, "MinusLabel", scanLabel, ctNr2, partNr2, dNr2, qtty2);
        result.add(rowMinusData);

        for (int i = 1; i < smallListAdapter.getItemCount(); i++) {
            smallLabel = smallListAdapter.getItem(i);

            ctNr2 = smallLabel.getOrDefault(Utils.CARTON_NR, "");
            partNr2 = smallLabel.getOrDefault(Utils.PART_NR, "");
            dNr2 = smallLabel.getOrDefault(Utils.D_NR, "");
            qtty2 = smallLabel.getOrDefault(Utils.QUANTITY, "");

            List<String> rowMinusPlusData = Arrays.asList(mUserName, currentTime, "MinusLabel+", scanLabel, ctNr2, partNr2, dNr2, qtty2);
            result.add(rowMinusPlusData);
        }

        HashMap<String, String> bigLabel = bigListAdapter.getItem(0);
        String ctNr3 = bigLabel.getOrDefault(Utils.CARTON_NR, "");
        String partNr3 = bigLabel.getOrDefault(Utils.PART_NR, "");
        String dNr3 = bigLabel.getOrDefault(Utils.D_NR, "");
        String qtty3 = bigLabel.getOrDefault(Utils.QUANTITY, "");

        List<String> rowGoodData = Arrays.asList(mUserName, currentTime, "GoodLabel", scanLabel, ctNr3, partNr3, dNr3, qtty3);
        result.add(rowGoodData);

        for (int i = 1; i < bigListAdapter.getItemCount(); i++) {
            bigLabel = bigListAdapter.getItem(i);
            ctNr3 = bigLabel.getOrDefault(Utils.CARTON_NR, "");
            partNr3 = bigLabel.getOrDefault(Utils.PART_NR, "");
            dNr3 = bigLabel.getOrDefault(Utils.D_NR, "");
            qtty3 = bigLabel.getOrDefault(Utils.QUANTITY, "");

            List<String> rowGoodPlusData = Arrays.asList(mUserName, currentTime, "GoodLabel+", scanLabel, ctNr3, partNr3, dNr3, qtty3);
            result.add(rowGoodPlusData);
        }

        appendToExcel(result);
    }


    private void appendToExcel(ArrayList<List<String>> result) {

        try {
            String fileName = getFileName();
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);

                if (!file.exists()) {
                    createExcelFile(file);
                }

                FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis);
                Sheet sheet = workbook.getSheetAt(0); // or getSheet("SheetName")

                for (List<String> rowData : result) {
                    int lastRowNum = sheet.getLastRowNum();
                    Row newRow = sheet.createRow(lastRowNum + 1);

                    for (int i = 0; i < rowData.size(); i++) {
                        Cell cell = newRow.createCell(i);
                        cell.setCellValue(rowData.get(i));
                    }
                }

                fis.close(); // important

                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                workbook.close();
                fos.close();

                Log.d("Excel", "Data appended successfully.");
            } else {
                System.out.println("External storage not available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Excel", "Error appending data: " + e.getMessage());
        }
    }

    private void createExcelFile(File file) {
        try {
            if (!file.exists()) {
                // Create a new workbook and sheet
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Sheet1");

                // (Optional) Create header row
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("User Name");
                header.createCell(1).setCellValue("Time");
                header.createCell(2).setCellValue("Label");
                header.createCell(3).setCellValue("Scan Number");
                header.createCell(4).setCellValue("Ct.Nr");
                header.createCell(5).setCellValue("PartNr");
                header.createCell(6).setCellValue("D-Nr");
                header.createCell(7).setCellValue("Qtty");

                // Save to file
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                fos.close();
                workbook.close();

                Log.d("Excel", "Excel file created.");
            } else {
                Log.d("Excel", "Excel file already exists.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Excel", "Failed to create Excel file: " + e.getMessage());
        }
    }

    private void reset() {
        isResetting = true;

        txtCartonCount.setText("0");
        txtMinusCount.setText("0");
        txtGoodCount.setText("0");

        txtQuantity2.setText("");
        txtQuantity3.setText("");

        txtTotalQuantity2.setText("");
        txtTotalQuantity3.setText("");

        txtCtNr1.setText("");
        txtPartNr1.setText("");
        txtDNr1.setText("");
        txtQuantity1.setText("");
        txtQuantityField1.setHelperText("");

        txtCtNr2.setText("");
        txtPartNr2.setText("");
        txtDNr2.setText("");
        txtQuantityField2.setHelperText("");

        txtCtNr3.setText("");
        txtPartNr3.setText("");
        txtDNr3.setText("");
        txtQuantityField3.setHelperText("");

        smallListAdapter.clear();
        bigListAdapter.clear();

        txtCtNr1.setBackgroundTintList(normalColors);
        txtCtNr2.setBackgroundTintList(normalColors);
        txtCtNr3.setBackgroundTintList(normalColors);
        txtPartNr1.setBackgroundTintList(normalColors);
        txtPartNr2.setBackgroundTintList(normalColors);
        txtPartNr3.setBackgroundTintList(normalColors);
        txtDNr1.setBackgroundTintList(normalColors);
        txtDNr2.setBackgroundTintList(normalColors);
        txtDNr3.setBackgroundTintList(normalColors);
        txtQuantity1.setBackgroundTintList(normalColors);
        txtQuantity2.setBackgroundTintList(normalColors);
        txtQuantity3.setBackgroundTintList(normalColors);

        txtTotalQuantity2.setVisibility(View.GONE);
        txtTotalQuantity3.setVisibility(View.GONE);

        btnPlus1.setEnabled(false);
        btnPlus2.setEnabled(false);

        txtCtNr1.requestFocus();
        isResetting = false;

        btnNext.setEnabled(false);
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

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);


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

        String fileName = String.format(Locale.getDefault(), "refillScan%s-%02d.xlsx", strDate, fileCounter);

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

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/logger/" + fileName);

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
                    File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);

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

        File dir = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("refillScan") && file.getName().endsWith(".xlsx")) {
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

        checkUploadAvailable();
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