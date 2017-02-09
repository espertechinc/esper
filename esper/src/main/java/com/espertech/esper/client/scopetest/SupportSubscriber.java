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

import com.espertech.esper.collection.UniformPair;

import java.util.LinkedList;
import java.util.List;

/**
 * Subscriber for that retains the events it receives for use in assertions.
 */
public class SupportSubscriber {
    private final List<Object[]> newDataList;
    private final List<Object[]> oldDataList;
    private Object[] lastNewData;
    private Object[] lastOldData;
    private boolean isInvoked;

    /**
     * Ctor.
     */
    public SupportSubscriber() {
        newDataList = new LinkedList<Object[]>();
        oldDataList = new LinkedList<Object[]>();
    }

    /**
     * Receive events.
     *
     * @param newData insert stream
     * @param oldData remove stream
     */
    public synchronized void update(Object[] newData, Object[] oldData) {
        this.oldDataList.add(oldData);
        this.newDataList.add(newData);

        this.lastNewData = newData;
        this.lastOldData = oldData;

        isInvoked = true;
    }

    /**
     * Reset subscriber, clearing all associated state.
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
    public Object[] getLastNewData() {
        return lastNewData;
    }

    /**
     * Returns the last array of events (insert stream) that were received and resets the subscriber.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public synchronized Object[] getAndResetLastNewData() {
        Object[] lastNew = lastNewData;
        reset();
        return lastNew;
    }

    /**
     * Asserts that exactly one insert stream event was received and no remove stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single insert-stream event
     */
    public synchronized Object assertOneGetNewAndReset() {
        ScopeTestHelper.assertTrue("Subscriber invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastNewData == null) {
            ScopeTestHelper.fail("No new-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of new-data events", 1, lastNewData.length);
        ScopeTestHelper.assertNull("No old-data events are expected but some were received", lastOldData);

        Object lastNew = lastNewData[0];
        reset();
        return lastNew;
    }

    /**
     * Asserts that exactly one remove stream event was received and no insert stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single remove-stream event
     */
    public Object assertOneGetOldAndReset() {
        ScopeTestHelper.assertTrue("Listener invocation not received but expected", isInvoked);

        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, newDataList.size());
        ScopeTestHelper.assertEquals("Mismatch in the number of invocations", 1, oldDataList.size());

        if (lastOldData == null) {
            ScopeTestHelper.fail("No old-data events received");
        }
        ScopeTestHelper.assertEquals("Mismatch in the number of old-data events", 1, lastOldData.length);
        ScopeTestHelper.assertNull("Expected no new-data events", lastNewData);

        Object lastNew = lastOldData[0];
        reset();
        return lastNew;
    }

    /**
     * Returns the last array of remove-stream events that were received.
     *
     * @return remove stream events or null if either a null value was received or when no events have been received since the last reset
     */
    public Object[] getLastOldData() {
        return lastOldData;
    }

    /**
     * Get a list of all insert-stream event arrays received.
     *
     * @return list of event arrays
     */
    public List<Object[]> getNewDataList() {
        return newDataList;
    }

    /**
     * Get a list of all remove-stream event arrays received.
     *
     * @return list of event arrays
     */
    public List<Object[]> getOldDataList() {
        return oldDataList;
    }

    /**
     * Returns true if the subscriber was invoked at least once.
     *
     * @return invoked flag
     */
    public boolean isInvoked() {
        return isInvoked;
    }

    /**
     * Returns true if the subscriber was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
    public synchronized boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }

    /**
     * Returns an event array that represents all insert-stream events received so far.
     *
     * @return event array
     */
    public synchronized Object[] getNewDataListFlattened() {
        return flatten(newDataList);
    }

    /**
     * Returns an event array that represents all remove-stream events received so far.
     *
     * @return event array
     */
    public synchronized Object[] getOldDataListFlattened() {
        return flatten(oldDataList);
    }

    /**
     * Returns a pair of insert and remove stream event arrays considering the all invocations.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    public synchronized UniformPair<Object[]> getDataListsFlattened() {
        return new UniformPair<Object[]>(flatten(newDataList), flatten(oldDataList));
    }

    private Object[] flatten(List<Object[]> list) {
        int count = 0;
        for (Object[] events : list) {
            if (events != null) {
                count += events.length;
            }
        }

        Object[] array = new Object[count];
        count = 0;
        for (Object[] events : list) {
            if (events != null) {
                for (int i = 0; i < events.length; i++) {
                    array[count++] = events[i];
                }
            }
        }
        return array;
    }
}
