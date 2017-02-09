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
package com.espertech.esper.type;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.util.Set;

public class TestRangeParameter extends TestCase {
    public void testIsWildcard() {
        RangeParameter rangeParameter = new RangeParameter(10, 20);
        assertTrue(rangeParameter.isWildcard(10, 20));
        assertTrue(rangeParameter.isWildcard(11, 20));
        assertTrue(rangeParameter.isWildcard(10, 19));
        assertFalse(rangeParameter.isWildcard(9, 21));
        assertFalse(rangeParameter.isWildcard(10, 21));
        assertFalse(rangeParameter.isWildcard(9, 20));
        assertTrue(rangeParameter.isWildcard(11, 19));
    }

    public void testGetValues() {
        RangeParameter rangeParameter = new RangeParameter(0, 5);
        Set<Integer> values = rangeParameter.getValuesInRange(1, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1, 2, 3}, values);

        values = rangeParameter.getValuesInRange(-2, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 1, 2, 3}, values);

        values = rangeParameter.getValuesInRange(4, 6);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{4, 5}, values);

        values = rangeParameter.getValuesInRange(10, 20);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, values);

        values = rangeParameter.getValuesInRange(-7, -1);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, values);
    }

    public void testContainsPoint() {
        RangeParameter rangeParameter = new RangeParameter(10, 20);
        assertTrue(rangeParameter.containsPoint(10));
        assertTrue(rangeParameter.containsPoint(11));
        assertTrue(rangeParameter.containsPoint(20));
        assertFalse(rangeParameter.containsPoint(9));
        assertFalse(rangeParameter.containsPoint(21));
    }

    public void testFormat() {
        RangeParameter rangeParameter = new RangeParameter(10, 20);
        assertEquals("10-20", rangeParameter.formatted());
    }
}
