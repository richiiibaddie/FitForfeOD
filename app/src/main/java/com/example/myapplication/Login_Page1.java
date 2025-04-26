package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils for checking empty strings
import android.util.Patterns; // Import Patterns for email validation
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log; // Import Log for better debugging

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
    private static final String TAG = "Login_Page1"; // TAG for logging

    private FirebaseAuth firebaseAuth;
    private EditText userInputUsername; // Changed to Email for clarity
    private EditText userInputPassword;
    private Button loginButton;
    private TextView forgotPassword;
    private TextView signupLink; // Added for potential "Don't have an account?" link

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login); // Make sure layout name is 'login.xml'

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Find Views
        findingViewById();

        // Setup Forgot Password Link
        forgotPassword.setOnClickListener(v -> {
            // Implement forgot password logic (e.g., start ForgotPasswordActivity)
            Toast.makeText(Login_Page1.this, "Forgot Password Clicked (Implement Logic)", Toast.LENGTH_SHORT).show();
            // Example: startActivity(new Intent(Login_Page1.this, ForgotPasswordActivity.class));
        });

        // Setup Login Button Listener
        loginButton.setOnClickListener(v -> attemptLogin());

        // Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Optional: Add a link to SignUp page if not handled elsewhere
        // signupLink = findViewById(R.id.signupLink); // Assuming you add this TextView in login.xml
        // if (signupLink != null) {
        //     signupLink.setOnClickListener(v -> {
        //         startActivity(new Intent(Login_Page1.this, SignUp_Page.class));
        //         finish(); // Optional: finish login activity when going to sign up
        //      });
        // }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null){
            Log.d(TAG, "User already logged in, redirecting to Navdraw.");
            // User is already logged in, redirect to main activity (Navdraw)
            startActivity(new Intent(Login_Page1.this, Navdraw.class));
            finish(); // *** ADDED: Close Login page so user can't navigate back to it ***
        } else {
            Log.d(TAG, "No user logged in.");
            // No user is logged in, stay on login page.
        }
    }

    private void findingViewById() {
        userInputUsername = findViewById(R.id.editTextTextEmailAddress);
        userInputPassword = findViewById(R.id.pword);
        loginButton = findViewById(R.id.loginbt);
        forgotPassword = findViewById(R.id.forgotPass);
        // Make sure the IDs match your login.xml
    }

    private void attemptLogin() {
        // Reset errors
        userInputUsername.setError(null);
        userInputPassword.setError(null);

        // Store values at the time of the login attempt.
        String email = userInputUsername.getText().toString().trim();
        String password = userInputPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            userInputPassword.setError(getString(R.string.error_field_required)); // Use string resources
            focusView = userInputPassword;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            userInputPassword.setError(getString(R.string.error_invalid_password)); // Use string resources
            focusView = userInputPassword;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            userInputUsername.setError(getString(R.string.error_field_required)); // Use string resources
            focusView = userInputUsername;
            cancel = true;
        } else if (!isEmailValid(email)) {
            userInputUsername.setError(getString(R.string.error_invalid_email)); // Use string resources
            focusView = userInputUsername;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            // TODO: Show Progress Dialog/Bar
            Log.d(TAG, "Attempting Firebase sign in for email: " + email);
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // TODO: Hide Progress Dialog/Bar
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            // Check if email is verified (optional but recommended)
                            if (user != null && user.isEmailVerified()) {
                                Log.d(TAG, "Email verified. Navigating to Navdraw.");
                                startActivity(new Intent(Login_Page1.this, Navdraw.class));
                                finish(); // Close login activity
                            } else if (user != null) {
                                // Email not verified
                                Log.w(TAG, "signInWithEmail:success but email not verified.");
                                Toast.makeText(Login_Page1.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                                // Optionally sign the user out until they verify
                                // firebaseAuth.signOut();
                                resetUI(); // Keep user on login page or clear fields
                            } else {
                                // This case should ideally not happen if task is successful, but good to handle
                                Log.e(TAG, "signInWithEmail:success but user is null");
                                Toast.makeText(Login_Page1.this, "Authentication successful but failed to get user.", Toast.LENGTH_SHORT).show();
                                resetUI();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(Login_Page1.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show(); // Show more specific error
                            resetUI();
                        }
                    });
        }
    }

    // More robust email validation
    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Basic password validation (adjust as needed)
    private boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 8; // Example: At least 8 chars
    }

    private void resetUI() {
        // You might want to clear only the password field for convenience
        // userInputUsername.setText("");
        userInputPassword.setText("");
        userInputUsername.requestFocus(); // Set focus back to email field
    }
}