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
package com.espertech.esper.core.context.util;

import com.espertech.esper.core.service.EPStatementDispatch;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.StatementAgentInstanceFilterVersion;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.core.service.multimatch.MultiMatchHandler;
import com.espertech.esper.filter.FilterFaultHandler;
import com.espertech.esper.filter.FilterFaultHandlerFactory;

public class EPStatementAgentInstanceHandle {
    private final EPStatementHandle statementHandle;
    private StatementAgentInstanceLock statementAgentInstanceLock = null;
    private final int agentInstanceId;
    private final StatementAgentInstanceFilterVersion statementFilterVersion;
    private EPStatementDispatch optionalDispatchable;
    private boolean destroyed;

    private final int hashCode;
    private FilterFaultHandler filterFaultHandler;

    public EPStatementAgentInstanceHandle(EPStatementHandle statementHandle, StatementAgentInstanceLock statementAgentInstanceLock, int agentInstanceId, StatementAgentInstanceFilterVersion statementFilterVersion, FilterFaultHandlerFactory filterFaultHandlerFactory) {
        this.statementHandle = statementHandle;
        this.statementAgentInstanceLock = statementAgentInstanceLock;
        this.agentInstanceId = agentInstanceId;
        hashCode = 31 * statementHandle.hashCode() + agentInstanceId;
        this.statementFilterVersion = statementFilterVersion;
        if (filterFaultHandlerFactory != null) {
            filterFaultHandler = filterFaultHandlerFactory.makeFilterFaultHandler();
        }
    }

    public EPStatementHandle getStatementHandle() {
        return statementHandle;
    }

    public StatementAgentInstanceLock getStatementAgentInstanceLock() {
        return statementAgentInstanceLock;
    }

    public int getAgentInstanceId() {
        return agentInstanceId;
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

    public boolean isCanSelfJoin() {
        return statementHandle.isCanSelfJoin();
    }

    public void setStatementAgentInstanceLock(StatementAgentInstanceLock statementAgentInstanceLock) {
        this.statementAgentInstanceLock = statementAgentInstanceLock;
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
        return other.getStatementHandle().getStatementId() == this.getStatementHandle().getStatementId() && other.agentInstanceId == this.agentInstanceId;
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
     * Invoked by {@link com.espertech.esper.client.EPRuntime} to indicate that a statements's
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
}
