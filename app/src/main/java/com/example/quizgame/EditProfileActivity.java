package com.example.quizgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editEmail, editPassword;
    private ImageView profileImage;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        // Views
        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        profileImage = findViewById(R.id.profileImage);

        // Email should not be edited
        editEmail.setEnabled(false);

        // Load existing data
        loadUserData();

        // Change picture
        profileImage.setOnClickListener(v -> openGallery());

        // Save
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
    }

    // 🔄 Load user data
    private void loadUserData() {
        if (currentUser == null) return;

        editEmail.setText(currentUser.getEmail());

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editUsername.setText(doc.getString("username"));

                        String imageUriString = doc.getString("imageUri");
                        if (imageUriString != null) {
                            imageUri = Uri.parse(imageUriString);
                            profileImage.setImageURI(imageUri);
                        }
                    }
                });
    }

    // 📸 Open gallery
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    profileImage.setImageURI(imageUri);
                }
            });

    // ✅ Save profile
    private void saveProfile() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editUsername.setError("Username required");
            return;
        }

        if (!TextUtils.isEmpty(password) && password.length() < 6) {
            editPassword.setError("Min 6 characters");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);

        if (imageUri != null) {
            updates.put("imageUri", imageUri.toString());
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {

                    if (!TextUtils.isEmpty(password)) {
                        currentUser.updatePassword(password);
                    }

                    showSuccessDialog();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                );
    }

    // 🎉 Success dialog
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Profile updated successfully")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }
}
