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
package com.espertech.esper.epl.datetime.calop;

import java.util.Calendar;

public class CalendarOpPlusFastAddResult {
    private final long factor;
    private final Calendar scheduled;

    public CalendarOpPlusFastAddResult(long factor, Calendar scheduled) {
        this.factor = factor;
        this.scheduled = scheduled;
    }

    public long getFactor() {
        return factor;
    }

    public Calendar getScheduled() {
        return scheduled;
    }
}
