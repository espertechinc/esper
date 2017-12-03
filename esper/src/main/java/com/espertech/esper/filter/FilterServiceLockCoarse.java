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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.filterspec.FilterValueSet;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FilterServiceLockCoarse extends FilterServiceBase {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FilterServiceLockCoarse(boolean allowIsolation) {
        super(FilterServiceGranularLockFactoryNone.INSTANCE, allowIsolation);
    }

    public void acquireWriteLock() {
        lock.writeLock().lock();
    }

    public void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    public FilterSet take(Set<Integer> statementId) {
        lock.writeLock().lock();
        try {
            return super.takeInternal(statementId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void apply(FilterSet filterSet) {
        lock.writeLock().lock();
        try {
            super.applyInternal(filterSet);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long evaluate(EventBean theEvent, Collection<FilterHandle> matches) {
        lock.readLock().lock();
        try {
            return super.evaluateInternal(theEvent, matches);
        } finally {
            lock.readLock().unlock();
        }
    }

    public long evaluate(EventBean theEvent, Collection<FilterHandle> matches, int statementId) {
        lock.readLock().lock();
        try {
            return super.evaluateInternal(theEvent, matches, statementId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public FilterServiceEntry add(FilterValueSet filterValueSet, FilterHandle callback) {
        lock.writeLock().lock();
        try {
            return super.addInternal(filterValueSet, callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(FilterHandle callback, FilterServiceEntry filterServiceEntry) {
        lock.writeLock().lock();
        try {
            super.removeInternal(callback, filterServiceEntry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeType(EventType type) {
        lock.writeLock().lock();
        try {
            super.removeTypeInternal(type);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
