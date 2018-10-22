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

import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

/**
 * Statement resource handle and callback for use with filter services.
 * <p>
 * Links the statement handle identifying a statement and containing the statement resource lock,
 * with the actual callback to invoke for a statement together.
 */
public class EPStatementHandleCallbackFilter implements FilterHandle {
    private EPStatementAgentInstanceHandle agentInstanceHandle;
    private FilterHandleCallback filterCallback;
    // private ScheduleHandleCallback scheduleCallback;

    /**
     * Ctor.
     *
     * @param agentInstanceHandle is a statement handle
     * @param callback            is a filter callback
     */
    public EPStatementHandleCallbackFilter(EPStatementAgentInstanceHandle agentInstanceHandle, FilterHandleCallback callback) {
        this.agentInstanceHandle = agentInstanceHandle;
        this.filterCallback = callback;
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

    public void setFilterCallback(FilterHandleCallback filterCallback) {
        this.filterCallback = filterCallback;
    }
}
