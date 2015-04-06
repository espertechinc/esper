package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

public class SimPlayerStrategyLooser implements SimPlayerStrategy {

    private final String playerId;
    private String currentQuestionId;

    public SimPlayerStrategyLooser(String playerId) {
        this.playerId = playerId;
    }

    public void newQuestion(Map<String, Object> currentQuestion) {
        currentQuestionId = (String) currentQuestion.get(EventFactory.QID);
    }

    public void update(long currentTime, Map<String, Object> currentQuestion, int sec, EPRuntime runtime) {
        if (sec != 1) {
            return;
        }

        Map<String, Object> answer = EventFactory.makePlayerAnswer(playerId, currentQuestionId, "B", currentTime);
        runtime.sendEvent(answer, "PlayerAnswer");
    }
}
