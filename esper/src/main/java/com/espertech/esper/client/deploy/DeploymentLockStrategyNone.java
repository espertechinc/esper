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
public class DeploymentLockStrategyNone implements DeploymentLockStrategy {

    public final static DeploymentLockStrategyNone INSTANCE = new DeploymentLockStrategyNone();

    private DeploymentLockStrategyNone() {
    }

    public void acquire(ManagedReadWriteLock engineWideLock) throws DeploymentLockException {
    }

    public void release(ManagedReadWriteLock engineWideLock) {
    }
}
