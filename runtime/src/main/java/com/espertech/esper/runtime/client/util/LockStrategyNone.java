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
package com.espertech.esper.runtime.client.util;

import com.espertech.esper.common.internal.util.ManagedReadWriteLock;

/**
 * Obtains the write lock of the runtime-wide event processing read-write lock by simply blocking until the lock was obtained.
 */
public class LockStrategyNone implements LockStrategy {

    /**
     * Instance of a lock strategy that does not obtain a lock.
     */
    public final static LockStrategyNone INSTANCE = new LockStrategyNone();

    private LockStrategyNone() {
    }

    public void acquire(ManagedReadWriteLock runtimeWideLock) throws LockStrategyException {
    }

    public void release(ManagedReadWriteLock runtimeWideLock) {
    }
}
