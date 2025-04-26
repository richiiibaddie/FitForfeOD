package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Gender extends AppCompatActivity {

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.gender);

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // A user is signed in
            String userId = currentUser.getUid(); // Get the user's unique ID
            String userEmail = currentUser.getEmail(); // Get the user's email
            assert userEmail != null;
            Log.d("current user", userEmail);
        } else {
            // No user is signed in
            Log.d("UserStatus", "No user is signed in.");
        }


        Button Male = findViewById(R.id.bttnm);
        Male.setOnClickListener(v -> startActivity(new Intent(Gender.this, Navdraw.class)));

    }
}
