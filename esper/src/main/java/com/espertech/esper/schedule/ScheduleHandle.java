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
package com.espertech.esper.schedule;

/**
 * Marker interface for use with {@link SchedulingService}. Implementations serve as a schedule trigger values when
 * the schedule is reached to trigger or return the handle.
 */
public interface ScheduleHandle {
    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    public int getStatementId();

    /**
     * Returns the agent instance id.
     *
     * @return agent instance id
     */
    public int getAgentInstanceId();
}
