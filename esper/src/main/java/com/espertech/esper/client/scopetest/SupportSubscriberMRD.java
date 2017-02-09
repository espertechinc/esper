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

import java.util.ArrayList;
import java.util.List;

/**
 * Subscriber for multi-row delivery that retains the events it receives for use in assertions.
 */
public class SupportSubscriberMRD {
    private boolean isInvoked;
    private List<Object[][]> insertStreamList = new ArrayList<Object[][]>();
    private List<Object[][]> removeStreamList = new ArrayList<Object[][]>();

    /**
     * Receive multi-row subscriber data through this methods.
     *
     * @param insertStream new data
     * @param removeStream removed data
     */
    public synchronized void update(Object[][] insertStream, Object[][] removeStream) {
        isInvoked = true;
        insertStreamList.add(insertStream);
        removeStreamList.add(insertStream);
    }

    /**
     * Returns all insert-stream events received so far.
     * <p>
     * The list contains an item for each delivery. Each item contains a row with the event and each event is itself a tuple (object array).
     * </p>
     *
     * @return list of Object array-array
     */
    public List<Object[][]> getInsertStreamList() {
        return insertStreamList;
    }

    /**
     * Returns all removed-stream events received so far.
     * <p>
     * The list contains an item for each delivery. Each item contains a row with the event and each event is itself a tuple (object array).
     * </p>
     *
     * @return list of Object array-array
     */
    public List<Object[][]> getRemoveStreamList() {
        return removeStreamList;
    }

    /**
     * Reset subscriber, clearing all associated state.
     */
    public synchronized void reset() {
        isInvoked = false;
        insertStreamList.clear();
        removeStreamList.clear();
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
     * Returns true if the subscriber  was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
    public synchronized boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }
}
