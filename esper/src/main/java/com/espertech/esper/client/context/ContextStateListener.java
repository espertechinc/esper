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
 * Listener for event in respect to context management. See @link{{@link ContextPartitionStateListener} for partition state.}
 */
public interface ContextStateListener {
    /**
     * Invoked when a new context is created.
     * @param event event
     */
    void onContextCreated(ContextStateEventContextCreated event);

    /**
     * Invoked when a context is destroyed.
     * @param event event
     */
    void onContextDestroyed(ContextStateEventContextDestroyed event);
}
