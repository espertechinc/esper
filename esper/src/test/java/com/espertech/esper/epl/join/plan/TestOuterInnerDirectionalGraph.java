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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

public class TestOuterInnerDirectionalGraph extends TestCase {
    private OuterInnerDirectionalGraph graph;

    public void setUp() {
        graph = new OuterInnerDirectionalGraph(4);
    }

    public void testAdd() {
        graph.add(0, 1);

        // testing duplicate add
        tryInvalidAdd(0, 1);

        // test adding out-of-bounds stream
        tryInvalidAdd(0, 4);
        tryInvalidAdd(4, 0);
        tryInvalidAdd(4, 4);
        tryInvalidAdd(2, -1);
        tryInvalidAdd(-1, 2);
    }

    public void testIsInner() {
        graph.add(0, 1);
        assertTrue(graph.isInner(0, 1));
        assertFalse(graph.isInner(1, 0));
        assertFalse(graph.isInner(2, 0));
        assertFalse(graph.isInner(0, 2));

        graph.add(1, 0);
        assertTrue(graph.isInner(0, 1));
        assertTrue(graph.isInner(1, 0));

        graph.add(2, 0);
        assertTrue(graph.isInner(2, 0));
        assertFalse(graph.isInner(0, 2));

        tryInvalidIsInner(4, 0);
        tryInvalidIsInner(0, 4);
        tryInvalidIsInner(1, 1);
        tryInvalidIsInner(1, -1);
        tryInvalidIsInner(-1, 1);
    }

    public void testIsOuter() {
        graph.add(0, 1);
        assertTrue(graph.isOuter(0, 1));
        assertFalse(graph.isOuter(1, 0));
        assertFalse(graph.isOuter(0, 2));
        assertFalse(graph.isOuter(2, 0));

        graph.add(1, 0);
        assertTrue(graph.isOuter(1, 0));
        assertTrue(graph.isOuter(0, 1));

        graph.add(2, 0);
        assertTrue(graph.isOuter(2, 0));
        assertFalse(graph.isOuter(0, 2));

        tryInvalidIsInner(4, 0);
        tryInvalidIsInner(0, 4);
        tryInvalidIsInner(1, 1);
        tryInvalidIsInner(1, -1);
        tryInvalidIsInner(-1, 1);
    }

    public void testGetInner() {
        tryInvalidGetInner(4);
        tryInvalidGetInner(-1);

        assertNull(graph.getInner(0));

        graph.add(0, 1);
        assertNull(graph.getInner(1));
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1}, graph.getInner(0));
        graph.add(0, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1, 3}, graph.getInner(0));
        graph.add(1, 0);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0}, graph.getInner(1));
        graph.add(1, 2);
        graph.add(1, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 2, 3}, graph.getInner(1));
    }

    public void testGetOuter() {
        tryInvalidGetOuter(4);
        tryInvalidGetOuter(-1);

        assertNull(graph.getOuter(0));

        graph.add(0, 1);
        assertNull(graph.getOuter(0));
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0}, graph.getOuter(1));
        graph.add(0, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0}, graph.getOuter(3));
        graph.add(1, 0);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0}, graph.getOuter(1));
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1}, graph.getOuter(0));
        graph.add(1, 3);
        graph.add(2, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{0, 1, 2}, graph.getOuter(3));
    }

    private void tryInvalidGetOuter(int stream) {
        try {
            graph.getOuter(stream);
            fail();
        } catch (Exception ex) {
            // expected
        }
    }

    private void tryInvalidGetInner(int stream) {
        try {
            graph.getInner(stream);
            fail();
        } catch (Exception ex) {
            // expected
        }
    }

    private void tryInvalidIsInner(int inner, int outer) {
        try {
            graph.isInner(inner, outer);
            fail();
        } catch (Exception ex) {
            // expected
        }
    }

    private void tryInvalidIsOuter(int inner, int outer) {
        try {
            graph.isOuter(outer, inner);
            fail();
        } catch (Exception ex) {
            // expected
        }
    }

    private void tryInvalidAdd(int inner, int outer) {
        try {
            graph.add(inner, outer);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
