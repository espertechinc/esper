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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple read-write lock based on {@link ReentrantReadWriteLock} that associates a
 * name with the lock and logs  read/write locking and unlocking.
 */
public class StatementAgentInstanceLockRWLogging implements StatementAgentInstanceLock {
    private static final Logger LOCK_LOG = LoggerFactory.getLogger(AuditPath.LOCK_LOG);
    private static final String WRITE = "write";
    private static final String READ = "read ";

    private final ReentrantReadWriteLock lock;
    private final String lockId;
    private final String statementName;
    private final int cpid;

    /**
     * Ctor.
     *
     * @param isFair true if a fair lock, false if not
     */
    public StatementAgentInstanceLockRWLogging(boolean isFair, String statementName, int cpid) {
        this.lock = new ReentrantReadWriteLock(isFair);
        this.lockId = "RWLock@" + Integer.toHexString(System.identityHashCode(lock));
        this.statementName = statementName;
        this.cpid = cpid;
    }

    /**
     * Lock write lock.
     */
    public void acquireWriteLock() {
        output(ACQUIRE_TEXT, WRITE, -1);
        lock.writeLock().lock();
        output(ACQUIRED_TEXT, WRITE, -1);
    }

    public boolean acquireWriteLock(long msecTimeout) {
        output(ACQUIRE_TEXT, WRITE, msecTimeout);

        boolean result = false;
        try {
            result = lock.writeLock().tryLock(msecTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        output(ACQUIRED_TEXT, WRITE, -1);
        return result;
    }

    /**
     * Unlock write lock.
     */
    public void releaseWriteLock() {
        output(RELEASE_TEXT, WRITE, -1);
        lock.writeLock().unlock();
        output(RELEASED_TEXT, WRITE, -1);
    }

    /**
     * Lock read lock.
     */
    public void acquireReadLock() {
        output(ACQUIRE_TEXT, READ, -1);
        lock.readLock().lock();
        output(ACQUIRED_TEXT, READ, -1);
    }

    /**
     * Unlock read lock.
     */
    public void releaseReadLock() {
        output(RELEASE_TEXT, READ, -1);
        lock.readLock().unlock();
        output(RELEASED_TEXT, READ, -1);
    }

    private void output(String action, String lockType, long timeoutMSec) {
        LOCK_LOG.info("{}{} {} stmt '{}' cpid {} timeoutMSec {} readLockCount {} isWriteLocked {}",
                action,
                lockType,
                lockId,
                statementName,
                cpid,
                timeoutMSec,
                lock.getReadLockCount(),
                lock.isWriteLocked());
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }
}
