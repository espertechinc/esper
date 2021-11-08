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
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatch;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchFactory;

import java.util.ArrayList;

import static com.espertech.esper.runtime.internal.kernel.service.WorkQueueNoPrecedenceMayLatch.processMayLatched;

public class WorkQueueWPrecedenceMayLatch implements WorkQueue {

    private final ArrayList<WorkQueueItemPrecedenced> front = new ArrayList<>();
    private final ArrayList<WorkQueueItemPrecedenced> back = new ArrayList<>();

    public void add(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence) {
        if (addToFront) {
            Object latch = epStatementHandle.getInsertIntoFrontLatchFactory().newLatch(theEvent);
            WorkQueueItemPrecedenced item = new WorkQueueItemPrecedenced(latch, precedence);
            insert(item, front);
        } else {
            Object latch = epStatementHandle.getInsertIntoBackLatchFactory().newLatch(theEvent);
            WorkQueueItemPrecedenced item = new WorkQueueItemPrecedenced(latch, precedence);
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

    private static boolean process(ArrayList<WorkQueueItemPrecedenced> queue, EPEventServiceQueueProcessor epEventService) {
        if (queue.isEmpty()) {
            return false;
        }
        WorkQueueItemPrecedenced item = queue.remove(0);
        processMayLatched(item.getLatchOrBean(), epEventService);
        return true;
    }

    private static void insert(WorkQueueItemPrecedenced item, ArrayList<WorkQueueItemPrecedenced> queue) {
        int insertionIndex = WorkQueueUtil.insert(item, queue);

        // Latch shuffling.
        // We are done if there is no latch for the current event
        shuffleLatches(queue, item, insertionIndex);
    }

    private static void shuffleLatches(ArrayList<WorkQueueItemPrecedenced> queue, WorkQueueItemPrecedenced inserted, int insertionIndex) {
        if (!(inserted.getLatchOrBean() instanceof InsertIntoLatch)) {
            return;
        }
        InsertIntoLatchFactory latchFactory = ((InsertIntoLatch) inserted.getLatchOrBean()).getFactory();

        WorkQueueItemPrecedenced currentItem = inserted;
        for (int i = insertionIndex + 1; i < queue.size(); i++) {
            WorkQueueItemPrecedenced olderItem = queue.get(i);
            if (!(olderItem.getLatchOrBean() instanceof InsertIntoLatch)) {
                continue;
            }
            InsertIntoLatch olderItemLatch = (InsertIntoLatch) olderItem.getLatchOrBean();
            if (olderItemLatch.getFactory() != latchFactory) {
                continue;
            }

            // swap latches keeping payload
            EventBean olderItemPayload = olderItemLatch.getEvent();
            EventBean currentItemPayload = ((InsertIntoLatch) (currentItem.getLatchOrBean())).getEvent();
            olderItem.setLatchOrBean(currentItem.getLatchOrBean());
            currentItem.setLatchOrBean(olderItemLatch);
            ((InsertIntoLatch) (olderItem.getLatchOrBean())).setEvent(olderItemPayload);
            ((InsertIntoLatch) (currentItem.getLatchOrBean())).setEvent(currentItemPayload);

            currentItem = olderItem;
        }
    }
}
