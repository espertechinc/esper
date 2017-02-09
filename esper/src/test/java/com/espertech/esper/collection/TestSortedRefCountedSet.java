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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TestSortedRefCountedSet extends TestCase {
    private SortedRefCountedSet<String> refSet;
    private Random random = new Random();

    public void setUp() {
        refSet = new SortedRefCountedSet<String>();
    }

    public void testMaxMinValue() {
        refSet.add("a");
        refSet.add("b");
        assertEquals("ba", refSet.maxValue() + refSet.minValue());
        refSet.remove("a");
        assertEquals("bb", refSet.maxValue() + refSet.minValue());
        refSet.remove("b");
        assertNull(refSet.maxValue());
        assertNull(refSet.minValue());

        refSet.add("b");
        refSet.add("a");
        refSet.add("d");
        refSet.add("a");
        refSet.add("c");
        refSet.add("a");
        refSet.add("c");
        assertEquals("da", refSet.maxValue() + refSet.minValue());

        refSet.remove("d");
        assertEquals("ca", refSet.maxValue() + refSet.minValue());

        refSet.remove("a");
        assertEquals("ca", refSet.maxValue() + refSet.minValue());

        refSet.remove("a");
        assertEquals("ca", refSet.maxValue() + refSet.minValue());

        refSet.remove("c");
        assertEquals("ca", refSet.maxValue() + refSet.minValue());

        refSet.remove("c");
        assertEquals("ba", refSet.maxValue() + refSet.minValue());

        refSet.remove("a");
        assertEquals("bb", refSet.maxValue() + refSet.minValue());

        refSet.remove("b");
        assertNull(refSet.maxValue());
        assertNull(refSet.minValue());
    }

    public void testAdd() {
        refSet.add("a");
        refSet.add("b");
        refSet.add("a");
        refSet.add("c");
        refSet.add("a");

        assertEquals("c", refSet.maxValue());
        assertEquals("a", refSet.minValue());
    }

    public void testRemove() {
        refSet.add("a");
        refSet.remove("a");
        assertNull(refSet.maxValue());
        assertNull(refSet.minValue());

        refSet.add("a");
        refSet.add("a");
        assertEquals("aa", refSet.maxValue() + refSet.minValue());

        refSet.remove("a");
        assertEquals("aa", refSet.maxValue() + refSet.minValue());

        refSet.remove("a");
        assertNull(refSet.maxValue());
        assertNull(refSet.minValue());

        // nothing to remove
        refSet.remove("c");

        refSet.add("a");
        refSet.remove("a");
        refSet.remove("a");
    }

    private final static Logger log = LoggerFactory.getLogger(TestSortedRefCountedSet.class);
}
