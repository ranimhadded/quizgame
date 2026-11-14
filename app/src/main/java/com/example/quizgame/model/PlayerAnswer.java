package com.example.quizgame.model;

public class PlayerAnswer {
    private String playerId;
    private String playerName;
    private String answer;
    private int selectedScore;
    private int selectedScorePosition;
    private long timestamp;
    private boolean isCorrect;
    private boolean skipped;

    // Required empty constructor for Firebase
    public PlayerAnswer() {}

    // Constructor with position
    public PlayerAnswer(String playerId, String playerName, String answer,
                        int selectedScore, int selectedScorePosition, long timestamp) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.answer = answer;
        this.selectedScore = selectedScore;
        this.selectedScorePosition = selectedScorePosition;
        this.timestamp = timestamp;
        this.isCorrect = false;
        this.skipped = false;
    }

    // Constructor without position (backward compatibility)
    public PlayerAnswer(String playerId, String playerName, String answer,
                        int selectedScore, long timestamp) {
        this(playerId, playerName, answer, selectedScore, -1, timestamp);
    }

    // Getters
    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAnswer() {
        return answer;
    }

    public int getSelectedScore() {
        return selectedScore;
    }

    public int getSelectedScorePosition() {
        return selectedScorePosition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public boolean isSkipped() {
        return skipped;
    }

    // Setters
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setSelectedScore(int selectedScore) {
        this.selectedScore = selectedScore;
    }

    public void setSelectedScorePosition(int selectedScorePosition) {
        this.selectedScorePosition = selectedScorePosition;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}