package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private FirebaseAuth firebaseAuth;
    String stringEmail;
    String stringPassword;
    EditText userInputUsername;
    EditText userInputPassword;
    Button loginButton;
    TextView forgotPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);
        forgotPassword = findViewById(R.id.forgotPass);
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login_Page1.this, SignUp_Page.class));
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        findingViewById();
        loginButton = findViewById(R.id.loginbt);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stringEmail = Objects.requireNonNull(userInputUsername.getText().toString());
                stringPassword = Objects.requireNonNull(userInputPassword.getText().toString());
                isValidData();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            startActivity(new Intent(Login_Page1.this, Navdraw.class));
        }
    }

    private void isValidData() {

        if(!isValidEmail(stringEmail)) {
            userInputUsername.setError("The provided email address is invalid");
        }

        if(stringPassword.isEmpty()) {
            userInputPassword.setError("Please enter your password");
        }

        if(stringPassword.length() < 8) {
            userInputPassword.setError("Please enter a password with a valid length");
        }


        firebaseAuth.signInWithEmailAndPassword(stringEmail, stringPassword).addOnCompleteListener(Login_Page1.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if(user != null && user.isEmailVerified()) {
                        startActivity(new Intent(Login_Page1.this, Navdraw.class));
                        finish();
                    } else {
                        Toast.makeText(Login_Page1.this, "Verification of your email is required", Toast.LENGTH_SHORT).show();
                        resetUI();
                    }
                } else {
                    Toast.makeText(Login_Page1.this, "Authentication failed. Please check your credentials or Internet connection", Toast.LENGTH_SHORT).show();
                    resetUI();
                }
            }
        });
    }
    private void findingViewById() {
        userInputUsername = findViewById(R.id.editTextTextEmailAddress);
        userInputPassword = findViewById(R.id.pword);
    }
    private void resetUI() {
        userInputUsername.setText("");
        userInputPassword.setText("");
    }
}
