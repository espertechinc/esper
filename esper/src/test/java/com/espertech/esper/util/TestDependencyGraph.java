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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

public class TestDependencyGraph extends TestCase {
    public void testGetRootNodes() {
        // 1 needs 3 and 4; 2 need 0
        DependencyGraph graph = new DependencyGraph(5, false);
        graph.addDependency(1, 4);
        graph.addDependency(1, 3);
        graph.addDependency(2, 0);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1, 2}, graph.getRootNodes());
        assertNull(graph.getFirstCircularDependency());

        // 2 need 0, 3, 4
        graph = new DependencyGraph(5, false);
        graph.addDependency(2, 0);
        graph.addDependency(2, 3);
        graph.addDependency(2, 4);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1, 2}, graph.getRootNodes());
        assertNull(graph.getFirstCircularDependency());

        // 2 need 0, 3, 4; 1 needs 2
        graph = new DependencyGraph(5, false);
        graph.addDependency(2, 0);
        graph.addDependency(2, 3);
        graph.addDependency(2, 4);
        graph.addDependency(1, 2);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1}, graph.getRootNodes());
        assertNull(graph.getFirstCircularDependency());

        // circular among 3 nodes
        graph = new DependencyGraph(3, false);
        graph.addDependency(1, 0);
        graph.addDependency(2, 1);
        graph.addDependency(0, 2);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, graph.getRootNodes());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{0, 2, 1}, graph.getFirstCircularDependency().toArray(new Integer[3]));

        // circular among 4 nodes
        graph = new DependencyGraph(4, false);
        graph.addDependency(1, 0);
        graph.addDependency(2, 0);
        graph.addDependency(0, 2);
        graph.addDependency(3, 1);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{3}, graph.getRootNodes());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{0, 2}, graph.getFirstCircularDependency().toArray(new Integer[2]));

        graph.addDependency(2, 3);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{}, graph.getRootNodes());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{0, 2}, graph.getFirstCircularDependency().toArray(new Integer[2]));

        // circular among 3 nodes
        graph = new DependencyGraph(3, false);
        graph.addDependency(1, 0);
        graph.addDependency(0, 1);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{2}, graph.getRootNodes());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{0, 1}, graph.getFirstCircularDependency().toArray(new Integer[2]));

        // circular among 6 nodes
        graph = new DependencyGraph(6, false);
        graph.addDependency(1, 0);
        graph.addDependency(0, 2);
        graph.addDependency(2, 3);
        graph.addDependency(2, 4);
        graph.addDependency(4, 0);
        EPAssertionUtil.assertEqualsAnyOrder(new int[]{1, 5}, graph.getRootNodes());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{0, 2, 4}, graph.getFirstCircularDependency().toArray(new Integer[3]));
    }
}
