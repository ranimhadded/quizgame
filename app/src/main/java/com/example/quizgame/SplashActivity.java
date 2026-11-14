package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Wait a moment then open main screen
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this,activity_signin.class));
            finish();
        }, 2000); // 2 seconds
    }
}