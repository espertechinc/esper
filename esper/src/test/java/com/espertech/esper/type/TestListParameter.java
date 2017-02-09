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

public class TestListParameter extends TestCase {
    private ListParameter listParam;

    public void setUp() {
        listParam = new ListParameter();
        listParam.add(new IntParameter(5));
        listParam.add(new FrequencyParameter(3));
    }

    public void testIsWildcard() {
        // Wildcard is expected to make only a best-guess effort, not be perfect
        assertTrue(listParam.isWildcard(5, 5));
        assertFalse(listParam.isWildcard(6, 10));
    }

    public void testGetValues() {
        Set<Integer> result = listParam.getValuesInRange(1, 8);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 5, 6}, result);
    }

    public void testContainsPoint() {
        assertTrue(listParam.containsPoint(0));
        assertFalse(listParam.containsPoint(1));
        assertFalse(listParam.containsPoint(2));
        assertTrue(listParam.containsPoint(3));
        assertFalse(listParam.containsPoint(4));
        assertTrue(listParam.containsPoint(5));
    }

    public void testFormat() {
        assertEquals("5, */3", listParam.formatted());
    }
}
