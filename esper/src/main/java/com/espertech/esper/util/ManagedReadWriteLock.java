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
package com.espertech.esper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple read-write lock based on {@link java.util.concurrent.locks.ReentrantReadWriteLock} that associates a
 * name with the lock and traces read/write locking and unlocking.
 */
public class ManagedReadWriteLock {
    private static final Logger log = LoggerFactory.getLogger(ManagedReadWriteLock.class);

    /**
     * Acquire text.
     */
    public final static String ACQUIRE_TEXT = "Acquire ";

    /**
     * Acquired text.
     */
    public final static String ACQUIRED_TEXT = "Got     ";

    /**
     * Acquired text.
     */
    public final static String TRY_TEXT = "Trying  ";

    /**
     * Release text.
     */
    public final static String RELEASE_TEXT = "Release ";

    /**
     * Released text.
     */
    public final static String RELEASED_TEXT = "Freed   ";

    private final ReentrantReadWriteLock lock;
    private final String name;

    /**
     * Ctor.
     *
     * @param name   of lock
     * @param isFair true if a fair lock, false if not
     */
    public ManagedReadWriteLock(String name, boolean isFair) {
        this.name = name;
        this.lock = new ReentrantReadWriteLock(isFair);
    }

    /**
     * Lock write lock.
     */
    public void acquireWriteLock() {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(ACQUIRE_TEXT + " write " + name, lock);
        }

        lock.writeLock().lock();

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(ACQUIRED_TEXT + " write " + name, lock);
        }
    }

    /**
     * Try write lock with timeout, returning an indicator whether the lock was acquired or not.
     *
     * @param msec number of milliseconds to wait for lock
     * @return indicator whether the lock could be acquired or not
     */
    public boolean tryWriteLock(long msec) {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(TRY_TEXT + " write " + name, lock);
        }

        boolean result = false;
        try {
            result = lock.writeLock().tryLock(msec, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.warn("Lock wait interupted");
        }

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(TRY_TEXT + " write " + name + " : " + result, lock);
        }

        return result;
    }

    /**
     * Unlock write lock.
     */
    public void releaseWriteLock() {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(RELEASE_TEXT + " write " + name, lock);
        }

        lock.writeLock().unlock();

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(RELEASED_TEXT + " write " + name, lock);
        }
    }

    /**
     * Lock read lock.
     */
    public void acquireReadLock() {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(ACQUIRE_TEXT + " read " + name, lock);
        }

        lock.readLock().lock();

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(ACQUIRED_TEXT + " read " + name, lock);
        }
    }

    /**
     * Unlock read lock.
     */
    public void releaseReadLock() {
        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(RELEASE_TEXT + " read " + name, lock);
        }

        lock.readLock().unlock();

        if (ThreadLogUtil.ENABLED_TRACE) {
            ThreadLogUtil.traceLock(RELEASED_TEXT + " read " + name, lock);
        }
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }
}
