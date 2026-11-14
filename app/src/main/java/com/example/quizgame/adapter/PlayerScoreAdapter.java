package com.example.quizgame.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.R;
import com.example.quizgame.model.PlayerScore;

import java.util.List;

public class PlayerScoreAdapter extends RecyclerView.Adapter<PlayerScoreAdapter.ViewHolder> {

    private List<PlayerScore> playerScores;
    private String currentUserId;

    // Colors for ranks
    private static final int COLOR_GOLD = Color.parseColor("#FFD700");
    private static final int COLOR_SILVER = Color.parseColor("#C0C0C0");
    private static final int COLOR_BRONZE = Color.parseColor("#CD7F32");
    private static final int COLOR_DEFAULT = Color.parseColor("#6A0DAD");

    public PlayerScoreAdapter(List<PlayerScore> playerScores, String currentUserId) {
        this.playerScores = playerScores;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerScore player = playerScores.get(position);
        holder.bind(player, position);
    }

    @Override
    public int getItemCount() {
        return playerScores.size();
    }

    public void updateData(List<PlayerScore> newScores) {
        this.playerScores = newScores;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, playerName, playerScore, youBadge;
        ImageView rankIcon, playerAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            rankIcon = itemView.findViewById(R.id.rank_icon);
            playerName = itemView.findViewById(R.id.player_name);
            playerScore = itemView.findViewById(R.id.player_score);
            playerAvatar = itemView.findViewById(R.id.player_avatar);
            youBadge = itemView.findViewById(R.id.you_badge);
        }

        void bind(PlayerScore player, int position) {
            int rank = position + 1;

            // Set rank text and color
            rankText.setText(String.valueOf(rank));

            // Set rank background color based on position
            GradientDrawable rankBg = (GradientDrawable) rankText.getBackground();
            if (rankBg != null) {
                switch (rank) {
                    case 1:
                        rankBg.setColor(COLOR_GOLD);
                        break;
                    case 2:
                        rankBg.setColor(COLOR_SILVER);
                        break;
                    case 3:
                        rankBg.setColor(COLOR_BRONZE);
                        break;
                    default:
                        rankBg.setColor(COLOR_DEFAULT);
                        break;
                }
            }

            // Show rank icons for top 3
            if (rank <= 3) {
                rankIcon.setVisibility(View.VISIBLE);
                switch (rank) {
                    case 1:
                        rankIcon.setImageResource(R.drawable.first);
                        break;
                    case 2:
                        rankIcon.setImageResource(R.drawable.second);
                        break;
                    case 3:
                        rankIcon.setImageResource(R.drawable.third);
                        break;
                }
            } else {
                rankIcon.setVisibility(View.GONE);
            }

            // Set player name
            playerName.setText(player.getPlayerName());

            // Set score
            playerScore.setText(String.valueOf(player.getScore()));

            // Highlight current user
            if (player.getPlayerId() != null && player.getPlayerId().equals(currentUserId)) {
                youBadge.setVisibility(View.VISIBLE);
                itemView.setBackgroundColor(Color.parseColor("#F5F0FF"));
            } else {
                youBadge.setVisibility(View.GONE);
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}