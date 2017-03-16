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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.matchmaker.eventbean.AgeRange;
import com.espertech.esper.example.matchmaker.eventbean.Gender;
import com.espertech.esper.example.matchmaker.eventbean.HairColor;
import com.espertech.esper.example.matchmaker.eventbean.MobileUserBean;
import com.espertech.esper.example.matchmaker.monitor.MatchAlertListener;
import com.espertech.esper.example.matchmaker.monitor.MatchMakingMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class MatchMakerMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MatchMakerMain.class);

    private final String engineURI;
    private final boolean continuousSimulation;

    public static void main(String[] args) {
        new MatchMakerMain("MatchMaker", false).run();
    }

    public MatchMakerMain(String engineURI, boolean continuousSimulation) {
        this.engineURI = engineURI;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        log.info("Setting up EPL");
        // This code runs as part of the automated regression test suite; Therefore disable internal timer theading to safe resources
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);

        MatchAlertListener listener = new MatchAlertListener();
        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, config);
        epService.initialize();

        new MatchMakingMonitor(epService, listener);

        log.info("Sending user information");
        MobileUserBean userOne = new MobileUserBean(1, 10, 10,
                Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4,
                Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1);
        epService.getEPRuntime().sendEvent(userOne);

        MobileUserBean userTwo = new MobileUserBean(2, 10, 10,
                Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_1,
                Gender.MALE, HairColor.BLONDE, AgeRange.AGE_4);
        epService.getEPRuntime().sendEvent(userTwo);

        log.info("Sending some near locations");
        userOne.setLocation(8.99999, 10);
        epService.getEPRuntime().sendEvent(userOne);

        userOne.setLocation(9, 10);
        epService.getEPRuntime().sendEvent(userOne);

        userOne.setLocation(11, 10);
        epService.getEPRuntime().sendEvent(userOne);

        userOne.setLocation(11.0000001, 10);
        epService.getEPRuntime().sendEvent(userOne);

        userTwo.setLocation(10.0000001, 9);
        epService.getEPRuntime().sendEvent(userTwo);

        userOne = new MobileUserBean(1, 10, 10,
                Gender.MALE, HairColor.RED, AgeRange.AGE_6,
                Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5);
        epService.getEPRuntime().sendEvent(userOne);

        // Test all combinations
        for (Gender gender : Gender.values()) {
            for (HairColor color : HairColor.values()) {
                for (AgeRange age : AgeRange.values()) {
                    // Try user preferences
                    MobileUserBean userA = new MobileUserBean(2, 10, 10,
                            Gender.FEMALE, HairColor.BLACK, AgeRange.AGE_5,
                            gender, color, age);
                    epService.getEPRuntime().sendEvent(userA);

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

            userTwo.setLocation(x, y);
            epService.getEPRuntime().sendEvent(userTwo);

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
}
