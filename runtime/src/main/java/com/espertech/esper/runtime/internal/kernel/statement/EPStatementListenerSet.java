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
package com.espertech.esper.runtime.internal.kernel.statement;

import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.runtime.client.UpdateListener;

/**
 * Provides update listeners for use by statement instances, and the management methods around these.
 * <p>
 * The collection of update listeners is based on copy-on-write:
 * When the runtimedispatches events to a set of listeners, then while iterating through the set there
 * may be listeners added or removed (the listener may remove itself).
 * Additionally, events may be dispatched by multiple threads to the same listener.
 */
public class EPStatementListenerSet {
    private final static UpdateListener[] EMPTY_UPDLISTEN_ARRAY = new UpdateListener[0];

    private Object subscriber;
    private String subscriberMethodName;
    private volatile UpdateListener[] listeners;


    /**
     * Ctor.
     */
    public EPStatementListenerSet() {
        listeners = EMPTY_UPDLISTEN_ARRAY;
    }

    public EPStatementListenerSet(UpdateListener[] listeners) {
        this.listeners = listeners;
    }

    /**
     * Returns the set of listeners to the statement.
     *
     * @return statement listeners
     */
    public UpdateListener[] getListeners() {
        return listeners;
    }

    /**
     * Set the update listener set to use.
     *
     * @param listenerSet a collection of update listeners
     */
    public void setListeners(EPStatementListenerSet listenerSet) {
        this.listeners = listenerSet.getListeners();
    }

    /**
     * Add a listener to the statement.
     *
     * @param listener to add
     */
    public synchronized void addListener(UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener reference supplied");
        }

        for (UpdateListener existing : listeners) {
            if (existing == listener) {
                return;
            }
        }
        listeners = (UpdateListener[]) CollectionUtil.arrayExpandAddSingle(listeners, listener);
    }

    /**
     * Remove a listeners to a statement.
     *
     * @param listener to remove
     */
    public synchronized void removeListener(UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener reference supplied");
        }

        int index = -1;
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return;
        }
        listeners = (UpdateListener[]) CollectionUtil.arrayShrinkRemoveSingle(listeners, index);
    }

    /**
     * Remove all listeners to a statement.
     */
    public synchronized void removeAllListeners() {
        listeners = EMPTY_UPDLISTEN_ARRAY;
    }

    /**
     * Sets a subscriber instance.
     *
     * @param subscriber is the subscriber to set
     * @param methodName method name
     */
    public void setSubscriber(Object subscriber, String methodName) {
        this.subscriber = subscriber;
        this.subscriberMethodName = methodName;
    }

    /**
     * Returns the subscriber instance.
     *
     * @return subscriber
     */
    public Object getSubscriber() {
        return subscriber;
    }

    public String getSubscriberMethodName() {
        return subscriberMethodName;
    }
}
