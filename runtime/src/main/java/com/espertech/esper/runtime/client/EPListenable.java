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
package com.espertech.esper.runtime.client;

import java.util.Iterator;

/**
 * Interface to add and remove update listeners.
 */
public interface EPListenable {
    /**
     * Add a listener that observes events.
     *
     * @param listener to add
     * @throws IllegalStateException when attempting to add a listener to a destroyed statement
     */
    void addListener(UpdateListener listener);

    /**
     * Remove a listener that observes events.
     *
     * @param listener to remove
     */
    void removeListener(UpdateListener listener);

    /**
     * Remove all listeners.
     */
    void removeAllListeners();

    /**
     * Returns an iterator of update listeners.
     * <p>
     * The returned iterator does not allow the remove operation.
     *
     * @return iterator of update listeners
     */
    Iterator<UpdateListener> getUpdateListeners();

    /**
     * Add an update listener replaying current statement results to the listener.
     * <p>
     * The listener receives current statement results as the first call to the update method
     * of the listener, passing in the newEvents parameter the current statement results as an array of zero or more events.
     * Subsequent calls to the update method of the listener are statement results.
     * <p>
     * Current statement results are the events returned by the iterator or safeIterator methods.
     * <p>
     * Delivery of current statement results in the first call is performed by the same thread invoking this method,
     * while subsequent calls to the listener may deliver statement results by the same or other threads.
     * <p>
     * Note: this is a blocking call, delivery is atomic: Events occurring during iteration and
     * delivery to the listener are guaranteed to be delivered in a separate call and not lost.
     * The listener implementation should minimize long-running or blocking operations.
     * <p>
     * Delivery is only atomic relative to the current statement. If the same listener instance is
     * registered with other statements it may receive other statement result
     * s simultaneously.
     * <p>
     * If a statement is not started an therefore does not have current results, the listener
     * receives a single invocation with a null value in newEvents.
     *
     * @param listener to add
     */
    void addListenerWithReplay(UpdateListener listener);
}

