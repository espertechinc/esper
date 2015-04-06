package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

public class SimPlayerStrategyFlipCoin implements SimPlayerStrategy {

    private final String playerId;
    private String currentQuestionId;
    private boolean coin;

    public SimPlayerStrategyFlipCoin(String playerId) {
        this.playerId = playerId;
    }

    public void newQuestion(Map<String, Object> currentQuestion) {
        currentQuestionId = (String) currentQuestion.get(EventFactory.QID);
    }

    public void update(long currentTime, Map<String, Object> currentQuestion, int sec, EPRuntime runtime) {
        if (sec != 25) {
            return;
        }

        String answerText = "B";
        if (coin) {
            answerText = "A";
        }
        coin = !coin;

        Map<String, Object> answer = EventFactory.makePlayerAnswer(playerId, currentQuestionId, answerText, currentTime);
        runtime.sendEvent(answer, "PlayerAnswer");
    }
}
