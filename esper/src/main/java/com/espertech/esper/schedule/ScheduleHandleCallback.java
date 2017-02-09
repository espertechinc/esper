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

import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;

/**
 * Interface for scheduled callbacks.
 */
public interface ScheduleHandleCallback {
    /**
     * Callback that is invoked as indicated by a schedule added to the scheduling service.
     *
     * @param engineLevelExtensionServicesContext is a marker interface for providing custom extension services
     *                                            passed to the triggered class
     */
    public void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext);
}
