package com.example.current;

import com.journeyapps.barcodescanner.CaptureActivity;

public class PortraitCaptureActivity extends CaptureActivity {

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
