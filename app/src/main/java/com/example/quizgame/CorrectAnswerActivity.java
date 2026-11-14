package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.adapter.PlayerAnswerAdapter;
import com.example.quizgame.utils.UserHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CorrectAnswerActivity extends AppCompatActivity {

    private static final String TAG = "CorrectAnswer";
    private static final String DB_URL = "https://quizop-bbadf-default-rtdb.europe-west1.firebasedatabase.app/";

    // Views
    private TextView questNbTv, questionTv, answerTv, resultBadge, waitingText;
    private Button nextBtn;
    private RecyclerView playersRecycler;
    private ProgressBar loadingAnswers;
    private View leaderboardCard;

    // Data
    private String roomCode, correctAnswer, playerAnswer, currentUserId;
    private int currentIndex, totalQuestions, selectedScorePosition, selectedScoreValue;
    private boolean isHost, wasCorrect;
    private int pointsEarned = 0;

    // Adapter
    private PlayerAnswerAdapter adapter;
    private List<PlayerAnswerAdapter.PlayerAnswerItem> playerAnswerItems = new ArrayList<>();

    // Firebase
    private DatabaseReference roomRef;
    private ValueEventListener nextQuestionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_correct_answer);
        String mode = getIntent().getStringExtra("mode");
        boolean isSolo = "solo".equals(mode);
        initUser();
        getIntentData();
        calculateResult();
        initViews();
        if (isSolo) {
            setupSoloMode();
        } else if (roomCode != null && !roomCode.isEmpty()) {
            loadPlayersAnswers();
            setupMultiplayer();
        }

    }

    private void initUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : "";
    }

    private void getIntentData() {
        Intent i = getIntent();
        roomCode = i.getStringExtra("roomCode");
        correctAnswer = i.getStringExtra("correctAnswer");
        playerAnswer = i.getStringExtra("playerAnswer");
        currentIndex = i.getIntExtra("currentQuestionIndex", 0);
        totalQuestions = i.getIntExtra("totalQuestions", 1);
        isHost = i.getBooleanExtra("isHost", false);
        selectedScorePosition = i.getIntExtra("selectedScorePosition", -1);
        selectedScoreValue = i.getIntExtra("selectedScoreValue", 0);
    }

    private void calculateResult() {
        if (playerAnswer != null && correctAnswer != null) {
            String decodedCorrect = Html.fromHtml(correctAnswer).toString().trim();
            String decodedPlayer = Html.fromHtml(playerAnswer).toString().trim();
            wasCorrect = decodedPlayer.equalsIgnoreCase(decodedCorrect);
        } else {
            wasCorrect = false;
        }
        pointsEarned = wasCorrect ? selectedScoreValue : 0;
    }

    private void initViews() {
        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("Results");
        toolbar.setNavigationOnClickListener(v -> showLeaveDialog());

        // Question number
        questNbTv = findViewById(R.id.questNb);
        questNbTv.setText((currentIndex + 1) + "/" + totalQuestions);

        // Question text
        questionTv = findViewById(R.id.currentQuestion);
        String question = getIntent().getStringExtra("question");
        if (question != null) {
            questionTv.setText(Html.fromHtml(question));
        }

        // Correct answer
        answerTv = findViewById(R.id.answer_text);
        answerTv.setText(Html.fromHtml(correctAnswer));

        leaderboardCard = findViewById(R.id.card_players);
        playersRecycler = findViewById(R.id.players_answers_recycler);
        loadingAnswers = findViewById(R.id.loading_answers);

        adapter = new PlayerAnswerAdapter(playerAnswerItems);
        playersRecycler.setLayoutManager(new LinearLayoutManager(this));
        playersRecycler.setAdapter(adapter);

        // Buttons
        nextBtn = findViewById(R.id.buttonnext);
        waitingText = findViewById(R.id.waitingText);

        // SOLO MODE: hide leaderboard completely
        String mode = getIntent().getStringExtra("mode");
        if ("solo".equals(mode)) {
            leaderboardCard.setVisibility(View.GONE);
        }

    }


    private void loadPlayersAnswers() {
        if (roomCode == null) return;

        if (loadingAnswers != null) loadingAnswers.setVisibility(View.VISIBLE);

        roomRef = FirebaseDatabase.getInstance(DB_URL).getReference("rooms").child(roomCode);

        roomRef.child("answers").child("q" + currentIndex)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        playerAnswerItems.clear();

                        if (!snapshot.exists()) {
                            if (loadingAnswers != null) loadingAnswers.setVisibility(View.GONE);
                            return;
                        }

                        List<DataSnapshot> answerSnapshots = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            answerSnapshots.add(child);
                        }

                        if (answerSnapshots.isEmpty()) {
                            if (loadingAnswers != null) loadingAnswers.setVisibility(View.GONE);
                            return;
                        }

                        AtomicInteger loaded = new AtomicInteger(0);
                        int total = answerSnapshots.size();

                        for (DataSnapshot answerSnap : answerSnapshots) {
                            String odei = answerSnap.getKey();
                            String answer = answerSnap.child("answer").getValue(String.class);
                            Integer wager = answerSnap.child("selectedScore").getValue(Integer.class);
                            Boolean isCorrectDb = answerSnap.child("isCorrect").getValue(Boolean.class);

                            int wagerPoints = wager != null ? wager : 0;

                            // Check if correct
                            boolean correct;
                            if (isCorrectDb != null) {
                                correct = isCorrectDb;
                            } else {
                                String decodedCorrect = Html.fromHtml(correctAnswer).toString().trim();
                                String decodedAnswer = answer != null ? Html.fromHtml(answer).toString().trim() : "";
                                correct = decodedAnswer.equalsIgnoreCase(decodedCorrect);
                            }

                            boolean isCurrentUser = odei != null && odei.equals(currentUserId);
                            boolean finalCorrect = correct;

                            // Get username from Firestore
                            UserHelper.getUsername(odei, (id, username, email) -> {
                                playerAnswerItems.add(new PlayerAnswerAdapter.PlayerAnswerItem(
                                        id, username, answer, wagerPoints, finalCorrect, isCurrentUser
                                ));

                                if (loaded.incrementAndGet() == total) {
                                    // Sort: current user first, then by correct/incorrect
                                    playerAnswerItems.sort((a, b) -> {
                                        if (a.isCurrentUser) return -1;
                                        if (b.isCurrentUser) return 1;
                                        if (a.isCorrect && !b.isCorrect) return -1;
                                        if (!a.isCorrect && b.isCorrect) return 1;
                                        return 0;
                                    });

                                    adapter.updateData(playerAnswerItems);
                                    if (loadingAnswers != null) loadingAnswers.setVisibility(View.GONE);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading answers: " + error.getMessage());
                        if (loadingAnswers != null) loadingAnswers.setVisibility(View.GONE);
                    }
                });
    }

    private void setupMultiplayer() {
        roomRef = FirebaseDatabase.getInstance(DB_URL).getReference("rooms").child(roomCode);

        if (isHost) {
            setupHostUI();
        } else {
            setupPlayerUI();
        }
    }

    private void setupSoloMode() {
        nextBtn.setVisibility(View.VISIBLE);
        if (waitingText != null) waitingText.setVisibility(View.GONE);

        boolean isLast = currentIndex >= totalQuestions - 1;
        nextBtn.setText(isLast ? "See Results" : "Next Question");

        nextBtn.setOnClickListener(v -> returnResultAndFinish(isLast));
    }


    private void setupHostUI() {
        nextBtn.setVisibility(View.VISIBLE);
        if (waitingText != null) waitingText.setVisibility(View.GONE);

        boolean isLast = currentIndex >= totalQuestions - 1;
        nextBtn.setText(isLast ? "Show Leaderboard" : "Next Question");

        nextBtn.setOnClickListener(v -> {
            if (isLast) {
                roomRef.child("roomStatus").setValue("finished");
                returnResultAndFinish(true);
            } else {
                int nextIndex = currentIndex + 1;
                roomRef.child("currentQuestionIndex").setValue(nextIndex);
                roomRef.child("questionState").setValue("active");
                returnResultAndFinish(false);
            }
        });
    }

    private void setupPlayerUI() {
        nextBtn.setVisibility(View.GONE);
        if (waitingText != null) {
            waitingText.setVisibility(View.VISIBLE);
            waitingText.setText("Waiting for host...");
        }

        listenForNextQuestion();
    }

    private void listenForNextQuestion() {
        nextQuestionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = snapshot.child("roomStatus").getValue(String.class);
                if ("finished".equals(status)) {
                    returnResultAndFinish(true);
                    return;
                }

                Integer newIndex = snapshot.child("currentQuestionIndex").getValue(Integer.class);
                String state = snapshot.child("questionState").getValue(String.class);

                if (newIndex != null && newIndex > currentIndex && "active".equals(state)) {
                    returnResultAndFinish(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Listener error: " + error.getMessage());
            }
        };
        roomRef.addValueEventListener(nextQuestionListener);
    }

    private void returnResultAndFinish(boolean isLastQuestion) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("wasCorrect", wasCorrect);
        resultIntent.putExtra("pointsEarned", pointsEarned);
        resultIntent.putExtra("usedScorePosition", selectedScorePosition);
        resultIntent.putExtra("isLastQuestion", isLastQuestion);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave?")
                .setPositiveButton("Leave", (d, w) -> {
                    setResult(RESULT_CANCELED);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Stay", null)
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nextQuestionListener != null && roomRef != null) {
            roomRef.removeEventListener(nextQuestionListener);
        }
    }
}