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

import com.espertech.esper.core.service.multimatch.MultiMatchHandler;
import com.espertech.esper.epl.metric.StatementMetricHandle;

import java.io.Serializable;

/**
 * Class exists once per statement and hold statement resource lock(s).
 * <p>
 * Use by {@link EPRuntimeImpl} for determining callback-statement affinity and locking of statement
 * resources.
 */
public class EPStatementHandle implements Serializable {
    private static final long serialVersionUID = 0L;

    private final String statementName;
    private final int statementId;
    private final String statementText;
    private final StatementType statementType;
    private final int hashCode;
    // handles self-join (ie. statement where from-clause lists the same event type or a super-type more then once)
    // such that the internal dispatching must occur after both matches are processed
    private boolean canSelfJoin;
    private boolean hasVariables;
    private MultiMatchHandler multiMatchHandler;
    private final int priority;
    private final boolean preemptive;
    private transient InsertIntoLatchFactory insertIntoFrontLatchFactory;
    private transient InsertIntoLatchFactory insertIntoBackLatchFactory;
    private transient StatementMetricHandle metricsHandle = null;
    private boolean hasTableAccess;

    /**
     * Ctor.
     *
     * @param statementId       is the statement id uniquely indentifying the handle
     * @param expressionText    is the expression
     * @param hasVariables      indicator whether the statement uses variables
     * @param metricsHandle     handle for metrics reporting
     * @param priority          priority, zero is default
     * @param preemptive        true for drop after done
     * @param statementName     statement name
     * @param statementText     epl
     * @param statementType     statement type
     * @param hasTableAccess    indicator whether tables are accessed
     * @param multiMatchHandler handler for multimatches for statement
     */
    public EPStatementHandle(int statementId, String statementName, String statementText, StatementType statementType, String expressionText, boolean hasVariables, StatementMetricHandle metricsHandle, int priority, boolean preemptive, boolean hasTableAccess, MultiMatchHandler multiMatchHandler) {
        this.statementId = statementId;
        this.statementName = statementName;
        this.statementText = statementText;
        this.statementType = statementType;
        this.hasVariables = hasVariables;
        this.metricsHandle = metricsHandle;
        this.priority = priority;
        this.preemptive = preemptive;
        this.hasTableAccess = hasTableAccess;
        this.multiMatchHandler = multiMatchHandler;
        hashCode = expressionText.hashCode() ^ statementName.hashCode() ^ statementId;
    }

    /**
     * Set the statement's self-join flag to indicate the the statement may join to itself,
     * that is a single event may dispatch into multiple streams or patterns for the same statement,
     * requiring internal dispatch logic to not shortcut evaluation of all filters for the statement
     * within one lock, requiring the callback handle to be sorted.
     *
     * @param canSelfJoin is true if the statement potentially self-joins, false if not
     */
    public void setCanSelfJoin(boolean canSelfJoin) {
        this.canSelfJoin = canSelfJoin;
    }

    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    public int getStatementId() {
        return statementId;
    }

    /**
     * Sets the factory for latches in insert-into guaranteed order of delivery.
     *
     * @param insertIntoFrontLatchFactory latch factory for the statement if it performs insert-into (route) of events
     */
    public void setInsertIntoFrontLatchFactory(InsertIntoLatchFactory insertIntoFrontLatchFactory) {
        this.insertIntoFrontLatchFactory = insertIntoFrontLatchFactory;
    }

    public void setInsertIntoBackLatchFactory(InsertIntoLatchFactory insertIntoBackLatchFactory) {
        this.insertIntoBackLatchFactory = insertIntoBackLatchFactory;
    }

    /**
     * Returns the factory for latches in insert-into guaranteed order of delivery.
     *
     * @return latch factory for the statement if it performs insert-into (route) of events
     */
    public InsertIntoLatchFactory getInsertIntoFrontLatchFactory() {
        return insertIntoFrontLatchFactory;
    }

    public InsertIntoLatchFactory getInsertIntoBackLatchFactory() {
        return insertIntoBackLatchFactory;
    }

    /**
     * Returns true if the statement uses variables, false if not.
     *
     * @return indicator if variables are used by statement
     */
    public boolean isHasVariables() {
        return hasVariables;
    }

    /**
     * Returns the statement priority.
     *
     * @return priority, default 0
     */
    public int getPriority() {
        return priority;
    }

    /**
     * True for preemptive (drop) statements.
     *
     * @return preemptive indicator
     */
    public boolean isPreemptive() {
        return preemptive;
    }

    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof EPStatementHandle)) {
            return false;
        }

        EPStatementHandle other = (EPStatementHandle) otherObj;
        return other.statementId == this.statementId;
    }

    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns true if the statement potentially self-joins amojng the events it processes.
     *
     * @return true for self-joins possible, false for not possible (most statements)
     */
    public boolean isCanSelfJoin() {
        return canSelfJoin;
    }

    /**
     * Returns handle for metrics reporting.
     *
     * @return handle for metrics reporting
     */
    public StatementMetricHandle getMetricsHandle() {
        return metricsHandle;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getEPL() {
        return statementText;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public MultiMatchHandler getMultiMatchHandler() {
        return multiMatchHandler;
    }

    public void setMultiMatchHandler(MultiMatchHandler multiMatchHandler) {
        this.multiMatchHandler = multiMatchHandler;
    }
}
