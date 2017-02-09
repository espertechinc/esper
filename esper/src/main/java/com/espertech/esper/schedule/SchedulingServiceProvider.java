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

import com.espertech.esper.timer.TimeSourceService;

/**
 * Static factory for implementations of the SchedulingService interface.
 */
public final class SchedulingServiceProvider {
    /**
     * Creates an implementation of the SchedulingService interface.
     *
     * @param timeSourceService time source provider
     * @return implementation
     */
    public static SchedulingServiceSPI newService(TimeSourceService timeSourceService) {
        return new SchedulingServiceImpl(timeSourceService);
    }
}
