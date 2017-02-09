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
package com.espertech.esper.pattern.observer;

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.supportunit.timer.SupportDateTimeUtil;
import junit.framework.TestCase;

import java.util.TimeZone;

public class TestTimerScheduleISO8601Parser extends TestCase {

    public void testParseDateFormats() throws Exception {
        // with timezone, without msec
        assertTimeParse("1997-07-16T19:20:30+01:00", 1997, 6, 16, 19, 20, 30, 0, "GMT+01:00");

        // with timezone, with msec
        assertTimeParse("1997-07-16T19:20:30.12+01:00", 1997, 6, 16, 19, 20, 30, 120, "GMT+01:00");
        assertTimeParse("1997-07-16T19:20:30.12+04:30", 1997, 6, 16, 19, 20, 30, 120, "GMT+04:30");

        // with timezone UTC, without msec
        assertTimeParse("1997-07-16T19:20:30Z", 1997, 6, 16, 19, 20, 30, 0, "GMT+00:00");

        // with timezone UTC, with msec
        assertTimeParse("1997-07-16T19:20:30.12Z", 1997, 6, 16, 19, 20, 30, 120, "GMT+00:00");
        assertTimeParse("1997-07-16T19:20:30.1Z", 1997, 6, 16, 19, 20, 30, 100, "GMT+00:00");
        assertTimeParse("1997-07-16T19:20:30.123Z", 1997, 6, 16, 19, 20, 30, 123, "GMT+00:00");

        // local timezone, with and without msec
        assertTimeParse("1997-07-16T19:20:30.123", 1997, 6, 16, 19, 20, 30, 123, TimeZone.getDefault().getDisplayName());
        assertTimeParse("1997-07-16T19:20:30", 1997, 6, 16, 19, 20, 30, 0, TimeZone.getDefault().getDisplayName());
    }

    public void testInvalid() {
        // date-only tests
        tryInvalid("5",
                "Failed to parse '5': Exception parsing date '5', the date is not a supported ISO 8601 date");
        tryInvalid(null,
                "Received a null value");
        tryInvalid("",
                "Received an empty string");
        tryInvalid("/",
                "Failed to parse '/': Invalid number of parts");

        // period-only tests
        tryInvalidPeriod("P");
        tryInvalidPeriod("P1");
        tryInvalidPeriod("P1D1D");
        tryInvalidPeriod("PT1D");
        tryInvalidPeriod("PD");
        tryInvalidPeriod("P0.1D");
        tryInvalidPeriod("P-10D");
        tryInvalidPeriod("P0D");

        // "date/period" tests
        tryInvalid("1997-07-16T19:20:30.12Z/x",
                "Failed to parse '1997-07-16T19:20:30.12Z/x': Invalid period 'x'");
        tryInvalid("1997-07-16T19:20:30.12Z/PT1D",
                "Failed to parse '1997-07-16T19:20:30.12Z/PT1D': Invalid period 'PT1D'");
        tryInvalid("dum-07-16T19:20:30.12Z/P1D",
                "Failed to parse 'dum-07-16T19:20:30.12Z/P1D': Exception parsing date 'dum-07-16T19:20:30.12Z', the date is not a supported ISO 8601 date");
        tryInvalid("/P1D",
                "Failed to parse '/P1D': Expected either a recurrence or a date but received an empty string");
        tryInvalid("1997-07-16T19:20:30.12Z/",
                "Failed to parse '1997-07-16T19:20:30.12Z/': Missing the period part");

        // "recurrence/period" tests
        tryInvalid("Ra/P1D",
                "Failed to parse 'Ra/P1D': Invalid repeat 'Ra', expecting an long-typed value but received 'a'");
        tryInvalid("R0.1/P1D",
                "Failed to parse 'R0.1/P1D': Invalid repeat 'R0.1', expecting an long-typed value but received '0.1'");
        tryInvalid("R100000000000000000000000000000/P1D",
                "Failed to parse 'R100000000000000000000000000000/P1D': Invalid repeat 'R100000000000000000000000000000', expecting an long-typed value but received '100000000000000000000000000000'");

        // "recurrence/date/period" tests
        tryInvalid("R/dummy/PT1M",
                "Failed to parse 'R/dummy/PT1M': Exception parsing date 'dummy', the date is not a supported ISO 8601 date");
        tryInvalid("Rx/1997-07-16T19:20:30.12Z/PT1M",
                "Failed to parse 'Rx/1997-07-16T19:20:30.12Z/PT1M': Invalid repeat 'Rx', expecting an long-typed value but received 'x'");
        tryInvalid("R1/1997-07-16T19:20:30.12Z/PT1D",
                "Failed to parse 'R1/1997-07-16T19:20:30.12Z/PT1D': Invalid period 'PT1D'");
    }

