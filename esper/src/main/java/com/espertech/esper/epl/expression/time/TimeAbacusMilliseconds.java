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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.util.JavaClassHelper;

import java.util.Calendar;
import java.util.Date;

public class TimeAbacusMilliseconds implements TimeAbacus {
    public final static TimeAbacusMilliseconds INSTANCE = new TimeAbacusMilliseconds();
    private static final long serialVersionUID = 7634550048792013972L;

    private TimeAbacusMilliseconds() {
    }

    public long deltaForSecondsDouble(double seconds) {
        return Math.round(1000d * seconds);
    }

    public long deltaForSecondsNumber(Number timeInSeconds) {
        if (JavaClassHelper.isFloatingPointNumber(timeInSeconds)) {
            return deltaForSecondsDouble(timeInSeconds.doubleValue());
        }
        return 1000 * timeInSeconds.longValue();
    }

    public long calendarSet(long fromTime, Calendar cal) {
        cal.setTimeInMillis(fromTime);
        return 0;
    }

    public long calendarGet(Calendar cal, long remainder) {
        return cal.getTimeInMillis();
    }

    public long getOneSecond() {
        return 1000;
    }

    public Date toDate(long ts) {
        return new Date(ts);
    }
}
