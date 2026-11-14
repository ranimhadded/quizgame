package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.adapter.ScoreButtonAdapter;
import com.example.quizgame.model.PlayerAnswer;
import com.example.quizgame.model.QuizQuestion;
import com.example.quizgame.model.ScoreButton;
import com.example.quizgame.utils.UserHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizQuestMultipleActivity extends AppCompatActivity
        implements ScoreButtonAdapter.OnScoreSelectedListener {

    private static final String TAG = "QuizMultiple";
    private static final String DB_URL = "https://quizop-bbadf-default-rtdb.europe-west1.firebasedatabase.app/";
    private static final int REQUEST_CORRECT_ANSWER = 100;

    // Views
    private TextView timerTv, questionTv, playerStatusTv, questionNumberTv, totalScoreTv, loadingText;
    private EditText answerField;
    private ExtendedFloatingActionButton submitBtn;
    private RecyclerView scoreButtonsRecycler;

    // Adapter
    private ScoreButtonAdapter scoreButtonAdapter;
    private List<ScoreButton> scoreButtonList = new ArrayList<>();

    // Game State
    private String roomCode, currentUserId, currentUserName, hostId;
    private int selectedTimer = 15;
    private int currentQuestionIndex = 0;
    private int questionCount = 10;
    private int totalScore = 0;
    private int selectedScorePosition = -1;
    private int selectedScoreValue = -1;
    private boolean hasSubmitted = false;
    private boolean isHost = false;
    private boolean userDataLoaded = false;

    // Questions
    private ArrayList<QuizQuestion> questions = new ArrayList<>();
    private CountDownTimer timer;

    // Firebase
    private DatabaseReference roomRef;
    private ValueEventListener questionIndexListener, questionStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_quest_multiple);

        roomCode = getIntent().getStringExtra("roomCode");
        if (roomCode == null) {
            Toast.makeText(this, "Room code required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initUser();
    }

    private void initUser() {
        showLoading(true);

        UserHelper.getCurrentUser((odei, username, email) -> {
            currentUserId = odei;
            currentUserName = username;
            userDataLoaded = true;
            Log.d(TAG, "User loaded: " + currentUserName);
            initFirebase();
        });
    }

    private void initViews() {
        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("Room: " + roomCode);
        toolbar.setNavigationOnClickListener(v -> showLeaveDialog());

        // TextViews
        timerTv = findViewById(R.id.timer);
        questionTv = findViewById(R.id.question_text);
        questionNumberTv = findViewById(R.id.question_number);
        totalScoreTv = findViewById(R.id.total_score_text);
        loadingText = findViewById(R.id.loading_text);

        // Input and Button
        answerField = findViewById(R.id.answer_field);
        submitBtn = findViewById(R.id.submit_button);
        submitBtn.setOnClickListener(v -> submitAnswer());

        // RecyclerView for score buttons
        scoreButtonsRecycler = findViewById(R.id.score_buttons_recycler);

        updateTotalScoreDisplay();
    }

    private void initFirebase() {
        showLoading(true);
        roomRef = FirebaseDatabase.getInstance(DB_URL).getReference("rooms").child(roomCode);

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(QuizQuestMultipleActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Get room data
                hostId = snapshot.child("hostId").getValue(String.class);
                isHost = currentUserId.equals(hostId);

                Integer t = snapshot.child("timer").getValue(Integer.class);
                selectedTimer = t != null ? t : 15;

                Integer qCount = snapshot.child("questionCount").getValue(Integer.class);
                questionCount = qCount != null ? qCount : 10;

                Integer idx = snapshot.child("currentQuestionIndex").getValue(Integer.class);
                currentQuestionIndex = idx != null ? idx : 0;

                // Parse questions
                String questionsJson = snapshot.child("questions").getValue(String.class);
                if (questionsJson != null) {
                    parseQuestions(questionsJson);
                    initScoreButtons();
                    loadPlayerState();
                    setupListeners();
                    showLoading(false);
                    loadCurrentQuestion();
                } else {
                    Toast.makeText(QuizQuestMultipleActivity.this, "No questions found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                finish();
            }
        });
    }

    private void parseQuestions(String json) {
        try {
            JSONObject response = new JSONObject(json);
            JSONArray results = response.getJSONArray("results");
            questions.clear();

            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                String q = obj.getString("question");
                String correct = obj.getString("correct_answer");
                JSONArray incorrect = obj.getJSONArray("incorrect_answers");

                List<String> incorrectList = new ArrayList<>();
                for (int j = 0; j < incorrect.length(); j++) {
                    incorrectList.add(incorrect.getString(j));
                }

                List<String> allOptions = new ArrayList<>(incorrectList);
                allOptions.add(correct);

                questions.add(new QuizQuestion(q, correct, incorrectList, allOptions));
            }

            questionCount = questions.size();
            Log.d(TAG, "Parsed " + questions.size() + " questions");
        } catch (JSONException e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
        }
    }

    private void initScoreButtons() {
        scoreButtonList.clear();
        for (int i = 1; i <= questionCount; i++) {
            scoreButtonList.add(new ScoreButton(i));
        }

        scoreButtonAdapter = new ScoreButtonAdapter(scoreButtonList, this);

        int spanCount = Math.min(questionCount, 5);
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        scoreButtonsRecycler.setLayoutManager(layoutManager);
        scoreButtonsRecycler.setAdapter(scoreButtonAdapter);
    }

    private void loadPlayerState() {
        roomRef.child("playerStates").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Integer savedScore = snapshot.child("totalScore").getValue(Integer.class);
                            if (savedScore != null) {
                                totalScore = savedScore;
                                updateTotalScoreDisplay();
                            }

                            DataSnapshot buttonsSnapshot = snapshot.child("buttonStates");
                            for (DataSnapshot buttonData : buttonsSnapshot.getChildren()) {
                                try {
                                    int position = Integer.parseInt(buttonData.getKey());
                                    String stateStr = buttonData.child("state").getValue(String.class);
                                    if (position < scoreButtonList.size() && stateStr != null) {
                                        ScoreButton.State state = ScoreButton.State.valueOf(stateStr);
                                        scoreButtonList.get(position).setState(state);

                                        Integer usedOn = buttonData.child("usedOn").getValue(Integer.class);
                                        if (usedOn != null) {
                                            scoreButtonList.get(position).setUsedOnQuestion(usedOn);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading button state: " + e.getMessage());
                                }
                            }
                            scoreButtonAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading player state: " + error.getMessage());
                    }
                });
    }

    private void savePlayerState() {
        Map<String, Object> playerState = new HashMap<>();
        playerState.put("totalScore", totalScore);

        Map<String, Object> buttonStates = new HashMap<>();
        for (int i = 0; i < scoreButtonList.size(); i++) {
            ScoreButton btn = scoreButtonList.get(i);
            if (btn.isUsed()) {
                Map<String, Object> btnData = new HashMap<>();
                btnData.put("state", btn.getState().name());
                btnData.put("usedOn", btn.getUsedOnQuestion());
                buttonStates.put(String.valueOf(i), btnData);
            }
        }
        playerState.put("buttonStates", buttonStates);

        roomRef.child("playerStates").child(currentUserId).setValue(playerState);
    }

    private void setupListeners() {
        questionIndexListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer newIndex = snapshot.getValue(Integer.class);
                    if (newIndex != null && newIndex != currentQuestionIndex) {
                        currentQuestionIndex = newIndex;
                        loadCurrentQuestion();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        roomRef.child("currentQuestionIndex").addValueEventListener(questionIndexListener);

        questionStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String state = snapshot.getValue(String.class);
                    if ("show-answer".equals(state)) {
                        cancelTimer();
                        handleTimeUp();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        roomRef.child("questionState").addValueEventListener(questionStateListener);
    }

    @Override
    public void onScoreSelected(int position, int scoreValue) {
        selectedScorePosition = position;
        selectedScoreValue = scoreValue;
        Log.d(TAG, "Score selected: " + scoreValue + " at position " + position);
    }

    private void loadCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        QuizQuestion q = questions.get(currentQuestionIndex);
        questionTv.setText(Html.fromHtml(q.getQuestion()));
        answerField.setText("");
        answerField.setEnabled(true);
        hasSubmitted = false;

        scoreButtonAdapter.clearSelection();
        selectedScorePosition = -1;
        selectedScoreValue = -1;

        submitBtn.setEnabled(true);
        submitBtn.setAlpha(1f);
        submitBtn.setText("Submit Answer");

        if (questionNumberTv != null) {
            questionNumberTv.setText((currentQuestionIndex + 1) + "/" + questions.size());
        }

        startTimer();
        updatePlayerStatus();
    }

    private void startTimer() {
        cancelTimer();

        if (isHost) {
            roomRef.child("questionState").setValue("active");
        }

        timer = new CountDownTimer(selectedTimer * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTv.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerTv.setText("0");
                if (isHost) {
                    roomRef.child("questionState").setValue("show-answer");
                }
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void submitAnswer() {
        if (hasSubmitted) {
            Toast.makeText(this, "Already submitted", Toast.LENGTH_SHORT).show();
            return;
        }

        String answer = answerField.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Enter an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedScoreValue == -1) {
            Toast.makeText(this, "Select a score to wager", Toast.LENGTH_SHORT).show();
            return;
        }

        PlayerAnswer pa = new PlayerAnswer(
                currentUserId,
                currentUserName,
                answer,
                selectedScoreValue,
                selectedScorePosition,
                System.currentTimeMillis()
        );

        roomRef.child("answers").child("q" + currentQuestionIndex).child(currentUserId)
                .setValue(pa)
                .addOnSuccessListener(aVoid -> {
                    hasSubmitted = true;
                    submitBtn.setEnabled(false);
                    submitBtn.setAlpha(0.5f);
                    submitBtn.setText("Submitted ✓");
                    answerField.setEnabled(false);
                    Toast.makeText(this, "Answer submitted!", Toast.LENGTH_SHORT).show();
                    updatePlayerStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Submit failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleTimeUp() {
        if (!hasSubmitted) {
            if (selectedScorePosition == -1) {
                selectedScorePosition = scoreButtonAdapter.getFirstAvailablePosition();
                if (selectedScorePosition != -1 && selectedScorePosition < scoreButtonList.size()) {
                    selectedScoreValue = scoreButtonList.get(selectedScorePosition).getScoreValue();
                }
            }

            PlayerAnswer pa = new PlayerAnswer(
                    currentUserId,
                    currentUserName,
                    "",
                    selectedScoreValue,
                    selectedScorePosition,
                    System.currentTimeMillis()
            );
            pa.setSkipped(true);

            roomRef.child("answers").child("q" + currentQuestionIndex).child(currentUserId).setValue(pa);
        }

        showCorrectAnswer();
    }

    private void updatePlayerStatus() {
        roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot playersSnap) {
                int total = (int) playersSnap.getChildrenCount();

                roomRef.child("answers").child("q" + currentQuestionIndex)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot answersSnap) {
                                int answered = (int) answersSnap.getChildrenCount();
                                if (playerStatusTv != null) {
                                    playerStatusTv.setText(answered + "/" + total + " answered");
                                }

                                if (answered >= total && isHost) {
                                    roomRef.child("questionState").setValue("show-answer");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCorrectAnswer() {
        if (currentQuestionIndex >= questions.size()) return;

        QuizQuestion q = questions.get(currentQuestionIndex);

        if (isHost) {
            calculateAllPlayersScores(q.getCorrectAnswer());
        }

        Intent intent = new Intent(this, CorrectAnswerActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("correctAnswer", q.getCorrectAnswer());
        intent.putExtra("question", q.getQuestion());
        intent.putExtra("currentQuestionIndex", currentQuestionIndex);
        intent.putExtra("totalQuestions", questions.size());
        intent.putExtra("isHost", isHost);
        intent.putExtra("selectedScorePosition", selectedScorePosition);
        intent.putExtra("selectedScoreValue", selectedScoreValue);
        intent.putExtra("playerAnswer", hasSubmitted ? answerField.getText().toString().trim() : "");
        startActivityForResult(intent, REQUEST_CORRECT_ANSWER);
    }

    private void calculateAllPlayersScores(String correctAnswer) {
        roomRef.child("answers").child("q" + currentQuestionIndex)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            PlayerAnswer pa = child.getValue(PlayerAnswer.class);
                            if (pa != null) {
                                boolean isCorrect = pa.getAnswer() != null &&
                                        pa.getAnswer().trim().equalsIgnoreCase(correctAnswer.trim());

                                child.getRef().child("isCorrect").setValue(isCorrect);

                                if (isCorrect) {
                                    addPlayerScore(pa.getPlayerId(), pa.getSelectedScore());
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void addPlayerScore(String playerId, int points) {
        DatabaseReference scoreRef = roomRef.child("scores").child(playerId);
        scoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int current = 0;
                if (snapshot.exists()) {
                    Object val = snapshot.getValue();
                    if (val instanceof Long) current = ((Long) val).intValue();
                    else if (val instanceof Integer) current = (Integer) val;
                }
                scoreRef.setValue(current + points);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CORRECT_ANSWER && data != null) {
            boolean wasCorrect = data.getBooleanExtra("wasCorrect", false);
            int usedPosition = data.getIntExtra("usedScorePosition", selectedScorePosition);
            int pointsEarned = data.getIntExtra("pointsEarned", 0);

            if (usedPosition >= 0 && usedPosition < scoreButtonList.size()) {
                scoreButtonAdapter.markButtonUsed(usedPosition, wasCorrect, currentQuestionIndex);
            }

            totalScore += pointsEarned;
            updateTotalScoreDisplay();
            savePlayerState();

            boolean isLastQuestion = data.getBooleanExtra("isLastQuestion", false);
            if (isLastQuestion) {
                finishQuiz();
            }
        }
    }

    private void updateTotalScoreDisplay() {
        if (totalScoreTv != null) {
            totalScoreTv.setText("Score: " + totalScore);
        }
    }

    private void finishQuiz() {
        savePlayerState();
        roomRef.child("scores").child(currentUserId).setValue(totalScore);

        if (isHost) {
            roomRef.child("roomStatus").setValue("finished");
        }

        Intent intent = new Intent(this, WinnerActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("playerScore", totalScore);
        startActivity(intent);
        finish();
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave?")
                .setPositiveButton("Leave", (d, w) -> leaveRoom())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void leaveRoom() {
        if (roomRef != null && currentUserId != null) {
            roomRef.child("players").child(currentUserId).removeValue();
            roomRef.child("playerStates").child(currentUserId).removeValue();
        }
        finish();
    }

    private void showLoading(boolean show) {
        if (loadingText != null) {
            loadingText.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();

        if (roomRef != null) {
            if (questionIndexListener != null) {
                roomRef.child("currentQuestionIndex").removeEventListener(questionIndexListener);
            }
            if (questionStateListener != null) {
                roomRef.child("questionState").removeEventListener(questionStateListener);
            }
        }
    }
}