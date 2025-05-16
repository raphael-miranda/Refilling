package com.qrcode.refilling;

import static com.qrcode.refilling.MainActivity.FILE_COUNTER;
import static com.qrcode.refilling.MainActivity.FILE_DATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ViewDataActivity extends AppCompatActivity {

    private RecyclerView listView;

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

        readExcelFile();
    }

    private void readExcelFile() {
        ArrayList<ArrayList<String>> cellList = new ArrayList<>();
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                File dir = Utils.getDocumentsDirectory(this);
//                File file = new File(dir, getFileName());

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/refilling/" + getFileName());

                FileInputStream fis = new FileInputStream(file);

                Workbook workbook = new XSSFWorkbook(fis);

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row: sheet) {
                    if (row.getRowNum() == 0) continue;

                    ArrayList<String> rowValue= new ArrayList<>();
                    for (Cell cell: row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowValue.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowValue.add(cell.getDateCellValue().toString());
                                } else {
                                    rowValue.add(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    cellList.add(rowValue);
                }

                workbook.close();
                fis.close();
            }
        }catch(Exception exp){
            exp.printStackTrace();
            Toast.makeText(this, "Sorry User don't have view report", Toast.LENGTH_SHORT).show();
        }


        listView = findViewById(R.id.listView);
        listView.setLayoutManager(new LinearLayoutManager(this));

        ReportAdapter reportAdapter = new ReportAdapter(cellList);

        listView.setAdapter(reportAdapter);

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
}