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

import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricHandle;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchFactory;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandler;

/**
 * Class exists once per statement and hold statement resource lock(s).
 * <p>
 * The statement's self-join flag indicates the the statement may join to itself,
 * that is a single event may dispatch into multiple streams or patterns for the same statement,
 * requiring internal dispatch logic to not shortcut evaluation of all filters for the statement
 * within one lock, requiring the callback handle to be sorted.
 */
public class EPStatementHandle {
    private final String statementName;
    private final String deploymentId;
    private final int statementId;
    private final String optionalStatementEPL;
    private final int priority;
    private final boolean preemptive;
    private final boolean canSelfJoin;
    private final MultiMatchHandler multiMatchHandler;
    private final boolean hasVariables;
    private final boolean hasTableAccess;
    private final StatementMetricHandle metricsHandle;
    private final InsertIntoLatchFactory insertIntoFrontLatchFactory;
    private final InsertIntoLatchFactory insertIntoBackLatchFactory;

    public EPStatementHandle(String statementName, String deploymentId, int statementId, String optionalStatementEPL, int priority, boolean preemptive, boolean canSelfJoin, MultiMatchHandler multiMatchHandler, boolean hasVariables, boolean hasTableAccess, StatementMetricHandle metricsHandle, InsertIntoLatchFactory insertIntoFrontLatchFactory, InsertIntoLatchFactory insertIntoBackLatchFactory) {
        this.statementName = statementName;
        this.deploymentId = deploymentId;
        this.statementId = statementId;
        this.optionalStatementEPL = optionalStatementEPL;
        this.priority = priority;
        this.preemptive = preemptive;
        this.canSelfJoin = canSelfJoin;
        this.multiMatchHandler = multiMatchHandler;
        this.hasVariables = hasVariables;
        this.hasTableAccess = hasTableAccess;
        this.metricsHandle = metricsHandle;
        this.insertIntoFrontLatchFactory = insertIntoFrontLatchFactory;
        this.insertIntoBackLatchFactory = insertIntoBackLatchFactory;
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
        return statementId;
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

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public MultiMatchHandler getMultiMatchHandler() {
        return multiMatchHandler;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getOptionalStatementEPL() {
        return optionalStatementEPL;
    }

    public InsertIntoLatchFactory getInsertIntoFrontLatchFactory() {
        return insertIntoFrontLatchFactory;
    }

    public InsertIntoLatchFactory getInsertIntoBackLatchFactory() {
        return insertIntoBackLatchFactory;
    }
}
