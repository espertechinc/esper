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

import junit.framework.TestCase;

public class TestRefCountedSetAtomicInteger extends TestCase {

    public void testFlow() {
        RefCountedSetAtomicInteger<String> set = new RefCountedSetAtomicInteger<String>();

        assertFalse(set.remove("K1"));
        assertTrue(set.isEmpty());

        assertTrue(set.add("K1"));
        assertFalse(set.isEmpty());
        assertTrue(set.remove("K1"));
        assertTrue(set.isEmpty());
        assertFalse(set.remove("K1"));

        assertTrue(set.add("K1"));
        assertFalse(set.isEmpty());
        assertFalse(set.add("K1"));
        assertFalse(set.remove("K1"));
        assertFalse(set.isEmpty());
        assertTrue(set.remove("K1"));
        assertFalse(set.remove("K1"));
        assertTrue(set.isEmpty());

        assertTrue(set.add("K1"));
        assertFalse(set.add("K1"));
        assertFalse(set.add("K1"));
        assertFalse(set.remove("K1"));
        assertFalse(set.remove("K1"));
        assertFalse(set.isEmpty());
        assertTrue(set.remove("K1"));
        assertFalse(set.remove("K1"));
        assertTrue(set.isEmpty());

        assertTrue(set.add("K1"));
        assertFalse(set.add("K1"));
        assertTrue(set.add("K2"));
        set.removeAll("K1");
        assertFalse(set.isEmpty());
        set.removeAll("K2");
        assertTrue(set.add("K1"));
        assertTrue(set.remove("K1"));
    }
}
