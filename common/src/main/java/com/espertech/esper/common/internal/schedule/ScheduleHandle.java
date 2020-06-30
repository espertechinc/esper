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
package com.espertech.esper.common.internal.schedule;

import com.espertech.esper.common.client.type.EPTypeClass;

public interface ScheduleHandle {
    EPTypeClass EPTYPE = new EPTypeClass(ScheduleHandle.class);
    EPTypeClass EPTYPE_SCHEDULEOBJECTTYPE = new EPTypeClass(ScheduleObjectType.class);

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
