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
package com.espertech.esper.epl.datetime;

import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.epl.datetime.calop.CalendarOpPlusFastAddHelper;
import com.espertech.esper.epl.datetime.calop.CalendarOpPlusFastAddResult;
import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;
import junit.framework.TestCase;

import java.util.Calendar;

public class TestCalendarOpPlusFastAddHelper extends TestCase {
    public void testCompute() {
        long defaultCurrent = DateTime.parseDefaultMSec("2002-05-30T09:51:01.150");

        // millisecond adds
        TimePeriod oneMsec = new TimePeriod().millis(1);
        assertCompute(defaultCurrent, oneMsec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60 * 60 * 1000L), "2002-05-30T09:51:01.151");
        assertCompute(defaultCurrent, oneMsec, "2001-06-01T00:00:00.000",
                new LongAssertionAtLeast(363 * 24 * 60 * 60 * 1000L), "2002-05-30T09:51:01.151");

        // 10-millisecond adds
        TimePeriod tenMsec = new TimePeriod().millis(10);
        assertCompute(defaultCurrent, tenMsec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60 * 60 * 100L), "2002-05-30T09:51:01.160");

        // 100-millisecond adds
        TimePeriod hundredMsec = new TimePeriod().millis(100);
        assertCompute(defaultCurrent, hundredMsec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60 * 60 * 10L), "2002-05-30T09:51:01.200");

        // 1-hour-in-millisecond adds
        TimePeriod oneHourInMsec = new TimePeriod().millis(60 * 60 * 1000);
        assertCompute(defaultCurrent, oneHourInMsec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24), "2002-05-30T10:00:00.000");

        // second adds
        TimePeriod oneSec = new TimePeriod().sec(1);
        assertCompute(defaultCurrent, oneSec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60 * 60L), "2002-05-30T09:51:02.000");
        assertCompute(defaultCurrent, oneSec, "2002-05-30T09:51:00.150",
                new LongAssertionAtLeast(2), "2002-05-30T09:51:02.150");
        assertCompute(defaultCurrent, oneSec, "2002-05-30T09:51:00.151",
                new LongAssertionAtLeast(1), "2002-05-30T09:51:01.151");
        assertCompute(defaultCurrent, oneSec, "2002-05-30T09:51:01.149",
                new LongAssertionAtLeast(1), "2002-05-30T09:51:02.149");
        assertCompute(defaultCurrent, oneSec, "2002-05-30T09:51:01.150",
                new LongAssertionAtLeast(1), "2002-05-30T09:51:02.150");
        assertCompute(defaultCurrent, oneSec, "2002-05-30T09:51:01.151",
                new LongAssertionAtLeast(0), "2002-05-30T09:51:01.151");

        // 10-second adds
        TimePeriod tenSec = new TimePeriod().sec(10);
        assertCompute(defaultCurrent, tenSec, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60 * 6L), "2002-05-30T09:51:10.000");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:50:00.000",
                new LongAssertionExact(7L), "2002-05-30T09:51:10.000");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:50:51.149",
                new LongAssertionExact(2L), "2002-05-30T09:51:11.149");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:50:51.150",
                new LongAssertionExact(2L), "2002-05-30T09:51:11.150");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:50:51.151",
                new LongAssertionExact(1L), "2002-05-30T09:51:01.151");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:51:00.149",
                new LongAssertionExact(1L), "2002-05-30T09:51:10.149");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:51:01.149",
                new LongAssertionExact(1L), "2002-05-30T09:51:11.149");
        assertCompute(defaultCurrent, tenSec, "2002-05-30T09:51:01.150",
                new LongAssertionExact(1L), "2002-05-30T09:51:11.150");

        // minute adds
        TimePeriod oneMin = new TimePeriod().min(1);
        assertCompute(defaultCurrent, oneMin, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 60), "2002-05-30T09:52:00.000");

        // 10-minute adds
        TimePeriod tenMin = new TimePeriod().min(10);
        assertCompute(defaultCurrent, tenMin, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24 * 6), "2002-05-30T10:00:00.000");

        // 1-hour adds
        TimePeriod oneHour = new TimePeriod().hours(1);
        assertCompute(defaultCurrent, oneHour, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365 * 24), "2002-05-30T10:00:00.000");

        // 1-day adds
        TimePeriod oneDay = new TimePeriod().days(1);
        assertCompute(defaultCurrent, oneDay, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 365), "2002-05-31T00:00:00.000");

        // 1-month adds
        TimePeriod oneMonth = new TimePeriod().months(1);
        assertCompute(defaultCurrent, oneMonth, "1980-01-01T00:00:00.000",
                new LongAssertionAtLeast(22 * 12), "2002-06-01T00:00:00.000");

        // 1-year adds
        TimePeriod oneYear = new TimePeriod().years(1);
        assertCompute(defaultCurrent, oneYear, "1980-01-01T00:00:00.000",
                new LongAssertionExact(23), "2003-01-01T00:00:00.000");

        // Uneven adds
        TimePeriod unevenOne = new TimePeriod().years(1).months(2).days(3);
        assertCompute(defaultCurrent, unevenOne, "1980-01-01T00:00:00.000",
                new LongAssertionExact(20), "2003-06-30T00:00:00.000");
        assertCompute(defaultCurrent, unevenOne, "2002-01-01T00:00:00.000",
                new LongAssertionExact(1), "2003-03-04T00:00:00.000");
        assertCompute(defaultCurrent, unevenOne, "2001-01-01T00:00:00.000",
                new LongAssertionExact(2), "2003-05-07T00:00:00.000");
    }

    private void assertCompute(long current, TimePeriod timePeriod, String reference,
                               LongAssertion factorAssertion, String expectedTarget) {

        Calendar referenceDate = DateTime.parseDefaultCal(reference);
        CalendarOpPlusFastAddResult result = CalendarOpPlusFastAddHelper.computeNextDue(current, timePeriod, referenceDate, TimeAbacusMilliseconds.INSTANCE, 0);
        assertEquals("\nExpected " + expectedTarget + "\n" +
                        "Received " + DateTime.print(result.getScheduled()) + "\n",
                DateTime.parseDefaultCal(expectedTarget), result.getScheduled());
        factorAssertion.assertLong(result.getFactor());
    }

    private interface LongAssertion {
        public void assertLong(long value);
    }

    private static class LongAssertionExact implements LongAssertion {
        private final long expected;

        private LongAssertionExact(long expected) {
            this.expected = expected;
        }

        public void assertLong(long value) {
            assertEquals(expected, value);
        }
    }

    private static class LongAssertionAtLeast implements LongAssertion {
        private final long atLeast;

        private LongAssertionAtLeast(long atLeast) {
            this.atLeast = atLeast;
        }

        public void assertLong(long value) {
            assertTrue(value >= atLeast);
        }
    }
}
