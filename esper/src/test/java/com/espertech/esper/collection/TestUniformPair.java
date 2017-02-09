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

public class TestUniformPair extends TestCase {
    private UniformPair<String> pair1 = new UniformPair<String>("a", "b");
    private UniformPair<String> pair2 = new UniformPair<String>("a", "b");
    private UniformPair<String> pair3 = new UniformPair<String>("a", null);
    private UniformPair<String> pair4 = new UniformPair<String>(null, "b");
    private UniformPair<String> pair5 = new UniformPair<String>(null, null);

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
