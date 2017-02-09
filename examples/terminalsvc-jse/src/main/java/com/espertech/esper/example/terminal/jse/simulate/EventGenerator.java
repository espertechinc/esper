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

import com.espertech.esper.example.terminal.jse.event.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * The primary class used to perform a standalone simulation.
 * <p/>
 * We generate terminal events per batch. For each batch the following can happen
 * - some terminal can be out of order straight
 * - some other can issue low paper events straight
 * - the working terminals are all used for checkin
 * - 33% of checkin do not complete in this batch (but will in the next batch)
 * - per checkin there is a chance that the operation is either cancelled or aborted due to a terminal switching
 * to out of order
 * <p/>
 * At each batch all terminals are assumed to be repaired
 */
public class EventGenerator {

    private static final int TERMINAL_COUNT = 100;

    private static final int TERMINAL_OUTOFORDER_LIKELYHOOD_PER_CHECKIN = 10; //max 1000

    private static final int TERMINAL_EVENT_LIKELYHOOD_PER_BATCH = 100; //max 100

    private final Random random;

    public EventGenerator() {
        this.random = new Random();
    }

    public List<BaseTerminalEvent> generateBatch() {
        List<BaseTerminalEvent> batch = new LinkedList<BaseTerminalEvent>();

        // Sometimes generate a low paper or out-of-order event
        List<String> terminalOutOfOrderIds = generateTerminalEvent(batch);

        // Generate a couple of checkin, completed and cancelled events, and sometimes an out-of-order
        generateCheckin(batch, terminalOutOfOrderIds);

        return batch;
    }

    private void generateCheckin(List<BaseTerminalEvent> eventBatch, List<String> outOfOrderIds) {
        // Generate unique terminal ids
        String[] termIds = new String[TERMINAL_COUNT];
        for (int i = 0; i < termIds.length; i++) {
            termIds[i] = Integer.toString(i);
        }

        // Swap terminals to get a random ordering
        randomize(termIds);

        // Add a check-in event for each
        for (String termId : termIds) {
            if (outOfOrderIds.contains(termId)) {
                continue;
            }
            Checkin checkin = new Checkin(new Terminal(termId));
            eventBatch.add(checkin);
        }

        // Add a cancelled or completed or out-of-order for each
        int completedCount = 0;
        int cancelledCount = 0;
        int outOfOrderCount = 0;
        int slowCustomer = 0;
        for (String termId : termIds) {
            if (outOfOrderIds.contains(termId)) {
                continue;
            }

            BaseTerminalEvent theEvent = null;

            // With a x in 1000 chance send an OutOfOrder
            if (random.nextInt(1000) < TERMINAL_OUTOFORDER_LIKELYHOOD_PER_CHECKIN) {
                outOfOrderCount++;
                theEvent = new OutOfOrder(new Terminal(termId));
                System.out.println("\tGenerated a Checkin followed by " + theEvent.getType() + " event for terminal " + theEvent.getTerminal().getId());
            } else if (random.nextInt(3) < 1) {
                completedCount++;
                theEvent = new Completed(new Terminal(termId));
            } else if (random.nextInt(3) < 2) {
                cancelledCount++;
                theEvent = new Cancelled(new Terminal(termId));
            } else {
                slowCustomer++;
                // 33% of customers are doing checking over 2 runs (reading all screens and talking etc)
                // this means we 'll have pending Checkin events in the system
                // that can later be completed, cancelled, and for which there can also be new Checkin events
                // that is a customer waiting after another customer to finish using this Terminal
                // By counting orphan Checkin events we can spot terminals for which there is a traffic jam or
                // compute latency statistics etc.
                //
                // Note
                // Further simulation should add a repair action that re-activate a terminal and keep tracks
                // of the terminal status between each batch
            }

            if (theEvent != null) {
                eventBatch.add(theEvent);
            }
        }

        System.out.println("\t=> Generated " + termIds.length + " Checkin events followed by " +
                completedCount + " Completed, " +
                cancelledCount + " Cancelled, " +
                outOfOrderCount + " OutOfOrder events" +
                " (" + slowCustomer + " pending)");
    }

    private List<String> generateTerminalEvent(List<BaseTerminalEvent> eventBatch) {
        List<String> outOfOrder = new ArrayList<String>();
        if (random.nextInt(100) >= TERMINAL_EVENT_LIKELYHOOD_PER_BATCH) {
            return outOfOrder;
        }

        BaseTerminalEvent theEvent = null;
        if (random.nextBoolean()) {
            theEvent = new LowPaper(getRandomTerminal());
        } else {
            Terminal terminal = getRandomTerminal();
            theEvent = new OutOfOrder(terminal);
            outOfOrder.add(terminal.getId());
        }

        eventBatch.add(theEvent);
        System.out.println("\tGenerated " + theEvent.getType() + " event for terminal " + theEvent.getTerminal().getId());
        return outOfOrder;
    }

    // Swap 100 values in the array
    private void randomize(String[] values) {
        for (int i = 0; i < values.length; i++) {
            int pos1 = random.nextInt(values.length);
            int pos2 = random.nextInt(values.length);
            String temp = values[pos2];
            values[pos2] = values[pos1];
            values[pos1] = temp;
        }
    }

    private Terminal getRandomTerminal() {
        return new Terminal(getRandomTerminalId());
    }

    private String getRandomTerminalId() {
        int id = random.nextInt(TERMINAL_COUNT);
        return Integer.toString(id);
    }
}
