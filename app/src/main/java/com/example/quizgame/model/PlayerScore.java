package com.example.quizgame.model;

public class PlayerScore implements Comparable<PlayerScore> {
    private String playerId;
    private String playerName;
    private int score;
    private int rank;

    public PlayerScore() {}

    public PlayerScore(String playerId, String playerName, int score) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.score = score;
        this.rank = 0;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public int compareTo(PlayerScore other) {
        // Sort descending by score (highest first)
        return Integer.compare(other.score, this.score);
    }
}
