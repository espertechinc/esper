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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.time.CurrentTimeEvent;

import java.util.Map;

public class Simulator implements Runnable {

    private final EPRuntime runtime;
    private final SimPlayerStrategy[] playerStrategies;
    private final int numQuestions;

    private Thread thread;
    private int currentQuestionCount;

    public Simulator(EPRuntime runtime, SimPlayerStrategy[] playerStrategies, int numQuestions) {
        this.runtime = runtime;
        this.playerStrategies = playerStrategies;
        this.numQuestions = numQuestions;
    }

    private static SimPlayerStrategy[] getDefaultPlayers() {
        SimPlayerStrategy[] players = new SimPlayerStrategy[5];
        players[0] = new SimPlayerStrategyWinner("P1");
        players[1] = new SimPlayerStrategyLooser("P2");
        players[2] = new SimPlayerStrategyFlipCoin("P3");
        players[3] = new SimPlayerStrategyAnuller("P4");
        players[4] = new SimPlayerStrategyFAAsker("P5");
        return players;
    }

    /**
     * Start simulator:
     * Assumption is that time starts at ZERO:
     * runtime.sendEvent(new CurrentTimeEvent(0));
     */
    public void startBlocking() throws InterruptedException {
        run(false);
    }

    public void startNonBlocking() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void destroy() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private Map<String, Object> generateQuestion(long currentTime) {
        String questionId = "Q" + Integer.toString(++currentQuestionCount);
        String question = "What is the answer to question " + questionId + "?";
        String answer = "A";

        return EventFactory.makeTriviaQuestion(questionId, question, answer, currentTime);
    }

    public void run() {
        try {
            run(true);
        } catch (InterruptedException e) {
            // expected
        }
    }

    private void run(boolean sleep) throws InterruptedException {
        long currentTime = 0;
        for (int i = 0; i < numQuestions; i++) {

            // generate and send question
            Map<String, Object> currentQuestion = generateQuestion(currentTime);
            runtime.sendEvent(currentQuestion, "TriviaQuestion");

            // let players know question
            for (int playerId = 0; playerId < playerStrategies.length; playerId++) {
                playerStrategies[playerId].newQuestion(currentQuestion);
            }

            // advance 30 seconds in 1-second intervals, giving each player a chance to react
            for (int sec = 0; sec < 30; sec++) {

                if (!sleep) {
                    currentTime += 1000;    // advance 30 seconds, second by second
                    runtime.sendEvent(new CurrentTimeEvent(currentTime));
                } else {
                    Thread.sleep(1000);
                }

                for (int playerId = 0; playerId < playerStrategies.length; playerId++) {
                    playerStrategies[playerId].update(currentTime, currentQuestion, sec, runtime);
                }
            }

            runtime.sendEvent(EventFactory.makeUpdateScore((String) currentQuestion.get("questionId")), "UpdateScore");
        }

        // let another 30 sec pass
        if (!sleep) {
            for (int sec = 0; sec < 30; sec++) {
                currentTime += 1000;    // advance 30 seconds, second by second
                runtime.sendEvent(new CurrentTimeEvent(currentTime));
            }
        } else {
            Thread.sleep(30000);
        }
    }
}
