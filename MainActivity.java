package com.example.current;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    String secretKey = "9f4e3818fd714c3c5d3e0cfb44f3c7a7a9dce8b2e1f3c6a7d8f9a1b2c3d4e5f6";

    Button button;
    TextView textView;

    private static final int PERMISSION_REQUEST_CODE = 1001; // Unique request code

    // Define the desired SSID and BSSID
    String desiredSSID = "SECE-TRAINING";
    String desiredBSSID = "30:de:4b:1f:f1:3e"; // Replace with your desired BSSID

    // Initialize Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference attendanceRef = database.getReference("attendance");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request both ACCESS_WIFI_STATE and ACCESS_FINE_LOCATION permissions
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are granted, continue with the app initialization
            initializeApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissions are granted, continue with the app initialization
                initializeApp();
            } else {
                // Permissions are not granted, display a message or handle as needed
                Log.d("MainActivity", "Permissions not granted");
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeApp() {
        button = findViewById(R.id.scanner);


        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("MainActivity", "Scan button clicked");

                // Check if connected to the desired Wi-Fi network
                String connectedSSID = getConnectedSSID();
                String connectedBSSID = getConnectedBSSID();
                Log.d("MainActivity", "Connected SSID: " + connectedSSID);
                Log.d("MainActivity", "Connected BSSID: " + connectedBSSID);
                Log.d("MainActivity", "Desired SSID: " + desiredSSID);
                Log.d("MainActivity", "Desired BSSID: " + desiredBSSID);

                if (connectedSSID != null && connectedSSID.equals(desiredSSID) &&
                        connectedBSSID != null && connectedBSSID.equals(desiredBSSID)) {
                    // Proceed with QR code scanning
                    startQRCodeScan();
                } else {
                    // Device is not connected to the desired Wi-Fi network, display a message
                    Log.d("MainActivity", "Device is not connected to the desired Wi-Fi network");
                    Toast.makeText(MainActivity.this, "Device is not connected to the desired Wi-Fi network", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private String getConnectedSSID() {
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                // Remove surrounding quotes from SSID
                return wifiInfo.getSSID().replace("\"", "");
            }
        }
        return "Unknown";
    }

    private String getConnectedBSSID() {
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getBSSID() != null) {
                return wifiInfo.getBSSID();
            }
        }
        return "Unknown";
    }

    private void startQRCodeScan() {
        Log.d("MainActivity", "Starting QR code scan...");
        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
        intentIntegrator.setOrientationLocked(true);
        intentIntegrator.setCaptureActivity(PortraitCaptureActivity.class);
        intentIntegrator.setPrompt("Scan a QR Code");
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        intentIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (intentResult != null) {
                String scannedData = intentResult.getContents();
                String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                Log.d("QRCodeData", "Scanned Data: " + scannedData);
                Log.d("QRCodeData", "Device ID: " + deviceID);

                if (scannedData != null && scannedData.startsWith(secretKey)) {
                    String[] parts = scannedData.split("\\+");
                    if (parts.length == 4 && parts[0].equals(secretKey)) {
                        String studentID = parts[2];
                        String studentName = parts[3];
                        String scannedDeviceID = parts[1];

                        if (scannedDeviceID.equals(deviceID)) {
                            markAttendance(studentID, studentName);
                        } else {
                            Log.d("MainActivity", "Invalid QR Code: Device ID mismatch");
                            Toast.makeText(this, "Invalid QR Code: Device ID mismatch", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("MainActivity", "Invalid QR Code: Incorrect format");
                        Toast.makeText(this, "Invalid QR Code: Incorrect format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("MainActivity", "Invalid QR Code");
                    Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private void markAttendance(String studentID, String studentName) {
        // Get the current time in hours and minutes
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR); // Use HOUR for 12-hour format
        int am_pm = calendar.get(Calendar.AM_PM); // Check AM/PM to adjust currentHour for 24-hour format

        // Adjust current hour for 24-hour format if PM
        if (am_pm == Calendar.PM) {
            currentHour += 12;
        }

        int currentMinute = calendar.get(Calendar.MINUTE);

        // Define start times and end times for each period
        int[] periodStartHours = {7, 9, 10, 11, 13, 14, 15}; // Start hours of each period
        int[] periodStartMinutes = {40, 35, 30, 45, 40, 30, 20}; // Start minutes of each period
        int[] periodEndHours = {9, 10, 11, 12, 14, 15, 16}; // End hours of each period
        int[] periodEndMinutes = {35, 30, 25, 40, 30, 20, 0}; // End minutes of each period

        final int[] currentPeriod = {0}; // Declared as final array to effectively make it final

        // Loop through each period and check if current time falls within its range
        for (int i = 0; i < periodStartHours.length; i++) {
            int periodStartHour = periodStartHours[i];
            int periodStartMinute = periodStartMinutes[i];
            int periodEndHour = periodEndHours[i];
            int periodEndMinute = periodEndMinutes[i];

            // Check if current time falls within the range of the current period
            if ((currentHour > periodStartHour || (currentHour == periodStartHour && currentMinute >= periodStartMinute)) &&
                    (currentHour < periodEndHour || (currentHour == periodEndHour && currentMinute < periodEndMinute))) {
                currentPeriod[0] = i + 1; // Period index starts from 1, so increment by 1
                break; // Exit the loop once the current period is found
            }
        }

        if (currentPeriod[0] == 0) {
            Log.d("MainActivity", "No period at this time");
            Toast.makeText(this, "No period at this time", Toast.LENGTH_SHORT).show();
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            DatabaseReference studentRef = attendanceRef.child(currentDate).child(studentID);
            studentRef.child("name").setValue(studentName);
            studentRef.child("period" + currentPeriod[0]).setValue("Present")
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("MainActivity", "Attendance marked for Student ID: " + studentID + ", Name: " + studentName + ", Period: " + currentPeriod[0]);
                            Toast.makeText(MainActivity.this, "Attendance marked for period " + currentPeriod[0], Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("MainActivity", "Failed to mark attendance: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
