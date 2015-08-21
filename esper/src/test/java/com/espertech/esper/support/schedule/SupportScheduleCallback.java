/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.schedule;

import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.schedule.ScheduleHandle;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SupportScheduleCallback implements ScheduleHandle, ScheduleHandleCallback 
{
    private static int orderAllCallbacks;

    private int orderTriggered = 0;

    public void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext)
    {
        log.debug(".scheduledTrigger");
        orderAllCallbacks++;
        orderTriggered = orderAllCallbacks;
    }

    public int clearAndGetOrderTriggered()
    {
        int result = orderTriggered;
        orderTriggered = 0;
        return result;
    }

    public static void setCallbackOrderNum(int orderAllCallbacks) {
        SupportScheduleCallback.orderAllCallbacks = orderAllCallbacks;
    }

    public String getStatementId() {
        return null;
    }

    public int getAgentInstanceId() {
        return 0;
    }

    private static final Log log = LogFactory.getLog(SupportScheduleCallback.class);
}
