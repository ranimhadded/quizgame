package com.example.quizgame.model;

public class ScoreButton {

    public enum State {
        AVAILABLE,      // White - can be selected
        SELECTED,       // Green highlight - currently selected for this question
        USED_CORRECT,   // Green permanent - was used and answer was correct
        USED_INCORRECT  // Red permanent - was used and answer was incorrect
    }

    private int scoreValue;
    private State state;
    private int usedOnQuestion; // -1 if not used, otherwise the question index

    public ScoreButton(int scoreValue) {
        this.scoreValue = scoreValue;
        this.state = State.AVAILABLE;
        this.usedOnQuestion = -1;
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getUsedOnQuestion() {
        return usedOnQuestion;
    }

    public void setUsedOnQuestion(int questionIndex) {
        this.usedOnQuestion = questionIndex;
    }

    public boolean isAvailable() {
        return state == State.AVAILABLE || state == State.SELECTED;
    }

    public boolean isUsed() {
        return state == State.USED_CORRECT || state == State.USED_INCORRECT;
    }
}