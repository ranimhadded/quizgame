package com.example.quizgame.model;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    public String question;
    public String correctAnswer;
    public List<String> incorrectAnswers;
    public List<String> allAnswers;

    public QuizQuestion(String question, String correctAnswer, List<String> incorrect, List<String> all) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.incorrectAnswers = incorrect;
        this.allAnswers = all;
    }

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getAllAnswers() {
        return allAnswers;
    }
}
