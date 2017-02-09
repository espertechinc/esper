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
package com.espertech.esper.collection;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

public class TestArrayBackedCollection extends TestCase {
    private ArrayBackedCollection<Integer> coll;

    public void setUp() {
        coll = new ArrayBackedCollection<Integer>(5);
    }

    public void testGet() {
        assertEquals(0, coll.size());
        assertEquals(5, coll.getArray().length);

        coll.add(5);
        EPAssertionUtil.assertEqualsExactOrder(coll.getArray(), new Object[]{5, null, null, null, null});
        coll.add(4);
        EPAssertionUtil.assertEqualsExactOrder(coll.getArray(), new Object[]{5, 4, null, null, null});
        assertEquals(2, coll.size());

        coll.add(1);
        coll.add(2);
        coll.add(3);
        EPAssertionUtil.assertEqualsExactOrder(coll.getArray(), new Object[]{5, 4, 1, 2, 3});
        assertEquals(5, coll.size());

        coll.add(10);
        EPAssertionUtil.assertEqualsExactOrder(coll.getArray(), new Object[]{5, 4, 1, 2, 3, 10, null, null, null, null});
        assertEquals(6, coll.size());

        coll.add(11);
        coll.add(12);
        coll.add(13);
        coll.add(14);
        coll.add(15);
        EPAssertionUtil.assertEqualsExactOrder(coll.getArray(), new Object[]{5, 4, 1, 2, 3, 10, 11, 12, 13, 14, 15,
                null, null, null, null, null, null, null, null, null});
        assertEquals(11, coll.size());

        coll.clear();
        assertEquals(0, coll.size());
    }
}
