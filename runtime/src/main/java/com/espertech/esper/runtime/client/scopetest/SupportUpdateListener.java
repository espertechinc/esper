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
package com.espertech.esper.runtime.client.scopetest;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.ScopeTestHelper;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.LinkedList;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertNameValuePairs;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.*;

/**
 * Update listener that retains the events it receives for use in assertions.
 */
public class SupportUpdateListener implements UpdateListener, SupportListener {
    private final List<EventBean[]> newDataList;
    private final List<EventBean[]> oldDataList;
    private EventBean[] lastNewData;
    private EventBean[] lastOldData;
    private boolean isInvoked;

    /**
     * Ctor.
     */
    public SupportUpdateListener() {
        newDataList = new LinkedList<EventBean[]>();
        oldDataList = new LinkedList<EventBean[]>();
    }

    /**
     * Wait for an invocation up to the given milliseconds
     *
     * @param msecWait wait time
     */
    public void waitForInvocation(long msecWait) {
        long startTime = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis() - startTime) > msecWait) {
                throw new RuntimeException("No result received");
            }
            if (isInvoked()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Wait for the listener invocation for up to the given number of milliseconds.
     *
     * @param msecWait          to wait
     * @param numberOfNewEvents in any number of separate invocations required before returning
     * @throws RuntimeException when no results or insufficient number of events were received
     */
    public void waitForInvocation(long msecWait, int numberOfNewEvents) {
        long startTime = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis() - startTime) > msecWait) {
                throw new RuntimeException("No events or less then the number of expected events received, expected " + numberOfNewEvents + " received " + getNewDataListFlattened().length);
            }

            EventBean[] events = getNewDataListFlattened();
            if (events.length >= numberOfNewEvents) {
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void update(EventBean[] newData, EventBean[] oldData, EPStatement statement, EPRuntime runtime) {
        this.oldDataList.add(oldData);
        this.newDataList.add(newData);

        this.lastNewData = newData;
        this.lastOldData = oldData;

        isInvoked = true;
    }

    public void assertInvokedFlagAndReset(boolean expected) {
        assertEquals(expected, getIsInvokedAndReset());
    }

    /**
     * Reset listener, clearing all associated state.
     */
    public synchronized void reset() {
        this.oldDataList.clear();
        this.newDataList.clear();
        this.lastNewData = null;
        this.lastOldData = null;
        isInvoked = false;
    }

    public EventBean[] getLastNewData() {
        return lastNewData;
    }

    public EventBean[] getLastOldData() {
        return lastOldData;
    }

    public synchronized EventBean[] getAndResetLastNewData() {
        EventBean[] lastNew = lastNewData;
        reset();
        return lastNew;
    }

    public synchronized EventBean[] getAndResetLastOldData() {
        EventBean[] lastOld = lastOldData;
        reset();
        return lastOld;
    }

    public synchronized EventBean assertOneGetNewAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            fail("No new-data events received");
        }
        assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        ScopeTestHelper.assertNull("No old-data events are expected but some were received", lastOldData);

        EventBean lastNew = lastNewData[0];
        reset();
        return lastNew;
    }

    public synchronized EventBean assertOneGetOldAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastOldData == null) {
            fail("No old-data events received");
        }
        assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);
        ScopeTestHelper.assertNull("Expected no new-data events", lastNewData);

        EventBean lastNew = lastOldData[0];
        reset();
        return lastNew;
    }

    public synchronized UniformPair<EventBean> assertPairGetIRAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            fail("No new-data events received");
        }
        if (lastOldData == null) {
            fail("No old-data events received");
        }
        assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);

        EventBean lastNew = lastNewData[0];
        EventBean lastOld = lastOldData[0];
        reset();
        return new UniformPair<EventBean>(lastNew, lastOld);
    }

    public synchronized EventBean assertOneGetNew() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            fail("No new-data events received");
        }
        assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        return lastNewData[0];
    }

    public synchronized EventBean assertOneGetOld() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastOldData == null) {
            fail("No old-data events received");
        }
        assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);
        return lastOldData[0];
    }

    public List<EventBean[]> getNewDataList() {
        return newDataList;
    }

    public List<EventBean[]> getOldDataList() {
        return oldDataList;
    }

    public boolean isInvoked() {
        return isInvoked;
    }

    public synchronized boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }

    public synchronized boolean getIsInvokedAndReset() {
        boolean invoked = isInvoked;
        reset();
        return invoked;
    }

    /**
     * Set an array of events as the last insert-stream events received.
     *
     * @param lastNewData to store
     */
    public void setLastNewData(EventBean[] lastNewData) {
        this.lastNewData = lastNewData;
    }

    /**
     * Set an array of events as the last remove-stream events received.
     *
     * @param lastOldData to store
     */
    public void setLastOldData(EventBean[] lastOldData) {
        this.lastOldData = lastOldData;
    }

    public synchronized EventBean[] getNewDataListFlattened() {
        return flatten(newDataList);
    }

    /**
     * Returns an event array that represents all remove-stream events received so far.
     *
     * @return event array
     */
    public synchronized EventBean[] getOldDataListFlattened() {
        return flatten(oldDataList);
    }

    private EventBean[] flatten(List<EventBean[]> list) {
        int count = 0;
        for (EventBean[] events : list) {
            if (events != null) {
                count += events.length;
            }
        }

        EventBean[] array = new EventBean[count];
        count = 0;
        for (EventBean[] events : list) {
            if (events != null) {
                for (int i = 0; i < events.length; i++) {
                    array[count++] = events[i];
                }
            }
        }
        return array;
    }

    public synchronized UniformPair<EventBean[]> assertInvokedAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);
        assertEquals("Received more then one invocation", 1, getNewDataList().size());
        assertEquals("Received more then one invocation", 1, getOldDataList().size());
        EventBean[] newEvents = getLastNewData();
        EventBean[] oldEvents = getLastOldData();
        reset();
        return new UniformPair<EventBean[]>(newEvents, oldEvents);
    }

    /**
     * Assert not invoked.
     */
    public synchronized void assertNotInvoked() {
        assertFalse(isInvoked);
    }

    public synchronized UniformPair<EventBean[]> getDataListsFlattened() {
        return new UniformPair<EventBean[]>(flatten(newDataList), flatten(oldDataList));
    }

    public synchronized UniformPair<EventBean[]> getAndResetDataListsFlattened() {
        UniformPair pair = getDataListsFlattened();
        reset();
        return pair;
    }

    /**
     * For multiple listeners, return the invoked flags and reset each listener
     *
     * @param listeners listeners
     * @return invoked flags
     */
    public static boolean[] getInvokedFlagsAndReset(SupportUpdateListener[] listeners) {
        boolean[] invoked = new boolean[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            invoked[i] = listeners[i].getIsInvokedAndReset();
        }
        return invoked;
    }

    /**
     * Produce an array of listeners
     *
     * @param size of array
     * @return array of listeners
     */
    public static SupportUpdateListener[] makeListeners(int size) {
        SupportUpdateListener[] listeners = new SupportUpdateListener[size];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportUpdateListener();
        }
        return listeners;
    }

    public void assertNewOldData(Object[][] nameAndValuePairsIStream, Object[][] nameAndValuePairsRStream) {
        assertEquals(1, newDataList.size());
        assertEquals(1, oldDataList.size());
        assertNameValuePairs(lastNewData, nameAndValuePairsIStream);
        assertNameValuePairs(lastOldData, nameAndValuePairsRStream);
        reset();
    }

    public UniformPair<EventBean> assertGetAndResetIRPair() {
        assertTrue(isInvoked);
        assertEquals(1, newDataList.size());
        assertEquals(1, oldDataList.size());
        assertNotNull(newDataList.get(0));
        assertEquals(1, newDataList.get(0).length);
        assertNotNull(oldDataList.get(0));
        assertEquals(1, oldDataList.get(0).length);
        EventBean newEvent = newDataList.get(0)[0];
        EventBean oldEvent = oldDataList.get(0)[0];
        reset();
        return new UniformPair<EventBean>(newEvent, oldEvent);
    }

    public UniformPair<EventBean[]> getAndResetIRPair() {
        EventBean[] newData = lastNewData;
        EventBean[] oldData = lastOldData;
        reset();
        return new UniformPair<EventBean[]>(newData, oldData);
    }
}
