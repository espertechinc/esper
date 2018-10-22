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
package com.espertech.esper.common.internal.view.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.FlushedEventBuffer;
import com.espertech.esper.common.internal.support.SupportBean_A;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestBufferView extends TestCase {
    private BufferView bufferView;
    private SupportBufferObserver observer;

    public void setUp() {
        observer = new SupportBufferObserver();
        bufferView = new BufferView(1);
        bufferView.setObserver(observer);
    }

    public void testUpdate() {
        // Observer starts with no data
        assertFalse(observer.getAndResetHasNewData());

        // Send some data
        EventBean newEvents[] = makeBeans("n", 1);
        EventBean oldEvents[] = makeBeans("o", 1);
        bufferView.update(newEvents, oldEvents);

        // make sure received
        assertTrue(observer.getAndResetHasNewData());
        assertEquals(1, observer.getAndResetStreamId());
        assertNotNull(observer.getAndResetNewEventBuffer());
        assertNotNull(observer.getAndResetOldEventBuffer());

        // Reset and send null data
        assertFalse(observer.getAndResetHasNewData());
        bufferView.update(null, null);
        assertTrue(observer.getAndResetHasNewData());
    }

    private EventBean[] makeBeans(String id, int numTrades) {
        EventBean[] trades = new EventBean[numTrades];
        for (int i = 0; i < numTrades; i++) {
            SupportBean_A bean = new SupportBean_A(id + i);
            trades[i] = SupportEventBeanFactory.createObject(bean);
        }
        return trades;
    }

    public static class SupportBufferObserver implements BufferObserver {
        private boolean hasNewData;
        private int streamId;
        private FlushedEventBuffer newEventBuffer;
        private FlushedEventBuffer oldEventBuffer;

        public boolean getAndResetHasNewData() {
            boolean result = hasNewData;
            hasNewData = false;
            return result;
        }

        public void newData(int streamId, FlushedEventBuffer newEventBuffer, FlushedEventBuffer oldEventBuffer) {
            if (hasNewData == true) {
                throw new IllegalStateException("Observer already has new data");
            }

            hasNewData = true;
            this.streamId = streamId;
            this.newEventBuffer = newEventBuffer;
            this.oldEventBuffer = oldEventBuffer;
        }

        public int getAndResetStreamId() {
            int id = streamId;
            streamId = 0;
            return id;
        }

        public FlushedEventBuffer getAndResetNewEventBuffer() {
            FlushedEventBuffer buf = newEventBuffer;
            newEventBuffer = null;
            return buf;
        }

        public FlushedEventBuffer getAndResetOldEventBuffer() {
            FlushedEventBuffer buf = oldEventBuffer;
            oldEventBuffer = null;
            return buf;
        }
    }
}