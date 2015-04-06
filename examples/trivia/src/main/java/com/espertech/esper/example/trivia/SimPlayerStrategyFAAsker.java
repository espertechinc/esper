package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

public class SimPlayerStrategyFAAsker implements SimPlayerStrategy {

    private final String playerId;
    private String currentQuestionId;

    public SimPlayerStrategyFAAsker(String playerId) {
        this.playerId = playerId;
    }

    public void newQuestion(Map<String, Object> currentQuestion) {
        currentQuestionId = (String) currentQuestion.get(EventFactory.QID);
    }

    public void update(long currentTime, Map<String, Object> currentQuestion, int sec, EPRuntime runtime) {
        if (sec == 0) {
            Map<String, Object> fa = EventFactory.makePlayerFARequest(playerId, currentQuestionId);
            runtime.sendEvent(fa, "PlayerFARequest");
            return;
        }

        if (sec == 29) {
            Map<String, Object> answer = EventFactory.makePlayerAnswer(playerId, currentQuestionId, "A", currentTime);
            runtime.sendEvent(answer, "PlayerAnswer");
        }
    }
}
