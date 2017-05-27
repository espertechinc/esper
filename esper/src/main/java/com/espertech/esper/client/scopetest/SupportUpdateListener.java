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
package com.espertech.esper.client.scopetest;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.collection.UniformPair;

import java.util.LinkedList;
import java.util.List;

/**
 * Update listener that retains the events it receives for use in assertions.
 */
public class SupportUpdateListener implements UpdateListener {
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
     * Wait for the listener invocation for up to the given number of milliseconds.
     *
     * @param msecWait to wait
     * @throws RuntimeException when no results were received
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

    public synchronized void update(EventBean[] newData, EventBean[] oldData) {
        this.oldDataList.add(oldData);
        this.newDataList.add(newData);

        this.lastNewData = newData;
        this.lastOldData = oldData;

        isInvoked = true;
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

    /**
     * Returns the last array of events (insert stream) that were received.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public EventBean[] getLastNewData() {
        return lastNewData;
    }

    /**
     * Returns the last array of remove-stream events that were received.
     *
     * @return remove stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public EventBean[] getLastOldData() {
        return lastOldData;
    }

    /**
     * Returns the last array of events (insert stream) that were received and resets the listener.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public synchronized EventBean[] getAndResetLastNewData() {
        EventBean[] lastNew = lastNewData;
        reset();
        return lastNew;
    }

    /**
     * Returns the last array of events (insert stream) that were received and resets the listener.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public synchronized EventBean[] getAndResetLastOldData() {
        EventBean[] lastOld = lastOldData;
        reset();
        return lastOld;
    }

    /**
     * Asserts that exactly one insert stream event was received and no remove stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single insert-stream event
     */
    public synchronized EventBean assertOneGetNewAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            ScopeTestHelper.fail("No new-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        ScopeTestHelper.assertNull("No old-data events are expected but some were received", lastOldData);

        EventBean lastNew = lastNewData[0];
        reset();
        return lastNew;
    }

    /**
     * Asserts that exactly one remove stream event was received and no insert stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single remove-stream event
     */
    public synchronized EventBean assertOneGetOldAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastOldData == null) {
            ScopeTestHelper.fail("No old-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);
        ScopeTestHelper.assertNull("Expected no new-data events", lastNewData);

        EventBean lastNew = lastOldData[0];
        reset();
        return lastNew;
    }

    /**
     * Asserts that exactly one insert stream event and exactly one remove stream event was received, resets the listener clearing all state and returns the received events as a pair.
     *
     * @return pair of insert-stream and remove-stream events
     */
    public synchronized UniformPair<EventBean> assertPairGetIRAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            ScopeTestHelper.fail("No new-data events received");
        }
        if (lastOldData == null) {
            ScopeTestHelper.fail("No old-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        ScopeTestHelper.assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);

        EventBean lastNew = lastNewData[0];
        EventBean lastOld = lastOldData[0];
        reset();
        return new UniformPair<EventBean>(lastNew, lastOld);
    }

    /**
     * Asserts that exactly one insert stream event was received not checking remove stream data, and returns the received event.
     *
     * @return single insert-stream event
     */
    public synchronized EventBean assertOneGetNew() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            ScopeTestHelper.fail("No new-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        return lastNewData[0];
    }

    /**
     * Asserts that exactly one remove stream event was received not checking insert stream data, and returns the received event.
     *
     * @return single remove-stream event
     */
    public synchronized EventBean assertOneGetOld() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastOldData == null) {
            ScopeTestHelper.fail("No old-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);
        return lastOldData[0];
    }

    /**
     * Get a list of all insert-stream event arrays received.
     *
     * @return list of event arrays
     */
    public List<EventBean[]> getNewDataList() {
        return newDataList;
    }

    /**
     * Get a list of all remove-stream event arrays received.
     *
     * @return list of event arrays
     */
    public List<EventBean[]> getOldDataList() {
        return oldDataList;
    }

    /**
     * Returns true if the listener was invoked at least once.
     *
     * @return invoked flag
     */
    public boolean isInvoked() {
        return isInvoked;
    }

    /**
     * Returns true if the listener was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
    public synchronized boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }

    /**
     * Returns true if the listener was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
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

    /**
     * Returns an event array that represents all insert-stream events received so far.
     *
     * @return event array
     */
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

    /**
     * Returns a pair of insert and remove stream event arrays considering the last invocation only,
     * asserting that only a single invocation occured, and resetting the listener.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    public synchronized UniformPair<EventBean[]> assertInvokedAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);
        ScopeTestHelper.assertEquals("Received more then one invocation", 1, getNewDataList().size());
        ScopeTestHelper.assertEquals("Received more then one invocation", 1, getOldDataList().size());
        EventBean[] newEvents = getLastNewData();
        EventBean[] oldEvents = getLastOldData();
        reset();
        return new UniformPair<EventBean[]>(newEvents, oldEvents);
    }

    /**
     * Returns a pair of insert and remove stream event arrays considering the all invocations.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    public synchronized UniformPair<EventBean[]> getDataListsFlattened() {
        return new UniformPair<EventBean[]>(flatten(newDataList), flatten(oldDataList));
    }

    /**
     * Returns a pair of insert and remove stream event arrays considering the all invocations, and resets the listener.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    public synchronized UniformPair<EventBean[]> getAndResetDataListsFlattened() {
        UniformPair pair = getDataListsFlattened();
        reset();
        return pair;
    }

    /**
     * For multiple listeners, return the invoked flags and reset each listener
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
}
