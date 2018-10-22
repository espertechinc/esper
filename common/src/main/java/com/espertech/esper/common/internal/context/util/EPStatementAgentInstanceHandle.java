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

import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandler;

public class EPStatementAgentInstanceHandle {
    private final EPStatementHandle statementHandle;
    private final StatementAgentInstanceLock statementAgentInstanceLock;
    private final int agentInstanceId;
    private final int hashCode;
    private final StatementAgentInstanceFilterVersion statementFilterVersion = new StatementAgentInstanceFilterVersion();
    private EPStatementDispatch optionalDispatchable;
    private boolean destroyed;
    private FilterFaultHandler filterFaultHandler;

    public EPStatementAgentInstanceHandle(EPStatementHandle statementHandle, int agentInstanceId, StatementAgentInstanceLock statementAgentInstanceLock) {
        this.statementHandle = statementHandle;
        this.agentInstanceId = agentInstanceId;
        this.statementAgentInstanceLock = statementAgentInstanceLock;
        this.hashCode = 31 * statementHandle.hashCode() + agentInstanceId;
    }

    public StatementAgentInstanceLock getStatementAgentInstanceLock() {
        return statementAgentInstanceLock;
    }

    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    public boolean isCanSelfJoin() {
        return statementHandle.isCanSelfJoin();
    }

    public int getPriority() {
        return statementHandle.getPriority();
    }

    public boolean isPreemptive() {
        return statementHandle.isPreemptive();
    }

    public boolean isHasVariables() {
        return statementHandle.isHasVariables();
    }

    public boolean isHasTableAccess() {
        return statementHandle.isHasTableAccess();
    }

    public StatementAgentInstanceFilterVersion getStatementFilterVersion() {
        return statementFilterVersion;
    }

    /**
     * Tests filter version.
     *
     * @param filterVersion to test
     * @return indicator whether version is up-to-date
     */
    public boolean isCurrentFilter(long filterVersion) {
        return statementFilterVersion.isCurrentFilter(filterVersion);
    }

    public boolean equals(Object otherObj) {
        if (this == otherObj) {
            return true;
        }

        if (!(otherObj instanceof EPStatementAgentInstanceHandle)) {
            return false;
        }

        EPStatementAgentInstanceHandle other = (EPStatementAgentInstanceHandle) otherObj;
        return other.getStatementHandle().getStatementId() == statementHandle.getStatementId() && other.agentInstanceId == this.agentInstanceId;
    }

    public int hashCode() {
        return hashCode;
    }

    /**
     * Provides a callback for use when statement processing for filters and schedules is done,
     * for use by join statements that require an explicit indicator that all
     * joined streams results have been processed.
     *
     * @param optionalDispatchable is the instance for calling onto after statement callback processing
     */
    public void setOptionalDispatchable(EPStatementDispatch optionalDispatchable) {
        this.optionalDispatchable = optionalDispatchable;
    }

    public EPStatementDispatch getOptionalDispatchable() {
        return optionalDispatchable;
    }

    /**
     * Invoked by runtime to indicate that a statements's
     * filer and schedule processing is done, and now it's time to process join results.
     */
    public void internalDispatch() {
        if (optionalDispatchable != null) {
            optionalDispatchable.execute();
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public String toString() {
        return "EPStatementAgentInstanceHandle{" +
                "name=" + statementHandle.getStatementName() +
                " id=" + agentInstanceId +
                '}';
    }

    public FilterFaultHandler getFilterFaultHandler() {
        return filterFaultHandler;
    }

    public void setFilterFaultHandler(FilterFaultHandler filterFaultHandler) {
        this.filterFaultHandler = filterFaultHandler;
    }

    public int getStatementId() {
        return statementHandle.getStatementId();
    }

    public MultiMatchHandler getMultiMatchHandler() {
        return statementHandle.getMultiMatchHandler();
    }

    public EPStatementHandle getStatementHandle() {
        return statementHandle;
    }
}
