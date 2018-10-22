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
 * Implement this interface to provide a custom deployment lock strategy.
 * The default lock strategy is {@link LockStrategyDefault}.
 */
public interface LockStrategy {
    /**
     * Acquire should acquire the write lock of the provided read-write lock and may retry and backoff or fail.
     *
     * @param runtimeWideLock the runtime-wide event processing read-write lock
     * @throws LockStrategyException to indicate lock attempt failed
     * @throws InterruptedException  when lock-taking is interrupted
     */
    void acquire(ManagedReadWriteLock runtimeWideLock) throws LockStrategyException, InterruptedException;

    /**
     * Release should release the write lock of the provided read-write lock and should never fail.
     *
     * @param runtimeWideLock the runtime-wide event processing read-write lock
     */
    void release(ManagedReadWriteLock runtimeWideLock);
}
