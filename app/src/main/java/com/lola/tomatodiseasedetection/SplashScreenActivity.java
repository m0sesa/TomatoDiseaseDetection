package com.lola.tomatodiseasedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The layout is to display app info
        setContentView(R.layout.activity_splash_sceen);

        // Start main activity
        loadLoginPageWithDelay();
    }

    private void loadLoginPageWithDelay(){
        new Handler().postDelayed(
                ()-> {
                    startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
                    finish();
                }
                , 1525
        );
    }
}
