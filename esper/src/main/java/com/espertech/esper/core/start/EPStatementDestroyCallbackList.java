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
package com.espertech.esper.core.start;

import com.espertech.esper.util.DestroyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Method to call to destroy an EPStatement.
 */
public class EPStatementDestroyCallbackList implements EPStatementDestroyMethod {
    private static final Logger log = LoggerFactory.getLogger(EPStatementDestroyCallbackList.class);

    private Deque<DestroyCallback> callbacks;

    public void addCallback(DestroyCallback destroyCallback) {
        if (callbacks == null) {
            callbacks = new ArrayDeque<DestroyCallback>(2);
        }
        callbacks.add(destroyCallback);
    }

    public void destroy() {
        if (callbacks == null) {
            return;
        }
        for (DestroyCallback destroyCallback : callbacks) {
            try {
                destroyCallback.destroy();
            } catch (RuntimeException ex) {
                log.error("Failed to destroy resource: " + ex.getMessage(), ex);
            }
        }
    }
}
