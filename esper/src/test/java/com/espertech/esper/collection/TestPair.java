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

public class TestPair extends TestCase {
    private Pair<String, String> pair1 = new Pair<String, String>("a", "b");
    private Pair<String, String> pair2 = new Pair<String, String>("a", "b");
    private Pair<String, String> pair3 = new Pair<String, String>("a", null);
    private Pair<String, String> pair4 = new Pair<String, String>(null, "b");
    private Pair<String, String> pair5 = new Pair<String, String>(null, null);

    public void testHashCode() {
        assertTrue(pair1.hashCode() == ("a".hashCode() ^ "b".hashCode()));
        assertTrue(pair3.hashCode() == "a".hashCode());
        assertTrue(pair4.hashCode() == "b".hashCode());
        assertTrue(pair5.hashCode() == 0);

        assertTrue(pair1.hashCode() == pair2.hashCode());
        assertTrue(pair1.hashCode() != pair3.hashCode());
        assertTrue(pair1.hashCode() != pair4.hashCode());
        assertTrue(pair1.hashCode() != pair5.hashCode());
    }

    public void testEquals() {
        assertEquals(pair2, pair1);
        assertEquals(pair1, pair2);

        assertTrue(pair1 != pair3);
        assertTrue(pair3 != pair1);
        assertTrue(pair1 != pair4);
        assertTrue(pair2 != pair5);
        assertTrue(pair3 != pair4);
        assertTrue(pair4 != pair5);

        assertTrue(pair1 == pair1);
        assertTrue(pair2 == pair2);
        assertTrue(pair3 == pair3);
        assertTrue(pair4 == pair4);
        assertTrue(pair5 == pair5);
    }
}
