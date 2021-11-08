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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchSpin;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchWait;

import java.util.ArrayDeque;

public class WorkQueueNoPrecedenceMayLatch implements WorkQueue {

    private final ArrayDeque<Object> front = new ArrayDeque<>();
    private final ArrayDeque<Object> back = new ArrayDeque<>();

    public void add(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence) {
        if (addToFront) {
            Object latch = epStatementHandle.getInsertIntoFrontLatchFactory().newLatch(theEvent);
            front.addLast(latch);
        } else {
            Object latch = epStatementHandle.getInsertIntoBackLatchFactory().newLatch(theEvent);
            back.addLast(latch);
        }
    }

    public void add(EventBean theEvent) {
        back.add(theEvent);
    }

    public boolean isFrontEmpty() {
        return front.isEmpty();
    }

    public boolean processFront(EPEventServiceQueueProcessor epEventService) {
        return process(front, epEventService);
    }

    public boolean processBack(EPEventServiceQueueProcessor epEventService) {
        return process(back, epEventService);
    }

    private static boolean process(ArrayDeque<Object> queue, EPEventServiceQueueProcessor epEventService) {
        Object item = queue.poll();
        if (item == null) {
            return false;
        }
        processMayLatched(item, epEventService);
        return true;
    }

    protected static void processMayLatched(Object item, EPEventServiceQueueProcessor epEventService) {
        if (item instanceof InsertIntoLatchSpin) {
            epEventService.processThreadWorkQueueLatchedSpin((InsertIntoLatchSpin) item);
        } else if (item instanceof InsertIntoLatchWait) {
            epEventService.processThreadWorkQueueLatchedWait((InsertIntoLatchWait) item);
        } else {
            epEventService.processThreadWorkQueueUnlatched(item);
        }
    }
}
