package com.example.myapplication;

import android.app.ProgressDialog;
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

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        // Find views by their IDs
        full_Name = findViewById(R.id.editTextFullName);
        email_address = findViewById(R.id.editTextEmail);
        passWord = findViewById(R.id.editTextPassword);
        rePassword = findViewById(R.id.editTextConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing up...");

        // Handle window insets for edge-to-edge design
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Signup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set click listener for the sign-up button
        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Get input values
        String stringFull_Name = full_Name.getText().toString().trim();
        String stringEmail = email_address.getText().toString().trim();
        String stringPassword = passWord.getText().toString().trim();
        String stringRe_Password = rePassword.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(stringFull_Name)) {
            full_Name.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(stringEmail) || !Patterns.EMAIL_ADDRESS.matcher(stringEmail).matches()) {
            email_address.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(stringPassword) || stringPassword.length() < 6) {
            passWord.setError("Password must be at least 6 characters");
            return;
        }
        if (!stringPassword.equals(stringRe_Password)) {
            rePassword.setError("Passwords do not match");
            return;
        }

        // Show progress dialog
        progressDialog.show();

        // Check if email is already registered
        firebaseAuth.fetchSignInMethodsForEmail(stringEmail)
                .addOnCompleteListener(task -> {
                    if (!Objects.requireNonNull(task.getResult().getSignInMethods()).isEmpty()) {
                        email_address.setError("Email already exists. Please log in.");
                        progressDialog.dismiss();
                    } else {
                        // Proceed with account creation
                        createFirebaseUser(stringEmail, stringPassword, stringFull_Name);
                    }
                });
    }

    // Separate function to create Firebase user
    private void createFirebaseUser(String email, String password, String fullName) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d("SignUp_Page", "User created in Firebase Auth: " + firebaseUser.getUid());

                            // Send verification email
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(SignUp_Page.this, "Verification email sent. Please check your email.", Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.e("SignUp_Page", "Email verification failed: " + task1.getException().getMessage());
                                        }
                                    });

                            // Save user data (WITHOUT PASSWORD) to Firestore
                            saveDataToFirestore(firebaseUser.getUid(), fullName, email);
                        }
                    } else {
                        progressDialog.dismiss();
                        Log.e("SignUp_Page", "Firebase Auth failed: " + task.getException().getMessage());
                        Toast.makeText(SignUp_Page.this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Save user data to Firestore
    private void saveDataToFirestore(String userId, String fullName, String email) {
        // Create a Map to store user data
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("userId", userId);

        Log.d("SignUp_Page", "Attempting to save data to Firestore: " + user.toString());

        // Save data to Firestore
        database.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Log.d("SignUp_Page", "Data saved to Firestore successfully");
                    Toast.makeText(SignUp_Page.this, "Sign-up successful!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SignUp_Page", "Error saving data to Firestore: " + e.getMessage());
                    Toast.makeText(SignUp_Page.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
