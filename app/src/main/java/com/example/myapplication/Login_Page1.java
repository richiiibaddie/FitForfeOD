package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Login_Page1 extends AppCompatActivity {
    private static final String TAG = "Login_Page1";

    private FirebaseAuth firebaseAuth;
    private EditText userInputUsername;
    private EditText userInputPassword;
    private Button loginButton;
    private TextView forgotPassword;
    private TextView signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        firebaseAuth = FirebaseAuth.getInstance();

        findingViewById();

        forgotPassword.setOnClickListener(v -> {

            Toast.makeText(Login_Page1.this, "Forgot Password Clicked (Implement Logic)", Toast.LENGTH_SHORT).show();

        });

        loginButton.setOnClickListener(v -> attemptLogin());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null){
            Log.d(TAG, "User already logged in, redirecting to Navdraw.");

            startActivity(new Intent(Login_Page1.this, Navdraw.class));
            finish();
        } else {
            Log.d(TAG, "No user logged in.");

        }
    }

    private void findingViewById() {
        userInputUsername = findViewById(R.id.editTextTextEmailAddress);
        userInputPassword = findViewById(R.id.pword);
        loginButton = findViewById(R.id.loginbt);
        forgotPassword = findViewById(R.id.forgotPass);

    }

    private void attemptLogin() {

        userInputUsername.setError(null);
        userInputPassword.setError(null);

        String email = userInputUsername.getText().toString().trim();
        String password = userInputPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            userInputPassword.setError(getString(R.string.error_field_required));
            focusView = userInputPassword;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            userInputPassword.setError(getString(R.string.error_invalid_password));
            focusView = userInputPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            userInputUsername.setError(getString(R.string.error_field_required));
            focusView = userInputUsername;
            cancel = true;
        } else if (!isEmailValid(email)) {
            userInputUsername.setError(getString(R.string.error_invalid_email));
            focusView = userInputUsername;
            cancel = true;
        }

        if (cancel) {

            focusView.requestFocus();
        } else {

            Log.d(TAG, "Attempting Firebase sign in for email: " + email);
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            if (user != null && user.isEmailVerified()) {
                                Log.d(TAG, "Email verified. Navigating to Navdraw.");
                                startActivity(new Intent(Login_Page1.this, Navdraw.class));
                                finish();
                            } else if (user != null) {

                                Log.w(TAG, "signInWithEmail:success but email not verified.");
                                Toast.makeText(Login_Page1.this, "Please verify your email address.", Toast.LENGTH_LONG).show();

                                resetUI();
                            } else {

                                Log.e(TAG, "signInWithEmail:success but user is null");
                                Toast.makeText(Login_Page1.this, "Authentication successful but failed to get user.", Toast.LENGTH_SHORT).show();
                                resetUI();
                            }
                        } else {

                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(Login_Page1.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            resetUI();
                        }
                    });
        }
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 8;
    }

    private void resetUI() {

        userInputPassword.setText("");
        userInputUsername.requestFocus();
    }
}