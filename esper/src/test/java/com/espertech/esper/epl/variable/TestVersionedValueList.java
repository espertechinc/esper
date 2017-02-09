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
package com.espertech.esper.epl.variable;

import junit.framework.TestCase;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestVersionedValueList extends TestCase {
    private VersionedValueList<String> list;

    public void setUp() {
        list = new VersionedValueList<String>("abc", 2, "a", 1000, 10000, new ReentrantReadWriteLock().readLock(), 10, true);
    }

    public void testFlowNoTime() {
        tryInvalid(0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("a", list.getVersion(3));

        list.addValue(4, "b", 0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("a", list.getVersion(3));
        assertEquals("b", list.getVersion(4));
        assertEquals("b", list.getVersion(5));

        list.addValue(6, "c", 0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("a", list.getVersion(3));
        assertEquals("b", list.getVersion(4));
        assertEquals("b", list.getVersion(5));
        assertEquals("c", list.getVersion(6));
        assertEquals("c", list.getVersion(7));

        list.addValue(7, "d", 0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("a", list.getVersion(3));
        assertEquals("b", list.getVersion(4));
        assertEquals("b", list.getVersion(5));
        assertEquals("c", list.getVersion(6));
        assertEquals("d", list.getVersion(7));
        assertEquals("d", list.getVersion(8));

        list.addValue(9, "e", 0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("a", list.getVersion(3));
        assertEquals("b", list.getVersion(4));
        assertEquals("b", list.getVersion(5));
        assertEquals("c", list.getVersion(6));
        assertEquals("d", list.getVersion(7));
        assertEquals("d", list.getVersion(8));
        assertEquals("e", list.getVersion(9));
        assertEquals("e", list.getVersion(10));
    }

    public void testHighWatermark() {
        list.addValue(3, "b", 3000);
        list.addValue(4, "c", 4000);
        list.addValue(5, "d", 5000);
        list.addValue(6, "e", 6000);
        list.addValue(7, "f", 7000);
        list.addValue(8, "g", 8000);
        list.addValue(9, "h", 9000);
        list.addValue(10, "i", 10000);
        list.addValue(11, "j", 10500);
        list.addValue(12, "k", 10600);
        assertEquals(9, list.getOlderVersions().size());

        tryInvalid(0);
        tryInvalid(1);
        assertEquals("a", list.getVersion(2));
        assertEquals("b", list.getVersion(3));
        assertEquals("c", list.getVersion(4));
        assertEquals("d", list.getVersion(5));
        assertEquals("e", list.getVersion(6));
        assertEquals("f", list.getVersion(7));
        assertEquals("g", list.getVersion(8));
        assertEquals("k", list.getVersion(12));
        assertEquals("k", list.getVersion(13));

        list.addValue(15, "x", 11000);  // 11th value added
        assertEquals(9, list.getOlderVersions().size());

        tryInvalid(0);
        tryInvalid(1);
        tryInvalid(2);
        assertEquals("b", list.getVersion(3));
        assertEquals("c", list.getVersion(4));
        assertEquals("d", list.getVersion(5));
        assertEquals("k", list.getVersion(13));
        assertEquals("k", list.getVersion(14));
        assertEquals("x", list.getVersion(15));

        // expire all before 5.5 sec
        list.addValue(20, "y", 15500);  // 11th value added
        assertEquals(7, list.getOlderVersions().size());

        tryInvalid(0);
        tryInvalid(1);
        tryInvalid(2);
        tryInvalid(3);
        tryInvalid(4);
        tryInvalid(5);
        assertEquals("e", list.getVersion(6));
        assertEquals("k", list.getVersion(13));
        assertEquals("x", list.getVersion(15));
        assertEquals("x", list.getVersion(16));
        assertEquals("y", list.getVersion(20));

        // expire all before 10.5 sec
        list.addValue(21, "z1", 20500);
        list.addValue(22, "z2", 20500);
        list.addValue(23, "z3", 20501);
        assertEquals(4, list.getOlderVersions().size());
        tryInvalid(9);
        tryInvalid(10);
        tryInvalid(11);
        assertEquals("k", list.getVersion(12));
        assertEquals("k", list.getVersion(13));
        assertEquals("k", list.getVersion(14));
        assertEquals("x", list.getVersion(15));
        assertEquals("x", list.getVersion(16));
        assertEquals("y", list.getVersion(20));
        assertEquals("z1", list.getVersion(21));
        assertEquals("z2", list.getVersion(22));
        assertEquals("z3", list.getVersion(23));
        assertEquals("z3", list.getVersion(24));
    }

    private void tryInvalid(int version) {
        try {
            list.getVersion(version);
            fail();
        } catch (IllegalStateException ex) {
        }
    }
}
