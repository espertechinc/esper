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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.internal.collection.ArrayBackedCollection;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;

import java.util.Map;

public class EPEventServiceThreadLocalEntry {
    private final WorkQueue workQueue;
    private final ArrayBackedCollection<FilterHandle> matchesArrayThreadLocal;
    private final ArrayBackedCollection<ScheduleHandle> scheduleArrayThreadLocal;
    private final Map<EPStatementAgentInstanceHandle, Object> matchesPerStmtThreadLocal;
    private final Map<EPStatementAgentInstanceHandle, Object> schedulePerStmtThreadLocal;
    private final ExprEvaluatorContext exprEvaluatorContext;

    public EPEventServiceThreadLocalEntry(WorkQueue workQueue, ArrayBackedCollection<FilterHandle> matchesArrayThreadLocal, ArrayBackedCollection<ScheduleHandle> scheduleArrayThreadLocal, Map<EPStatementAgentInstanceHandle, Object> matchesPerStmtThreadLocal, Map<EPStatementAgentInstanceHandle, Object> schedulePerStmtThreadLocal, ExprEvaluatorContext exprEvaluatorContext) {
        this.workQueue = workQueue;
        this.matchesArrayThreadLocal = matchesArrayThreadLocal;
        this.scheduleArrayThreadLocal = scheduleArrayThreadLocal;
        this.matchesPerStmtThreadLocal = matchesPerStmtThreadLocal;
        this.schedulePerStmtThreadLocal = schedulePerStmtThreadLocal;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public WorkQueue getWorkQueue() {
        return workQueue;
    }

    public ArrayBackedCollection<FilterHandle> getMatchesArrayThreadLocal() {
        return matchesArrayThreadLocal;
    }

    public ArrayBackedCollection<ScheduleHandle> getScheduleArrayThreadLocal() {
        return scheduleArrayThreadLocal;
    }

    public Map<EPStatementAgentInstanceHandle, Object> getMatchesPerStmtThreadLocal() {
        return matchesPerStmtThreadLocal;
    }

    public Map<EPStatementAgentInstanceHandle, Object> getSchedulePerStmtThreadLocal() {
        return schedulePerStmtThreadLocal;
    }

    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }
}
