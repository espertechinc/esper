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

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for logging threading-related messages.
 * <p>
 * Prints thread information and lock-specific info.
 */
public class ThreadLogUtil {
    /**
     * Set trace log level.
     */
    public static int trace = 0;

    /**
     * Set info log level.
     */
    public static int info = 1;

    /**
     * Enable trace logging.
     */
    public final static boolean ENABLED_TRACE = false;

    /**
     * Enable info logging.
     */
    public final static boolean ENABLED_INFO = false;

    /**
     * If enabled, logs for trace level the given objects and text
     *
     * @param text    to log
     * @param objects to write
     */
    public static void trace(String text, Object... objects) {
        if (!ENABLED_TRACE) {
            return;
        }
        write(text, objects);
    }

    /**
     * If enabled, logs for info level the given objects and text
     *
     * @param text    to log
     * @param objects to write
     */
    public static void info(String text, Object... objects) {
        if (!ENABLED_INFO) {
            return;
        }
        write(text, objects);
    }

    /**
     * Logs the lock and action.
     *
     * @param lockAction is the action towards the lock
     * @param lock       is the lock instance
     */
    public static void traceLock(String lockAction, ReentrantLock lock) {
        if (!ENABLED_TRACE) {
            return;
        }
        write(lockAction + " " + getLockInfo(lock));
    }

    /**
     * Logs the lock and action.
     *
     * @param lockAction is the action towards the lock
     * @param lock       is the lock instance
     */
    public static void traceLock(String lockAction, ReentrantReadWriteLock lock) {
        if (!ENABLED_TRACE) {
            return;
        }
        write(lockAction + " " + getLockInfo(lock));
    }

    private static String getLockInfo(ReentrantLock lock) {
        String lockid = "Lock@" + Integer.toHexString(lock.hashCode());
        return "lock " + lockid + " held=" + lock.getHoldCount() + " isHeldMe=" + lock.isHeldByCurrentThread() +
                " hasQueueThreads=" + lock.hasQueuedThreads();
    }

    private static String getLockInfo(ReentrantReadWriteLock lock) {
        String lockid = "RWLock@" + Integer.toHexString(lock.hashCode());
        return lockid +
                " readLockCount=" + lock.getReadLockCount() +
                " isWriteLocked=" + lock.isWriteLocked();
    }

    private static void write(String text, Object... objects) {
        StringBuilder buf = new StringBuilder();
        buf.append(text);
        buf.append(' ');
        for (Object obj : objects) {
            if ((obj instanceof String) || (obj instanceof Number)) {
                buf.append(obj.toString());
            } else {
                buf.append(obj.getClass().getSimpleName());
                buf.append('@');
                buf.append(Integer.toHexString(obj.hashCode()));
            }
            buf.append(' ');
        }
        write(buf.toString());
    }

    private static void write(String text) {
        log.info(".write Thread " + Thread.currentThread().getId() + " " + text);
    }

    private static final Logger log = LoggerFactory.getLogger(ThreadLogUtil.class);
}
