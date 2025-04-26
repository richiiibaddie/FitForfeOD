package com.example.myapplication.ui.gallery; // Make sure this package matches your project structure

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.MainActivity; // Or your initial Activity after logout
import com.example.myapplication.R; // Import your R class
import com.example.myapplication.databinding.FragmentGalleryBinding; // Import generated binding class
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // To merge data

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GalleryFragment extends Fragment {

    private static final String TAG = "GalleryFragment";
    private FragmentGalleryBinding binding; // View Binding instance
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout using View Binding
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUser != null) {
            // Get reference to the user's document in Firestore
            userDocRef = db.collection("Users").document(currentUser.getUid());
            loadUserData();
            setupClickListeners();
        } else {
            // Handle case where user is somehow not logged in when accessing this screen
            Log.e(TAG, "User is not logged in!");
            Toast.makeText(getContext(), "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            // Optional: Navigate back to login screen
            navigateToLogin();
        }
    }

    private void loadUserData() {
        // Load email from FirebaseAuth
        binding.textViewEmailValue.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");

        // Load other data from Firestore
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "Firestore data found for user: " + currentUser.getUid());
                String name = documentSnapshot.getString("name");
                String gender = documentSnapshot.getString("gender");
                // Use getDouble() or getLong() depending on how you store weights
                Number currentWeightNum = (Number) documentSnapshot.get("currentWeight");
                Number targetWeightNum = (Number) documentSnapshot.get("targetWeight");

                String currentWeight = (currentWeightNum != null) ? String.valueOf(currentWeightNum) : "N/A";
                String targetWeight = (targetWeightNum != null) ? String.valueOf(targetWeightNum) : "N/A";


                binding.textViewUsername.setText(name != null ? name : "User"); // Update top username
                binding.textViewFullnameValue.setText(name != null ? name : "N/A");
                binding.textViewGenderValue.setText(gender != null ? gender : "N/A");
                binding.textViewCurrentWeightValue.setText(currentWeight);
                binding.textViewTargetWeightValue.setText(targetWeight);

            } else {
                Log.w(TAG, "No Firestore data found for user: " + currentUser.getUid());
                // Set default values or prompt user to complete profile
                binding.textViewUsername.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User"); // Fallback display name
                binding.textViewFullnameValue.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Not Set");
                binding.textViewGenderValue.setText("Not Set");
                binding.textViewCurrentWeightValue.setText("Not Set");
                binding.textViewTargetWeightValue.setText("Not Set");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading Firestore data", e);
            Toast.makeText(getContext(), "Error loading profile data.", Toast.LENGTH_SHORT).show();
            // Set default values maybe
            binding.textViewUsername.setText("User");
            binding.textViewFullnameValue.setText("Error");
            binding.textViewGenderValue.setText("Error");
            binding.textViewCurrentWeightValue.setText("Error");
            binding.textViewTargetWeightValue.setText("Error");
        });
    }

    private void setupClickListeners() {
        binding.changeFullname.setOnClickListener(v -> showInputDialog("Full Name", "name"));
        binding.changeGender.setOnClickListener(v -> showInputDialog("Gender", "gender"));
        binding.changeCurrentWeight.setOnClickListener(v -> showInputDialog("Current Weight (kg)", "currentWeight", true)); // Indicate numeric input
        binding.changeTargetWeight.setOnClickListener(v -> showInputDialog("Target Weight (kg)", "targetWeight", true)); // Indicate numeric input

        binding.changeEmail.setOnClickListener(v -> {
            // Changing email is more complex and often requires re-authentication.
            // Usually handled in a separate, dedicated screen/flow.
            Toast.makeText(getContext(), "Changing email requires re-authentication (Not implemented here).", Toast.LENGTH_LONG).show();
            // Example: startActivity(new Intent(getActivity(), ChangeEmailActivity.class));
        });

        binding.buttonLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked.");
            mAuth.signOut(); // Sign out from Firebase
            Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            navigateToLogin(); // Navigate back to Login/Main screen
        });
    }

    // Helper method to show a simple input dialog for changing data
    private void showInputDialog(String title, String fieldKey) {
        showInputDialog(title, fieldKey, false); // Default to non-numeric
    }

    private void showInputDialog(String title, String fieldKey, boolean isNumeric) {
        if (getContext() == null) return; // Prevent crash if context is not available

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change " + title);

        // Set up the input
        final EditText input = new EditText(getContext());
        // Specify the type of input expected
        if (isNumeric) {
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                updateFirestoreField(fieldKey, value, isNumeric);
            } else {
                Toast.makeText(getContext(), "Input cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Helper method to update a specific field in Firestore
    private void updateFirestoreField(String fieldKey, String value, boolean isNumeric) {
        if (userDocRef == null) {
            Log.e(TAG, "userDocRef is null, cannot update field: " + fieldKey);
            Toast.makeText(getContext(), "Error: User reference not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        Object dataValue;

        if (isNumeric) {
            try {
                // Store weights as numbers (Double for flexibility)
                dataValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number format.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid number format for " + fieldKey + ": " + value, e);
                return;
            }
        } else {
            dataValue = value;
        }
        data.put(fieldKey, dataValue);


        userDocRef.set(data, SetOptions.merge()) // Use merge to only update specified fields
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, fieldKey + " updated successfully in Firestore.");
                    Toast.makeText(getContext(), fieldKey + " updated.", Toast.LENGTH_SHORT).show();
                    // Update the UI immediately
                    loadUserData(); // Reload data to show changes

                    // Special handling for 'name' to update Firebase Auth display name too (optional)
                    if ("name".equals(fieldKey) && currentUser != null) {
                        updateFirebaseAuthProfileName(value);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating " + fieldKey + " in Firestore", e);
                    Toast.makeText(getContext(), "Error updating " + fieldKey + ".", Toast.LENGTH_SHORT).show();
                });
    }

    // Optional: Update Firebase Auth display name when name is changed in Firestore
    private void updateFirebaseAuthProfileName(String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth User profile display name updated.");
                    } else {
                        Log.e(TAG, "Failed to update Firebase Auth display name.", task.getException());
                    }
                });
    }


    private void navigateToLogin() {
        if (getActivity() != null) {
            // Change MainActivity.class to your actual Login Activity if different
            Intent intent = new Intent(getActivity(), MainActivity.class);
            // Clear the back stack and start new task
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish(); // Finish the current activity containing the fragment
        } else {
            Log.e(TAG, "Cannot navigate to login, getActivity() is null");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release the binding instance when the view is destroyed
        binding = null;
    }
}