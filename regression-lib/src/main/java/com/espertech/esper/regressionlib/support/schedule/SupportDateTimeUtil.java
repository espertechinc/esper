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
package com.espertech.esper.regressionlib.support.schedule;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

public class SupportDateTimeUtil {
    public static void compareDate(Calendar cal, int year, int month, int day, int hour, int minute, int second, int millis, String timeZoneId) {
        compareDate(cal, year, month, day, hour, minute, second, millis);
        assertEquals(timeZoneId, cal.getTimeZone().getID());
    }

    public static void compareDate(Calendar cal, int year, int month, int day, int hour, int minute, int second, int millis) {
        assertEquals(year, cal.get(Calendar.YEAR));
        assertEquals(month, cal.get(Calendar.MONTH));
        assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, cal.get(Calendar.MINUTE));
        assertEquals(second, cal.get(Calendar.SECOND));
        assertEquals(millis, cal.get(Calendar.MILLISECOND));
    }

    public static long timePlusMonth(long timeInMillis, int monthToAdd) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.add(Calendar.MONTH, monthToAdd);
        return cal.getTimeInMillis();
    }
}
