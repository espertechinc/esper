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

import java.util.ArrayList;

public class WorkQueueWPrecedenceNoLatch implements WorkQueue {

    private final ArrayList<WorkQueueItemPrecedenced> front = new ArrayList<>();
    private final ArrayList<WorkQueueItemPrecedenced> back = new ArrayList<>();

    public void add(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence) {
        WorkQueueItemPrecedenced item = new WorkQueueItemPrecedenced(theEvent, precedence);
        if (addToFront) {
            insert(item, front);
        } else {
            insert(item, back);
        }
    }

    public void add(EventBean theEvent) {
        back.add(new WorkQueueItemPrecedenced(theEvent, 0));
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

    private static void insert(WorkQueueItemPrecedenced item, ArrayList<WorkQueueItemPrecedenced> queue) {
        WorkQueueUtil.insert(item, queue);
    }

    private static boolean process(ArrayList<WorkQueueItemPrecedenced> queue, EPEventServiceQueueProcessor epEventService) {
        if (queue.isEmpty()) {
            return false;
        }
        WorkQueueItemPrecedenced item = queue.remove(0);
        epEventService.processThreadWorkQueueUnlatched(item.getLatchOrBean());
        return true;
    }
}
