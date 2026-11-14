package com.example.quizgame.model;

public class Player {
    private String playerId;
    private String playerName;
    private String email;
    private boolean isHost;
    private long joinedAt;

    // Default constructor required for Firebase
    public Player() {}

    public Player(String playerId, String playerName, String email, boolean isHost) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.email = email;
        this.isHost = isHost;
        this.joinedAt = System.currentTimeMillis();
    }

    // Getters and setters for ALL fields
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}