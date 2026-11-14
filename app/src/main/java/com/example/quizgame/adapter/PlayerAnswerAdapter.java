package com.example.quizgame.adapter;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.R;

import java.util.List;

public class PlayerAnswerAdapter extends RecyclerView.Adapter<PlayerAnswerAdapter.ViewHolder> {

    public static class PlayerAnswerItem {
        public String odei;
        public String playerName;
        public String answer;
        public int wagerPoints;
        public boolean isCorrect;
        public boolean isCurrentUser;

        public PlayerAnswerItem(String odei, String playerName, String answer,
                                int wagerPoints, boolean isCorrect, boolean isCurrentUser) {
            this.odei = odei;
            this.playerName = playerName;
            this.answer = answer;
            this.wagerPoints = wagerPoints;
            this.isCorrect = isCorrect;
            this.isCurrentUser = isCurrentUser;
        }
    }

    private List<PlayerAnswerItem> items;

    public PlayerAnswerAdapter(List<PlayerAnswerItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerAnswerItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<PlayerAnswerItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView playerAvatar, resultIcon;
        TextView playerName, playerAnswer, wagerPoints;
        LinearLayout container;

        ViewHolder(View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
            playerAvatar = itemView.findViewById(R.id.player_avatar);
            playerName = itemView.findViewById(R.id.player_name);
            playerAnswer = itemView.findViewById(R.id.player_answer);
            wagerPoints = itemView.findViewById(R.id.wager_points);
            resultIcon = itemView.findViewById(R.id.result_icon);
        }

        void bind(PlayerAnswerItem item) {
            // Player name with "You" indicator
            if (item.isCurrentUser) {
                playerName.setText(item.playerName + " (You)");
                container.setBackgroundColor(Color.parseColor("#F5F5F5"));
            } else {
                playerName.setText(item.playerName);
                container.setBackgroundColor(Color.TRANSPARENT);
            }

            // Answer (handle empty/skipped)
            if (item.answer == null || item.answer.isEmpty()) {
                playerAnswer.setText("(No answer)");
                playerAnswer.setTextColor(Color.parseColor("#999999"));
            } else {
                playerAnswer.setText(Html.fromHtml(item.answer));
                playerAnswer.setTextColor(Color.parseColor("#333333"));
            }

            // Wager points and result
            if (item.isCorrect) {
                wagerPoints.setText("+" + item.wagerPoints);
                wagerPoints.setTextColor(Color.parseColor("#4CAF50"));
                resultIcon.setImageResource(R.drawable.correct);
            } else {
                wagerPoints.setText("0");
                wagerPoints.setTextColor(Color.parseColor("#F44336"));
                resultIcon.setImageResource(R.drawable.wrong);
            }
        }
    }
}