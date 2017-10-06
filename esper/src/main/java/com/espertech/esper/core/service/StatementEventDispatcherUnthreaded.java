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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Dispatcher for statement lifecycle events to service provider statement state listeners.
 */
public class StatementEventDispatcherUnthreaded implements StatementLifecycleObserver {
    private final static Logger log = LoggerFactory.getLogger(StatementEventDispatcherUnthreaded.class);
    private final EPServiceProvider serviceProvider;
    private final Iterable<EPStatementStateListener> statementListeners;

    /**
     * Ctor.
     *
     * @param serviceProvider    engine instance
     * @param statementListeners listeners to dispatch to
     */
    public StatementEventDispatcherUnthreaded(EPServiceProvider serviceProvider, Iterable<EPStatementStateListener> statementListeners) {
        this.serviceProvider = serviceProvider;
        this.statementListeners = statementListeners;
    }

    public void observe(StatementLifecycleEvent theEvent) {
        if (theEvent.getEventType() == StatementLifecycleEvent.LifecycleEventType.CREATE) {
            Iterator<EPStatementStateListener> it = statementListeners.iterator();
            for (; it.hasNext(); ) {
                try {
                    it.next().onStatementCreate(serviceProvider, theEvent.getStatement());
                } catch (RuntimeException ex) {
                    log.error("Caught runtime exception in onStatementCreate callback:" + ex.getMessage(), ex);
                }
            }
        } else if (theEvent.getEventType() == StatementLifecycleEvent.LifecycleEventType.STATECHANGE) {
            Iterator<EPStatementStateListener> it = statementListeners.iterator();
            for (; it.hasNext(); ) {
                try {
                    it.next().onStatementStateChange(serviceProvider, theEvent.getStatement());
                } catch (RuntimeException ex) {
                    log.error("Caught runtime exception in onStatementCreate callback:" + ex.getMessage(), ex);
                }
            }
        }
    }
}
