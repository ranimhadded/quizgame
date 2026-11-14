package com.example.quizgame;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.adapter.PlayerScoreAdapter;
import com.example.quizgame.model.PlayerScore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WinnerActivity extends AppCompatActivity {

    private static final String TAG = "WinnerActivity";
    private static final String DB_URL = "https://quizop-bbadf-default-rtdb.europe-west1.firebasedatabase.app/";

    // Views
    private TextView titleText, winnerScoreText, noPlayersText;
    private ImageView profilePhoto, crownIcon;
    private RecyclerView playersRecycler;
    private ProgressBar loadingProgress;
    private Button playAgainBtn, quitBtn;

    // Data
    private String roomCode;
    private String currentUserId;
    private List<PlayerScore> playerScores = new ArrayList<>();
    private PlayerScoreAdapter adapter;

    // Firebase
    private DatabaseReference roomRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_winner);

        // Initialize UI first
        initViews();

        // Get data from intent
        String mode = getIntent().getStringExtra("mode");
        int score = getIntent().getIntExtra("score", 0);
        roomCode = getIntent().getStringExtra("roomCode");

        Toast.makeText(this, "Score received: " + score, Toast.LENGTH_SHORT).show();

        if ("solo".equals(mode) || roomCode == null || roomCode.isEmpty()) {
            handleSoloMode(score);
        } else {
            // Multiplayer mode
            initUser();
            setupRecyclerView();
            loadScoresFromFirebase();
        }
    }
    private void handleSoloMode(int score) {
        // Hide leaderboard RecyclerView
        playersRecycler.setVisibility(View.GONE);
        noPlayersText.setVisibility(View.GONE);

        // Show top layout elements
        crownIcon.setVisibility(View.VISIBLE);
        profilePhoto.setVisibility(View.VISIBLE);

        // Set title
        titleText.setText("Game Over!");

        // Set winner score
        winnerScoreText.setText("Your Score: " + score);

        // Fetch current user name from Firestore (or fallback to displayName)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "You";
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("username");
                            titleText.setText(userName != null ? userName : displayName);
                        } else {
                            titleText.setText(displayName);
                        }
                    })
                    .addOnFailureListener(e -> titleText.setText(displayName));
        } else {
            titleText.setText("You");
        }

        // Optionally set profile photo from user data if available
        // profilePhoto.setImageBitmap(...) or load with Glide/Picasso
    }

    private void initUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            currentUserId = "";
        }
    }

 /*   private void getIntentData() {
        roomCode = getIntent().getStringExtra("roomCode");

        // If no room code, this might be solo mode
        if (roomCode == null || roomCode.isEmpty()) {
            int soloScore = getIntent().getIntExtra("score", 0);
            int playerScore = getIntent().getIntExtra("playerScore", soloScore);
            handleSoloMode(playerScore);
        }
    }*/

    private void initViews() {
        titleText = findViewById(R.id.title_text);
        winnerScoreText = findViewById(R.id.winner_score_text);
        profilePhoto = findViewById(R.id.profile_photo);
        crownIcon = findViewById(R.id.crown_icon);
        playersRecycler = findViewById(R.id.players_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        noPlayersText = findViewById(R.id.no_players_text);
        playAgainBtn = findViewById(R.id.playAgain);
        quitBtn = findViewById(R.id.quitBtn);

        // Setup button listeners
        quitBtn.setOnClickListener(v -> showQuitConfirmationDialog());
        playAgainBtn.setOnClickListener(v -> playAgain());
    }

    private void setupRecyclerView() {
        adapter = new PlayerScoreAdapter(playerScores, currentUserId);
        playersRecycler.setLayoutManager(new LinearLayoutManager(this));
        playersRecycler.setAdapter(adapter);
    }


    private void loadScoresFromFirebase() {
        if (roomCode == null || roomCode.isEmpty()) {
            return; // Solo mode already handled
        }

        showLoading(true);
        roomRef = FirebaseDatabase.getInstance(DB_URL).getReference("rooms").child(roomCode);

        // First get player names from players node
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showNoPlayers();
                    return;
                }

                playerScores.clear();

                // Get players info
                DataSnapshot playersSnapshot = snapshot.child("players");
                DataSnapshot scoresSnapshot = snapshot.child("scores");

                for (DataSnapshot playerData : playersSnapshot.getChildren()) {
                    String odei = playerData.getKey();
                    String playerName = playerData.child("playerName").getValue(String.class);

                    if (playerName == null) {
                        playerName = "Player";
                    }

                    // Get score for this player
                    int score = 0;
                    if (scoresSnapshot.hasChild(odei)) {
                        Object scoreVal = scoresSnapshot.child(odei).getValue();
                        if (scoreVal instanceof Long) {
                            score = ((Long) scoreVal).intValue();
                        } else if (scoreVal instanceof Integer) {
                            score = (Integer) scoreVal;
                        }
                    }

                    playerScores.add(new PlayerScore(odei, playerName, score));
                }

                // Sort by score descending
                Collections.sort(playerScores);

                // Update UI
                updateWinnerDisplay();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading scores: " + error.getMessage());
                showNoPlayers();
            }
        });
    }

    private void updateWinnerDisplay() {
        if (playerScores.isEmpty()) {
            showNoPlayers();
            return;
        }

        // Show RecyclerView
        playersRecycler.setVisibility(View.VISIBLE);
        noPlayersText.setVisibility(View.GONE);
        adapter.updateData(playerScores);

        // Get winner (first in sorted list)
        PlayerScore winner = playerScores.get(0);

        // Check for tie
        boolean isTie = playerScores.size() > 1 &&
                playerScores.get(0).getScore() == playerScores.get(1).getScore();

        if (isTie) {
            titleText.setText("It's a Tie! 🎉");
            winnerScoreText.setText("Top Score: " + winner.getScore() + " pts");
        } else {
            // Check if current user is the winner
            if (winner.getPlayerId() != null && winner.getPlayerId().equals(currentUserId)) {
                titleText.setText("You Won! 🎉");
            } else {
                titleText.setText(winner.getPlayerName() + " Won!");
            }
            winnerScoreText.setText(winner.getScore() + " points");
        }

        // Show crown for winner
        if (crownIcon != null) {
            crownIcon.setVisibility(View.VISIBLE);
        }
    }

    private void showNoPlayers() {
        showLoading(false);
        playersRecycler.setVisibility(View.GONE);
        noPlayersText.setVisibility(View.VISIBLE);
        titleText.setText("Game Over");
        winnerScoreText.setText("");
    }

    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (playersRecycler != null) {
            playersRecycler.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void playAgain() {
        // Navigate back to main activity to start a new game
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Quit");
        builder.setMessage("Are you sure you want to quit?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Clean up room data if needed
            cleanupRoom();

            Intent intent = new Intent(WinnerActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> customizeDialog(dialog));
        dialog.show();
    }

    private void cleanupRoom() {
        // Optionally remove player from room or delete room if host
        if (roomRef != null && currentUserId != null) {
            roomRef.child("players").child(currentUserId).removeValue();
        }
    }

    private void customizeDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setCornerRadius(32f);
            background.setStroke(2, Color.parseColor("#30000000")); // Light border
            window.setElevation(20f);
            window.setBackgroundDrawable(background);
        }
    }
}
