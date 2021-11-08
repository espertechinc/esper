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

import java.util.ArrayDeque;

public class WorkQueueNoPrecedenceNoLatch implements WorkQueue {

    private final ArrayDeque<EventBean> front = new ArrayDeque<>();
    private final ArrayDeque<EventBean> back = new ArrayDeque<>();

    public void add(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence) {
        if (addToFront) {
            front.addLast(theEvent);
        } else {
            back.addLast(theEvent);
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

    private static boolean process(ArrayDeque<EventBean> queue, EPEventServiceQueueProcessor epEventService) {
        EventBean item = queue.poll();
        if (item == null) {
            return false;
        }
        epEventService.processThreadWorkQueueUnlatched(item);
        return true;
    }
}
