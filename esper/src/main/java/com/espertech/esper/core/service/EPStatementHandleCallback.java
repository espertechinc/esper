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

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.filter.FilterHandle;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.schedule.ScheduleHandle;
import com.espertech.esper.schedule.ScheduleHandleCallback;

/**
 * Statement resource handle and callback for use with {@link com.espertech.esper.filter.FilterService} and
 * {@link com.espertech.esper.schedule.SchedulingService}.
 * <p>
 * Links the statement handle identifying a statement and containing the statement resource lock,
 * with the actual callback to invoke for a statement together.
 */
public class EPStatementHandleCallback implements FilterHandle, ScheduleHandle {
    private EPStatementAgentInstanceHandle agentInstanceHandle;
    private FilterHandleCallback filterCallback;
    private ScheduleHandleCallback scheduleCallback;

    /**
     * Ctor.
     *
     * @param agentInstanceHandle is a statement handle
     * @param callback            is a filter callback
     */
    public EPStatementHandleCallback(EPStatementAgentInstanceHandle agentInstanceHandle, FilterHandleCallback callback) {
        this.agentInstanceHandle = agentInstanceHandle;
        this.filterCallback = callback;
    }

    /**
     * Ctor.
     *
     * @param agentInstanceHandle is a statement handle
     * @param callback            is a schedule callback
     */
    public EPStatementHandleCallback(EPStatementAgentInstanceHandle agentInstanceHandle, ScheduleHandleCallback callback) {
        this.agentInstanceHandle = agentInstanceHandle;
        this.scheduleCallback = callback;
    }

    public int getStatementId() {
        return agentInstanceHandle.getStatementId();
    }

    public int getAgentInstanceId() {
        return agentInstanceHandle.getAgentInstanceId();
    }

    /**
     * Returns the statement handle.
     *
     * @return handle containing a statement resource lock
     */
    public EPStatementAgentInstanceHandle getAgentInstanceHandle() {
        return agentInstanceHandle;
    }

    /**
     * Returns the statement filter callback, or null if this is a schedule callback handle.
     *
     * @return filter callback
     */
    public FilterHandleCallback getFilterCallback() {
        return filterCallback;
    }

    /**
     * Returns the statement schedule callback, or null if this is a filter callback handle.
     *
     * @return schedule callback
     */
    public ScheduleHandleCallback getScheduleCallback() {
        return scheduleCallback;
    }

    public void setScheduleCallback(ScheduleHandleCallback scheduleCallback) {
        this.scheduleCallback = scheduleCallback;
    }

    public void setFilterCallback(FilterHandleCallback filterCallback) {
        this.filterCallback = filterCallback;
    }
}
