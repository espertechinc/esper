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

public class TestRefCountedMap extends TestCase {
    private RefCountedMap<String, Integer> refMap;

    public void setUp() {
        refMap = new RefCountedMap<String, Integer>();
        refMap.put("a", 100);
    }

    public void testPut() {
        try {
            refMap.put("a", 10);
            TestCase.fail();
        } catch (IllegalStateException ex) {
            // Expected exception
        }

        try {
            refMap.put(null, 10);
            TestCase.fail();
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    public void testGet() {
        Integer val = refMap.get("b");
        assertNull(val);

        val = refMap.get("a");
        assertEquals(100, (int) val);
    }

    public void testReference() {
        refMap.reference("a");

        try {
            refMap.reference("b");
            TestCase.fail();
        } catch (IllegalStateException ex) {
            // Expected exception
        }
    }

    public void testDereference() {
        boolean isLast = refMap.dereference("a");
        assertTrue(isLast);

        refMap.put("b", 100);
        refMap.reference("b");
        assertFalse(refMap.dereference("b"));
        assertTrue(refMap.dereference("b"));

        try {
            refMap.dereference("b");
            TestCase.fail();
        } catch (IllegalStateException ex) {
            // Expected exception
        }
    }

    public void testFlow() {
        refMap.put("b", -1);
        refMap.reference("b");

        assertEquals(-1, (int) refMap.get("b"));
        assertFalse(refMap.dereference("b"));
        assertEquals(-1, (int) refMap.get("b"));
        assertTrue(refMap.dereference("b"));
        assertNull(refMap.get("b"));

        refMap.put("b", 2);
        refMap.reference("b");

        refMap.put("c", 3);
        refMap.reference("c");

        refMap.dereference("b");
        refMap.reference("b");

        assertEquals(2, (int) refMap.get("b"));
        assertFalse(refMap.dereference("b"));
        assertTrue(refMap.dereference("b"));
        assertNull(refMap.get("b"));

        assertEquals(3, (int) refMap.get("c"));
        assertFalse(refMap.dereference("c"));
        assertEquals(3, (int) refMap.get("c"));
        assertTrue(refMap.dereference("c"));
        assertNull(refMap.get("c"));
    }
}
