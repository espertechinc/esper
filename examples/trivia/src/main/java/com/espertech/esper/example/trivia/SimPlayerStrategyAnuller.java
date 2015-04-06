package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

public class SimPlayerStrategyAnuller implements SimPlayerStrategy {

    private final String playerId;
    private String currentQuestionId;

    public SimPlayerStrategyAnuller(String playerId) {
        this.playerId = playerId;
    }

    public void newQuestion(Map<String, Object> currentQuestion) {
        currentQuestionId = (String) currentQuestion.get(EventFactory.QID);
    }

    public void update(long currentTime, Map<String, Object> currentQuestion, int sec, EPRuntime runtime) {
        if (sec == 0) {
            Map<String, Object> anul = EventFactory.makePlayerAnswer(playerId, currentQuestionId, "A", currentTime);
            runtime.sendEvent(anul, "PlayerAnswer");
            return;
        }

        if (sec == 29) {
            Map<String, Object> answer = EventFactory.makePlayerAnnulment(playerId, currentQuestionId, currentTime);
            runtime.sendEvent(answer, "PlayerAnnulment");
            return;
        }
    }
}
