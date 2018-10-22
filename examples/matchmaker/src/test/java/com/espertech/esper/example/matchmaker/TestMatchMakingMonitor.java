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
package com.espertech.esper.example.matchmaker;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import junit.framework.TestCase;

import static com.espertech.esper.example.matchmaker.MatchMakerMain.EVENTTYPE;

public class TestMatchMakingMonitor extends TestCase {
    private final int USER_ID_1 = 1;
    private final int USER_ID_2 = 2;

    private MatchAlertListener listener;
    private EPRuntime runtime = null;

    protected void setUp() throws Exception {
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = new Configuration();
        config.getCommon().addEventType(MobileUserBean.class);
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        listener = new MatchAlertListener();
        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        MatchMakerEPL.setup(runtime, listener);
    }

    public void tearDown() {
        runtime.destroy();
    }

    public void testSimple() {
        MobileUserBean user_1 = new MobileUserBean(USER_ID_1, 10, 10,
            Gender.MALE, HairColor.RED, AgeRange.AGE_6,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);

        MobileUserBean user_2 = new MobileUserBean(USER_ID_2, 10, 10,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5,
            Gender.MALE, HairColor.RED, AgeRange.AGE_6);
        runtime.getEventService().sendEventBean(user_2, EVENTTYPE);

        assertEquals(1, listener.getAndClearEmittedCount());
    }

    public void testLocationChanges() {
        MobileUserBean user_1 = new MobileUserBean(USER_ID_1, 10, 10,
            Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);

        MobileUserBean user_2 = new MobileUserBean(USER_ID_2, 10, 10,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1,
            Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4);
        runtime.getEventService().sendEventBean(user_2, EVENTTYPE);

        assertEquals(1, listener.getAndClearEmittedCount());

        user_1.setLocation(8.99999, 10);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);
        assertEquals(0, listener.getAndClearEmittedCount());

        user_1.setLocation(9, 10);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);
        assertEquals(1, listener.getAndClearEmittedCount());

        user_1.setLocation(11, 10);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);
        assertEquals(1, listener.getAndClearEmittedCount());

        user_1.setLocation(11.0000001, 10);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);
        assertEquals(0, listener.getAndClearEmittedCount());

        user_2.setLocation(10.0000001, 9);
        runtime.getEventService().sendEventBean(user_2, EVENTTYPE);
        assertEquals(1, listener.getAndClearEmittedCount());
    }

    public void testPreferredMatching() {
        MobileUserBean user_1 = new MobileUserBean(USER_ID_1, 10, 10,
            Gender.MALE, HairColor.RED, AgeRange.AGE_6,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);

        // Test all combinations
        for (Gender gender : Gender.values()) {
            for (HairColor color : HairColor.values()) {
                for (AgeRange age : AgeRange.values()) {
                    // Try user preferences
                    MobileUserBean userA = new MobileUserBean(USER_ID_2, 10, 10,
                        Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5,
                        gender, color, age);
                    runtime.getEventService().sendEventBean(userA, EVENTTYPE);

                    if (listener.getEmittedList().size() == 1) {
                        assertEquals(gender, Gender.MALE);
                        assertEquals(color, HairColor.RED);
                        assertEquals(age, AgeRange.AGE_6);
                        listener.clearEmitted();
                    } else {
                        assertEquals(0, listener.getAndClearEmittedCount());
                    }
                }
            }
        }
    }

    public void testPreferredMatchingBackwards() {
        MobileUserBean user_1 = new MobileUserBean(USER_ID_1, 10, 10,
            Gender.MALE, HairColor.RED, AgeRange.AGE_6,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5);
        runtime.getEventService().sendEventBean(user_1, EVENTTYPE);

        // Test all combinations
        for (Gender gender : Gender.values()) {
            for (HairColor color : HairColor.values()) {
                for (AgeRange age : AgeRange.values()) {
                    // Try user preferences backwards
                    MobileUserBean userB = new MobileUserBean(USER_ID_2, 10, 10,
                        gender, color, age,
                        Gender.MALE, HairColor.RED, AgeRange.AGE_6);
                    runtime.getEventService().sendEventBean(userB, EVENTTYPE);

                    if (listener.getEmittedList().size() == 1) {
                        assertEquals(gender, Gender.FEMALE);
                        assertEquals(color, HairColor.BLACK);
                        assertEquals(age, AgeRange.AGE_5);
                        listener.clearEmitted();
                    } else {
                        assertEquals(0, listener.getAndClearEmittedCount());
                    }
                }
            }
        }
    }
}
