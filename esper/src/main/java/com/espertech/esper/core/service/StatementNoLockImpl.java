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
package com.espertech.esper.core.service;

import java.util.concurrent.locks.Lock;

/**
 * A Statement-lock implementation that doesn't lock.
 */
public class StatementNoLockImpl implements StatementAgentInstanceLock {
    private final String name;

    /**
     * Ctor.
     *
     * @param name of lock
     */
    public StatementNoLockImpl(String name) {
        this.name = name;
    }

    /**
     * Lock write lock.
     */
    public void acquireWriteLock() {
    }

    /**
     * Lock write lock.
     */
    public boolean acquireWriteLock(long msecTimeout) {
        return true;
    }

    /**
     * Unlock write lock.
     */
    public void releaseWriteLock() {
    }

    /**
     * Lock read lock.
     */
    public void acquireReadLock() {
    }

    /**
     * Unlock read lock.
     */
    public void releaseReadLock() {
    }

    public String toString() {
        return this.getClass().getSimpleName() + " name=" + name;
    }

    public boolean addAcquiredLock(Lock lock) {
        return false;
    }
}
