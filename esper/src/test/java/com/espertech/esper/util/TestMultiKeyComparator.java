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
package com.espertech.esper.util;

import com.espertech.esper.collection.MultiKeyUntyped;
import junit.framework.TestCase;

import java.util.Comparator;

public class TestMultiKeyComparator extends TestCase {
    Comparator<MultiKeyUntyped> comparator;
    MultiKeyUntyped firstValues;
    MultiKeyUntyped secondValues;

    public void testCompareSingleProperty() {
        comparator = new MultiKeyComparator(new boolean[]{false});

        firstValues = new MultiKeyUntyped(new Object[]{3d});
        secondValues = new MultiKeyUntyped(new Object[]{4d});
        assertTrue(comparator.compare(firstValues, secondValues) < 0);

        comparator = new MultiKeyComparator(new boolean[]{true});

        assertTrue(comparator.compare(firstValues, secondValues) > 0);
        assertTrue(comparator.compare(firstValues, firstValues) == 0);
    }

    public void testCompareTwoProperties() {
        comparator = new MultiKeyComparator(new boolean[]{false, false});

        firstValues = new MultiKeyUntyped(new Object[]{3d, 3L});
        secondValues = new MultiKeyUntyped(new Object[]{3d, 4L});
        assertTrue(comparator.compare(firstValues, secondValues) < 0);

        comparator = new MultiKeyComparator(new boolean[]{false, true});

        assertTrue(comparator.compare(firstValues, secondValues) > 0);
        assertTrue(comparator.compare(firstValues, firstValues) == 0);
    }

    public void testInvalid() {
        comparator = new MultiKeyComparator(new boolean[]{false, false});

        firstValues = new MultiKeyUntyped(new Object[]{3d});
        secondValues = new MultiKeyUntyped(new Object[]{3d, 4L});
        try {
            comparator.compare(firstValues, secondValues);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

        firstValues = new MultiKeyUntyped(new Object[]{3d});
        secondValues = new MultiKeyUntyped(new Object[]{3d});
        try {
            comparator.compare(firstValues, secondValues);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

    }
}
