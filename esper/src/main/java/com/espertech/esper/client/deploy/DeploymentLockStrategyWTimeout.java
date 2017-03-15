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
package com.espertech.esper.client.deploy;

import com.espertech.esper.util.ManagedReadWriteLock;

import java.util.concurrent.TimeUnit;

/**
 * Obtains the write lock of the engine-wide event processing read-write lock by trying the lock
 * waiting for the timeout and throwing an exception if the lock was not taken.
 */
public class DeploymentLockStrategyWTimeout implements DeploymentLockStrategy {

    private final long timeout;
    private final TimeUnit unit;

    /**
     * Ctor.
     * @param timeout timeout value in the unit given
     * @param unit unit
     */
    public DeploymentLockStrategyWTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    public void acquire(ManagedReadWriteLock engineWideLock) throws DeploymentLockException, InterruptedException {
        boolean success = engineWideLock.getLock().writeLock().tryLock(timeout, unit);
        if (!success) {
            throw new DeploymentLockException("Failed to obtain write lock of engine-wide processing read-write lock");
        }
    }

    public void release(ManagedReadWriteLock engineWideLock) {
        engineWideLock.getLock().writeLock().unlock();
    }
}
