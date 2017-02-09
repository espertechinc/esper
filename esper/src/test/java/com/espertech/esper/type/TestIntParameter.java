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

public class TestIntParameter extends TestCase {
    public void testIsWildcard() {
        IntParameter intParam = new IntParameter(5);
        assertTrue(intParam.isWildcard(5, 5));
        assertFalse(intParam.isWildcard(4, 5));
        assertFalse(intParam.isWildcard(5, 6));
        assertFalse(intParam.isWildcard(4, 6));
    }

    public void testGetValues() {
        IntParameter intParam = new IntParameter(3);
        Set<Integer> result = intParam.getValuesInRange(1, 8);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3}, result);

        result = intParam.getValuesInRange(1, 2);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, result);

        result = intParam.getValuesInRange(4, 10);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, result);

        result = intParam.getValuesInRange(1, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3}, result);

        result = intParam.getValuesInRange(3, 5);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3}, result);
    }

    public void testContainsPoint() {
        IntParameter intParam = new IntParameter(3);
        assertTrue(intParam.containsPoint(3));
        assertFalse(intParam.containsPoint(2));
    }

    public void testFormat() {
        IntParameter intParam = new IntParameter(3);
        assertEquals("3", intParam.formatted());
    }
}
