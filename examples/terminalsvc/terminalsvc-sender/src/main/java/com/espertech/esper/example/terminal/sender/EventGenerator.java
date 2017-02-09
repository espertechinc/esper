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
package com.espertech.esper.example.terminal.sender;

import com.espertech.esper.example.terminal.common.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class EventGenerator {
    private final Random random;

    public EventGenerator() {
        this.random = new Random();
    }

    public List<BaseTerminalEvent> generateBatch() {
        List<BaseTerminalEvent> batch = new LinkedList<BaseTerminalEvent>();

        // Sometimes generate a low paper or out-of-order event
        generateTerminalEvent(batch);

        // Generate a couple of checkin, completed and cancelled events, and sometimes an out-of-order
        generateCheckin(batch);

        return batch;
    }

    private void generateCheckin(List<BaseTerminalEvent> eventBatch) {
        // Generate up to 100 unique terminal ids between 100 and 200
        String[] termIds = new String[100];
        for (int i = 0; i < termIds.length; i++) {
            termIds[i] = Long.toString(i + 1000);
        }

        // Swap terminals to get a random ordering
        randomize(termIds);

        // Add a check-in event for each
        for (int i = 0; i < termIds.length; i++) {
            Checkin checkin = new Checkin(new TerminalInfo(termIds[i]));
            eventBatch.add(checkin);
        }

        // Add a cancelled or completed or out-of-order for each
        int completedCount = 0;
        int cancelledCount = 0;
        int outOfOrderCount = 0;
        for (int i = 0; i < termIds.length; i++) {
            BaseTerminalEvent theEvent = null;

            // With a 1 in 1000 chance send an OutOfOrder
            if (random.nextInt(1000) == 0) {
                outOfOrderCount++;
                theEvent = new OutOfOrder(new TerminalInfo(termIds[i]));
                System.out.println("Generated an Checkin followed by " + theEvent.getType() + " event for terminal " + theEvent.getTerm().getId());
            } else if (random.nextBoolean()) {
                completedCount++;
                theEvent = new Completed(new TerminalInfo(termIds[i]));
            } else {
                cancelledCount++;
                theEvent = new Cancelled(new TerminalInfo(termIds[i]));
            }

            eventBatch.add(theEvent);
        }

        System.out.println("Generated " + termIds.length + " Checkin events followed by " +
                completedCount + " Completed and " +
                cancelledCount + " Cancelled and " +
                outOfOrderCount + " OutOfOrder events");
    }

    private void generateTerminalEvent(List<BaseTerminalEvent> eventBatch) {
        if (random.nextInt(10) > 0) {
            return;
        }

        BaseTerminalEvent theEvent = null;
        if (random.nextBoolean()) {
            theEvent = new LowPaper(getRandomTermInfo());
        } else {
            theEvent = new OutOfOrder(getRandomTermInfo());
        }

        eventBatch.add(theEvent);
        System.out.println("Generated " + theEvent.getType() + " event for terminal " + theEvent.getTerm().getId());
    }

    // Swap 100 values in the array
    private void randomize(String[] values) {
        for (int i = 0; i < 100; i++) {
            int pos1 = random.nextInt(values.length);
            int pos2 = random.nextInt(values.length);
            String temp = values[pos2];
            values[pos2] = values[pos1];
            values[pos1] = temp;
        }
    }

    private TerminalInfo getRandomTermInfo() {
        return new TerminalInfo(getRandomTermId());
    }

    private String getRandomTermId() {
        int id = random.nextInt(1000);
        return Integer.toString(id);
    }
}
