package com.example.quizgame.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserHelper {

    public interface OnUserLoadedListener {
        void onLoaded(String userId, String username, String email);
    }

    public static void getCurrentUser(OnUserLoadedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            listener.onLoaded("guest_" + System.currentTimeMillis(), "Guest", "");
            return;
        }

        String odei = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(odei)
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.exists() && doc.getString("username") != null
                            ? doc.getString("username")
                            : "Player";
                    listener.onLoaded(odei, username, email);
                })
                .addOnFailureListener(e -> {
                    listener.onLoaded(odei, "Player", email);
                });
    }

    public static void getUsername(String odei, OnUserLoadedListener listener) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(odei)
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.exists() && doc.getString("username") != null
                            ? doc.getString("username")
                            : "Player";
                    listener.onLoaded(odei, username, "");
                })
                .addOnFailureListener(e -> {
                    listener.onLoaded(odei, "Player", "");
                });
    }
}