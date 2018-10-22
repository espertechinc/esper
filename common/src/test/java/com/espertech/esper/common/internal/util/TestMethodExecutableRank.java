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
package com.espertech.esper.common.internal.util;

import junit.framework.TestCase;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestMethodExecutableRank extends TestCase {

    public void testRank() {
        assertEquals(-1, "a".compareTo("b"));
        assertEquals(1, "b".compareTo("a"));

        MethodExecutableRank r1 = new MethodExecutableRank(1, false);
        assertEquals(1, r1.compareTo(0, false));
        assertEquals(-1, r1.compareTo(2, false));
        assertEquals(0, r1.compareTo(1, false));
        assertEquals(-1, r1.compareTo(1, true));

        MethodExecutableRank r2 = new MethodExecutableRank(0, true);
        assertEquals(1, r2.compareTo(0, false));
        assertEquals(0, r2.compareTo(0, true));
        assertEquals(-1, r2.compareTo(1, false));
        assertEquals(-1, r2.compareTo(1, true));

        SortedSet<MethodExecutableRank> ranks = new TreeSet<>(new Comparator<MethodExecutableRank>() {
            public int compare(MethodExecutableRank o1, MethodExecutableRank o2) {
                return o1.compareTo(o2);
            }
        });
        ranks.add(new MethodExecutableRank(2, true));
        ranks.add(new MethodExecutableRank(1, false));
        ranks.add(new MethodExecutableRank(2, false));
        ranks.add(new MethodExecutableRank(0, true));
        ranks.add(new MethodExecutableRank(1, true));
        ranks.add(new MethodExecutableRank(0, false));

        Iterator<MethodExecutableRank> it = ranks.iterator();
        for (int i = 0; i < 6; i++) {
            MethodExecutableRank rank = it.next();
            assertEquals("failed for " + i, i / 2, rank.getConversionCount());
            assertEquals("failed for " + i, i % 2 == 1, rank.isVarargs());
        }
    }
}
