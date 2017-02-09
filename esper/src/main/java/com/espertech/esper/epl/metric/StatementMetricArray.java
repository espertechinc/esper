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
package com.espertech.esper.epl.metric;

import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.util.ManagedReadWriteLock;

import java.util.HashSet;
import java.util.Set;

/**
 * Holder for statement group's statement metrics.
 * <p>
 * Changes to StatementMetric instances must be done in a read-lock:
 * <pre>
 * getRwLock.readLock.lock()
 * metric = getAddMetric(index)
 * metric.accountFor(cpu, wall, etc)
 * getRwLock.readLock.unlock()
 * </pre>
 * <p>
 * All other changes are done under write lock for this class.
 * <p>
 * This is a collection backed by an array that grows by 50% each time expanded, maintains a free/busy list of statement names,
 * maintains an element number of last used element.
 * <p>
 * The flush operaton copies the complete array, thereby keeping array size. Statement names are only removed on the next flush.
 */
public class StatementMetricArray {
    private final String engineURI;

    // Lock
    //  Read lock applies to each current transaction on a StatementMetric instance
    //  Write lock applies to flush and to add a new statement
    private final ManagedReadWriteLock rwLock;
    private final boolean isReportInactive;

    // Active statements
    private String[] statementNames;

    // Count of active statements
    private int currentLastElement;

    // Flushed metric per statement
    private volatile StatementMetric[] metrics;

    // Statements ids to remove with the next flush
    private Set<String> removedStatementNames;

    /**
     * Ctor.
     *
     * @param engineURI        engine URI
     * @param name             name of statement group
     * @param initialSize      initial size of array
     * @param isReportInactive true to indicate to report on inactive statements
     */
    public StatementMetricArray(String engineURI, String name, int initialSize, boolean isReportInactive) {
        this.engineURI = engineURI;
        this.isReportInactive = isReportInactive;

        metrics = new StatementMetric[initialSize];
        statementNames = new String[initialSize];
        currentLastElement = -1;
        rwLock = new ManagedReadWriteLock("StatementMetricArray-" + name, true);
        removedStatementNames = new HashSet<String>();
    }

    /**
     * Remove a statement.
     * <p>
     * Next flush actually frees the slot that this statement occupied.
     *
     * @param statementName to remove
     */
    public void removeStatement(String statementName) {
        rwLock.acquireWriteLock();
        try {
            removedStatementNames.add(statementName);

            if (removedStatementNames.size() > 1000) {
                for (int i = 0; i <= currentLastElement; i++) {
                    if (removedStatementNames.contains(statementNames[i])) {
                        statementNames[i] = null;
                    }
                }
                removedStatementNames.clear();
            }
        } finally {
            rwLock.releaseWriteLock();
        }
    }

    /**
     * Adds a statement and returns the index added at.
     * <p>
     * May reuse an empty slot, grow the underlying array, or append to the end.
     *
     * @param statementName to add
     * @return index added to
     */
    public int addStatementGetIndex(String statementName) {
        rwLock.acquireWriteLock();
        try {
            // see if there is room
            if ((currentLastElement + 1) < metrics.length) {
                currentLastElement++;
                statementNames[currentLastElement] = statementName;
                return currentLastElement;
            }

            // no room, try to use an existing slot of a removed statement
            for (int i = 0; i < statementNames.length; i++) {
                if (statementNames[i] == null) {
                    statementNames[i] = statementName;
                    if ((i + 1) > currentLastElement) {
                        currentLastElement = i;
                    }
                    return i;
                }
            }

            // still no room, expand storage by 50%
            int newSize = (int) (metrics.length * 1.5);
            String[] newStatementNames = new String[newSize];
            StatementMetric[] newMetrics = new StatementMetric[newSize];
            System.arraycopy(statementNames, 0, newStatementNames, 0, statementNames.length);
            System.arraycopy(metrics, 0, newMetrics, 0, metrics.length);

            statementNames = newStatementNames;
            metrics = newMetrics;

            currentLastElement++;
            statementNames[currentLastElement] = statementName;

            return currentLastElement;
        } finally {
            rwLock.releaseWriteLock();
        }
    }

    /**
     * Flushes the existing metrics via array copy and swap.
     * <p>
     * May report all statements (empty and non-empty slots) and thereby null values.
     * <p>
     * Returns null to indicate no reports to do.
     *
     * @return metrics
     */
    public StatementMetric[] flushMetrics() {
        rwLock.acquireWriteLock();
        try {
            boolean isEmpty = false;
            if (currentLastElement == -1) {
                isEmpty = true;
            }

            // first fill in the blanks if there are no reports and we report inactive statements
            if (isReportInactive) {
                for (int i = 0; i <= currentLastElement; i++) {
                    if (statementNames[i] != null) {
                        metrics[i] = new StatementMetric(engineURI, statementNames[i]);
                    }
                }
            }

            // remove statement ids that disappeared during the interval
            if ((currentLastElement > -1) && (!removedStatementNames.isEmpty())) {
                for (int i = 0; i <= currentLastElement; i++) {
                    if (removedStatementNames.contains(statementNames[i])) {
                        statementNames[i] = null;
                    }
                }
            }

            // adjust last used element
            while ((currentLastElement != -1) && (statementNames[currentLastElement] == null)) {
                currentLastElement--;
            }

            if (isEmpty) {
                return null;    // no copies made if empty collection
            }

            // perform flush
            StatementMetric[] newMetrics = new StatementMetric[metrics.length];
            StatementMetric[] oldMetrics = metrics;
            metrics = newMetrics;
            return oldMetrics;
        } finally {
            rwLock.releaseWriteLock();
        }
    }

    /**
     * Returns the read-write lock, for read-lock when modifications are made.
     *
     * @return lock
     */
    public ManagedReadWriteLock getRwLock() {
        return rwLock;
    }

    /**
     * Returns an existing or creates a new statement metric for the index.
     *
     * @param index of statement
     * @return metric to modify under read lock
     */
    public StatementMetric getAddMetric(int index) {
        StatementMetric metric = metrics[index];
        if (metric == null) {
            metric = new StatementMetric(engineURI, statementNames[index]);
            metrics[index] = metric;
        }
        return metric;
    }

    /**
     * Returns maximum collection size (last used element), which may not truely reflect the number
     * of actual statements held as some slots may empty up when statements are removed.
     *
     * @return known maximum size
     */
    public int sizeLastElement() {
        return currentLastElement + 1;
    }
}
