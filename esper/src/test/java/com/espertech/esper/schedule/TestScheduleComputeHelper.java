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

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;
import com.espertech.esper.type.ScheduleUnit;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TestScheduleComputeHelper extends TestCase {
    private final static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void testCompute() throws Exception {
        ScheduleSpec spec = null;

        // Try next "5 minutes past the hour"
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MINUTES, 5);

        checkCorrect(spec, "2004-12-9 15:45:01", "2004-12-9 16:05:00");
        checkCorrect(spec, "2004-12-9 16:04:59", "2004-12-9 16:05:00");
        checkCorrect(spec, "2004-12-9 16:05:00", "2004-12-9 17:05:00");
        checkCorrect(spec, "2004-12-9 16:05:01", "2004-12-9 17:05:00");
        checkCorrect(spec, "2004-12-9 16:05:01", "2004-12-9 17:05:00");
        checkCorrect(spec, "2004-12-9 23:58:01", "2004-12-10 00:05:00");

        // Try next "5, 10 and 15 minutes past the hour"
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MINUTES, 5);
        spec.addValue(ScheduleUnit.MINUTES, 10);
        spec.addValue(ScheduleUnit.MINUTES, 15);

        checkCorrect(spec, "2004-12-9 15:45:01", "2004-12-9 16:05:00");
        checkCorrect(spec, "2004-12-9 16:04:59", "2004-12-9 16:05:00");
        checkCorrect(spec, "2004-12-9 16:05:00", "2004-12-9 16:10:00");
        checkCorrect(spec, "2004-12-9 16:10:00", "2004-12-9 16:15:00");
        checkCorrect(spec, "2004-12-9 16:14:59", "2004-12-9 16:15:00");
        checkCorrect(spec, "2004-12-9 16:15:00", "2004-12-9 17:05:00");

        // Try next "0 and 30 and 59 minutes past the hour"
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MINUTES, 0);
        spec.addValue(ScheduleUnit.MINUTES, 30);
        spec.addValue(ScheduleUnit.MINUTES, 59);

        checkCorrect(spec, "2004-12-9 15:45:01", "2004-12-9 15:59:00");
        checkCorrect(spec, "2004-12-9 15:59:01", "2004-12-9 16:00:00");
        checkCorrect(spec, "2004-12-9 16:04:59", "2004-12-9 16:30:00");
        checkCorrect(spec, "2004-12-9 16:30:00", "2004-12-9 16:59:00");
        checkCorrect(spec, "2004-12-9 16:59:30", "2004-12-9 17:00:00");

        // Try minutes combined with seconds
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MINUTES, 0);
        spec.addValue(ScheduleUnit.MINUTES, 30);
        spec.addValue(ScheduleUnit.SECONDS, 0);
        spec.addValue(ScheduleUnit.SECONDS, 30);

        checkCorrect(spec, "2004-12-9 15:59:59", "2004-12-9 16:00:00");
        checkCorrect(spec, "2004-12-9 16:00:00", "2004-12-9 16:00:30");
        checkCorrect(spec, "2004-12-9 16:00:29", "2004-12-9 16:00:30");
        checkCorrect(spec, "2004-12-9 16:00:30", "2004-12-9 16:30:00");
        checkCorrect(spec, "2004-12-9 16:29:59", "2004-12-9 16:30:00");
        checkCorrect(spec, "2004-12-9 16:30:00", "2004-12-9 16:30:30");
        checkCorrect(spec, "2004-12-9 17:00:00", "2004-12-9 17:00:30");

        // Try hours combined with seconds
        spec = new ScheduleSpec();
        for (int i = 10; i <= 14; i++) {
            spec.addValue(ScheduleUnit.HOURS, i);
        }
        spec.addValue(ScheduleUnit.SECONDS, 15);

        checkCorrect(spec, "2004-12-9 15:59:59", "2004-12-10 10:00:15");
        checkCorrect(spec, "2004-12-10 10:00:15", "2004-12-10 10:01:15");
        checkCorrect(spec, "2004-12-10 10:01:15", "2004-12-10 10:02:15");
        checkCorrect(spec, "2004-12-10 14:01:15", "2004-12-10 14:02:15");
        checkCorrect(spec, "2004-12-10 14:59:15", "2004-12-11 10:00:15");

        // Try hours combined with minutes
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.HOURS, 9);
        spec.addValue(ScheduleUnit.MINUTES, 5);

        checkCorrect(spec, "2004-12-9 15:59:59", "2004-12-10 9:05:00");
        checkCorrect(spec, "2004-11-30 15:59:59", "2004-12-1 9:05:00");
        checkCorrect(spec, "2004-11-30 9:04:59", "2004-11-30 9:05:00");
        checkCorrect(spec, "2004-12-31 9:05:01", "2005-01-01 9:05:00");

        // Try day of month as the 31st
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 31);

        checkCorrect(spec, "2004-11-30 15:59:59", "2004-12-31 0:00:00");
        checkCorrect(spec, "2004-12-30 15:59:59", "2004-12-31 0:00:00");
        checkCorrect(spec, "2004-12-31 00:00:00", "2004-12-31 0:01:00");
        checkCorrect(spec, "2005-01-01 00:00:00", "2005-01-31 0:00:00");
        checkCorrect(spec, "2005-02-01 00:00:00", "2005-03-31 0:00:00");
        checkCorrect(spec, "2005-04-01 00:00:00", "2005-05-31 0:00:00");

        // Try day of month as the 29st, for february testing
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 29);

        checkCorrect(spec, "2004-11-30 15:59:59", "2004-12-29 0:00:00");
        checkCorrect(spec, "2004-12-29 00:00:00", "2004-12-29 0:01:00");
        checkCorrect(spec, "2004-12-29 00:01:00", "2004-12-29 0:02:00");
        checkCorrect(spec, "2004-02-28 15:59:59", "2004-02-29 0:00:00");
        checkCorrect(spec, "2003-02-28 15:59:59", "2003-03-29 0:00:00");
        checkCorrect(spec, "2005-02-27 15:59:59", "2005-03-29 0:00:00");

        // Try 4:00 every day
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.HOURS, 16);
        spec.addValue(ScheduleUnit.MINUTES, 0);

        checkCorrect(spec, "2004-10-01 15:59:59", "2004-10-01 16:00:00");
        checkCorrect(spec, "2004-10-01 00:00:00", "2004-10-01 16:00:00");
        checkCorrect(spec, "2004-09-30 16:00:00", "2004-10-01 16:00:00");
        checkCorrect(spec, "2004-12-30 16:00:00", "2004-12-31 16:00:00");
        checkCorrect(spec, "2004-12-31 16:00:00", "2005-01-01 16:00:00");

        // Try every weekday at 10 am - scrum time!
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.HOURS, 10);
        spec.addValue(ScheduleUnit.MINUTES, 0);
        for (int i = 1; i <= 5; i++) {
            spec.addValue(ScheduleUnit.DAYS_OF_WEEK, i);
        }

        checkCorrect(spec, "2004-12-05 09:50:59", "2004-12-06 10:00:00");
        checkCorrect(spec, "2004-12-06 09:59:59", "2004-12-06 10:00:00");
        checkCorrect(spec, "2004-12-07 09:50:00", "2004-12-07 10:00:00");
        checkCorrect(spec, "2004-12-08 09:00:00", "2004-12-08 10:00:00");
        checkCorrect(spec, "2004-12-09 08:00:00", "2004-12-09 10:00:00");
        checkCorrect(spec, "2004-12-10 09:50:50", "2004-12-10 10:00:00");
        checkCorrect(spec, "2004-12-11 00:00:00", "2004-12-13 10:00:00");
        checkCorrect(spec, "2004-12-12 09:00:50", "2004-12-13 10:00:00");
        checkCorrect(spec, "2004-12-13 09:50:50", "2004-12-13 10:00:00");
        checkCorrect(spec, "2004-12-13 10:00:00", "2004-12-14 10:00:00");
        checkCorrect(spec, "2004-12-13 10:00:01", "2004-12-14 10:00:00");

        // Every Monday and also on the 1st and 15th of each month, at midnight
        // (tests the or between DAYS_OF_MONTH and DAYS_OF_WEEK)
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 1);
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 15);
        spec.addValue(ScheduleUnit.HOURS, 0);
        spec.addValue(ScheduleUnit.MINUTES, 0);
        spec.addValue(ScheduleUnit.SECONDS, 0);
        spec.addValue(ScheduleUnit.DAYS_OF_WEEK, 1);

        checkCorrect(spec, "2004-12-05 09:50:59", "2004-12-06 00:00:00");
        checkCorrect(spec, "2004-12-06 00:00:00", "2004-12-13 00:00:00");
        checkCorrect(spec, "2004-12-07 00:80:00", "2004-12-13 00:00:00");
        checkCorrect(spec, "2004-12-12 23:00:00", "2004-12-13 00:00:00");
        checkCorrect(spec, "2004-12-13 23:00:00", "2004-12-15 00:00:00");
        checkCorrect(spec, "2004-12-14 23:00:00", "2004-12-15 00:00:00");
        checkCorrect(spec, "2004-12-15 23:00:00", "2004-12-20 00:00:00");
        checkCorrect(spec, "2004-12-18 23:00:00", "2004-12-20 00:00:00");
        checkCorrect(spec, "2004-12-20 00:01:00", "2004-12-27 00:00:00");
        checkCorrect(spec, "2004-12-27 00:01:00", "2005-01-01 00:00:00");
        checkCorrect(spec, "2005-01-01 00:01:00", "2005-01-03 00:00:00");
        checkCorrect(spec, "2005-01-03 00:01:00", "2005-01-10 00:00:00");
        checkCorrect(spec, "2005-01-10 00:01:00", "2005-01-15 00:00:00");
        checkCorrect(spec, "2005-01-15 00:01:00", "2005-01-17 00:00:00");
        checkCorrect(spec, "2005-01-17 00:01:00", "2005-01-24 00:00:00");
        checkCorrect(spec, "2005-01-24 00:01:00", "2005-01-31 00:00:00");
        checkCorrect(spec, "2005-01-31 00:01:00", "2005-02-01 00:00:00");

        // Every second month on every second weekday
        spec = new ScheduleSpec();
        for (int i = 1; i <= 12; i += 2) {
            spec.addValue(ScheduleUnit.MONTHS, i);
        }
        for (int i = 0; i <= 6; i += 2) // Adds Sunday, Tuesday, Thursday, Saturday
        {
            spec.addValue(ScheduleUnit.DAYS_OF_WEEK, i);
        }

        checkCorrect(spec, "2004-09-01 00:00:00", "2004-09-02 00:00:00");   // Sept 1 2004 is a Wednesday
        checkCorrect(spec, "2004-09-02 00:00:00", "2004-09-02 00:01:00");
        checkCorrect(spec, "2004-09-02 23:59:00", "2004-09-04 00:00:00");
        checkCorrect(spec, "2004-09-04 23:59:00", "2004-09-05 00:00:00");   // Sept 5 2004 is a Sunday
        checkCorrect(spec, "2004-09-05 23:57:00", "2004-09-05 23:58:00");
        checkCorrect(spec, "2004-09-05 23:58:00", "2004-09-05 23:59:00");
        checkCorrect(spec, "2004-09-05 23:59:00", "2004-09-07 00:00:00");
        checkCorrect(spec, "2004-09-30 23:58:00", "2004-09-30 23:59:00");   // Sept 30 in a Thursday
        checkCorrect(spec, "2004-09-30 23:59:00", "2004-11-02 00:00:00");

        // Every second month on every second weekday
        spec = new ScheduleSpec();
        for (int i = 1; i <= 12; i += 2) {
            spec.addValue(ScheduleUnit.MONTHS, i);
        }
        for (int i = 0; i <= 6; i += 2) // Adds Sunday, Tuesday, Thursday, Saturday
        {
            spec.addValue(ScheduleUnit.DAYS_OF_WEEK, i);
        }

        checkCorrect(spec, "2004-09-01 00:00:00", "2004-09-02 00:00:00");   // Sept 1 2004 is a Wednesday
        checkCorrect(spec, "2004-09-02 00:00:00", "2004-09-02 00:01:00");
        checkCorrect(spec, "2004-09-02 23:59:00", "2004-09-04 00:00:00");
        checkCorrect(spec, "2004-09-04 23:59:00", "2004-09-05 00:00:00");   // Sept 5 2004 is a Sunday
        checkCorrect(spec, "2004-09-05 23:57:00", "2004-09-05 23:58:00");
        checkCorrect(spec, "2004-09-05 23:58:00", "2004-09-05 23:59:00");
        checkCorrect(spec, "2004-09-05 23:59:00", "2004-09-07 00:00:00");

        // Every 5 seconds, between 9am and until 4pm, all weekdays except Saturday and Sunday
        spec = new ScheduleSpec();
        for (int i = 0; i <= 59; i += 5) {
            spec.addValue(ScheduleUnit.SECONDS, i);
        }
        for (int i = 1; i <= 5; i++) {
            spec.addValue(ScheduleUnit.DAYS_OF_WEEK, i);
        }
        for (int i = 9; i <= 15; i++) {
            spec.addValue(ScheduleUnit.HOURS, i);
        }

        checkCorrect(spec, "2004-12-12 20:00:00", "2004-12-13 09:00:00");  // Dec 12 2004 is a Sunday
        checkCorrect(spec, "2004-12-13 09:00:01", "2004-12-13 09:00:05");
        checkCorrect(spec, "2004-12-13 09:00:05", "2004-12-13 09:00:10");
        checkCorrect(spec, "2004-12-13 09:00:11", "2004-12-13 09:00:15");
        checkCorrect(spec, "2004-12-13 09:00:15", "2004-12-13 09:00:20");
        checkCorrect(spec, "2004-12-13 09:00:24", "2004-12-13 09:00:25");
        checkCorrect(spec, "2004-12-13 15:59:50", "2004-12-13 15:59:55");
        checkCorrect(spec, "2004-12-13 15:59:55", "2004-12-14 09:00:00");
        checkCorrect(spec, "2004-12-14 12:27:35", "2004-12-14 12:27:40");
        checkCorrect(spec, "2004-12-14 12:29:55", "2004-12-14 12:30:00");
        checkCorrect(spec, "2004-12-17 00:03:00", "2004-12-17 09:00:00");
        checkCorrect(spec, "2004-12-17 15:59:50", "2004-12-17 15:59:55");
        checkCorrect(spec, "2004-12-17 15:59:55", "2004-12-20 09:00:00");

        // Feb 14, 12pm
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MONTHS, 2);
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 14);
        spec.addValue(ScheduleUnit.HOURS, 12);
        spec.addValue(ScheduleUnit.MINUTES, 0);

        checkCorrect(spec, "2004-12-12 20:00:00", "2005-02-14 12:00:00");
        checkCorrect(spec, "2003-12-12 20:00:00", "2004-02-14 12:00:00");
        checkCorrect(spec, "2004-02-01 20:00:00", "2004-02-14 12:00:00");

        // Dec 31, 23pm and 50 seconds (countdown)
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.MONTHS, 12);
        spec.addValue(ScheduleUnit.DAYS_OF_MONTH, 31);
        spec.addValue(ScheduleUnit.HOURS, 23);
        spec.addValue(ScheduleUnit.MINUTES, 59);
        spec.addValue(ScheduleUnit.SECONDS, 50);

        checkCorrect(spec, "2004-12-12 20:00:00", "2004-12-31 23:59:50");
        checkCorrect(spec, "2004-12-31 23:59:55", "2005-12-31 23:59:50");

        // CST timezone 7:00:00am
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.HOURS, 7);
        spec.addValue(ScheduleUnit.MINUTES, 0);
        spec.addValue(ScheduleUnit.SECONDS, 0);
        spec.setOptionalTimeZone("CST");

        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-10:00", "2008-02-02T03:00:00.000GMT-10:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-9:00", "2008-02-02T04:00:00.000GMT-9:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-8:00", "2008-02-02T05:00:00.000GMT-8:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-7:00", "2008-02-02T06:00:00.000GMT-7:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-6:00", "2008-02-01T07:00:00.000GMT-6:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-5:00", "2008-02-01T08:00:00.000GMT-5:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-4:00", "2008-02-01T09:00:00.000GMT-4:00");

        // EST timezone 7am, any minute
        spec = new ScheduleSpec();
        spec.addValue(ScheduleUnit.HOURS, 7);
        spec.addValue(ScheduleUnit.SECONDS, 0);
        spec.setOptionalTimeZone("EST");

        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-7:00", "2008-02-02T05:00:00.000GMT-7:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-6:00", "2008-02-01T06:01:00.000GMT-6:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-5:00", "2008-02-01T07:00:00.000GMT-5:00");
        checkCorrectWZone(spec, "2008-02-01T06:00:00.000GMT-4:00", "2008-02-01T08:00:00.000GMT-4:00");
    }

    public void checkCorrect(ScheduleSpec spec, String now, String expected) throws Exception {
        Date nowDate = timeFormat.parse(now);
        Date expectedDate = timeFormat.parse(expected);

        long result = ScheduleComputeHelper.computeNextOccurance(spec, nowDate.getTime(), TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE);
        Date resultDate = new Date(result);

        if (!(resultDate.equals(expectedDate))) {
            log.debug(".checkCorrect Difference in result found, spec=" + spec);
            log.debug(".checkCorrect      now=" + timeFormat.format(nowDate) +
                    " long=" + nowDate.getTime());
            log.debug(".checkCorrect expected=" + timeFormat.format(expectedDate) +
                    " long=" + expectedDate.getTime());
            log.debug(".checkCorrect   result=" + timeFormat.format(resultDate) +
                    " long=" + resultDate.getTime());
            assertTrue(false);
        }
    }

    public void checkCorrectWZone(ScheduleSpec spec, String nowWZone, String expectedWZone) throws Exception {
        long nowDate = DateTime.parseDefaultMSecWZone(nowWZone);
        long expectedDate = DateTime.parseDefaultMSecWZone(expectedWZone);

        long result = ScheduleComputeHelper.computeNextOccurance(spec, nowDate, TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE);
        Date resultDate = new Date(result);

        if (result != expectedDate) {
            log.debug(".checkCorrect Difference in result found, spec=" + spec);
            log.debug(".checkCorrect      now=" + timeFormat.format(nowDate) +
                    " long=" + nowDate);
            log.debug(".checkCorrect expected=" + timeFormat.format(expectedDate) +
                    " long=" + expectedDate);
            log.debug(".checkCorrect   result=" + timeFormat.format(resultDate) +
                    " long=" + resultDate.getTime());
            assertTrue(false);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestScheduleComputeHelper.class);
}
