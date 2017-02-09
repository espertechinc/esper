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
package com.espertech.esper.client;

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
    public void addListener(UpdateListener listener);

    /**
     * Remove a listener that observes events.
     *
     * @param listener to remove
     */
    public void removeListener(UpdateListener listener);

    /**
     * Remove all listeners.
     */
    public void removeAllListeners();

    /**
     * Add a statement-aware listener that observes events.
     *
     * @param listener to add
     * @throws IllegalStateException when attempting to add a listener to a destroyed statement
     */
    public void addListener(StatementAwareUpdateListener listener);

    /**
     * Remove a statement-aware listener that observes events.
     *
     * @param listener to remove
     */
    public void removeListener(StatementAwareUpdateListener listener);

    /**
     * Returns an iterator of statement-aware update listeners.
     * <p>
     * The returned iterator does not allow the remove operation.
     *
     * @return iterator of statement-aware update listeners
     */
    public Iterator<StatementAwareUpdateListener> getStatementAwareListeners();

    /**
     * Returns an iterator of update listeners.
     * <p>
     * The returned iterator does not allow the remove operation.
     *
     * @return iterator of update listeners
     */
    public Iterator<UpdateListener> getUpdateListeners();
}

