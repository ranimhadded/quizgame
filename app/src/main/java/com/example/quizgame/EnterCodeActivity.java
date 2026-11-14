package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EnterCodeActivity extends AppCompatActivity {

    private static final String TAG = "EnterCodeActivity";

    private EditText answerField;
    private Button joinButton;
    private String currentUserId;
    private String currentUserName;
    private String currentUserEmail;

    private FirebaseDatabase database;
    private DatabaseReference roomsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_code);

        // Initialize Firebase
        try {
            database = FirebaseDatabase.getInstance(
                    "https://quizop-bbadf-default-rtdb.europe-west1.firebasedatabase.app/"
            );
            roomsRef = database.getReference("rooms");
            Log.d(TAG, "Database initialized");
        } catch (Exception e) {
            Log.e(TAG, "Firebase error", e);
            Toast.makeText(this, "Firebase connection error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            currentUserName = currentUser.getDisplayName() != null ?
                    currentUser.getDisplayName() : "Player";
            currentUserEmail = currentUser.getEmail();
            Log.d(TAG, "User authenticated: " + currentUserName);
        } else {
            // If no authenticated user, generate guest credentials
            currentUserId = "guest_" + System.currentTimeMillis();
            currentUserName = "Guest_" + System.currentTimeMillis() % 10000;
            currentUserEmail = null;
            Log.d(TAG, "Guest user created: " + currentUserName);
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("Enter Room Code");
        toolbar.setNavigationOnClickListener(v -> finish());

        answerField = findViewById(R.id.answer_field);
        joinButton = findViewById(R.id.join_button);
    }

    private void setupClickListeners() {
        joinButton.setOnClickListener(v -> {
            String roomCode = answerField.getText().toString().trim();

            if (TextUtils.isEmpty(roomCode)) {
                Toast.makeText(this, "Please enter a room code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (roomCode.length() != 6) {
                Toast.makeText(this, "Room code must be 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button to prevent multiple clicks
            joinButton.setEnabled(false);
            checkRoomExists(roomCode);
        });
    }

    private void checkRoomExists(final String roomCode) {
        Log.d(TAG, "Checking if room exists: " + roomCode);

        DatabaseReference roomRef = roomsRef.child(roomCode);

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Room snapshot exists: " + snapshot.exists());

                if (snapshot.exists()) {
                    // Room exists, check its status
                    String roomStatus = snapshot.child("roomStatus").getValue(String.class);
                    String gameState = snapshot.child("gameState").getValue(String.class);

                    Log.d(TAG, "Room status: " + roomStatus + ", Game state: " + gameState);

                    // Allow joining if room is waiting or if game state is starting/waiting
                    if ("waiting".equals(roomStatus) ||
                            "waiting".equals(gameState) ||
                            gameState == null) {

                        joinRoom(roomCode, snapshot);

                    } else if ("in-progress".equals(roomStatus) ||
                            "starting".equals(gameState) ||
                            "question-active".equals(gameState)) {
                        Toast.makeText(EnterCodeActivity.this,
                                "Game has already started", Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);

                    } else if ("finished".equals(roomStatus) ||
                            "quiz-finished".equals(gameState)) {
                        Toast.makeText(EnterCodeActivity.this,
                                "Game has finished", Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);

                    } else {
                        Toast.makeText(EnterCodeActivity.this,
                                "Room is not available", Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                    }
                } else {
                    Toast.makeText(EnterCodeActivity.this,
                            "Room not found. Check the code and try again.", Toast.LENGTH_SHORT).show();
                    joinButton.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Room check cancelled", error.toException());

                String errorMsg = "Error: ";
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    errorMsg += "Permission denied. Please check Firebase rules.";
                } else {
                    errorMsg += error.getMessage();
                }

                Toast.makeText(EnterCodeActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                joinButton.setEnabled(true);
            }
        });
    }

    private void joinRoom(String roomCode, DataSnapshot roomSnapshot) {
        Log.d(TAG, "Attempting to join room: " + roomCode);

        DatabaseReference roomRef = roomsRef.child(roomCode);
        DatabaseReference playersRef = roomRef.child("players");

        // Check if player is already in the room
        playersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Player already in room");
                    Toast.makeText(EnterCodeActivity.this,
                            "You're already in this room", Toast.LENGTH_SHORT).show();
                    goToJoinRoomActivity(roomCode, roomSnapshot, true);
                } else {
                    // Add player to room
                    addPlayerToRoom(roomCode, roomSnapshot, playersRef);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Player check cancelled", error.toException());
                Toast.makeText(EnterCodeActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                joinButton.setEnabled(true);
            }
        });
    }

    private void addPlayerToRoom(String roomCode, DataSnapshot roomSnapshot,
                                 DatabaseReference playersRef) {
        Log.d(TAG, "Adding player to room: " + currentUserName);

        // Create player data
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("playerId", currentUserId);
        playerData.put("playerName", currentUserName);
        playerData.put("playerEmail", currentUserEmail != null ? currentUserEmail : "");
        playerData.put("isHost", false);
        playerData.put("joinedAt", System.currentTimeMillis());

        playersRef.child(currentUserId).setValue(playerData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Player successfully joined room: " + roomCode);
                    Toast.makeText(EnterCodeActivity.this,
                            "Joined room successfully!", Toast.LENGTH_SHORT).show();
                    goToJoinRoomActivity(roomCode, roomSnapshot, false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join room", e);

                    String errorMsg = "Failed to join: ";
                    if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                        errorMsg += "Permission denied. Check Firebase rules.";
                    } else {
                        errorMsg += e.getMessage();
                    }

                    Toast.makeText(EnterCodeActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    joinButton.setEnabled(true);
                });
    }

    private void goToJoinRoomActivity(String roomCode, DataSnapshot roomSnapshot,
                                      boolean alreadyInRoom) {
        // Get room data with defaults
        String topicName = roomSnapshot.child("topicName").getValue(String.class);
        if (topicName == null) topicName = "Quiz Game";

        String difficulty = roomSnapshot.child("difficulty").getValue(String.class);
        if (difficulty == null) difficulty = "medium";

        Integer categoryId = roomSnapshot.child("categoryId").getValue(Integer.class);
        if (categoryId == null) categoryId = 9; // Default category

        Integer timer = roomSnapshot.child("timer").getValue(Integer.class);
        if (timer == null) timer = 15;

        String numQuestionsStr = roomSnapshot.child("numQuestions").getValue(String.class);
        Integer numQuestions = 10; // Default
        if (numQuestionsStr != null) {
            try {
                numQuestions = Integer.parseInt(numQuestionsStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid numQuestions format", e);
            }
        } else {
            // Try as Integer
            Integer numQuestionsInt = roomSnapshot.child("numQuestions").getValue(Integer.class);
            if (numQuestionsInt != null) {
                numQuestions = numQuestionsInt;
            }
        }

        // Get topic image
        int topicImageResId = R.drawable.game; // Default
        Object topicImageObj = roomSnapshot.child("topicImage").getValue();
        if (topicImageObj != null) {
            if (topicImageObj instanceof Long) {
                topicImageResId = ((Long) topicImageObj).intValue();
            } else if (topicImageObj instanceof Integer) {
                topicImageResId = (Integer) topicImageObj;
            }
        }

        Log.d(TAG, "Room data - Topic: " + topicName + ", Difficulty: " + difficulty +
                ", Questions: " + numQuestions + ", Timer: " + timer);

        // Create intent for JoinRoomActivity
        Intent intent = new Intent(EnterCodeActivity.this, JoinRoomActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("topicName", topicName);
        intent.putExtra("topicImage", topicImageResId);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("questions", numQuestions);
        intent.putExtra("categoryId", categoryId);
        intent.putExtra("timer", timer);
        intent.putExtra("isJoining", true);
        intent.putExtra("alreadyInRoom", alreadyInRoom);
        intent.putExtra("mode", "multiplayer");

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Re-enable button if activity is destroyed
        if (joinButton != null) {
            joinButton.setEnabled(true);
        }
    }
}