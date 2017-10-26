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

import com.espertech.esper.collection.HashableMultiKey;
import junit.framework.TestCase;

import java.util.Comparator;

public class TestHashableMultiKeyComparator extends TestCase {
    Comparator<HashableMultiKey> comparator;
    HashableMultiKey firstValues;
    HashableMultiKey secondValues;

    public void testCompareSingleProperty() {
        comparator = new HashableMultiKeyComparator(new boolean[]{false});

        firstValues = new HashableMultiKey(new Object[]{3d});
        secondValues = new HashableMultiKey(new Object[]{4d});
        assertTrue(comparator.compare(firstValues, secondValues) < 0);

        comparator = new HashableMultiKeyComparator(new boolean[]{true});

        assertTrue(comparator.compare(firstValues, secondValues) > 0);
        assertTrue(comparator.compare(firstValues, firstValues) == 0);
    }

    public void testCompareTwoProperties() {
        comparator = new HashableMultiKeyComparator(new boolean[]{false, false});

        firstValues = new HashableMultiKey(new Object[]{3d, 3L});
        secondValues = new HashableMultiKey(new Object[]{3d, 4L});
        assertTrue(comparator.compare(firstValues, secondValues) < 0);

        comparator = new HashableMultiKeyComparator(new boolean[]{false, true});

        assertTrue(comparator.compare(firstValues, secondValues) > 0);
        assertTrue(comparator.compare(firstValues, firstValues) == 0);
    }

    public void testInvalid() {
        comparator = new HashableMultiKeyComparator(new boolean[]{false, false});

        firstValues = new HashableMultiKey(new Object[]{3d});
        secondValues = new HashableMultiKey(new Object[]{3d, 4L});
        try {
            comparator.compare(firstValues, secondValues);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

        firstValues = new HashableMultiKey(new Object[]{3d});
        secondValues = new HashableMultiKey(new Object[]{3d});
        try {
            comparator.compare(firstValues, secondValues);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

    }
}
