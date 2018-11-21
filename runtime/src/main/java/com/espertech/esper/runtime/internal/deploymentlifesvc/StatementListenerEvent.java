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
package com.espertech.esper.runtime.internal.deploymentlifesvc;

import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

/**
 * Event indicating statement lifecycle management.
 */
public class StatementListenerEvent {
    private EPStatement statement;
    private ListenerEventType eventType;
    private UpdateListener listener;

    /**
     * Event types.
     */
    public static enum ListenerEventType {
        /**
         * listener added
         */
        LISTENER_ADD,
        /**
         * Listener removed.
         */
        LISTENER_REMOVE,
        /**
         * All listeners removed.
         */
        LISTENER_REMOVE_ALL
    }

    /**
     * Ctor.
     *
     * @param statement  the statement
     * @param eventType  the type of event
     * @param listener the listener
     */
    public StatementListenerEvent(EPStatement statement, ListenerEventType eventType, UpdateListener listener) {
        this.statement = statement;
        this.eventType = eventType;
        this.listener = listener;
    }

    public StatementListenerEvent(EPStatement statement, ListenerEventType eventType) {
        this(statement, eventType, null);
    }

    /**
     * Returns the statement instance for the event.
     *
     * @return statement
     */
    public EPStatement getStatement() {
        return statement;
    }

    /**
     * Returns the event type.
     *
     * @return type of event
     */
    public ListenerEventType getEventType() {
        return eventType;
    }

    /**
     * Returns the listener
     * @return listener
     */
    public UpdateListener getListener() {
        return listener;
    }
}
