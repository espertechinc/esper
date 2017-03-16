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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CycleDetectMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CycleDetectMain.class);

    private final EPServiceProvider engine;

    public static void main(String[] args) {
        new CycleDetectMain().run();
    }

    public CycleDetectMain() {
        engine = EPServiceProviderManager.getDefaultProvider();
        engine.getEPAdministrator().getConfiguration().addEventType(TransactionEvent.class);
        String[] functionNames = new String[]{CycleDetectorConstant.CYCLEDETECTED_NAME, CycleDetectorConstant.CYCLEOUTPUT_NAME};
        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction(functionNames, CycleDetectorAggregationFactory.class.getName());
        engine.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);

        String eplCycleDetectEachEvent = "@Name('CycleDetector') " +
                "select cycleoutput() as out " +
                "from TransactionEvent#length(1000) " +
                "having cycledetected(fromAcct, toAcct)";

        String eplCycleDetectEvery1Sec = "@Name('CycleDetector') " +
                "select (select cycleoutput(fromAcct, toAcct) from TransactionEvent#length(1000)) as out " +
                "from pattern [every timer:interval(1)]";

        EPStatement stmt = engine.getEPAdministrator().createEPL(eplCycleDetectEachEvent);

        stmt.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                for (EventBean event : newEvents) {
                    Collection<String> accts = (Collection<String>) event.get("out");
                    System.out.println("Cycle detected: " + accts);
                }
            }
        });
    }

    public EPServiceProvider getEngine() {
        return engine;
    }

    public void run() {
        final int range = 1000;
        int count = 0;
        int numEvents = 1000000;

        for (int i = 0; i < numEvents; i++) {

            // Generate random from-account and to-acocunt
            String from;
            String to;
            do {
                from = Integer.toString((int) (Math.random() * range));
                to = Integer.toString((int) (Math.random() * range));
            }
            while (from.equals(to));

            // send event
            // cycles are attempted to be detected for every single invocation
            engine.getEPRuntime().sendEvent(new TransactionEvent(from, to, 0));
            count++;

            // every 1000 events send some events that are always a cycle just to test and produce some output
            if (count % 1000 == 0) {
                System.out.println("Processed " + count + " events, sending cycle test events");
                engine.getEPRuntime().sendEvent(new TransactionEvent("CycleAssertion__A", "CycleAssertion__B", 0));
                engine.getEPRuntime().sendEvent(new TransactionEvent("CycleAssertion__B", "CycleAssertion__A", 0));
            }
        }
    }
}

