package com.example.quizgame;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Arrays;
import java.util.List;

public class HostPackActivity extends AppCompatActivity {
    // Who can join
    private Button btnInviteOnly, btnMyFriends, btnAnyone;
    // Questions
    private Button btnQuestions10, btnQuestions15, btnQuestions20;

    // Timer
    private Button btnTimer15s, btnTimer30s, btnTimer60s;

    // Difficulty
    private Button btnDifficultyEasy, btnDifficultyMedium, btnDifficultyHard;

    private Button Hostroom;

    // Selected values
    private String selectedWho = "invite", topicName;
    private int selectedQuestions = 10;
    private int selectedTimer = 15;
    private String selectedDifficulty = "easy";
    private int selectedCategoryId = 0, topicImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_host_pack);

        String mode = getIntent().getStringExtra("mode");
        selectedCategoryId = getIntent().getIntExtra("topicId", 9);

        initializeViews();
        setupToolbar();
        setupButtonGroups();

        // Hide "Who can join" when SOLO
        LinearLayout layoutWhoCanJoin = findViewById(R.id.layoutWhoCanJoin);
        if ("solo".equals(mode)) {
            layoutWhoCanJoin.setVisibility(View.GONE);
            Hostroom.setText("Start Game");
        }

        Hostroom.setOnClickListener(v -> {
            if ("solo".equals(mode)) {
                Intent intent = new Intent(this, QuizQuestActivity.class);
                intent.putExtra("categoryId", selectedCategoryId);
                intent.putExtra("questions", selectedQuestions);
                intent.putExtra("difficulty", selectedDifficulty);
                intent.putExtra("timer", selectedTimer);
                intent.putExtra("mode",mode);
                startActivity(intent);
            } else if ("host".equals(mode)) {
                Intent intent = new Intent(this, JoinRoomActivity.class);
                intent.putExtra("topicName", topicName);
                intent.putExtra("topicImage", topicImage);
                intent.putExtra("selectedWho", selectedWho);
                intent.putExtra("categoryId", selectedCategoryId);
                intent.putExtra("questions", selectedQuestions);
                intent.putExtra("difficulty", selectedDifficulty);
                intent.putExtra("timer", selectedTimer);
                intent.putExtra("mode",mode);
                startActivity(intent);
            }
        });
    }

    private void initializeViews() {
        topicName = getIntent().getStringExtra("topicName");
        topicImage = getIntent().getIntExtra("topicImage", R.drawable.game);

        TextView nameText = findViewById(R.id.topicNameText);
        ImageView imageView = findViewById(R.id.topicImageView);

        nameText.setText(topicName);
        imageView.setImageResource(topicImage);

        // Buttons
        btnInviteOnly = findViewById(R.id.btnInviteOnly);
        btnMyFriends = findViewById(R.id.btnMyFriends);
        btnAnyone = findViewById(R.id.btnAnyone);

        btnQuestions10 = findViewById(R.id.btnQuestions10);
        btnQuestions15 = findViewById(R.id.btnQuestions15);
        btnQuestions20 = findViewById(R.id.btnQuestions20);

        btnTimer15s = findViewById(R.id.btnTimer15s);
        btnTimer30s = findViewById(R.id.btnTimer30s);
        btnTimer60s = findViewById(R.id.btnTimer60s);

        btnDifficultyEasy = findViewById(R.id.btnDifficultyEasy);
        btnDifficultyMedium = findViewById(R.id.btnDifficultyMedium);
        btnDifficultyHard = findViewById(R.id.btnDifficultyHard);

        Hostroom = findViewById(R.id.hostroombutton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setTitle("Host This Pack");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtonGroups() {

        // --- WHO CAN JOIN ---
        List<Button> whoButtons = Arrays.asList(btnInviteOnly, btnMyFriends, btnAnyone);
        updateButtonGroupSelection(whoButtons, btnInviteOnly);

        btnInviteOnly.setOnClickListener(v -> {
            updateButtonGroupSelection(whoButtons, btnInviteOnly);
            selectedWho = "invite";
        });

        btnMyFriends.setOnClickListener(v -> {
            updateButtonGroupSelection(whoButtons, btnMyFriends);
            selectedWho = "friends";
        });

        btnAnyone.setOnClickListener(v -> {
            updateButtonGroupSelection(whoButtons, btnAnyone);
            selectedWho = "anyone";
        });

        // --- QUESTIONS ---
        List<Button> questionButtons = Arrays.asList(btnQuestions10, btnQuestions15, btnQuestions20);
        updateButtonGroupSelection(questionButtons, btnQuestions10);

        btnQuestions10.setOnClickListener(v -> {
            updateButtonGroupSelection(questionButtons, btnQuestions10);
            selectedQuestions = 10;
        });

        btnQuestions15.setOnClickListener(v -> {
            updateButtonGroupSelection(questionButtons, btnQuestions15);
            selectedQuestions = 15;
        });

        btnQuestions20.setOnClickListener(v -> {
            updateButtonGroupSelection(questionButtons, btnQuestions20);
            selectedQuestions = 20;
        });

        // --- TIMER ---
        List<Button> timerButtons = Arrays.asList(btnTimer15s, btnTimer30s, btnTimer60s);
        updateButtonGroupSelection(timerButtons, btnTimer15s);

        btnTimer15s.setOnClickListener(v -> {
            updateButtonGroupSelection(timerButtons, btnTimer15s);
            selectedTimer = 15;
        });

        btnTimer30s.setOnClickListener(v -> {
            updateButtonGroupSelection(timerButtons, btnTimer30s);
            selectedTimer = 30;
        });

        btnTimer60s.setOnClickListener(v -> {
            updateButtonGroupSelection(timerButtons, btnTimer60s);
            selectedTimer = 60;
        });

        // --- DIFFICULTY ---
        List<Button> difficultyButtons = Arrays.asList(btnDifficultyEasy, btnDifficultyMedium, btnDifficultyHard);
        updateButtonGroupSelection(difficultyButtons, btnDifficultyEasy);

        btnDifficultyEasy.setOnClickListener(v -> {
            updateButtonGroupSelection(difficultyButtons, btnDifficultyEasy);
            selectedDifficulty = "easy";
        });

        btnDifficultyMedium.setOnClickListener(v -> {
            updateButtonGroupSelection(difficultyButtons, btnDifficultyMedium);
            selectedDifficulty = "medium";
        });

        btnDifficultyHard.setOnClickListener(v -> {
            updateButtonGroupSelection(difficultyButtons, btnDifficultyHard);
            selectedDifficulty = "hard";
        });
    }

    private void updateButtonGroupSelection(List<Button> buttons, Button selectedButton) {
        for (Button button : buttons) {
            if (button == selectedButton) {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6331B5")));
                button.setTextColor(Color.WHITE);
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDE9FE")));
                button.setTextColor(Color.parseColor("#666666"));
            }
        }
    }
}