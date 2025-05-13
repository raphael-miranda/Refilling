package com.qrcode.refilling;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.CaptureActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText txtUserName;
    private EditText txtPassword;

    private static final int REQUEST_CODE_QR_SCAN = 101;

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
                    if (credentials.length >= 3) {
                        callLogin(finalText.trim());
                    }

                }
            }
        });

        findViewById(R.id.qrscan).setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CaptureActivity.class);
            startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
//            if (txtUserName.getText().toString().isEmpty()) {
//                Toast.makeText(this, "please enter username.", Toast.LENGTH_SHORT).show();
//            } else if (txtPassword.getText().toString().isEmpty()) {
//                Toast.makeText(this, "please enter password.", Toast.LENGTH_SHORT).show();
//            } else {
                login(txtUserName.getText().toString(), txtPassword.getText().toString());
//            }
        });

    }

    public void callLogin(String data){

        String[] res;

        if (data != null) {
            try {
                // Split the result string using ";" as the delimiter
                res = data.split(";");

                if (res.length >= 2) {
                    String username = res[0];  // Get the username
                    String password = res[1];  // Get the password

                    txtUserName.setText(username);
                    txtPassword.setText(password);

                    if (txtUserName.getText().toString().isEmpty()) {
                        Toast.makeText(this, "please enter username.", Toast.LENGTH_SHORT).show();
                    } else if (txtPassword.getText().toString().isEmpty()) {
                        Toast.makeText(this, "please enter password.", Toast.LENGTH_SHORT).show();
                    } else {
                        login(txtUserName.getText().toString(), txtPassword.getText().toString());
                    }

                } else {
                    Toast.makeText(getApplication(),"Invalid QR code format.",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplication(),"Error parsing QR code: " + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplication(),"No result found.",Toast.LENGTH_SHORT).show();
        }
        // ---------------reset--------------------------------------
        txtUserName.requestFocus();
        txtUserName.setEnabled(true);

    }

    private void login(String userName, String password) {
//        if (userName.equals("test") && password.equals("1234")) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("UserName", userName);
            startActivity(intent);
//        }
    }

}