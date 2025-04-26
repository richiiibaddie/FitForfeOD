package com.example.myapplication.ui.gallery;

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

import com.example.myapplication.MainActivity;

import com.example.myapplication.databinding.FragmentGalleryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;


public class GalleryFragment extends Fragment {

    private static final String TAG = "GalleryFragment";
    private FragmentGalleryBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUser != null) {

            userDocRef = db.collection("Users").document(currentUser.getUid());
            loadUserData();
            setupClickListeners();
        } else {

            Log.e(TAG, "User is not logged in!");
            Toast.makeText(getContext(), "Error: User not logged in.", Toast.LENGTH_SHORT).show();

            navigateToLogin();
        }
    }

    private void loadUserData() {

        binding.textViewEmailValue.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");


        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "Firestore data found for user: " + currentUser.getUid());
                String name = documentSnapshot.getString("name");
                String gender = documentSnapshot.getString("gender");
                Number currentWeightNum = (Number) documentSnapshot.get("currentWeight");
                Number targetWeightNum = (Number) documentSnapshot.get("targetWeight");

                String currentWeight = (currentWeightNum != null) ? String.valueOf(currentWeightNum) : "N/A";
                String targetWeight = (targetWeightNum != null) ? String.valueOf(targetWeightNum) : "N/A";


                binding.textViewUsername.setText(name != null ? name : "User");
                binding.textViewFullnameValue.setText(name != null ? name : "N/A");
                binding.textViewGenderValue.setText(gender != null ? gender : "N/A");
                binding.textViewCurrentWeightValue.setText(currentWeight);
                binding.textViewTargetWeightValue.setText(targetWeight);

            } else {
                Log.w(TAG, "No Firestore data found for user: " + currentUser.getUid());

                binding.textViewUsername.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User"); // Fallback display name
                binding.textViewFullnameValue.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Not Set");
                binding.textViewGenderValue.setText("Not Set");
                binding.textViewCurrentWeightValue.setText("Not Set");
                binding.textViewTargetWeightValue.setText("Not Set");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading Firestore data", e);
            Toast.makeText(getContext(), "Error loading profile data.", Toast.LENGTH_SHORT).show();

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
            Toast.makeText(getContext(), "Changing email requires re-authentication (Not implemented here).", Toast.LENGTH_LONG).show();
        });

        binding.buttonLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked.");
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
    }


    private void showInputDialog(String title, String fieldKey) {
        showInputDialog(title, fieldKey, false);
    }

    private void showInputDialog(String title, String fieldKey, boolean isNumeric) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change " + title);


        final EditText input = new EditText(getContext());
        if (isNumeric) {
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }
        builder.setView(input);
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


        userDocRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, fieldKey + " updated successfully in Firestore.");
                    Toast.makeText(getContext(), fieldKey + " updated.", Toast.LENGTH_SHORT).show();
                    loadUserData();

                    if ("name".equals(fieldKey) && currentUser != null) {
                        updateFirebaseAuthProfileName(value);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating " + fieldKey + " in Firestore", e);
                    Toast.makeText(getContext(), "Error updating " + fieldKey + ".", Toast.LENGTH_SHORT).show();
                });
    }

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

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        } else {
            Log.e(TAG, "Cannot navigate to login, getActivity() is null");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}