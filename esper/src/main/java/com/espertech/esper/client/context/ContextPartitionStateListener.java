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
package com.espertech.esper.client.context;

/**
 * Listener for context-specific partition-related events. See @link{{@link ContextStateListener} for context-creation and state.}
 */
public interface ContextPartitionStateListener {
    /**
     * Invoked when a context is activated.
     * @param event event
     */
    void onContextActivated(ContextStateEventContextActivated event);

    /**
     * Invoked when a context is de-activated.
     * @param event event
     */
    void onContextDeactivated(ContextStateEventContextDeactivated event);

    /**
     * Invoked when a statement is added to a context.
     * @param event event
     */
    void onContextStatementAdded(ContextStateEventContextStatementAdded event);

    /**
     * Invoked when a statement is removed from a context.
     * @param event event
     */
    void onContextStatementRemoved(ContextStateEventContextStatementRemoved event);

    /**
     * Invoked when a context partition is allocated, provided once per context
     * and per partition independent of the number of statements.
     * @param event event
     */
    void onContextPartitionAllocated(ContextStateEventContextPartitionAllocated event);

    /**
     * Invoked when a context partition is destroyed, provided once per context
     * and per partition independent of the number of statements.
     * @param event event
     */
    void onContextPartitionDeallocated(ContextStateEventContextPartitionDeallocated event);
}
