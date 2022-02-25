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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class MatchMakerMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MatchMakerMain.class);
    final static String EVENTTYPE = "MobileUserBean";

    private final String runtimeURI;
    private final boolean continuousSimulation;

    public static void main(String[] args) {
        new MatchMakerMain("MatchMaker", false).run();
    }

    public MatchMakerMain(String runtimeURI, boolean continuousSimulation) {
        this.runtimeURI = runtimeURI;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        log.info("Setting up EPL");
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = new Configuration();
        config.getCommon().addEventType(MobileUserBean.class);
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        MatchAlertListener listener = new MatchAlertListener();
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, config);
        runtime.initialize();

        MatchMakerEPL.setup(runtime, listener);

        log.info("Sending user information");
        runtime.getEventService().sendEventBean(makeUserOne(10, 10), EVENTTYPE);
        runtime.getEventService().sendEventBean(makeUserTwo(10, 10), EVENTTYPE);

        log.info("Sending some near locations");
        runtime.getEventService().sendEventBean(makeUserOne(8.99999, 10), EVENTTYPE);

        runtime.getEventService().sendEventBean(makeUserOne(9, 10), EVENTTYPE);

        runtime.getEventService().sendEventBean(makeUserOne(11, 10), EVENTTYPE);

        runtime.getEventService().sendEventBean(makeUserOne(11.0000001, 10), EVENTTYPE);

        runtime.getEventService().sendEventBean(makeUserTwo(10.0000001, 9), EVENTTYPE);

        MobileUserBean userOneUpdated = new MobileUserBean(1, 10, 10,
            Gender.MALE, HairColor.RED, AgeRange.AGE_6,
            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5);
        runtime.getEventService().sendEventBean(userOneUpdated, EVENTTYPE);

        // Test all combinations
        for (Gender gender : Gender.values()) {
            for (HairColor color : HairColor.values()) {
                for (AgeRange age : AgeRange.values()) {
                    // Try user preferences
                    MobileUserBean userA = new MobileUserBean(2, 10, 10,
                        Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5,
                        gender, color, age);
                    runtime.getEventService().sendEventBean(userA, EVENTTYPE);

                }
            }
        }

        Random random = new Random();
        int maxEvents;
        if (continuousSimulation) {
            maxEvents = Integer.MAX_VALUE;
        } else {
            maxEvents = 100000;
            log.info("Sending 100k of random locations");
        }

        for (int i = 1; i < maxEvents; i++) {
            int x = 10 + random.nextInt(i) / 100000;
            int y = 10 + random.nextInt(i) / 100000;

            runtime.getEventService().sendEventBean(makeUserTwo(x, y), EVENTTYPE);

            if (continuousSimulation) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log.debug("Interrupted", e);
                }
            }
        }

        log.info("Done.");
    }

    private MobileUserBean makeUserOne(double locationX, double locationY) {
        return new MobileUserBean(1, locationX, locationY,
                Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4,
                Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1);
    }

    private MobileUserBean makeUserTwo(double locationX, double locationY) {
        return new MobileUserBean(2, locationX, locationY,
                Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1,
                Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4);
    }
}
