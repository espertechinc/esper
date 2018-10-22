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
package com.espertech.esper.example.terminal.jse.simulate;

import com.espertech.esper.example.terminal.jse.event.BaseTerminalEvent;

import java.util.List;

/**
 * The main class to run the simulation and observe the ESP/CEP statements issueing notification to the registered
 * event processing agents (EPA) turning them to complex composite events that end up in the registered
 * TerminalComplexEventListener
 * <p/>
 * Run with "java -cp ... TerminalEventSimulator" (or see Ant build.xml file) or use Eclipse "Run" menu in the Eclipse
 * provided project.
 * <p/>
 * You may want to change the number of ITERATION, the SLEEP period between each iteration (ms)
 * and you may familiarize yourself with the Esper ESP/CEP statements in the TerminalEventProcessingAgent class
 * <p/>
 * It is also possible to place breakpoints and launch this in debug mode from your IDE to better understand
 * the execution flow (put breakpoints in the BaseTerminalEvent subclasses like CountPerTypeListener
 */
public class TerminalEventSimulator {

    private static final int ITERATION = 100;

    private static final int SLEEP = 2000;

    private final EventGenerator eventGenerator;

    private final TerminalEventProcessingAgent terminalEventProcessingAgent;

    public TerminalEventSimulator() {
        eventGenerator = new EventGenerator();
        terminalEventProcessingAgent = new TerminalEventProcessingAgent(new TerminalComplexEventListener());
    }

    public void sendEvents() throws InterruptedException {
        List<BaseTerminalEvent> eventsToSend = eventGenerator.generateBatch();

        for (BaseTerminalEvent theEvent : eventsToSend) {
            terminalEventProcessingAgent.sendEvent(theEvent);
        }

        // Throttle the sender to roughly send a batch every SLEEP ms
        if (SLEEP > 0) {
            System.out.printf("\n\tSleeping %d ms\n", SLEEP);
            Thread.sleep(SLEEP);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("TerminalEventSimulator starting %d iteration, sleep %d ms between\n", ITERATION, SLEEP);

        TerminalEventSimulator simulator = new TerminalEventSimulator();
        for (int i = 0; i < ITERATION; i++) {
            simulator.sendEvents();
        }

        System.out.println("TerminalEventSimulator ended");
    }

}
