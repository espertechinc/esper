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

import com.espertech.esper.type.ScheduleUnit;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestScheduleSpec extends TestCase {
    public void testValidate() {
        // Test all units missing
        EnumMap<ScheduleUnit, SortedSet<Integer>> unitValues = new EnumMap<ScheduleUnit, SortedSet<Integer>>(ScheduleUnit.class);
        assertInvalid(unitValues);

        // Test one unit missing
        unitValues = (new ScheduleSpec()).getUnitValues();
        unitValues.remove(ScheduleUnit.HOURS);
        assertInvalid(unitValues);

        // Test all units are wildcards
        unitValues = (new ScheduleSpec()).getUnitValues();
        new ScheduleSpec(unitValues, null, null, null);

        // Test invalid value in month
        SortedSet<Integer> values = new TreeSet<Integer>();
        values.add(0);
        unitValues.put(ScheduleUnit.MONTHS, values);
        assertInvalid(unitValues);

        // Test valid value in month
        values = new TreeSet<Integer>();
        values.add(1);
        values.add(5);
        unitValues.put(ScheduleUnit.MONTHS, values);
        new ScheduleSpec(unitValues, null, null, null);
    }

    public void testCompress() {
        EnumMap<ScheduleUnit, SortedSet<Integer>> unitValues = new EnumMap<ScheduleUnit, SortedSet<Integer>>(ScheduleUnit.class);
        unitValues = (new ScheduleSpec()).getUnitValues();

        // Populate Month with all valid values
        SortedSet<Integer> monthValues = new TreeSet<Integer>();
        for (int i = ScheduleUnit.MONTHS.min(); i <= ScheduleUnit.MONTHS.max(); i++) {
            monthValues.add(i);
        }
        unitValues.put(ScheduleUnit.MONTHS, monthValues);

        // Construct spec, test that month was replaced with wildcards
        ScheduleSpec spec = new ScheduleSpec(unitValues, null, null, null);
        assertTrue(spec.getUnitValues().get(ScheduleUnit.MONTHS) == null);
    }

    private void assertInvalid(EnumMap<ScheduleUnit, SortedSet<Integer>> unitValues) {
        try {
            new ScheduleSpec(unitValues, null, null, null);
            assertFalse(true);
        } catch (IllegalArgumentException ex) {
            log.debug(".assertInvalid Expected exception, msg=" + ex.getMessage());
            // Expected exception
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestScheduleSpec.class);
}
