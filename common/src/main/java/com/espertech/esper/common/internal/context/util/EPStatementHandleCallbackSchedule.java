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

import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;

public class EPStatementHandleCallbackSchedule implements ScheduleHandle {
    private EPStatementAgentInstanceHandle agentInstanceHandle;
    private ScheduleHandleCallback scheduleCallback;

    /**
     * Ctor.
     *
     * @param agentInstanceHandle is a statement handle
     * @param callback            is a schedule callback
     */
    public EPStatementHandleCallbackSchedule(EPStatementAgentInstanceHandle agentInstanceHandle, ScheduleHandleCallback callback) {
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

    public ScheduleHandleCallback getScheduleCallback() {
        return scheduleCallback;
    }
}
