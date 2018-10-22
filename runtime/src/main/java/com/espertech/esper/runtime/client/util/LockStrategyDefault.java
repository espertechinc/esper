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
public class LockStrategyDefault implements LockStrategy {

    /**
     * The instance of the default lock strategy.
     */
    public final static LockStrategyDefault INSTANCE = new LockStrategyDefault();

    private LockStrategyDefault() {
    }

    public void acquire(ManagedReadWriteLock runtimeWideLock) throws LockStrategyException {
        runtimeWideLock.acquireWriteLock();
    }

    public void release(ManagedReadWriteLock runtimeWideLock) {
        runtimeWideLock.releaseWriteLock();
    }
}
