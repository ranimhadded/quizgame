package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.quizgame.adapter.PlayerAdapter;
import com.example.quizgame.model.Player;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import com.example.quizgame.utils.UserHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JoinRoomActivity extends AppCompatActivity {

    private static final String TAG = "JoinRoomActivity";
    private static final String DB_URL = "https://quizop-bbadf-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView topicNameText;
    private ImageView topicImageView;
    private Button codeButton, startButton;
    private RecyclerView playersRecyclerView;
    private PlayerAdapter playerAdapter;
    private List<Player> playerList = new ArrayList<>();

    private String topicName, roomCode, currentUserId, currentUserName, currentUserEmail;
    private String difficulty;
    private int topicImage, selectedTimer, questionCount, categoryId;
    private boolean isJoiningExistingRoom = false;
    private boolean questionsLoaded = false;
    private boolean userDataLoaded = false;

    private FirebaseDatabase database;
    private DatabaseReference roomRef;
    private ValueEventListener roomStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_room);

        initFirebase();
        getIntentData();
        initViews();

        // Load user data from Firestore, then setup room
        initCurrentUser();
    }

    private void initFirebase() {
        database = FirebaseDatabase.getInstance(DB_URL);
    }

    private void initCurrentUser() {
        if (startButton != null) {
            startButton.setText("Loading...");
            startButton.setEnabled(false);
        }

        UserHelper.getCurrentUser((odei, username, email) -> {
            currentUserId = odei;
            currentUserName = username;
            currentUserEmail = email;
            userDataLoaded = true;
            Log.d(TAG, "User loaded: " + currentUserName);
            setupRoom();
        });
    }

    private void getIntentData() {
        Intent i = getIntent();
        topicName = i.getStringExtra("topicName");
        topicImage = i.getIntExtra("topicImage", R.drawable.game);
        categoryId = i.getIntExtra("categoryId", 9);
        difficulty = i.getStringExtra("difficulty");
        questionCount = i.getIntExtra("questions", 10);
        selectedTimer = i.getIntExtra("timer", 15);
        isJoiningExistingRoom = i.hasExtra("isJoining");

        if (isJoiningExistingRoom) {
            roomCode = i.getStringExtra("roomCode");
        } else {
            roomCode = generateRoomCode();
        }

        if (difficulty == null || difficulty.isEmpty()) {
            difficulty = "medium";
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("Room: " + roomCode);
        toolbar.setNavigationOnClickListener(v -> finish());

        topicNameText = findViewById(R.id.topicNameText);
        topicImageView = findViewById(R.id.topicImageView);
        codeButton = findViewById(R.id.CodeBtn);
        startButton = findViewById(R.id.submit_button);
        playersRecyclerView = findViewById(R.id.players_recycler_view);

        topicNameText.setText(topicName);
        topicImageView.setImageResource(topicImage);
        codeButton.setText(roomCode);
        codeButton.setOnClickListener(v -> shareRoomCode());

        playerAdapter = new PlayerAdapter(playerList);
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playersRecyclerView.setAdapter(playerAdapter);

        startButton.setEnabled(false);
        startButton.setAlpha(0.5f);
        startButton.setText("Loading questions...");
    }

    private void setupRoom() {
        roomRef = database.getReference("rooms").child(roomCode);

        if (isJoiningExistingRoom) {
            joinExistingRoom();
        } else {
            createNewRoom();
        }

        listenForPlayers();
        listenForRoomStatus();
    }

    private void createNewRoom() {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("topicName", topicName);
        roomData.put("topicImage", topicImage);
        roomData.put("categoryId", categoryId);
        roomData.put("difficulty", difficulty);
        roomData.put("questionCount", questionCount);
        roomData.put("timer", selectedTimer);
        roomData.put("hostId", currentUserId);
        roomData.put("roomStatus", "waiting");
        roomData.put("createdAt", System.currentTimeMillis());

        roomRef.setValue(roomData).addOnSuccessListener(aVoid -> {
            addPlayerToRoom(true);
            fetchAndSaveQuestions();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create room", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void joinExistingRoom() {
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(JoinRoomActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String status = snapshot.child("roomStatus").getValue(String.class);
                if ("in-progress".equals(status)) {
                    navigateToQuiz();
                    return;
                }

                topicName = snapshot.child("topicName").getValue(String.class);
                Integer img = snapshot.child("topicImage").getValue(Integer.class);
                topicImage = img != null ? img : R.drawable.game;
                Integer cat = snapshot.child("categoryId").getValue(Integer.class);
                categoryId = cat != null ? cat : 9;
                difficulty = snapshot.child("difficulty").getValue(String.class);
                Integer qCount = snapshot.child("questionCount").getValue(Integer.class);
                questionCount = qCount != null ? qCount : 10;
                Integer t = snapshot.child("timer").getValue(Integer.class);
                selectedTimer = t != null ? t : 15;

                topicNameText.setText(topicName);
                topicImageView.setImageResource(topicImage);

                addPlayerToRoom(false);
                checkQuestionsLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(JoinRoomActivity.this, "Error joining room", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fetchAndSaveQuestions() {
        String url = "https://opentdb.com/api.php?amount=" + questionCount
                + "&category=" + categoryId
                + "&difficulty=" + difficulty
                + "&type=multiple";

        Log.d(TAG, "Fetching questions: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        int responseCode = response.getInt("response_code");
                        if (responseCode != 0) {
                            Toast.makeText(this, "Not enough questions available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        roomRef.child("questions").setValue(response.toString())
                                .addOnSuccessListener(aVoid -> {
                                    questionsLoaded = true;
                                    Log.d(TAG, "Questions saved to Firebase");
                                    updateStartButton();
                                });
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error.getMessage());
                    Toast.makeText(this, "Failed to load questions. Retrying...", Toast.LENGTH_SHORT).show();
                    new android.os.Handler().postDelayed(this::fetchAndSaveQuestions, 3000);
                }
        );
        queue.add(request);
    }

    private void checkQuestionsLoaded() {
        roomRef.child("questions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionsLoaded = snapshot.exists();
                updateStartButton();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking questions: " + error.getMessage());
            }
        });
    }

    private void addPlayerToRoom(boolean isHost) {
        Player player = new Player(currentUserId, currentUserName, currentUserEmail, isHost);
        roomRef.child("players").child(currentUserId).setValue(player);
    }

    private void listenForPlayers() {
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                playerList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Player p = child.getValue(Player.class);
                    if (p != null) playerList.add(p);
                }
                playerAdapter.notifyDataSetChanged();
                updateStartButton();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Players listener error: " + error.getMessage());
            }
        });
    }

    private void listenForRoomStatus() {
        roomStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && "in-progress".equals(snapshot.getValue(String.class))) {
                    navigateToQuiz();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Room status listener error: " + error.getMessage());
            }
        };
        roomRef.child("roomStatus").addValueEventListener(roomStatusListener);
    }

    private void updateStartButton() {
        boolean isHost = playerList.stream().anyMatch(p ->
                p.getPlayerId().equals(currentUserId) && p.isHost());

        if (!questionsLoaded) {
            startButton.setEnabled(false);
            startButton.setAlpha(0.5f);
            startButton.setText("Loading questions...");
        } else if (!isHost) {
            startButton.setEnabled(false);
            startButton.setAlpha(0.5f);
            startButton.setText("Waiting for host...");
        } else if (playerList.size() < 2) {
            startButton.setEnabled(false);
            startButton.setAlpha(0.5f);
            startButton.setText("Need 2+ players");
        } else {
            startButton.setEnabled(true);
            startButton.setAlpha(1f);
            startButton.setText("Start Game");
            startButton.setOnClickListener(v -> startGame());
        }
    }

    private void startGame() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("roomStatus", "in-progress");
        updates.put("currentQuestionIndex", 0);
        updates.put("gameStartedAt", System.currentTimeMillis());

        roomRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Game started");
            navigateToQuiz();
        });
    }

    private void navigateToQuiz() {
        if (isFinishing()) return;

        Intent intent = new Intent(this, QuizQuestMultipleActivity.class);
        intent.putExtra("roomCode", roomCode);
        startActivity(intent);
        finish();
    }

    private String generateRoomCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void shareRoomCode() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, "Join my quiz!\nTopic: " + topicName + "\nCode: " + roomCode);
        startActivity(Intent.createChooser(share, "Share Room Code"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomStatusListener != null && roomRef != null) {
            roomRef.child("roomStatus").removeEventListener(roomStatusListener);
        }

        if (roomRef != null && currentUserId != null) {
            roomRef.child("roomStatus").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if ("waiting".equals(snapshot.getValue(String.class))) {
                        roomRef.child("players").child(currentUserId).removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}