    public void testParse() throws Exception {
        assertParse("R3/2012-10-01T05:52:00Z/PT2S", 3L, "2012-10-01T05:52:00.000GMT-0:00", new TimePeriod().sec(2));
        assertParse("2012-10-01T05:52:00Z", null, "2012-10-01T05:52:00.000GMT-0:00", null);
        assertParse("R3/PT2S", 3L, null, new TimePeriod().sec(2));

        assertParseRepeat("R", -1);
        assertParseRepeat("R0", 0);
        assertParseRepeat("R1", 1);
        assertParseRepeat("R10", 10);
        assertParseRepeat("R365", 365);
        assertParseRepeat("R10000000000000", 10000000000000L);

        assertParsePeriod("1Y", new TimePeriod().years(1));
        assertParsePeriod("5M", new TimePeriod().months(5));
        assertParsePeriod("6W", new TimePeriod().weeks(6));
        assertParsePeriod("10D", new TimePeriod().days(10));

        assertParsePeriod("T3H", new TimePeriod().hours(3));
        assertParsePeriod("T4M", new TimePeriod().min(4));
        assertParsePeriod("T5S", new TimePeriod().sec(5));
        assertParsePeriod("T2S", new TimePeriod().sec(2));
        assertParsePeriod("T1S", new TimePeriod().sec(1));
        assertParsePeriod("T10S", new TimePeriod().sec(10));

        assertParsePeriod("1YT30M", new TimePeriod().years(1).min(30));
        assertParsePeriod("1Y2M10DT2H30M", new TimePeriod().years(1).months(2).days(10).hours(2).min(30));
        assertParsePeriod("T10H20S", new TimePeriod().hours(10).sec(20));
        assertParsePeriod("100Y2000M801W100DT29800H3000M304394S", new TimePeriod().years(100).months(2000).weeks(801).days(100).hours(29800).min(3000).sec(304394));
    }

    private void assertParsePeriod(String period, TimePeriod expected) throws Exception {
        assertParse("R/2012-10-01T05:52:00Z/P" + period, -1L, "2012-10-01T05:52:00.000GMT-0:00", expected);
    }

    private void assertParseRepeat(String repeat, long expected) throws Exception {
        assertParse(repeat + "/2012-10-01T05:52:00Z/PT2S", expected, "2012-10-01T05:52:00.000GMT-0:00", new TimePeriod().sec(2));
    }

    private void assertParse(String text, Long expectedNumRepeats, String expectedDate, TimePeriod expectedTimePeriod) throws Exception {
        TimerScheduleSpec spec = TimerScheduleISO8601Parser.parse(text);
        assertEquals(expectedNumRepeats, (Object) spec.getOptionalRepeatCount());
        if (expectedTimePeriod == null) {
            assertNull(spec.getOptionalTimePeriod());
        } else {
            assertEquals("expected '" + expectedTimePeriod.toStringISO8601() + "' got '" + spec.getOptionalTimePeriod().toStringISO8601() + "'", expectedTimePeriod, spec.getOptionalTimePeriod());
        }
        if (expectedDate == null) {
            assertNull(spec.getOptionalDate());
        } else {
            assertEquals(DateTime.parseDefaultMSecWZone(expectedDate), spec.getOptionalDate().getTimeInMillis());
        }
    }

    private void assertTimeParse(String date, int year, int month, int day, int hour, int minute, int second, int millis, String zone) throws Exception {
        TimerScheduleSpec spec = TimerScheduleISO8601Parser.parse(date);
        SupportDateTimeUtil.compareDate(spec.getOptionalDate(), year, month, day, hour, minute, second, millis);
        assertEquals(zone, spec.getOptionalDate().getTimeZone().getDisplayName());
    }

    private void tryInvalidPeriod(String period) {
        tryInvalid(period,
                "Failed to parse '" + period + "': Invalid period '" + period + "'");
    }

    private void tryInvalid(String iso8601, String message) {
        try {
            TimerScheduleISO8601Parser.parse(iso8601);
            fail();
        } catch (ScheduleParameterException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}
