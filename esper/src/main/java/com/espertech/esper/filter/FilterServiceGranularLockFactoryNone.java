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
package com.espertech.esper.filter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class FilterServiceGranularLockFactoryNone implements FilterServiceGranularLockFactory {
    public static final FilterServiceGranularLockFactoryNone INSTANCE = new FilterServiceGranularLockFactoryNone();

    private FilterServiceGranularLockFactoryNone() {
    }

    private final static Lock LOCKNONE = new Lock() {
        public void lock() {
        }

        public void lockInterruptibly() throws InterruptedException {
        }

        public boolean tryLock() {
            return true;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void unlock() {
        }

        public Condition newCondition() {
            return null;
        }
    };

    private final static ReadWriteLock RWLOCKNONE = new ReadWriteLock() {
        public Lock readLock() {
            return LOCKNONE;
        }

        public Lock writeLock() {
            return LOCKNONE;
        }
    };

    public ReadWriteLock obtainNew() {
        return RWLOCKNONE;
    }
}
