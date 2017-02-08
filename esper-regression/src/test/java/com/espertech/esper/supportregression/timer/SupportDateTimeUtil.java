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
package com.espertech.esper.supportregression.timer;

import org.junit.Assert;

import java.util.Calendar;

public class SupportDateTimeUtil {
    public static void compareDate(Calendar cal, int year, int month, int day, int hour, int minute, int second, int millis, String timeZoneId) {
        compareDate(cal, year, month, day, hour, minute, second, millis);
        Assert.assertEquals(timeZoneId, cal.getTimeZone().getID());
    }

    public static void compareDate(Calendar cal, int year, int month, int day, int hour, int minute, int second, int millis) {
        Assert.assertEquals(year, cal.get(Calendar.YEAR));
        Assert.assertEquals(month, cal.get(Calendar.MONTH));
        Assert.assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, cal.get(Calendar.MINUTE));
        Assert.assertEquals(second, cal.get(Calendar.SECOND));
        Assert.assertEquals(millis, cal.get(Calendar.MILLISECOND));
    }
}
