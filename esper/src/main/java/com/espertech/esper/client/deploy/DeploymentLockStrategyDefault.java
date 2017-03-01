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
 * Obtains the write lock of the engine-wide event processing read-write lock by simply blocking until the lock was obtained.
 */
public class DeploymentLockStrategyDefault implements DeploymentLockStrategy {

    public final static DeploymentLockStrategyDefault INSTANCE = new DeploymentLockStrategyDefault();

    private DeploymentLockStrategyDefault() {
    }

    public void acquire(ManagedReadWriteLock engineWideLock) throws DeploymentLockException {
        engineWideLock.acquireWriteLock();
    }

    public void release(ManagedReadWriteLock engineWideLock) {
        engineWideLock.releaseWriteLock();
    }
}
