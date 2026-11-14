package com.example.quizgame.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.R;
import com.example.quizgame.adapter.FriendsAdapter;
import com.example.quizgame.model.Friend;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private LinearLayout searchLayout;
    private EditText searchEditText;
    private FriendsAdapter adapter;
    private List<Friend> friendsList = new ArrayList<>();
    private boolean isAddFriendsTab = false;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "";

    public FriendsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        Log.d("DEBUG", "=== FriendsFragment onCreateView ===");

        ImageButton searchButton = view.findViewById(R.id.searchButton);
        searchLayout = view.findViewById(R.id.searchLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        TextView tabMyFriends = view.findViewById(R.id.tabMyFriends);
        TextView tabAddFriends = view.findViewById(R.id.tabAddFriends);
        RecyclerView recyclerView = view.findViewById(R.id.friendsRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Log.d("DEBUG", "Creating adapter with initial list size: " + friendsList.size());
        adapter = new FriendsAdapter(friendsList, isAddFriendsTab, friend -> {
            if (isAddFriendsTab) addFriend(friend);
            else removeFriend(friend);
        });

        recyclerView.setAdapter(adapter);
        Log.d("DEBUG", "Adapter set on RecyclerView");

        searchButton.setOnClickListener(v -> toggleSearch());
        setupSearch();

        selectTab(tabMyFriends, tabAddFriends);
        loadMyFriends();

        tabMyFriends.setOnClickListener(v -> {
            isAddFriendsTab = false;
            adapter.setAddFriendsTab(false);
            selectTab(tabMyFriends, tabAddFriends);
            loadMyFriends();
        });

        tabAddFriends.setOnClickListener(v -> {
            isAddFriendsTab = true;
            adapter.setAddFriendsTab(true);
            selectTab(tabAddFriends, tabMyFriends);
            loadAddFriends();
        });

        return view;
    }

    private void toggleSearch() {
        if (searchLayout.getVisibility() == View.VISIBLE) {
            searchLayout.setVisibility(View.GONE);
            searchEditText.setText("");
            adapter.filter("");
        } else {
            searchLayout.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void selectTab(TextView selected, TextView other) {
        selected.setTypeface(null, Typeface.BOLD);
        selected.setTextColor(getResources().getColor(R.color.purple));
        other.setTypeface(null, Typeface.NORMAL);
        other.setTextColor(getResources().getColor(R.color.black));
    }

    private void loadMyFriends() {
        friendsList.clear();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d("Firestore", "No friends yet");
                        Toast.makeText(getContext(), "You have no friends yet", Toast.LENGTH_SHORT).show();
                    } else {
                        for (var doc : querySnapshot.getDocuments()) {
                            Friend friend = doc.toObject(Friend.class);
                            if (friend != null) {
                                friend.setUid(doc.getId());
                                friendsList.add(friend);
                            }
                        }
                    }
                    adapter.updateData(new ArrayList<>(friendsList));
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load friends: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAddFriends() {
        friendsList.clear();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(friendsSnapshot -> {

                    List<String> myFriendIds = new ArrayList<>();
                    for (var doc : friendsSnapshot.getDocuments()) {
                        myFriendIds.add(doc.getId());
                    }

                    db.collection("users")
                            .get()
                            .addOnSuccessListener(allUsersSnapshot -> {

                                for (var doc : allUsersSnapshot.getDocuments()) {
                                    String uid = doc.getId();
                                    if (uid.equals(currentUserId)) continue;
                                    if (myFriendIds.contains(uid)) continue;

                                    String username = doc.getString("username");
                                    String email = doc.getString("email");

                                    friendsList.add(new Friend(uid, username, email));
                                }

                                if (friendsList.isEmpty()) {
                                    Log.d("Firestore", "No users to add as friends");
                                }

                                adapter.updateData(new ArrayList<>(friendsList));
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Failed to load users: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load current friends: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllUsersExceptFriends(List<String> myFriendIds) {

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    for (var doc : querySnapshot.getDocuments()) {

                        String uid = doc.getId();
                        if (uid.equals(currentUserId)) continue;
                        if (myFriendIds.contains(uid)) continue;

                        String username = doc.getString("username");
                        String email = doc.getString("email");

                        friendsList.add(new Friend(uid, username, email));
                        if (querySnapshot.isEmpty()) {
                            Log.d("Firestore", "No friends yet");
                            return;
                        }

                    }

                    adapter.updateData(friendsList);
                });
    }

    private void addFriend(Friend friend) {
        db.collection("users").document(currentUserId)
                .collection("friends")
                .document(friend.getUid())
                .set(friend)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(),
                            friend.getUsername() + " added",
                            Toast.LENGTH_SHORT).show();
                    loadAddFriends(); // refresh list
                });
    }

    private void removeFriend(Friend friend) {
        db.collection("users").document(currentUserId)
                .collection("friends").document(friend.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), friend.getUsername() + " removed", Toast.LENGTH_SHORT).show();
                    loadMyFriends();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to remove friend", Toast.LENGTH_SHORT).show());
    }

}
