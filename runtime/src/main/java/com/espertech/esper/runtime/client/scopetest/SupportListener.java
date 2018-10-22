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
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.List;

/**
 * Listener interface for use in regression testing for asserting receive events.
 */
public interface SupportListener extends UpdateListener {
    /**
     * Returns a pair of insert and remove stream event arrays considering the last invocation only,
     * asserting that only a single invocation occured, and resetting the listener.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    UniformPair<EventBean[]> assertInvokedAndReset();

    /**
     * Assert the invoked-flag against the expected value and reset the flag
     *
     * @param expected expected value
     */
    void assertInvokedFlagAndReset(boolean expected);

    /**
     * Returns true if the listener was invoked at least once.
     *
     * @return invoked flag
     */
    boolean isInvoked();

    /**
     * Asserts that exactly one insert stream event was received and no remove stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single insert-stream event
     */
    EventBean assertOneGetNewAndReset();

    /**
     * Asserts that exactly one insert stream event and exactly one remove stream event was received, resets the listener clearing all state and returns the received events as a pair.
     *
     * @return pair of insert-stream and remove-stream events
     */
    UniformPair<EventBean> assertPairGetIRAndReset();

    /**
     * Returns true if the listener was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
    boolean getIsInvokedAndReset();

    /**
     * Returns the last array of events (insert stream) that were received and resets the listener.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    EventBean[] getAndResetLastNewData();

    /**
     * Returns the last array of events (insert stream) that were received and resets the listener.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    EventBean[] getAndResetLastOldData();

    /**
     * Get a list of all insert-stream event arrays received.
     *
     * @return list of event arrays
     */
    List<EventBean[]> getNewDataList();

    /**
     * Returns the last array of events (insert stream) that were received.
     *
     * @return insert stream events or null if either a null value was received or when no events have been received since the last reset
     */
    EventBean[] getLastNewData();

    /**
     * Reset listener, clearing all associated state.
     */
    void reset();

    /**
     * Returns true if the listener was invoked at least once and clears the invocation flag.
     *
     * @return invoked flag
     */
    boolean getAndClearIsInvoked();

    /**
     * Returns the last array of remove-stream events that were received.
     *
     * @return remove stream events or null if either a null value was received or when no events have been received since the last reset
     */
    EventBean[] getLastOldData();

    /**
     * Returns an event array that represents all insert-stream events received so far.
     *
     * @return event array
     */
    EventBean[] getNewDataListFlattened();

    /**
     * Returns an event array that represents all remove-stream events received so far.
     *
     * @return event array
     */
    EventBean[] getOldDataListFlattened();

    /**
     * Get a list of all remove-stream event arrays received.
     *
     * @return list of event arrays
     */
    List<EventBean[]> getOldDataList();

    /**
     * Returns a pair of insert and remove stream event arrays considering the all invocations, and resets the listener.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    UniformPair<EventBean[]> getAndResetDataListsFlattened();

    /**
     * Asserts name-value pairs of insert and remove stream events
     *
     * @param nameAndValuePairsIStream insert-stream assertions
     * @param nameAndValuePairsRStream remove-stream assertions
     */
    void assertNewOldData(Object[][] nameAndValuePairsIStream, Object[][] nameAndValuePairsRStream);

    /**
     * Asserts that exactly one remove stream event was received and no insert stream events, resets the listener clearing all state and returns the received event.
     *
     * @return single remove-stream event
     */
    EventBean assertOneGetOldAndReset();

    /**
     * Asserts that there is exactly one insert-stream event and one remove-stream event available and resets.
     *
     * @return pair of insert-stream event and remove-stream event
     */
    UniformPair<EventBean> assertGetAndResetIRPair();

    /**
     * Returns a pair of last-invocation insert and remove stream events and resets
     *
     * @return pair of events
     */
    UniformPair<EventBean[]> getAndResetIRPair();

    /**
     * Returns a pair of insert and remove stream event arrays considering the all invocations.
     *
     * @return pair of event arrays, the first in the pair is the insert stream data, the second in the pair is the remove stream data
     */
    UniformPair<EventBean[]> getDataListsFlattened();

    /**
     * Asserts that exactly one insert stream event was received not checking remove stream data, and returns the received event.
     *
     * @return single insert-stream event
     */
    EventBean assertOneGetNew();

    /**
     * Asserts that exactly one remove stream event was received not checking insert stream data, and returns the received event.
     *
     * @return single remove-stream event
     */
    EventBean assertOneGetOld();

    /**
     * Wait for the listener invocation for up to the given number of milliseconds.
     *
     * @param numberOfNewEvents number of events to await
     * @param msecWait          to wait
     * @throws RuntimeException when no results were received
     */
    void waitForInvocation(long msecWait, int numberOfNewEvents);
}
