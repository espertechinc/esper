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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.epl.datetime.calop.CalendarForge;
import com.espertech.esper.epl.datetime.interval.IntervalForge;

import java.util.List;

public abstract class DTLocalForgeCalOpsIntervalBase implements DTLocalForge, DTLocalForgeIntervalComp {
    protected final List<CalendarForge> calendarForges;
    protected final IntervalForge intervalForge;

    protected DTLocalForgeCalOpsIntervalBase(List<CalendarForge> calendarForges, IntervalForge intervalForge) {
        this.calendarForges = calendarForges;
        this.intervalForge = intervalForge;
    }
}
