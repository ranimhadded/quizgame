package com.example.quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.quizgame.adapter.ScoreButtonAdapter;
import com.example.quizgame.model.QuizQuestion;
import com.example.quizgame.model.ScoreButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestActivity extends AppCompatActivity {

    // UI
    private TextView timerTv, questionTv;
    private EditText answerField;
    private ExtendedFloatingActionButton submitBtn;
    private RecyclerView wagerRecycler;

    // Quiz
    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int totalScore = 0;
    private int selectedTimer;
    private String mode;

    // Wager
    private List<ScoreButton> wagerButtons;
    private ScoreButtonAdapter wagerAdapter;
    private int selectedWagerPosition = -1;
    private int selectedWagerValue = -1;

    // Timer
    private CountDownTimer timer;

    private static final int REQUEST_CORRECT_ANSWER = 100;
    private static final int DEFAULT_TIMER = 15;
    private static final int DEFAULT_CATEGORY = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_quest);

        setupToolbar();
        initViews();
        getIntentExtras();
        setupWagerRecycler();
        fetchQuestions();
        setupSubmitButton();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Question");
    }

    private void initViews() {
        timerTv = findViewById(R.id.timer);
        questionTv = findViewById(R.id.question_text);
        answerField = findViewById(R.id.answer_field);
        submitBtn = findViewById(R.id.submit_button);
        wagerRecycler = findViewById(R.id.score_buttons_recycler);
    }

    private void getIntentExtras() {
        selectedTimer = getIntent().getIntExtra("timer", DEFAULT_TIMER);
        mode = getIntent().getStringExtra("mode");
    }

    // ---------------- WAGER SYSTEM ----------------

    private void setupWagerRecycler() {
        wagerButtons = new ArrayList<>();

        // Dynamic wager points (1 → number of questions)
        int questionCount = getIntent().getIntExtra("questions", 10);
        for (int i = 1; i <= questionCount; i++) {
            wagerButtons.add(new ScoreButton(i));
        }

        wagerAdapter = new ScoreButtonAdapter(wagerButtons,
                (position, scoreValue) -> {
                    selectedWagerPosition = position;
                    selectedWagerValue = scoreValue;
                });

        wagerRecycler.setLayoutManager(new GridLayoutManager(this, 5));
        wagerRecycler.setAdapter(wagerAdapter);
    }

    // ---------------- QUESTIONS ----------------

    private void fetchQuestions() {
        int categoryId = getIntent().getIntExtra("categoryId", DEFAULT_CATEGORY);
        String difficulty = getIntent().getStringExtra("difficulty");
        int questionCount = getIntent().getIntExtra("questions", 10);

        String apiUrl = "https://opentdb.com/api.php?amount=" + questionCount
                + "&category=" + categoryId
                + "&difficulty=" + difficulty
                + "&type=multiple";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, apiUrl, null,
                this::parseQuestions,
                error -> showToast("Failed to fetch questions")
        );

        queue.add(request);
    }

    private void parseQuestions(JSONObject response) {
        try {
            JSONArray results = response.getJSONArray("results");
            questions.clear();

            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                questions.add(new QuizQuestion(
                        obj.getString("question"),
                        obj.getString("correct_answer"),
                        new ArrayList<>(),
                        new ArrayList<>()
                ));
            }

            loadQuestion();

        } catch (JSONException e) {
            showToast("Parsing error");
        }
    }

    private void loadQuestion() {
        if (currentIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        QuizQuestion q = questions.get(currentIndex);
        questionTv.setText(Html.fromHtml(q.getQuestion()));
        answerField.setText("");

        // Reset wager selection
        wagerAdapter.clearSelection();
        selectedWagerPosition = -1;
        selectedWagerValue = -1;

        startTimer();
    }

    // ---------------- TIMER ----------------

    private void startTimer() {
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(selectedTimer * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTv.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                checkAnswer();
            }
        }.start();
    }

    // ---------------- SUBMIT ----------------

    private void setupSubmitButton() {
        submitBtn.setOnClickListener(v -> checkAnswer());
    }

    private void checkAnswer() {
        if (timer != null) timer.cancel();

        if (selectedWagerPosition == -1) {
            showToast("Select a wager first");
            startTimer();
            return;
        }

        QuizQuestion q = questions.get(currentIndex);
        String userAnswer = answerField.getText().toString().trim();

        boolean correct = userAnswer.equalsIgnoreCase(
                Html.fromHtml(q.getCorrectAnswer()).toString().trim()
        );

        wagerAdapter.markButtonUsed(
                selectedWagerPosition,
                correct,
                currentIndex
        );

        if (correct) {
            totalScore += selectedWagerValue;
        }

        navigateToResult(q);
    }

    private void navigateToResult(QuizQuestion q) {
        Intent intent = new Intent(this, CorrectAnswerActivity.class);
        intent.putExtra("correctAnswer", q.getCorrectAnswer());
        intent.putExtra("question", q.getQuestion());
        intent.putExtra("mode", mode);
        intent.putExtra("currentQuestionIndex", currentIndex);
        intent.putExtra("totalQuestions", questions.size());
        intent.putExtra("selectedScorePosition", selectedWagerPosition);
        intent.putExtra("selectedScoreValue", selectedWagerValue);

        startActivityForResult(intent, REQUEST_CORRECT_ANSWER);
    }

    // ---------------- RESULT ----------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CORRECT_ANSWER && resultCode == RESULT_OK) {
            currentIndex++;
            loadQuestion();
        }
    }

    private void finishQuiz() {
        Intent intent = new Intent(this, CorrectAnswerActivity.class);
        intent.putExtra("isLastQuestion", true);
        intent.putExtra("mode", mode);
        startActivity(intent);
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
