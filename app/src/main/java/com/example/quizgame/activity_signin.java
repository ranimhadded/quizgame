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

public class activity_signin extends AppCompatActivity {

    private EditText emailEt;
    private EditText passwordEt;
    private Button loginBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView forgotPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        emailEt = findViewById(R.id.login_email);
        passwordEt = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.passwordreset);
        // CardView padding for nav bars
        View card = findViewById(R.id.card_signin);
        ViewCompat.setOnApplyWindowInsetsListener(card, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        // Signup redirect
        TextView signupRedirect = findViewById(R.id.signupRedirectText);
        signupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(activity_signin.this, SignUpActivity.class));
        });

        // Login button click
        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            String uid = mAuth.getCurrentUser().getUid();

                            // Fetch username from Firestore
                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if(documentSnapshot.exists()){
                                            String username = documentSnapshot.getString("username");

                                            // Pass username to MainActivity
                                            Intent intent = new Intent(this, MainActivity.class);
                                            intent.putExtra("username", username);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to get username: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(this, "Login failed: "+task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        // Forgot Password click
        forgotPassword.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();

            if(email.isEmpty()){
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send password reset email
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}

