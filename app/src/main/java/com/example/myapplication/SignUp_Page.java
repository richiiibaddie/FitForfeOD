package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent; // Import Intent for navigation
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUp_Page extends AppCompatActivity {
    private static final String TAG = "SignUp_Page"; // TAG for logging

    EditText full_Name, email_address, passWord, rePassword;
    FirebaseFirestore database;
    FirebaseAuth firebaseAuth;
    ProgressDialog progressDialog;
    Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup); // Ensure layout name is 'signup.xml'

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        // Find views by their IDs
        full_Name = findViewById(R.id.editTextFullName);
        email_address = findViewById(R.id.editTextEmail);
        passWord = findViewById(R.id.editTextPassword);
        rePassword = findViewById(R.id.editTextConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        // Ensure these IDs match your signup.xml layout

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Signing Up"); // Set a title
        progressDialog.setMessage("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false); // Prevent dismissing by tapping outside

        // Handle window insets for edge-to-edge design
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Signup), (v, insets) -> { // Ensure R.id.Signup is the root layout ID in signup.xml
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set click listener for the sign-up button
        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Reset errors
        full_Name.setError(null);
        email_address.setError(null);
        passWord.setError(null);
        rePassword.setError(null);

        // Get input values
        String stringFull_Name = full_Name.getText().toString().trim();
        String stringEmail = email_address.getText().toString().trim();
        String stringPassword = passWord.getText().toString().trim();
        String stringRe_Password = rePassword.getText().toString().trim();

        // --- Input Validation ---
        boolean cancel = false;
        android.view.View focusView = null;

        if (TextUtils.isEmpty(stringFull_Name)) {
            full_Name.setError("Full name is required");
            focusView = full_Name;
            cancel = true;
        }
        if (TextUtils.isEmpty(stringEmail)) {
            email_address.setError("Email is required");
            focusView = focusView == null ? email_address : focusView; // Keep first error focus
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(stringEmail).matches()) {
            email_address.setError("Enter a valid email address");
            focusView = focusView == null ? email_address : focusView;
            cancel = true;
        }
        if (TextUtils.isEmpty(stringPassword)) {
            passWord.setError("Password is required");
            focusView = focusView == null ? passWord : focusView;
            cancel = true;
        } else if (stringPassword.length() < 8) { // Increased minimum length to 8 for better security
            passWord.setError("Password must be at least 8 characters");
            focusView = focusView == null ? passWord : focusView;
            cancel = true;
        }
        if (TextUtils.isEmpty(stringRe_Password)) {
            rePassword.setError("Please confirm your password");
            focusView = focusView == null ? rePassword : focusView;
            cancel = true;
        } else if (!stringPassword.equals(stringRe_Password)) {
            rePassword.setError("Passwords do not match");
            // *** THIS IS THE CORRECTED LINE ***
            focusView = focusView == null ? rePassword : focusView;
            // ***********************************
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt registration and focus the first form field with an error.
            if (focusView != null) {
                focusView.requestFocus();
            }
            return; // Stop the registration process
        }
        // --- End Validation ---


        // Show progress dialog
        progressDialog.show();

        // --- Registration Process ---
        // 1. Check if email already exists (using fetchSignInMethodsForEmail is good practice)
        firebaseAuth.fetchSignInMethodsForEmail(stringEmail)
                .addOnCompleteListener(task -> {
                    boolean isNewUser = task.isSuccessful() && task.getResult().getSignInMethods().isEmpty();

                    if (!isNewUser) {
                        progressDialog.dismiss();
                        email_address.setError("Email already registered. Please log in or use a different email.");
                        email_address.requestFocus();
                        Log.w(TAG, "Email check failed or email exists.", task.getException());
                    } else {
                        // 2. Email is not registered, proceed with creating the user account
                        Log.d(TAG, "Email is new. Proceeding with account creation.");
                        createFirebaseUser(stringEmail, stringPassword, stringFull_Name);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure in checking email existence (e.g., network error)
                    progressDialog.dismiss();
                    Log.e(TAG, "Error checking email existence: " + e.getMessage());
                    Toast.makeText(SignUp_Page.this, "Error checking email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Method to create the user in Firebase Auth
    private void createFirebaseUser(String email, String password, String fullName) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Added 'this' for activity context if needed
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            // 3. Send Verification Email (Recommended)
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d(TAG,"Verification email sent to " + firebaseUser.getEmail());
                                            // Toast is now shown after data save
                                        } else {
                                            Log.e(TAG, "sendEmailVerification failed", task1.getException());
                                            Toast.makeText(SignUp_Page.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            // 4. Save user data to Firestore (this happens asynchronously)
                            saveDataToFirestore(firebaseUser.getUid(), fullName, email, firebaseUser); // Pass user to sign out later

                        } else {
                            // This case should not happen if task is successful, but handle defensively
                            progressDialog.dismiss();
                            Log.e(TAG, "createUserWithEmail:success, but firebaseUser is null");
                            Toast.makeText(SignUp_Page.this, "Sign-up succeeded but failed to get user details.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // If sign up fails, display a message to the user.
                        progressDialog.dismiss();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        // Provide more specific error messages if possible
                        Toast.makeText(SignUp_Page.this, "Sign-up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Method to save user data to Firestore
    private void saveDataToFirestore(String userId, String fullName, String email, FirebaseUser userToSignOut) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("userId", userId);


        Log.d(TAG, "Attempting to save data to Firestore for userId: " + userId);

        database.collection("Users").document(userId) // Use "Users" consistently
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Firestore.");
                    progressDialog.dismiss();
                    Toast.makeText(SignUp_Page.this, "Registration successful! Please check your email for verification and log in.", Toast.LENGTH_LONG).show(); // Combined messages


                    Log.d(TAG, "Signing out user after successful registration and data save.");
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(SignUp_Page.this, Login_Page1.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack fully
                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error writing user data to Firestore", e);
                    Toast.makeText(SignUp_Page.this, "Registration succeeded but failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();

                });
    }


    private void navigateToLogin() {
        Intent intent = new Intent(SignUp_Page.this, Login_Page1.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}