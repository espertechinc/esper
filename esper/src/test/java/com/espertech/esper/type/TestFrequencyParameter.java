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

public class TestFrequencyParameter extends TestCase {
    public void testInvalid() {
        try {
            new FrequencyParameter(0);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testIsWildcard() {
        FrequencyParameter freq = new FrequencyParameter(1);
        assertTrue(freq.isWildcard(1, 10));

        freq = new FrequencyParameter(2);
        assertFalse(freq.isWildcard(1, 20));
    }

    public void testGetValues() {
        FrequencyParameter freq = new FrequencyParameter(3);
        Set<Integer> result = freq.getValuesInRange(1, 8);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3, 6}, result);

        freq = new FrequencyParameter(4);
        result = freq.getValuesInRange(6, 16);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{8, 12, 16}, result);

        freq = new FrequencyParameter(4);
        result = freq.getValuesInRange(0, 14);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 4, 8, 12}, result);

        freq = new FrequencyParameter(1);
        result = freq.getValuesInRange(2, 5);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{2, 3, 4, 5}, result);
    }

    public void testContainsPoint() {
        FrequencyParameter freqThree = new FrequencyParameter(3);
        assertTrue(freqThree.containsPoint(0));
        assertTrue(freqThree.containsPoint(3));
        assertTrue(freqThree.containsPoint(6));
        assertFalse(freqThree.containsPoint(1));
        assertFalse(freqThree.containsPoint(2));
        assertFalse(freqThree.containsPoint(4));

        FrequencyParameter freqOne = new FrequencyParameter(1);
        assertTrue(freqOne.containsPoint(1));
        assertTrue(freqOne.containsPoint(2));
    }

    public void testFormat() {
        FrequencyParameter freqThree = new FrequencyParameter(3);
        assertEquals("*/3", freqThree.formatted());
    }
}
