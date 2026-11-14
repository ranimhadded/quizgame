package com.example.quizgame.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.R;
import com.example.quizgame.model.ScoreButton;

import java.util.List;

public class ScoreButtonAdapter extends RecyclerView.Adapter<ScoreButtonAdapter.ViewHolder> {

    public interface OnScoreSelectedListener {
        void onScoreSelected(int position, int scoreValue);
    }

    private static final int COLOR_WHITE = Color.WHITE;
    private static final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private static final int COLOR_GREEN_SELECTED = Color.parseColor("#81C784");
    private static final int COLOR_RED = Color.parseColor("#F44336");
    private static final int COLOR_TEXT_DARK = Color.parseColor("#333333");

    private List<ScoreButton> scoreButtons;
    private OnScoreSelectedListener listener;
    private int selectedPosition = -1;

    public ScoreButtonAdapter(List<ScoreButton> scoreButtons, OnScoreSelectedListener listener) {
        this.scoreButtons = scoreButtons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_score_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScoreButton button = scoreButtons.get(position);
        holder.bind(button, position);
    }

    @Override
    public int getItemCount() {
        return scoreButtons.size();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;

        if (oldPosition != -1 && oldPosition < scoreButtons.size()) {
            ScoreButton oldButton = scoreButtons.get(oldPosition);
            if (oldButton.getState() == ScoreButton.State.SELECTED) {
                oldButton.setState(ScoreButton.State.AVAILABLE);
            }
            notifyItemChanged(oldPosition);
        }

        if (position != -1 && position < scoreButtons.size()) {
            ScoreButton newButton = scoreButtons.get(position);
            if (newButton.isAvailable()) {
                newButton.setState(ScoreButton.State.SELECTED);
            }
            notifyItemChanged(position);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public int getSelectedScore() {
        if (selectedPosition != -1 && selectedPosition < scoreButtons.size()) {
            return scoreButtons.get(selectedPosition).getScoreValue();
        }
        return -1;
    }

    public void markButtonUsed(int position, boolean isCorrect, int questionIndex) {
        if (position >= 0 && position < scoreButtons.size()) {
            ScoreButton button = scoreButtons.get(position);
            button.setState(isCorrect ? ScoreButton.State.USED_CORRECT : ScoreButton.State.USED_INCORRECT);
            button.setUsedOnQuestion(questionIndex);
            notifyItemChanged(position);
        }
        selectedPosition = -1;
    }

    public void clearSelection() {
        if (selectedPosition != -1 && selectedPosition < scoreButtons.size()) {
            ScoreButton button = scoreButtons.get(selectedPosition);
            if (button.getState() == ScoreButton.State.SELECTED) {
                button.setState(ScoreButton.State.AVAILABLE);
                notifyItemChanged(selectedPosition);
            }
        }
        selectedPosition = -1;
    }

    public int getFirstAvailablePosition() {
        for (int i = 0; i < scoreButtons.size(); i++) {
            if (scoreButtons.get(i).isAvailable() && !scoreButtons.get(i).isUsed()) {
                return i;
            }
        }
        return -1;
    }

    public void updateButtonStates(List<ScoreButton> newStates) {
        this.scoreButtons = newStates;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        FrameLayout container;
        TextView scoreText;
        ImageView checkIcon;
        ImageView xIcon;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.score_button_card);
            container = itemView.findViewById(R.id.score_button_container);
            scoreText = itemView.findViewById(R.id.score_value_text);
            checkIcon = itemView.findViewById(R.id.check_icon);
            xIcon = itemView.findViewById(R.id.x_icon);
        }

        void bind(ScoreButton button, int position) {
            scoreText.setText(String.valueOf(button.getScoreValue()));

            // Reset visibility
            checkIcon.setVisibility(View.GONE);
            xIcon.setVisibility(View.GONE);

            switch (button.getState()) {
                case AVAILABLE:
                    container.setBackgroundColor(COLOR_WHITE);
                    scoreText.setTextColor(COLOR_TEXT_DARK);
                    cardView.setCardElevation(4f);
                    container.setAlpha(1f);
                    break;

                case SELECTED:
                    container.setBackgroundColor(COLOR_GREEN_SELECTED);
                    scoreText.setTextColor(Color.WHITE);
                    cardView.setCardElevation(8f);
                    container.setAlpha(1f);
                    break;

                case USED_CORRECT:
                    container.setBackgroundColor(COLOR_GREEN);
                    scoreText.setTextColor(Color.WHITE);
                    checkIcon.setVisibility(View.VISIBLE);
                    cardView.setCardElevation(2f);
                    container.setAlpha(0.8f);
                    break;

                case USED_INCORRECT:
                    container.setBackgroundColor(COLOR_RED);
                    scoreText.setTextColor(Color.WHITE);
                    xIcon.setVisibility(View.VISIBLE);
                    cardView.setCardElevation(2f);
                    container.setAlpha(0.8f);
                    break;
            }

            container.setOnClickListener(v -> {
                if (button.isAvailable() && !button.isUsed()) {
                    setSelectedPosition(position);
                    if (listener != null) {
                        listener.onScoreSelected(position, button.getScoreValue());
                    }
                }
            });

            container.setClickable(button.isAvailable() && !button.isUsed());
        }
    }
}