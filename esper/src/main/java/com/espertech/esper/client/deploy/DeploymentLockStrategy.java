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

/**
 * Implement this interface to provide a custom deployment lock strategy.
 * The default lock strategy is {@link DeploymentLockStrategyDefault}.
 */
public interface DeploymentLockStrategy {
    /**
     * Acquire should acquire the write lock of the provided read-write lock and may retry and backoff or fail.
     * @param engineWideLock the engine-wide event processing read-write lock
     * @throws DeploymentLockException to indicate lock attempt failed
     * @throws InterruptedException when lock-taking is interrupted
     */
    void acquire(ManagedReadWriteLock engineWideLock) throws DeploymentLockException, InterruptedException;

    /**
     * Release should release the write lock of the provided read-write lock and should never fail.
     * @param engineWideLock the engine-wide event processing read-write lock
     */
    void release(ManagedReadWriteLock engineWideLock);
}
