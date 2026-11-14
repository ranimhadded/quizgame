package com.example.quizgame.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.R;
import com.example.quizgame.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<Friend> friends;
    private List<Friend> fullList;
    private boolean isAddFriendsTab;
    private OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onFriendAction(Friend friend);
    }

    public void updateData(List<Friend> newFriends) {
        Log.d("DEBUG", "=== Adapter updateData START ===");

        if (newFriends == null) {
            newFriends = new ArrayList<>();
        }

        Log.d("DEBUG", "New friends count: " + newFriends.size());

        friends.clear();
        fullList.clear();

        friends.addAll(newFriends);
        fullList.addAll(newFriends);

        for (int i = 0; i < friends.size(); i++) {
            Log.d("DEBUG", "Item " + i + ": " + friends.get(i).getUsername());
        }

        Log.d("DEBUG", "Calling notifyDataSetChanged()");
        notifyDataSetChanged();
        Log.d("DEBUG", "=== Adapter updateData END ===");
    }


    public FriendsAdapter(List<Friend> friends, boolean isAddFriendsTab, OnFriendActionListener listener) {
        Log.d("DEBUG", "=== Adapter constructor ===");
        this.friends = friends;
        this.fullList = new ArrayList<>(friends);
        this.isAddFriendsTab = isAddFriendsTab;
        this.listener = listener;
        Log.d("DEBUG", "Initial friends count: " + friends.size());
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("DEBUG", "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Log.d("DEBUG", "=== onBindViewHolder START ===");
        Log.d("DEBUG", "Position: " + position + ", Total items: " + getItemCount());

        if (position >= friends.size()) {
            Log.e("DEBUG", "ERROR: Position " + position + " out of bounds for list size " + friends.size());
            return;
        }

        Friend friend = friends.get(position);
        Log.d("DEBUG", "Binding friend at position " + position + ": " + friend.getUsername());

        // Check if TextViews are found
        if (holder.nameText == null) {
            Log.e("DEBUG", "ERROR: nameText is null!");
        }
        if (holder.usernameText == null) {
            Log.e("DEBUG", "ERROR: usernameText is null!");
        }
        if (holder.actionButton == null) {
            Log.e("DEBUG", "ERROR: actionButton is null!");
        }

        // Set data
        holder.nameText.setText(friend.getUsername());

        // For usernameText, show email
        if (friend.getEmail() != null) {
            holder.usernameText.setText(friend.getEmail());
        } else {
            holder.usernameText.setText("@" + friend.getUsername());
        }

        holder.profileImage.setImageResource(R.drawable.ic_profile);

        holder.actionButton.setText(isAddFriendsTab ? "Add Friend" : "Remove Friend");
        holder.actionButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Button clicked for: " + friend.getUsername());
            if (listener != null) listener.onFriendAction(friend);
        });

        Log.d("DEBUG", "=== onBindViewHolder END ===");
    }

    @Override
    public int getItemCount() {
        int count = friends.size();
        Log.d("DEBUG", "getItemCount: " + count);
        return count;
    }

    public void filter(String text) {
        Log.d("DEBUG", "filter called with: '" + text + "'");
        text = text.toLowerCase();
        friends.clear();
        if (text.isEmpty()) {
            friends.addAll(fullList);
        } else {
            for (Friend f : fullList) {
                String username = f.getUsername() != null ? f.getUsername().toLowerCase() : "";
                String email = f.getEmail() != null ? f.getEmail().toLowerCase() : "";

                if (username.contains(text) || email.contains(text)) {
                    friends.add(f);
                }
            }
        }
        Log.d("DEBUG", "After filter, items: " + friends.size());
        notifyDataSetChanged();
    }

    public void setAddFriendsTab(boolean isAddFriendsTab) {
        Log.d("DEBUG", "setAddFriendsTab: " + isAddFriendsTab);
        this.isAddFriendsTab = isAddFriendsTab;
        notifyDataSetChanged();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, usernameText;
        Button actionButton;

        public FriendViewHolder(View itemView) {
            super(itemView);
            Log.d("DEBUG", "FriendViewHolder constructor");

            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.friendName);
            usernameText = itemView.findViewById(R.id.friendUsername);
            actionButton = itemView.findViewById(R.id.removeButton);

            // Log if any views are null
            if (profileImage == null) Log.e("DEBUG", "profileImage not found!");
            if (nameText == null) Log.e("DEBUG", "nameText not found!");
            if (usernameText == null) Log.e("DEBUG", "usernameText not found!");
            if (actionButton == null) Log.e("DEBUG", "actionButton not found!");

            Log.d("DEBUG", "Views initialized: " +
                    (profileImage != null) + ", " +
                    (nameText != null) + ", " +
                    (usernameText != null) + ", " +
                    (actionButton != null));
        }
    }
}