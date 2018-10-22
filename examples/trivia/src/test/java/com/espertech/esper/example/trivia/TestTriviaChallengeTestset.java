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

import com.espertech.esper.runtime.client.EPRuntime;
import junit.framework.TestCase;

import java.util.Map;

public class TestTriviaChallengeTestset extends TestCase {

    private EPRuntime runtime;

    public void setUp() throws Exception {

        TriviaExample example = new TriviaExample();
        runtime = example.setup();
    }

    public void testRounds() {

        // Round 1
        setTime("09:00:00.000");
        Map<String, Object> q1 = EventFactory.makeTriviaQuestion("Q1", "Who was the director of the CIA from 1976-81?", "2", getTime("09:00:00.000"),
            new String[]{"Ronald Reagan", "George Bush", "Bill Clinton", "Richard Nixon"});
        runtime.getEventService().sendEventMap(q1, "TriviaQuestion");

        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q1", "1", getTime("09:00:01.320")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q1", "2", getTime("09:00:01.180")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User3", "Q1", "2", getTime("09:00:02.100")), "PlayerAnswer");

        runtime.getEventService().sendEventMap(EventFactory.makeUpdateScore("Q1"), "UpdateScore");
        TriviaHelper.assertScore(runtime, "User1=-1,User2=105,User3=5");

        // Round 2 (Testing Most Frequent Answer)
        setTime("09:00:30.000");
        Map<String, Object> q2 = EventFactory.makeTriviaQuestion("Q2", "In which mountains are Camp David?", "1", getTime("09:00:30.000"),
            new String[]{"Appalachian", "Ouachita", "Rocky", "Blue Ridge"});
        runtime.getEventService().sendEventMap(q2, "TriviaQuestion");

        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q2", "1", getTime("09:00:31.120")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerFARequest("User2", "Q2"), "PlayerFARequest");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q2", "1", getTime("09:00:32.150")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User3", "Q2", "3", getTime("09:00:33.000")), "PlayerAnswer");

        runtime.getEventService().sendEventMap(EventFactory.makeUpdateScore("Q2"), "UpdateScore");
        TriviaHelper.assertScore(runtime, "User1=104,User2=106,User3=4");

        // Round 3 (Testing answer annulment)
        setTime("09:01:00.000");
        Map<String, Object> q3 = EventFactory.makeTriviaQuestion("Q3", "Which song say, \"The words of the prophet are written on the subway walls?\"", "4", getTime("09:01:00.000"),
            new String[]{"Blessed", "I am a Rock", "Kathy's song", "Sounds of Silence"});
        runtime.getEventService().sendEventMap(q3, "TriviaQuestion");

        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q3", "1", getTime("09:01:02.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnnulment("User1", "Q3", getTime("09:01:05.000")), "PlayerAnnulment");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q3", "4", getTime("09:02:10.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q3", "4", getTime("09:01:10.100")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User3", "Q3", "3", getTime("09:01:50.000")), "PlayerAnswer");

        runtime.getEventService().sendEventMap(EventFactory.makeUpdateScore("Q3"), "UpdateScore");
        TriviaHelper.assertScore(runtime, "User1=109,User2=211,User3=3");

        // Round 4 (Testing late annulment and three incorrect answers)
        setTime("09:01:30.000");
        Map<String, Object> q4 = EventFactory.makeTriviaQuestion("Q4", "Who sang the title song for the Bond film A View To A Kill?", "2", getTime("09:01:30.000"),
            new String[]{"Led Zeppelin", "Duran Duran", "The Doors", "Iggy Pop"});
        runtime.getEventService().sendEventMap(q4, "TriviaQuestion");

        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q4", "2", getTime("09:01:32.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q4", "3", getTime("09:01:45.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnnulment("User2", "Q4", getTime("09:02:02.000")), "PlayerAnnulment");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q4", "2", getTime("09:02:04.100")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User3", "Q4", "1", getTime("09:01:35.000")), "PlayerAnswer");

        runtime.getEventService().sendEventMap(EventFactory.makeUpdateScore("Q4"), "UpdateScore");
        TriviaHelper.assertScore(runtime, "User1=214,User2=210,User3=-48");

        // Round 5 (Testing change of rules)
        runtime.getEventService().sendEventMap(EventFactory.makeChangeRule("1", 200), "ChangeRule");

        setTime("09:02:00.000");
        Map<String, Object> q5 = EventFactory.makeTriviaQuestion("Q5", "In which country did General Jaruzelski impose marital law in 1981?", "3", getTime("09:02:00.000"),
            new String[]{"Brazil", "Russia", "Poland", "Spain"});
        runtime.getEventService().sendEventMap(q5, "TriviaQuestion");

        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User1", "Q5", "3", getTime("09:02:10.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User2", "Q5", "3", getTime("09:02:15.000")), "PlayerAnswer");
        runtime.getEventService().sendEventMap(EventFactory.makePlayerAnswer("User3", "Q5", "3", getTime("09:02:02.000")), "PlayerAnswer");

        runtime.getEventService().sendEventMap(EventFactory.makeUpdateScore("Q5"), "UpdateScore");
        TriviaHelper.assertScore(runtime, "User1=219,User2=215,User3=157");
    }

    private long getTime(String time) {
        String date = "2011-07-01T" + time;
        return TriviaHelper.parseGetMSec(date);
    }

    private void setTime(String time) {
        runtime.getEventService().advanceTime(getTime(time));
    }
}
