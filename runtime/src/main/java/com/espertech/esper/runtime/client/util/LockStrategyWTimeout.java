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

import java.util.concurrent.TimeUnit;

/**
 * Obtains the write lock of the runtime-wide event processing read-write lock by trying the lock
 * waiting for the timeout and throwing an exception if the lock was not taken.
 */
public class LockStrategyWTimeout implements LockStrategy {

    private final long timeout;
    private final TimeUnit unit;

    /**
     * Ctor.
     *
     * @param timeout timeout value in the unit given
     * @param unit    unit
     */
    public LockStrategyWTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    public void acquire(ManagedReadWriteLock runtimeWideLock) throws LockStrategyException, InterruptedException {
        boolean success = runtimeWideLock.getLock().writeLock().tryLock(timeout, unit);
        if (!success) {
            throw new LockStrategyException("Failed to obtain write lock of runtime-wide processing read-write lock");
        }
    }

    public void release(ManagedReadWriteLock runtimeWideLock) {
        runtimeWideLock.getLock().writeLock().unlock();
    }
}
