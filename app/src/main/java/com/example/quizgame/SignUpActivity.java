package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private EditText signupUserName;
    private EditText signupEmail;
    private EditText signupPassword;
    private Button signupButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        signupUserName = findViewById(R.id.login_username);
        signupEmail = findViewById(R.id.login_email);
        signupPassword = findViewById(R.id.login_password);
        signupButton = findViewById(R.id.login_button);

        // Handle CardView padding for nav bars
        View card = findViewById(R.id.card_signup);
        ViewCompat.setOnApplyWindowInsetsListener(card, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        // SignUp redirect to login
        TextView signupRedirect = findViewById(R.id.signupRedirectText);
        signupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, activity_signin.class));
        });

        // Signup button click
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String username = signupUserName.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty() || username.isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user in Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            // Save username in Firestore under user UID
                            String uid = mAuth.getCurrentUser().getUid();
                            Map<String, Object> data = new HashMap<>();
                            data.put("username", username);
                            data.put("email", email);

                            db.collection("users").document(uid)
                                    .set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, activity_signin.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save username: "+e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(this, "Signup failed: "+task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
