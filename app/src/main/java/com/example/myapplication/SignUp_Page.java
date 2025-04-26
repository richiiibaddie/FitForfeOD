package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
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
    private static final String TAG = "SignUp_Page";

    EditText full_Name, email_address, passWord, rePassword;
    FirebaseFirestore database;
    FirebaseAuth firebaseAuth;
    ProgressDialog progressDialog;
    Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        full_Name = findViewById(R.id.editTextFullName);
        email_address = findViewById(R.id.editTextEmail);
        passWord = findViewById(R.id.editTextPassword);
        rePassword = findViewById(R.id.editTextConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Signing Up");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Signup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        full_Name.setError(null);
        email_address.setError(null);
        passWord.setError(null);
        rePassword.setError(null);

        String stringFull_Name = full_Name.getText().toString().trim();
        String stringEmail = email_address.getText().toString().trim();
        String stringPassword = passWord.getText().toString().trim();
        String stringRe_Password = rePassword.getText().toString().trim();

        boolean cancel = false;
        android.view.View focusView = null;

        if (TextUtils.isEmpty(stringFull_Name)) {
            full_Name.setError("Full name is required");
            focusView = full_Name;
            cancel = true;
        }
        if (TextUtils.isEmpty(stringEmail)) {
            email_address.setError("Email is required");
            focusView = focusView == null ? email_address : focusView;
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
        } else if (stringPassword.length() < 8) {
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

            focusView = focusView == null ? rePassword : focusView;

            cancel = true;
        }

        if (cancel) {

            if (focusView != null) {
                focusView.requestFocus();
            }
            return;
        }

        progressDialog.show();

        firebaseAuth.fetchSignInMethodsForEmail(stringEmail)
                .addOnCompleteListener(task -> {
                    boolean isNewUser = task.isSuccessful() && task.getResult().getSignInMethods().isEmpty();

                    if (!isNewUser) {
                        progressDialog.dismiss();
                        email_address.setError("Email already registered. Please log in or use a different email.");
                        email_address.requestFocus();
                        Log.w(TAG, "Email check failed or email exists.", task.getException());
                    } else {

                        Log.d(TAG, "Email is new. Proceeding with account creation.");
                        createFirebaseUser(stringEmail, stringPassword, stringFull_Name);
                    }
                })
                .addOnFailureListener(e -> {

                    progressDialog.dismiss();
                    Log.e(TAG, "Error checking email existence: " + e.getMessage());
                    Toast.makeText(SignUp_Page.this, "Error checking email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createFirebaseUser(String email, String password, String fullName) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {

                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d(TAG,"Verification email sent to " + firebaseUser.getEmail());

                                        } else {
                                            Log.e(TAG, "sendEmailVerification failed", task1.getException());
                                            Toast.makeText(SignUp_Page.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            saveDataToFirestore(firebaseUser.getUid(), fullName, email, firebaseUser);

                        } else {

                            progressDialog.dismiss();
                            Log.e(TAG, "createUserWithEmail:success, but firebaseUser is null");
                            Toast.makeText(SignUp_Page.this, "Sign-up succeeded but failed to get user details.", Toast.LENGTH_LONG).show();
                        }
                    } else {

                        progressDialog.dismiss();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());

                        Toast.makeText(SignUp_Page.this, "Sign-up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveDataToFirestore(String userId, String fullName, String email, FirebaseUser userToSignOut) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("userId", userId);

        Log.d(TAG, "Attempting to save data to Firestore for userId: " + userId);

        database.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Firestore.");
                    progressDialog.dismiss();
                    Toast.makeText(SignUp_Page.this, "Registration successful! Please check your email for verification and log in.", Toast.LENGTH_LONG).show();

                    Log.d(TAG, "Signing out user after successful registration and data save.");
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(SignUp_Page.this, Login_Page1.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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