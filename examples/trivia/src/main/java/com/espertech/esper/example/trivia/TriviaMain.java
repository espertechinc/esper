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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.time.CurrentTimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TriviaMain {

    private final static Logger log = LoggerFactory.getLogger(TriviaMain.class);

    public static void main(String[] args) {

        try {
            TriviaMain main = new TriviaMain();
            main.run();
        } catch (RuntimeException ex) {
            log.error("Unexpected exception encountered: " + ex.getMessage(), ex);
        }
    }

    public void run() {
        TriviaExample example = new TriviaExample();
        EPServiceProvider engine = example.setup();

        // parse test data
        List<Round> rounds = parse(TESTDATA);

        // loop through the rounds
        for (int i = 0; i < rounds.size(); i++) {
            Round round = rounds.get(i);
            System.out.println("\n\nRound " + i);
            System.out.println("============");
            System.out.println(round);

            // set time
            setTime(engine, round.getTime());

            // send question event
            engine.getEPRuntime().sendEvent(round.getQuestion(), "TriviaQuestion");

            // send player activity
            for (PlayerAction action : round.getPlayerActions()) {
                engine.getEPRuntime().sendEvent(action.getData(), action.getEventName());
            }

            // trigger score update
            engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore(round.getQuestionId()), "UpdateScore");

            printScore(round.getQuestionId(), TriviaHelper.getScores(engine));
        }

        engine.destroy();
    }

    private List<Round> parse(String testdata) {
        List<String> lines = parseLines(testdata);
        List<List<String>> roundLines = parseRounds(lines);

        List<Round> rounds = new ArrayList<Round>();
        for (List<String> roundLineBuf : roundLines) {
            rounds.add(parseRoundBuf(roundLineBuf));
        }
        return rounds;
    }

    private Round parseRoundBuf(List<String> roundLineBuf) {
        String question = roundLineBuf.remove(0);
        if (!question.startsWith("QuestionEvent")) {
            throw new RuntimeException("First line is not a QuestionEvent");
        }

        // Format expected:
        // QuestionEvent(Q4, q, a, b, c, d) at 09:01:30
        String questionTime = question.split(" at ")[1] + ".000";
        String questionId = question.split(",")[0].substring(question.indexOf("(") + 1);
        String correctAnswer = question.split(",")[1].trim();
        Map<String, Object> questionEvent = EventFactory.makeTriviaQuestion(questionId, "<Question Here>", correctAnswer, getTime(questionTime), new String[]{"a", "b", "c", "d"});

        List<PlayerAction> playerActions = parsePlayerActions(roundLineBuf);

        return new Round(questionId, questionTime, questionEvent, playerActions);
    }

    private List<PlayerAction> parsePlayerActions(List<String> roundLineBuf) {
        // Format expected:
        // AnswerEvent(User1, Q1, 1) at 09:00:01.32
        // RequestAnswerEvent(User6, Q2) at 09:00:45.37
        // AnswerAnnulmentEvent(User1, Q10) at 09:04:48.51

        List<PlayerAction> actions = new ArrayList<PlayerAction>();
        for (String line : roundLineBuf) {
            String[] split = line.split(",");
            String playerId = split[0].substring(line.indexOf("(") + 1).trim();
            if (line.startsWith("AnswerEvent")) {
                String questionId = split[1].trim();
                String answer = split[2].substring(0, split[2].indexOf(")")).trim();
                String answerTime = line.split(" at ")[1] + "0";
                Map<String, Object> data = EventFactory.makePlayerAnswer(playerId, questionId, answer, getTime(answerTime));
                actions.add(new PlayerAction("PlayerAnswer", data));
            } else if (line.startsWith("RequestAnswerEvent")) {
                String questionId = split[1].substring(1, split[1].indexOf(")")).trim();
                String requestTime = line.split(" at ")[1] + "0";
                Map<String, Object> data = EventFactory.makePlayerFARequest(playerId, questionId);
                actions.add(new PlayerAction("PlayerFARequest", data));
            } else if (line.startsWith("AnswerAnnulmentEvent")) {
                String questionId = split[1].trim();
                String requestTime = line.split(" at ")[1] + "0";
                Map<String, Object> data = EventFactory.makePlayerAnnulment(playerId, questionId, getTime(requestTime));
                actions.add(new PlayerAction("PlayerAnnulment", data));
            } else {
                throw new RuntimeException("Unrecognized line: " + line);
            }
        }
        return actions;
    }

    private List<List<String>> parseRounds(List<String> lines) {
        List<List<String>> result = new ArrayList<List<String>>();

        List<String> currentRound = null;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith("QuestionEvent")) {
                if (currentRound != null) {
                    result.add(currentRound);
                }
                currentRound = new ArrayList<String>();
            }
            currentRound.add(line.trim());
        }

        result.add(currentRound);
        return result;
    }

    private List<String> parseLines(String testdata) {
        List<String> result = new ArrayList<String>();
        StringReader buf = new StringReader(testdata);
        LineNumberReader reader = new LineNumberReader(buf);
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse lines: " + ex.getMessage(), ex);
        }
        return result;
    }

    private void printScore(String questionId, Map<String, Integer> scores) {
        System.out.println("Score after question " + questionId + ":");
        for (Map.Entry<String, Integer> score : scores.entrySet()) {
            System.out.println("  User " + score.getKey() + " : " + score.getValue());
        }
    }

    private static long getTime(String time) {
        String date = "2011-07-01T" + time;
        return TriviaHelper.parseGetMSec(date);
    }

    private static void setTime(EPServiceProvider engine, String time) {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(getTime(time)));
    }

    private static String printMapWithTime(Map<String, Object> map) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        writer.write("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            writer.write(delimiter);
            writer.write(entry.getKey());
            writer.write("=");
            if (entry.getKey().toLowerCase(Locale.ENGLISH).contains("time")) {
                writer.write(TriviaHelper.print(entry.getValue()));
            } else {
                writer.write(entry.getValue().toString());
            }
            delimiter = ", ";
        }
        writer.write("}");
        return writer.toString();
    }

    private static class Round {
        private final String questionId;
        private final String time;
        private final Map<String, Object> question;
        private final List<PlayerAction> playerActions;

        private Round(String questionId, String time, Map<String, Object> question, List<PlayerAction> playerActions) {
            this.questionId = questionId;
            this.time = time;
            this.question = question;
            this.playerActions = playerActions;
        }

        public String getQuestionId() {
            return questionId;
        }

        public String getTime() {
            return time;
        }

        public Map<String, Object> getQuestion() {
            return question;
        }

        public List<PlayerAction> getPlayerActions() {
            return playerActions;
        }

        public String toString() {
            return "Round{" +
                    "questionId='" + questionId + '\'' +
                    ", time='" + time + '\'' +
                    ", question=" + printMapWithTime(question) +
                    ", \nplayerActions=" + playerActions +
                    '}';
        }
    }

    private static class PlayerAction {
        private final String eventName;
        private final Map<String, Object> data;

        private PlayerAction(String eventName, Map<String, Object> data) {
            this.eventName = eventName;
            this.data = data;
        }

        public String getEventName() {
            return eventName;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public String toString() {
            return "\n\t\tPlayerAction{" +
                    "eventName='" + eventName + '\'' +
                    ", data=" + printMapWithTime(data) +
                    '}';
        }
    }

    private final static String TESTDATA = "QuestionEvent(Q1, 2, a, b, c, d) at 09:00:00\n" +
            "AnswerEvent(User1, Q1, 1) at 09:00:01.32\n" +
            "AnswerEvent(User2, Q1, 2) at 09:00:01.18\n" +
            "AnswerEvent(User3, Q1, 3) at 09:00:02.10\n" +
            "AnswerEvent(User4, Q1, 1) at 09:00:03.34\n" +
            "AnswerEvent(User5, Q1, 2) at 09:00:04.21\n" +
            "AnswerEvent(User6, Q1, 4) at 09:00:04.44\n" +
            "AnswerEvent(User7, Q1, 1) at 09:00:05.32\n" +
            "AnswerEvent(User8, Q1, 2) at 09:00:06.55\n" +
            "AnswerEvent(User9, Q1, 2) at 09:00:12.10\n" +
            "AnswerEvent(User10, Q1, 1) at 09:00:15.34\n" +
            "AnswerEvent(User11, Q1, 3) at 09:00:24.21\n" +
            "AnswerEvent(User12, Q1, 2) at 09:00:28.44\n" +
            "\n" +
            "QuestionEvent(Q2, 1, a, b, c, d) at 09:00:30\n" +
            "AnswerEvent(User1, Q2, 1) at 09:00:31.32\n" +
            "AnswerEvent(User8, Q2, 2) at 09:00:42.18\n" +
            "AnswerEvent(User11, Q2, 3) at 09:00:44.10\n" +
            "AnswerEvent(User7, Q2, 1) at 09:00:45.34\n" +
            "RequestAnswerEvent(User6, Q2) at 09:00:45.37\n" +
            "AnswerEvent(User12, Q2, 3) at 09:00:46.21\n" +
            "AnswerEvent(User6, Q2, 3) at 09:00:54.44\n" +
            "AnswerEvent(User4, Q2, 1) at 09:00:55.32\n" +
            "AnswerEvent(User2, Q2, 1) at 09:00:56.55\n" +
            "AnswerEvent(User9, Q2, 2) at 09:00:57.10\n" +
            "AnswerEvent(User10, Q2, 2) at 09:00:58.34\n" +
            "AnswerEvent(User3, Q2, 1) at 09:00:58.41\n" +
            "AnswerEvent(User5, Q2, 1) at 09:00:59.44\n" +
            "\n" +
            "QuestionEvent(Q3, 4, a, b, c, d) at 09:01:00\n" +
            "AnswerEvent(User11, Q3, 4) at 09:01:01.33\n" +
            "AnswerEvent(User12, Q3, 4) at 09:01:02.18\n" +
            "RequestAnswerEvent(User7, Q3) at 09:01:02.37\n" +
            "AnswerEvent(User4, Q3, 3) at 09:01:02.44\n" +
            "AnswerAnnulmentEvent(User11, Q3) at 09:01:02.51\n" +
            "AnswerEvent(User3, Q3, 4) at 09:01:03.54\n" +
            "AnswerEvent(User5, Q3, 2) at 09:01:07.21\n" +
            "AnswerEvent(User7, Q3, 4) at 09:01:11.44\n" +
            "AnswerEvent(User6, Q3, 4) at 09:01:15.32\n" +
            "AnswerEvent(User8, Q3, 2) at 09:01:19.56\n" +
            "AnswerEvent(User9, Q3, 3) at 09:01:21.10\n" +
            "AnswerEvent(User10, Q3, 4) at 09:01:23.34\n" +
            "AnswerEvent(User1, Q3, 3) at 09:01:24.24\n" +
            "AnswerEvent(User2, Q3, 4) at 09:01:29.55\n" +
            "\n" +
            "QuestionEvent(Q4, 2, a, b, c, d) at 09:01:30\n" +
            "AnswerEvent(User5, Q4, 2) at 09:01:32.32\n" +
            "AnswerEvent(User9, Q4, 2) at 09:01:33.18\n" +
            "AnswerEvent(User3, Q4, 2) at 09:01:34.10\n" +
            "AnswerEvent(User4, Q4, 2) at 09:01:35.34\n" +
            "AnswerEvent(User1, Q4, 3) at 09:01:41.21\n" +
            "AnswerEvent(User7, Q4, 2) at 09:01:44.44\n" +
            "AnswerEvent(User6, Q4, 1) at 09:01:45.32\n" +
            "AnswerAnnulmentEvent(User1, Q4) at 09:01:45.51\n" +
            "AnswerEvent(User12, Q4, 2) at 09:01:46.55\n" +
            "AnswerEvent(User2, Q4, 2) at 09:01:57.10\n" +
            "AnswerEvent(User1, Q4, 2) at 09:01:41.21\n" +
            "AnswerEvent(User10, Q4, 2) at 09:01:59.34\n" +
            "AnswerEvent(User11, Q4, 1) at 09:01:59.42\n" +
            "AnswerEvent(User8, Q4, 1) at 09:01:59.44\n" +
            "\n" +
            "QuestionEvent(Q5, 3, a, b, c, d) at 09:02:00\n" +
            "AnswerEvent(User7, Q5, 3) at 09:02:01.33\n" +
            "AnswerEvent(User6, Q5, 1) at 09:02:02.18\n" +
            "AnswerEvent(User4, Q5, 3) at 09:02:02.15\n" +
            "RequestAnswerEvent(User8, Q5) at 09:02:02.37\n" +
            "AnswerEvent(User3, Q5, 3) at 09:02:03.54\n" +
            "AnswerEvent(User5, Q5, 3) at 09:02:07.21\n" +
            "AnswerEvent(User2, Q5, 2) at 09:02:11.44\n" +
            "AnswerEvent(User1, Q5, 4) at 09:02:15.32\n" +
            "AnswerEvent(User11, Q5, 1) at 09:02:19.56\n" +
            "AnswerEvent(User9, Q5, 3) at 09:02:21.10\n" +
            "RequestAnswerEvent(User11, Q5) at 09:02:22.37\n" +
            "AnswerEvent(User11, Q5, 4) at 09:02:23.34\n" +
            "AnswerEvent(User8, Q5, 3) at 09:02:24.24\n" +
            "AnswerEvent(User12, Q5, 3) at 09:02:29.55\n" +
            "\n" +
            "QuestionEvent(Q6, 4, a, b, c, d) at 09:02:30\n" +
            "AnswerEvent(User4, Q6, 2) at 09:02:34.32\n" +
            "AnswerEvent(User8, Q6, 4) at 09:02:35.18\n" +
            "AnswerEvent(User6, Q6, 1) at 09:02:36.10\n" +
            "AnswerEvent(User1, Q6, 4) at 09:02:36.34\n" +
            "AnswerEvent(User5, Q6, 3) at 09:02:38.21\n" +
            "RequestAnswerEvent(User2, Q6) at 09:02:39.37\n" +
            "AnswerEvent(User6, Q6, 2) at 09:02:41.44\n" +
            "AnswerEvent(User3, Q6, 4) at 09:02:41.33\n" +
            "AnswerEvent(User2, Q6, 2) at 09:02:43.55\n" +
            "AnswerEvent(User9, Q6, 4) at 09:02:46.10\n" +
            "AnswerEvent(User10, Q6, 4) at 09:02:48.34\n" +
            "AnswerEvent(User11, Q6, 4) at 09:02:52.23\n" +
            "AnswerEvent(User12, Q6, 1) at 09:02:54.46\n" +
            "\n" +
            "QuestionEvent(Q7, 3, a, b, c, d) at 09:03:00\n" +
            "AnswerEvent(User1, Q7, 3) at 09:00:03.32\n" +
            "AnswerEvent(User2, Q7, 3) at 09:00:03.17\n" +
            "AnswerEvent(User3, Q7, 3) at 09:00:03.05\n" +
            "AnswerEvent(User4, Q7, 1) at 09:00:03.34\n" +
            "AnswerEvent(User5, Q7, 2) at 09:03:04.21\n" +
            "AnswerEvent(User6, Q7, 1) at 09:03:05.12\n" +
            "AnswerEvent(User7, Q7, 3) at 09:03:05.32\n" +
            "RequestAnswerEvent(User10, Q7) at 09:03:06.37\n" +
            "AnswerEvent(User8, Q7, 2) at 09:03:07.52\n" +
            "AnswerEvent(User9, Q7, 3) at 09:03:08.10\n" +
            "AnswerEvent(User10, Q7, 1) at 09:03:11.33\n" +
            "AnswerEvent(User11, Q7, 3) at 09:03:12.57\n" +
            "AnswerEvent(User12, Q7, 3) at 09:03:15.42\n" +
            "\n" +
            "QuestionEvent(Q8, 1, a, b, c, d) at 09:03:30\n" +
            "AnswerEvent(User1, Q8, 1) at 09:03:31.32\n" +
            "AnswerEvent(User8, Q8, 1) at 09:03:42.18\n" +
            "AnswerEvent(User7, Q8, 3) at 09:03:44.10\n" +
            "AnswerEvent(User11, Q8, 1) at 09:03:45.34\n" +
            "AnswerEvent(User12, Q8, 3) at 09:03:46.21\n" +
            "AnswerEvent(User4, Q8, 3) at 09:03:54.44\n" +
            "AnswerEvent(User6, Q8, 2) at 09:03:55.32\n" +
            "AnswerEvent(User9, Q8, 1) at 09:03:56.55\n" +
            "AnswerEvent(User2, Q8, 2) at 09:03:57.10\n" +
            "AnswerEvent(User10, Q8, 1) at 09:03:58.34\n" +
            "AnswerEvent(User3, Q8, 1) at 09:03:58.41\n" +
            "AnswerEvent(User5, Q8, 1) at 09:03:59.44\n" +
            "\n" +
            "QuestionEvent(Q9, 4, a, b, c, d) at 09:04:00\n" +
            "AnswerEvent(User11, Q9, 4) at 09:04:01.33\n" +
            "AnswerEvent(User12, Q9, 4) at 09:04:02.18\n" +
            "AnswerEvent(User5, Q9, 3) at 09:04:02.15\n" +
            "AnswerEvent(User3, Q9, 4) at 09:04:03.54\n" +
            "AnswerEvent(User4, Q9, 2) at 09:04:07.21\n" +
            "RequestAnswerEvent(User1, Q9) at 09:04:10.37\n" +
            "AnswerEvent(User7, Q9, 4) at 09:04:11.44\n" +
            "AnswerEvent(User6, Q9, 4) at 09:04:15.32\n" +
            "AnswerEvent(User8, Q9, 2) at 09:04:19.56\n" +
            "AnswerEvent(User1, Q9, 3) at 09:04:21.10\n" +
            "AnswerEvent(User10, Q9, 4) at 09:04:23.34\n" +
            "AnswerEvent(User1, Q9, 3) at 09:04:24.24\n" +
            "AnswerEvent(User2, Q9, 4) at 09:04:29.55\n" +
            "\n" +
            "QuestionEvent(Q10, 2, a, b, c, d) at 09:04:30\n" +
            "AnswerEvent(User5, Q10, 2) at 09:04:32.32\n" +
            "AnswerEvent(User9, Q10, 2) at 09:04:33.18\n" +
            "AnswerEvent(User3, Q10, 2) at 09:04:34.10\n" +
            "AnswerEvent(User4, Q10, 2) at 09:04:35.34\n" +
            "AnswerEvent(User1, Q10, 3) at 09:04:41.21\n" +
            "AnswerEvent(User7, Q10, 2) at 09:04:44.44\n" +
            "AnswerEvent(User6, Q10, 1) at 09:04:45.32\n" +
            "AnswerEvent(User12, Q10, 2) at 09:04:46.55\n" +
            "AnswerAnnulmentEvent(User1, Q10) at 09:04:48.51\n" +
            "AnswerEvent(User2, Q10, 2) at 09:04:57.10\n" +
            "AnswerEvent(User10, Q10, 2) at 09:04:59.34\n" +
            "AnswerEvent(User11, Q10, 1) at 09:04:59.42\n" +
            "AnswerEvent(User8, Q10, 1) at 09:01:59.44\n" +
            "\n" +
            "QuestionEvent(Q11, 2, a, b, c, d) at 09:05:00\n" +
            "AnswerEvent(User7, Q11, 2) at 09:05:01.33\n" +
            "AnswerEvent(User6, Q11, 2) at 09:05:02.18\n" +
            "AnswerEvent(User2, Q11, 2) at 09:05:02.15\n" +
            "AnswerEvent(User9, Q11, 2) at 09:05:03.54\n" +
            "AnswerEvent(User5, Q11, 3) at 09:05:07.21\n" +
            "AnswerEvent(User4, Q11, 2) at 09:05:11.44\n" +
            "AnswerEvent(User1, Q11, 2) at 09:05:15.32\n" +
            "AnswerEvent(User11, Q11, 1) at 09:05:19.56\n" +
            "AnswerEvent(User3, Q11, 2) at 09:05:21.10\n" +
            "AnswerEvent(User11, Q11, 2) at 09:05:23.34\n" +
            "AnswerEvent(User8, Q11, 3) at 09:05:24.24\n" +
            "AnswerEvent(User12, Q11, 3) at 09:05:29.55\n" +
            "\n" +
            "QuestionEvent(Q12, 2, a, b, c, d) at 09:05:30\n" +
            "AnswerEvent(User4, Q12, 2) at 09:05:33.32\n" +
            "AnswerEvent(User8, Q12, 4) at 09:05:35.18\n" +
            "AnswerEvent(User6, Q12, 2) at 09:05:36.10\n" +
            "AnswerEvent(User1, Q12, 2) at 09:05:36.34\n" +
            "AnswerEvent(User5, Q12, 3) at 09:05:39.23\n" +
            "AnswerEvent(User6, Q12, 2) at 09:05:41.44\n" +
            "AnswerEvent(User3, Q12, 2) at 09:05:42.34\n" +
            "AnswerEvent(User2, Q12, 2) at 09:05:43.55\n" +
            "AnswerEvent(User9, Q12, 4) at 09:05:47.10\n" +
            "AnswerEvent(User10, Q12, 4) at 09:05:49.33\n" +
            "AnswerEvent(User11, Q12, 3) at 09:05:53.23\n" +
            "AnswerEvent(User12, Q12, 1) at 09:05:53.46\n" +
            "\n" +
            "QuestionEvent(Q13, 4, a, b, c, d) at 09:06:00\n" +
            "AnswerEvent(User1, Q13, 1) at 09:06:01.32\n" +
            "AnswerEvent(User2, Q13, 2) at 09:06:02.18\n" +
            "AnswerEvent(User3, Q13, 4) at 09:06:03.10\n" +
            "AnswerEvent(User4, Q13, 1) at 09:06:05.34\n" +
            "AnswerEvent(User5, Q13, 2) at 09:06:08.21\n" +
            "AnswerEvent(User6, Q13, 4) at 09:06:08.44\n" +
            "AnswerEvent(User7, Q13, 1) at 09:06:09.32\n" +
            "AnswerEvent(User8, Q13, 2) at 09:06:10.55\n" +
            "AnswerEvent(User9, Q13, 2) at 09:06:12.10\n" +
            "AnswerEvent(User10, Q13, 1) at 09:06:15.34\n" +
            "AnswerEvent(User11, Q13, 3) at 09:06:24.21\n" +
            "AnswerEvent(User12, Q13, 2) at 09:06:28.44\n" +
            "\n" +
            "QuestionEvent(Q14, 4, a, b, c, d) at 09:06:30\n" +
            "AnswerEvent(User1, Q14, 2) at 09:06:32.32\n" +
            "AnswerEvent(User3, Q14, 4) at 09:06:33.18\n" +
            "AnswerEvent(User9, Q14, 4) at 09:06:34.10\n" +
            "AnswerEvent(User4, Q14, 2) at 09:06:37.34\n" +
            "AnswerEvent(User5, Q14, 3) at 09:06:39.21\n" +
            "AnswerEvent(User7, Q14, 2) at 09:06:41.42\n" +
            "AnswerEvent(User6, Q14, 1) at 09:06:43.32\n" +
            "AnswerEvent(User2, Q14, 2) at 09:06:46.55\n" +
            "AnswerEvent(User12, Q14, 2) at 09:06:57.10\n" +
            "AnswerEvent(User11, Q14, 2) at 09:06:59.34\n" +
            "AnswerEvent(User10, Q14, 1) at 09:06:59.42\n" +
            "AnswerEvent(User7, Q14, 1) at 09:06:59.44\n" +
            "\n" +
            "QuestionEvent(Q15, 2, a, b, c, d) at 09:07:00\n" +
            "AnswerEvent(User7, Q15, 3) at 09:07:01.33\n" +
            "AnswerEvent(User6, Q15, 4) at 09:07:02.18\n" +
            "AnswerEvent(User4, Q15, 3) at 09:07:02.15\n" +
            "AnswerEvent(User3, Q15, 3) at 09:07:03.54\n" +
            "AnswerEvent(User5, Q15, 3) at 09:07:07.21\n" +
            "AnswerEvent(User2, Q15, 2) at 09:07:11.44\n" +
            "AnswerEvent(User1, Q15, 2) at 09:07:15.32\n" +
            "AnswerEvent(User11, Q15, 1) at 09:07:19.56\n" +
            "AnswerEvent(User9, Q15, 2) at 09:07:21.10\n" +
            "AnswerEvent(User11, Q15, 2) at 09:02:23.34\n" +
            "AnswerEvent(User8, Q15, 3) at 09:02:24.24\n" +
            "AnswerEvent(User12, Q15, 2) at 09:02:29.55\n" +
            "\n" +
            "QuestionEvent(Q16, 1, a, b, c, d) at 09:07:30\n" +
            "AnswerEvent(User4, Q16, 1) at 09:07:34.32\n" +
            "AnswerEvent(User8, Q16, 1) at 09:07:35.18\n" +
            "AnswerEvent(User6, Q16, 1) at 09:07:36.10\n" +
            "AnswerEvent(User1, Q16, 4) at 09:07:36.34\n" +
            "AnswerEvent(User5, Q16, 3) at 09:07:38.21\n" +
            "AnswerEvent(User6, Q16, 2) at 09:07:41.22\n" +
            "AnswerEvent(User3, Q16, 4) at 09:07:41.33\n" +
            "AnswerEvent(User2, Q16, 2) at 09:07:44.55\n" +
            "AnswerEvent(User9, Q16, 1) at 09:07:46.10\n" +
            "AnswerEvent(User10, Q16, 4) at 09:07:48.34\n" +
            "AnswerEvent(User11, Q16, 4) at 09:07:52.23\n" +
            "AnswerEvent(User12, Q16, 1) at 09:07:54.46";

}
