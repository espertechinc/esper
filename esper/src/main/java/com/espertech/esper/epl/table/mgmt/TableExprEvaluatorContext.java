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
package com.espertech.esper.epl.table.mgmt;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public class TableExprEvaluatorContext {
    private final ThreadLocal<Set<Lock>> threadLocal = new ThreadLocal<Set<Lock>>() {
        protected synchronized Set<Lock> initialValue() {
            return new HashSet<Lock>();
        }
    };

    public boolean addAcquiredLock(Lock lock) {
        return threadLocal.get().add(lock);
    }

    public void releaseAcquiredLocks() {
        Set<Lock> locks = threadLocal.get();
        if (locks.isEmpty()) {
            return;
        }
        for (Lock lock : locks) {
            lock.unlock();
        }
        locks.clear();
    }

    public int getLockHeldCount() {
        return threadLocal.get().size();
    }
}
