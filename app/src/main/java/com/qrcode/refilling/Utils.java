package com.qrcode.refilling;


import android.content.Context;
import android.os.Build;
import android.os.Environment;

public class Utils {

    public static final String CARTON_NR = "CartonNr";
    public static final String PART_NR = "PartNr";
    public static final String D_NR = "DNr";
    public static final String QUANTITY = "Quantity";

    public static String getMainFilePath(Context context) {
        String fPath;

        // Check the SDK version to determine the appropriate storage path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, MANAGE_EXTERNAL_STORAGE permission is required
            if (Environment.isExternalStorageManager()) {
                fPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                // Inform the user they need to grant permission
                // You need to handle this permission request separately
                throw new SecurityException("Permission not granted to access external storage.");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API 29)
            fPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            // For Android 9 and below
            fPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        return fPath;
    }

}
