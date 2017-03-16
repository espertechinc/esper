/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.example.trivia;

import java.util.HashMap;
import java.util.Map;

public class EventFactory {

    public static final String QID = "questionId";

    public static Map<String, Object> makePlayerAnswer(String playerId, String questionId, String answer, long clientAnswerTime) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("playerId", playerId);
        map.put("questionId", questionId);
        map.put("answer", answer);
        map.put("clientAnswerTime", clientAnswerTime);
        return map;
    }

    public static Map<String, Object> makePlayerAnnulment(String playerId, String questionId, long annulTime) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("playerId", playerId);
        map.put("questionId", questionId);
        map.put("annulTime", annulTime);
        return map;
    }

    public static Map<String, Object> makeTriviaQuestion(String questionId, String question, String answer, long questionTime) {
        return makeTriviaQuestion(questionId, question, answer, questionTime, new String[0]);
    }

    public static Map<String, Object> makeTriviaQuestion(String questionId, String question, String answer, long questionTime, String[] choices) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("questionId", questionId);
        map.put("question", question);
        map.put("answer", answer);
        map.put("questionTime", questionTime);
        map.put("choices", choices);
        return map;
    }

    public static Map<String, Object> makePlayerFARequest(String playerId, String questionId) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("playerId", playerId);
        map.put("questionId", questionId);
        return map;
    }

    public static Map<String, Object> makeChangeRule(String ruleId, int points) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("ruleId", ruleId);
        map.put("points", points);
        return map;
    }

    public static Map<String, Object> makeUpdateScore(String questionId) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("questionId", questionId);
        return map;
    }
}
