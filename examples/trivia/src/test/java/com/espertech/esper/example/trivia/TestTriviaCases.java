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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.time.CurrentTimeEvent;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestTriviaCases extends TestCase {

    private EPServiceProvider engine;

    public void setUp() throws Exception {

        TriviaExample example = new TriviaExample();
        engine = example.setup();

        /**
         * Comment-in for debugging.
         engine.getEPAdministrator().createEPL("select * from TriviaQuestion").addListener(new PrintUpdateListener());
         engine.getEPAdministrator().createEPL("select * from PlayerAnswer").addListener(new PrintUpdateListener());
         engine.getEPAdministrator().createEPL("select * from TriggerScore").addListener(new PrintUpdateListener());
         engine.getEPAdministrator().createEPL("select * from PlayerFastestAnswerWindow").addListener(new PrintUpdateListener());
         engine.getEPAdministrator().createEPL("select * from TriggerPlayerScore").addListener(new PrintUpdateListener());
         engine.getEPAdministrator().createEPL("select * from PlayerScoreWindow").addListener(new PrintUpdateListener());
         */
    }

    public void testSimulator() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        SimPlayerStrategy[] players = new SimPlayerStrategy[5];
        players[0] = new SimPlayerStrategyWinner("P1");
        players[1] = new SimPlayerStrategyLooser("P2");
        players[2] = new SimPlayerStrategyFlipCoin("P3");
        players[3] = new SimPlayerStrategyAnuller("P4");
        players[4] = new SimPlayerStrategyFAAsker("P5");

        Simulator sim = new Simulator(engine.getEPRuntime(), players, 11);

        sim.startBlocking();

        assertScore("P1", 1550);
        assertScore("P2", -461);
        assertScore("P3", 19);
        assertNotScore("P4");
        assertScore("P5", 11);
    }

    // Test FARequest -> Annulment -> Answer
    public void test1PRequestFAAnnulAnswer() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What clock never ticks?", "The un-clog", 0), "TriviaQuestion");

        engine.getEPRuntime().sendEvent(EventFactory.makePlayerFARequest("P1", "Q1"), "PlayerFARequest");

        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnnulment("P1", "Q1", 1), "PlayerAnnulment");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "The un-clog", System.currentTimeMillis()), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertScore("P1", 1);
    }

    // Test Answer -> Annulment
    public void test1PAnnulment() throws Exception {
        // send Q + A
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What clock never ticks?", "The un-clog", 0), "TriviaQuestion");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "peter pan", System.currentTimeMillis()), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnnulment("P1", "Q1", 1), "PlayerAnnulment");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertNotScore("P1");
    }

    // Test Answer:P1 -> FARequest:P2 -> Answer:P2
    public void test1PReceiveFAQ() throws Exception {
        // send Q + A
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What clock never ticks?", "The un-clog", 0), "TriviaQuestion");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "peter pan", System.currentTimeMillis()), "PlayerAnswer");

        // send FA request, assert response
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerFARequest("P2", "Q1"), "PlayerFARequest");

        // send answer
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P2", "Q1", "The un-clog", System.currentTimeMillis()), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertScore("P1", -1);
        assertScore("P2", 1);
    }

    // Test 100-bonus fastest player: honest tie
    public void test3PFastedBonusTied() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What is the square root of 2", "1.41421356", 0), "TriviaQuestion");
        long msec = System.currentTimeMillis();
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "1.41421356", msec), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P2", "Q1", "1.41421356", msec), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P3", "Q1", "1.41421356", msec), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertEquals(115, getScore("P1") + getScore("P2") + getScore("P3"));
    }

    // Test 100-bonus fasted player:  no-tie, OOO + incorrect
    public void test3PFastedBonusUnordered() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What is the square root of 2", "1.41421356", 0), "TriviaQuestion");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "1.41421356", System.currentTimeMillis() + 10), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P2", "Q1", "1.41421356", System.currentTimeMillis()), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P3", "Q1", "0", System.currentTimeMillis() - 1), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertScore("P1", 5);
        assertScore("P2", 105);
        assertScore("P3", -1);
    }

    // Test 100-bonus fasted player: no-tie OOO
    public void test2PFastedBonusOrdered() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "What is the square root of 2", "1.41421356", 0), "TriviaQuestion");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "1.41421356", System.currentTimeMillis()), "PlayerAnswer");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P2", "Q1", "1.41421356", System.currentTimeMillis() + 10), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));

        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertScore("P1", 105);
        assertScore("P2", 5);
    }

    // Test 100-bonus fasted player: single-player
    public void test1PFastedBonusCorrect() throws Exception {
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        engine.getEPRuntime().sendEvent(EventFactory.makeTriviaQuestion("Q1", "question-text", "question-answer", 0), "TriviaQuestion");
        engine.getEPRuntime().sendEvent(EventFactory.makePlayerAnswer("P1", "Q1", "question-answer", System.currentTimeMillis()), "PlayerAnswer");

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(35000));
        engine.getEPRuntime().sendEvent(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        assertScore("P1", 105);
    }

    private void assertScore(String playerId, int score) {
        assertEquals(score, getScore(playerId));
    }

    private int getScore(String playerId) {
        EPStatement stmt = engine.getEPAdministrator().getStatement("Score window");
        for (Iterator<EventBean> it = stmt.iterator(); it.hasNext(); ) {
            EventBean next = it.next();
            if (next.get("playerId").equals(playerId)) {
                return (Integer) next.get("score");
            }
        }
        fail();
        return -1;
    }

    private void assertNotScore(String playerId) {
        EPStatement stmt = engine.getEPAdministrator().getStatement("Score window");
        for (Iterator<EventBean> it = stmt.iterator(); it.hasNext(); ) {
            EventBean next = it.next();
            if (next.get("playerId").equals(playerId)) {
                fail();
            }
        }
    }

    private void assertFAResponse(EventBean eventBean, String playerId, String questionId, String answerFA) {
        assertEquals(playerId, eventBean.get("playerId"));
        assertEquals(questionId, eventBean.get("questionId"));
        assertEquals(answerFA, eventBean.get("answerFA"));
    }
}
