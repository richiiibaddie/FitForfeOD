package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setContentView(R.layout.activity_main);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Button loginButton = findViewById(R.id.logbutt);
        Button signupButton = findViewById(R.id.button2);
        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView subtitleText = findViewById(R.id.subtitleText);

        // Set button click listeners
        loginButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Login_Page1.class)));
        signupButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignUp_Page.class)));

        // Apply Gradient Effect to Welcome Text
        applyGradientEffect(welcomeText, new int[]{Color.RED, Color.YELLOW});

        // Apply Gradient Effect to Subtitle Text
        applyGradientEffect(subtitleText, new int[]{Color.BLUE, Color.CYAN});
    }

    // Method to apply gradient effect to TextView
    private void applyGradientEffect(TextView textView, int[] colors) {
        if (textView != null) {
            TextPaint paint = textView.getPaint();
            Shader textShader = new LinearGradient(
                    0, 0, 0, textView.getTextSize(),
                    colors,
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(textShader);
        }
    }
}
