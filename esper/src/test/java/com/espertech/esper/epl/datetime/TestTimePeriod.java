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

import com.espertech.esper.client.util.TimePeriod;
import junit.framework.TestCase;

public class TestTimePeriod extends TestCase {

    public void testLargestAbsoluteValue() {
        assertEquals(1, (int) new TimePeriod().years(1).largestAbsoluteValue());
        assertEquals(2, (int) new TimePeriod().months(2).largestAbsoluteValue());
        assertEquals(3, (int) new TimePeriod().days(3).largestAbsoluteValue());
        assertEquals(4, (int) new TimePeriod().weeks(4).largestAbsoluteValue());
        assertEquals(5, (int) new TimePeriod().hours(5).largestAbsoluteValue());
        assertEquals(6, (int) new TimePeriod().min(6).largestAbsoluteValue());
        assertEquals(7, (int) new TimePeriod().sec(7).largestAbsoluteValue());
        assertEquals(8, (int) new TimePeriod().millis(8).largestAbsoluteValue());
        assertEquals(9, (int) new TimePeriod().micros(9).largestAbsoluteValue());
        assertEquals(10, (int) new TimePeriod().millis(9).sec(10).hours(3).largestAbsoluteValue());
        assertEquals(10, (int) new TimePeriod().micros(1).millis(9).sec(10).hours(3).largestAbsoluteValue());
        assertEquals(1, (int) new TimePeriod().years(1).months(1).weeks(1).days(1).hours(1).min(1).sec(1).millis(1).micros(1).largestAbsoluteValue());
    }

    public void testToStringISO8601() {
        assertEquals("T10M", new TimePeriod().min(10).toStringISO8601());
        assertEquals("9DT10M", new TimePeriod().min(10).days(9).toStringISO8601());
        assertEquals("4Y", new TimePeriod().years(4).toStringISO8601());
        assertEquals("1Y1M1W1DT1H1M1S", new TimePeriod().years(1).months(1).weeks(1).days(1).hours(1).min(1).sec(1).millis(1).toStringISO8601());
    }
}
