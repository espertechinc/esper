package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPRuntime;

import java.util.Map;

public interface SimPlayerStrategy {
    public void newQuestion(Map<String,Object> currentQuestion);
    public void update(long currentTime, Map<String,Object> currentQuestion, int sec, EPRuntime runtime);
}
